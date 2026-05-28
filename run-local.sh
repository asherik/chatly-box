#!/usr/bin/env sh
set -eu

docker compose -f docker-compose-local up -d

echo "Infra is running."
echo "Start backend: cd backend && gradle bootRun"
echo "Start frontend: cd frontend && npm install && npm run db:push && npm run db:seed && npm run dev"
