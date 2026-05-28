# API Examples

## REST

```http
GET /api/search?q=регламент&limit=10
```

```http
POST /api/search/reindex
```

```http
GET /api/ingestion/events
Accept: text/event-stream
```

## gRPC

```sh
grpcurl -plaintext -d "{\"query\":\"регламент\",\"limit\":5}" localhost:9090 chatlybox.grpc.RagService/SearchDocuments
```

```sh
grpcurl -plaintext -d "{\"question\":\"Что написано про SLA?\",\"top_k\":6}" localhost:9090 chatlybox.grpc.RagService/Ask
```
