#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# Rebuilds and restarts the production stack from the current checkout.
# Run on the server (the GitHub Actions deploy workflow calls this after
# fast-forwarding the repo). Safe to run by hand too.
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

cd "$(dirname "$0")/.."   # repo root

if [ ! -f .env ]; then
  echo "ERROR: .env not found in $(pwd)." >&2
  echo "       Copy .env.example to .env and fill in real values first." >&2
  exit 1
fi

COMPOSE="docker compose -f docker-compose.prod.yml"

echo "▶ Building and starting containers…"
$COMPOSE up -d --build --remove-orphans

echo "▶ Removing dangling images…"
docker image prune -f

echo "✔ Deploy complete."
$COMPOSE ps
