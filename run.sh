#!/usr/bin/env sh
set -eu

docker compose -f docker-compose-local up -d

echo "Local infrastructure is running:"
echo "- Postgres: localhost:5432"
echo "- MinIO API: localhost:9000"
echo "- MinIO console: http://localhost:9001"
echo "- Meilisearch: http://localhost:7700"
echo ""
echo "OpenSearch is optional:"
echo "  docker compose -f docker-compose-local --profile opensearch up -d"
