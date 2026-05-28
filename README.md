# Chatly Box

## Локальная отладка

Требуется локально:

- Docker Desktop
- Java 22+
- Rust/Cargo
- Node.js 22+

### 1. Поднять инфраструктуру

```sh
sh run.sh
```

Запустятся:

- Postgres: `localhost:5432`
- MinIO: `http://localhost:9001`
- Elasticsearch: `http://localhost:9200`

### 2. Положить модели

```powershell
mkdir models
```

Нужные файлы:

```text
models/embedding.gguf
models/chat.gguf
```

### 3. Запустить backend

Сначала собрать Rust DLL:

```powershell
cd llamalib
cargo build --release
```

Потом запустить backend:

```powershell
cd backend
.\gradlew.bat bootRun
```

### 4. Запустить frontend

В отдельном терминале:

```powershell
npm install
npm run db:push
npm run db:seed
npm run dev
```

### 5. Открыть приложение

```text
http://localhost:3000
```

Логин:

```text
admin@company.local
admin12345
```

## Продовый запуск в Docker

### 1. Положить модели

```text
models/embedding.gguf
models/chat.gguf
```

### 2. Запустить весь стек

```sh
sh run-full-docker.sh
```

Или напрямую:

```sh
docker compose up --build
```

Запустятся:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Postgres
- MinIO
- Elasticsearch

## Полезные команды

Остановить локальную инфраструктуру:

```sh
docker compose -f docker-compose-local down
```

Остановить полный Docker-стек:

```sh
docker compose down
```

Пересобрать Rust-библиотеку вручную:

```powershell
cd llamalib
cargo build --release
```
