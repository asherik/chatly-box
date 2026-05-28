use anyhow::{anyhow, Context, Result};
use arrow_array::cast::AsArray;
use arrow_array::types::Float32Type;
use arrow_array::{FixedSizeListArray, RecordBatch, StringArray};
use arrow_schema::{DataType, Field, Schema};
use ezllama::{ContextParams as ChatContextParams, Model, ModelParams};
use futures::TryStreamExt;
use lancedb::query::{ExecutableQuery, QueryBase};
use lancedb::{connect, Table};
use llama_cpp_2::context::params::{LlamaContextParams, LlamaPoolingType};
use llama_cpp_2::llama_backend::LlamaBackend;
use llama_cpp_2::llama_batch::LlamaBatch;
use llama_cpp_2::model::params::LlamaModelParams;
use llama_cpp_2::model::{AddBos, LlamaModel};
use llama_cpp_2::{send_logs_to_tracing, LogOptions};
use serde::{Deserialize, Serialize};
use std::ffi::{CStr, CString};
use std::num::NonZeroU32;
use std::os::raw::c_char;
use std::path::PathBuf;
use std::sync::{Arc, OnceLock};
use tokio::runtime::Runtime;

const TABLE_NAME: &str = "document_chunks";

static RUNTIME: OnceLock<Runtime> = OnceLock::new();
static LLAMA_BACKEND: OnceLock<LlamaBackend> = OnceLock::new();

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct IndexRequest {
    db_path: String,
    embedding_model_path: String,
    document_id: Option<String>,
    title: String,
    uri: String,
    chunks: Vec<String>,
    ctx_size: Option<u32>,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct RagRequest {
    db_path: String,
    embedding_model_path: String,
    chat_model_path: String,
    question: String,
    top_k: Option<usize>,
    max_tokens: Option<usize>,
    temperature: Option<f32>,
    ctx_size: Option<usize>,
    system_prompt: Option<String>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct SourceHit {
    title: String,
    uri: String,
    content: String,
    distance: Option<f32>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct RagResponse {
    answer: String,
    sources: Vec<SourceHit>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct IndexResponse {
    status: &'static str,
    indexed_chunks: usize,
    uri: String,
}

#[no_mangle]
pub extern "C" fn chatly_rag_query(input: *const c_char) -> *mut c_char {
    ffi_boundary(input, |json| {
        let request: RagRequest = serde_json::from_str(json)?;
        runtime().block_on(rag_query(request))
    })
}

#[no_mangle]
pub extern "C" fn chatly_index_document(input: *const c_char) -> *mut c_char {
    ffi_boundary(input, |json| {
        let request: IndexRequest = serde_json::from_str(json)?;
        runtime().block_on(index_document(request))
    })
}

#[no_mangle]
pub extern "C" fn chatly_free_string(ptr: *mut c_char) {
    if ptr.is_null() {
        return;
    }
    unsafe {
        let _ = CString::from_raw(ptr);
    }
}

async fn index_document(request: IndexRequest) -> Result<String> {
    if request.chunks.is_empty() {
        return Err(anyhow!("index request must contain at least one chunk"));
    }

    let embedder = Embedder::load(&request.embedding_model_path, request.ctx_size)?;
    let db = connect(&request.db_path).execute().await?;
    let table = open_or_create_table(&db, embedder.dim()).await?;

    let mut ids = Vec::with_capacity(request.chunks.len());
    let mut uris = Vec::with_capacity(request.chunks.len());
    let mut titles = Vec::with_capacity(request.chunks.len());
    let mut contents = Vec::with_capacity(request.chunks.len());
    let mut vectors = Vec::with_capacity(request.chunks.len());
    let document_id = request.document_id.unwrap_or_else(|| request.uri.clone());

    for (index, chunk) in request.chunks.iter().enumerate() {
        ids.push(format!("{document_id}:{index}"));
        uris.push(request.uri.clone());
        titles.push(request.title.clone());
        contents.push(chunk.clone());
        vectors.push(embedder.embed(chunk)?);
    }

    table
        .add(record_batch(embedder.dim(), ids, uris, titles, contents, vectors)?)
        .execute()
        .await?;

    Ok(serde_json::to_string(&IndexResponse {
        status: "indexed",
        indexed_chunks: request.chunks.len(),
        uri: request.uri,
    })?)
}

async fn rag_query(request: RagRequest) -> Result<String> {
    let top_k = request.top_k.unwrap_or(6).max(1);
    let embedder = Embedder::load(&request.embedding_model_path, request.ctx_size.map(|v| v as u32))?;
    let question_embedding = embedder.embed(&request.question)?;

    let db = connect(&request.db_path).execute().await?;
    let table = db.open_table(TABLE_NAME).execute().await?;
    let sources = search_sources(&table, &question_embedding, top_k).await?;
    let answer = generate_answer(&request, &sources)?;

    Ok(serde_json::to_string(&RagResponse { answer, sources })?)
}

async fn open_or_create_table(db: &lancedb::connection::Connection, dim: usize) -> Result<Table> {
    let names = db.table_names().execute().await?;
    if names.iter().any(|name| name == TABLE_NAME) {
        return Ok(db.open_table(TABLE_NAME).execute().await?);
    }

    let schema = Arc::new(Schema::new(vec![
        Field::new("id", DataType::Utf8, false),
        Field::new("uri", DataType::Utf8, false),
        Field::new("title", DataType::Utf8, false),
        Field::new("content", DataType::Utf8, false),
        Field::new(
            "vector",
            DataType::FixedSizeList(Arc::new(Field::new("item", DataType::Float32, true)), dim as i32),
            false,
        ),
    ]));

    Ok(db.create_empty_table(TABLE_NAME, schema).execute().await?)
}

async fn search_sources(table: &Table, embedding: &[f32], top_k: usize) -> Result<Vec<SourceHit>> {
    let batches = table
        .query()
        .nearest_to(embedding)?
        .limit(top_k)
        .execute()
        .await?
        .try_collect::<Vec<_>>()
        .await?;

    let mut hits = Vec::new();
    for batch in batches {
        let titles = batch.column_by_name("title").context("missing title column")?.as_string::<i32>();
        let uris = batch.column_by_name("uri").context("missing uri column")?.as_string::<i32>();
        let contents = batch
            .column_by_name("content")
            .context("missing content column")?
            .as_string::<i32>();
        let distances = batch
            .column_by_name("_distance")
            .and_then(|column| column.as_primitive_opt::<Float32Type>());

        for row in 0..batch.num_rows() {
            hits.push(SourceHit {
                title: titles.value(row).to_string(),
                uri: uris.value(row).to_string(),
                content: contents.value(row).to_string(),
                distance: distances.map(|array| array.value(row)),
            });
        }
    }
    Ok(hits)
}

fn generate_answer(request: &RagRequest, sources: &[SourceHit]) -> Result<String> {
    let model = Model::new(&ModelParams {
        model_path: PathBuf::from(&request.chat_model_path),
        ..Default::default()
    })?;

    let context = sources
        .iter()
        .enumerate()
        .map(|(index, source)| {
            format!(
                "[{}] {}\nURI: {}\n{}",
                index + 1,
                source.title,
                source.uri,
                source.content
            )
        })
        .collect::<Vec<_>>()
        .join("\n\n");

    let system = request.system_prompt.clone().unwrap_or_else(|| {
        "Ты корпоративный RAG-ассистент. Отвечай только по контексту. Если ответа нет в документах, прямо скажи, что данных недостаточно. В конце укажи источники номерами.".to_string()
    });

    let prompt = format!(
        "Контекст:\n{}\n\nВопрос: {}\n\nОтвет:",
        if context.is_empty() { "Нет найденного контекста." } else { &context },
        request.question
    );

    let params = ChatContextParams {
        ctx_size: request.ctx_size.or(Some(4096)),
        ..Default::default()
    };
    let mut session = model.create_chat_session_with_system(&system, &params)?;
    let limit = request.max_tokens.unwrap_or(512);
    let answer = session.prompt(&prompt)?.take(limit).collect::<String>();
    Ok(answer)
}

struct Embedder {
    model: LlamaModel,
    ctx_size: u32,
}

impl Embedder {
    fn load(path: &str, ctx_size: Option<u32>) -> Result<Self> {
        let model = LlamaModel::load_from_file(
            backend(),
            PathBuf::from(path),
            &LlamaModelParams::default(),
        )
        .with_context(|| format!("failed to load embedding GGUF model: {path}"))?;
        Ok(Self {
            model,
            ctx_size: ctx_size.unwrap_or(512),
        })
    }

    fn dim(&self) -> usize {
        self.model.n_embd() as usize
    }

    fn embed(&self, text: &str) -> Result<Vec<f32>> {
        let n_ctx = NonZeroU32::new(self.ctx_size).context("ctx_size must be non-zero")?;
        let ctx_params = LlamaContextParams::default()
            .with_n_ctx(Some(n_ctx))
            .with_embeddings(true)
            .with_pooling_type(LlamaPoolingType::Mean);
        let mut context = self.model.new_context(backend(), ctx_params)?;
        let tokens = self.model.str_to_token(text, AddBos::Always)?;
        if tokens.is_empty() {
            return Err(anyhow!("cannot embed empty text"));
        }

        let mut batch = LlamaBatch::new(tokens.len(), 1);
        batch.add_sequence(&tokens, 0, false)?;
        context.decode(&mut batch)?;
        let embedding = context.embeddings_seq_ith(0)?.to_vec();
        Ok(normalize(embedding))
    }
}

fn record_batch(
    dim: usize,
    ids: Vec<String>,
    uris: Vec<String>,
    titles: Vec<String>,
    contents: Vec<String>,
    vectors: Vec<Vec<f32>>,
) -> Result<RecordBatch> {
    let schema = Arc::new(Schema::new(vec![
        Field::new("id", DataType::Utf8, false),
        Field::new("uri", DataType::Utf8, false),
        Field::new("title", DataType::Utf8, false),
        Field::new("content", DataType::Utf8, false),
        Field::new(
            "vector",
            DataType::FixedSizeList(Arc::new(Field::new("item", DataType::Float32, true)), dim as i32),
            false,
        ),
    ]));

    let vector_array = FixedSizeListArray::from_iter_primitive::<Float32Type, _, _>(
        vectors
            .into_iter()
            .map(|vector| Some(vector.into_iter().map(Some).collect::<Vec<_>>())),
        dim as i32,
    );

    Ok(RecordBatch::try_new(
        schema,
        vec![
            Arc::new(StringArray::from(ids)),
            Arc::new(StringArray::from(uris)),
            Arc::new(StringArray::from(titles)),
            Arc::new(StringArray::from(contents)),
            Arc::new(vector_array),
        ],
    )?)
}

fn normalize(mut vector: Vec<f32>) -> Vec<f32> {
    let norm = vector.iter().map(|value| value * value).sum::<f32>().sqrt();
    if norm > 0.0 {
        for value in &mut vector {
            *value /= norm;
        }
    }
    vector
}

fn runtime() -> &'static Runtime {
    RUNTIME.get_or_init(|| Runtime::new().expect("failed to create tokio runtime"))
}

fn backend() -> &'static LlamaBackend {
    LLAMA_BACKEND.get_or_init(|| {
        send_logs_to_tracing(LogOptions::default().with_logs_enabled(false));
        LlamaBackend::init().expect("failed to initialize llama backend")
    })
}

fn ffi_boundary<F>(input: *const c_char, handler: F) -> *mut c_char
where
    F: FnOnce(&str) -> Result<String>,
{
    if input.is_null() {
        return CString::new(r#"{"error":"null input"}"#).unwrap().into_raw();
    }

    let result = unsafe { CStr::from_ptr(input) }
        .to_str()
        .map_err(anyhow::Error::from)
        .and_then(handler)
        .unwrap_or_else(|error| format!(r#"{{"error":"{}"}}"#, escape_json(&error.to_string())));

    CString::new(result)
        .unwrap_or_else(|_| CString::new(r#"{"error":"invalid nul byte"}"#).unwrap())
        .into_raw()
}

fn escape_json(input: &str) -> String {
    input.replace('\\', "\\\\").replace('"', "\\\"")
}
