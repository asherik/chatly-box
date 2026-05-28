#!/usr/bin/env sh
set -eu

docker compose -f docker-compose-local up -d

echo "Local infrastructure is running:"
echo "- Postgres: localhost:5432"
echo "- MinIO API: localhost:9000"
echo "- MinIO console: http://localhost:9001"
echo "- Elasticsearch: http://localhost:9200"
