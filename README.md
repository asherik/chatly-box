# Chatly Box

## Локальная отладка

Нужно локально: Docker Desktop, Java 22+, Rust/Cargo, Node.js 22+.

### 1. Поднять инфраструктуру

```sh
sh run.sh
```

Поднимутся Postgres, MinIO и Meilisearch. Backend и frontend локально запускаются руками.

### 2. Положить модели

```powershell
mkdir models
```

Ожидаемые файлы:

```text
models/embedding.gguf
models/chat.gguf
```

### 3. Собрать Rust-библиотеку

```powershell
cd C:\projects\chatly-box\llamalib
.\build-local.ps1
```

### 4. Запустить backend

```powershell
cd C:\projects\chatly-box\backend
.\gradlew.bat bootRun
```

Backend сам применяет Liquibase-миграции и создает дефолтного администратора.

### 5. Запустить frontend

```powershell
cd C:\projects\chatly-box\frontend
npm install
npm run dev
```

Frontend не ходит в Postgres, MinIO, Meilisearch, OpenSearch или LanceDB. Он ходит только в backend: `http://localhost:8080`.

### 6. Открыть приложение

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

Запустятся frontend, backend, Postgres, MinIO и Meilisearch.

Поиск по умолчанию работает через Meilisearch:

```text
SEARCH_ENGINE=meili
```

Для enterprise-сценариев можно включить OpenSearch:

```sh
SEARCH_ENGINE=opensearch docker compose --profile opensearch up --build
```

## Полезные команды

Остановить локальную инфраструктуру:

```sh
docker compose -f docker-compose-local down
```

Остановить полный Docker-стек:

```sh
docker compose down
```
