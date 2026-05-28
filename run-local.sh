#!/usr/bin/env sh
set -eu

docker compose -f docker-compose-local up -d

echo "Infra is running."
echo "Start backend: powershell -ExecutionPolicy Bypass -File ./run-local-backend.ps1"
echo "Start frontend: powershell -ExecutionPolicy Bypass -File ./run-local-frontend.ps1"
