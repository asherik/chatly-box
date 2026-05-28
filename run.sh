#!/usr/bin/env sh
set -eu

COMPOSE="${COMPOSE:-docker compose}"

echo "Starting local infrastructure..."
$COMPOSE -f docker-compose-local up -d postgres minio minio-init

echo "Building and starting Chatly Box..."
$COMPOSE up --build
