# Chatly Box

## Локальная отладка

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

```powershell
powershell -ExecutionPolicy Bypass -File .\run-local-backend.ps1
```

Скрипт сам найдёт Java 22+ через `JAVA_HOME`, `PATH` или стандартные папки установки JDK.

Если Rust DLL ещё нет, скрипт сам выполнит:

```powershell
cargo build --release
```

### 4. Запустить frontend

В отдельном терминале:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-local-frontend.ps1
```

Скрипт сам выполнит установку зависимостей, миграцию схемы Prisma, seed админа и запуск Next.js.

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
