# Chatly Box

On-premise RAG MVP layout:

- `frontend` - Next.js admin panel and chat UI.
- `backend` - Java 22 + Spring Boot API with Postgres/Flyway and Project Panama native bridge.
- `llamalib` - Rust `cdylib` boundary for local embedding/chat/indexing engine.

The Rust library currently exposes a safe FFI stub. It is intentionally shaped for replacing the internals with `llama-cpp-2` and LanceDB without changing the Java boundary.

## One-command local start

```sh
sh run.sh
```

The script starts Postgres and MinIO from `docker-compose-local`, then builds and starts the full stack from `docker-compose.yml`.

Default URLs:

- Frontend: http://localhost:3000
- Backend: http://localhost:8080
- MinIO console: http://localhost:9001

Default admin login:

- Email: `admin@company.local`
- Password: `admin12345`

## Local development without full Docker

1. Start Postgres and MinIO:

```powershell
docker compose -f docker-compose-local up -d
```

2. Copy `frontend/.env.example` to `frontend/.env` for Next.js, and set Spring env vars if needed.
3. Build native library:

```powershell
cd llamalib
cargo build --release
```

4. Run Java backend:

```powershell
cd backend
mvn spring-boot:run
```

5. Run Next.js panel:

```powershell
cd frontend
npm install
npm run dev
```
