use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use std::ffi::{CStr, CString};
use std::os::raw::c_char;

#[derive(Debug, Deserialize)]
struct RagRequest {
    question: Option<String>,
    #[serde(rename = "chatId")]
    chat_id: Option<String>,
}

#[derive(Debug, Deserialize)]
struct IndexRequest {
    #[serde(rename = "sourceId")]
    source_id: Option<String>,
    uri: Option<String>,
}

#[derive(Debug, Serialize)]
struct IndexResponse {
    status: &'static str,
    source_id: String,
    uri: String,
}

#[no_mangle]
pub extern "C" fn chatly_rag_query(input: *const c_char) -> *mut c_char {
    ffi_boundary(input, |json| {
        let request: RagRequest = serde_json::from_str(json)?;
        let question = request.question.unwrap_or_default();
        let chat_id = request.chat_id.unwrap_or_default();
        let fingerprint = stable_fingerprint(&question);

        Ok(format!(
            "Native llamalib подключен. Вопрос: {question}\nChat ID: {chat_id}\nFingerprint: {fingerprint}\n\nСледующий шаг: заменить stub на llama-cpp-2 генерацию и LanceDB retrieval."
        ))
    })
}

#[no_mangle]
pub extern "C" fn chatly_index_document(input: *const c_char) -> *mut c_char {
    ffi_boundary(input, |json| {
        let request: IndexRequest = serde_json::from_str(json)?;
        let response = IndexResponse {
            status: "indexed",
            source_id: request.source_id.unwrap_or_default(),
            uri: request.uri.unwrap_or_default(),
        };
        Ok(serde_json::to_string(&response)?)
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

fn ffi_boundary<F>(input: *const c_char, handler: F) -> *mut c_char
where
    F: FnOnce(&str) -> anyhow::Result<String>,
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

fn stable_fingerprint(input: &str) -> String {
    let mut hasher = Sha256::new();
    hasher.update(input.as_bytes());
    let digest = hasher.finalize();
    format!("{:x}", digest)[..16].to_string()
}

fn escape_json(input: &str) -> String {
    input.replace('\\', "\\\\").replace('"', "\\\"")
}
