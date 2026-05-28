# Chatly Box

On-premise RAG platform layout:

- `frontend` - Next.js admin panel and chat UI.
- `backend` - Java 22 + Spring Boot API, Postgres/Liquibase, S3/local source handling, parsing, OCR orchestration, chunking.
- `llamalib` - Rust `cdylib` for expensive native work: GGUF embeddings, GGUF generation, LanceDB indexing/search.

## Local Development

Prerequisites:

- Docker Desktop for Postgres and MinIO.
- Java 22. The default script uses `C:\Users\asher\.jdks\corretto-22.0.2`.
- Rust/Cargo.
- Node.js 22.
- LLVM/libclang for local `llama-cpp-2` builds on Windows.
- `tesseract` in `PATH` if OCR is needed outside Docker.

Create model files:

```powershell
mkdir models
```

Put GGUF models here:

```text
models/embedding.gguf
models/chat.gguf
```

Start infrastructure:

```sh
sh run-local-infra.sh
```

This starts:

- Postgres: `localhost:5432`
- MinIO API: `localhost:9000`
- MinIO console: `http://localhost:9001`

Build Rust native library manually if needed:

```powershell
cd C:\projects\chatly-box\llamalib
cargo build --release
```

Expected output:

```text
C:\projects\chatly-box\llamalib\target\release\chatly_llamalib.dll
```

If Cargo fails with `Unable to find libclang`, install LLVM and set:

```powershell
$env:LIBCLANG_PATH="C:\Program Files\LLVM\bin"
```

Start backend:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-local-backend.ps1
```

The backend script sets Java 22, builds `llamalib` if the DLL is missing, and runs Spring Boot through Gradle.

Start frontend in another terminal:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-local-frontend.ps1
```

The frontend script installs npm packages if needed, runs Prisma schema push, seeds the admin user, and starts Next.js.

Open:

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`
- MinIO console: `http://localhost:9001`

Default login:

```text
admin@company.local
admin12345
```

Local defaults:

- Postgres: `postgresql://postgres:postgres@localhost:5432/chatly_box`
- LanceDB local path: `data/lancedb`
- Embedding model: `models/embedding.gguf`
- Chat model: `models/chat.gguf`
- Native library: `llamalib/target/release/chatly_llamalib.dll`

## Full Docker Start

When Docker Desktop is running:

```sh
docker compose up --build
```

The full compose starts Postgres, MinIO, backend, and frontend.

## Backend Interfaces

The backend exposes practical transport styles over the same application services:

- REST: `/api/rag`, `/api/search`, `/api/documents`, `/api/ingestion`
- Server-Sent Events / Reactor: `/api/search/stream`, `/api/ingestion/events`

The heavy native work remains in Rust (`llama-cpp-2` + LanceDB). Java owns source orchestration, parsing, OCR process orchestration, transactions, projections, and REST/SSE transport.

Removed by KISS/YAGNI:

- SOAP: no current legacy integration requirement.
- JSON-RPC: duplicated REST without adding value.
- gRPC: no separate internal service yet, so it was premature.

## Search Projection

Processed documents are stored in Postgres and indexed into Elasticsearch:

- Postgres is the source-of-truth metadata/read model.
- JPA `@NamedEntityGraph("document.withChunks")` is used for document detail reads.
- Elasticsearch is a full-text search projection for processed chunks.
- LanceDB stores vector data locally or in S3/MinIO.

Manual Elastic reindex:

```text
POST /api/search/reindex
```

Request examples live in `docs/api-examples.md`.

## LanceDB Storage

By default local development writes LanceDB data to:

```text
data/lancedb
```

For S3/MinIO-backed LanceDB, set backend environment variables in `docker-compose.yml`:

```yaml
LANCEDB_URI: s3://chatly-docs/lancedb
AWS_ENDPOINT_URL: http://minio:9000
AWS_ACCESS_KEY_ID: minioadmin
AWS_SECRET_ACCESS_KEY: minioadmin
AWS_REGION: us-east-1
```
