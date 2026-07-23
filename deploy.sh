#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# SkyCarWash – production deploy script (run ON the server)
#
#   ssh root@161.97.183.139
#   cd /opt/skycarwash-management-app        # wherever you cloned the repo
#   ./deploy.sh
#
# Pulls the latest code, (re)builds images, and brings the stack up. Safe to
# re-run for every deploy.
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

COMPOSE_FILE="docker-compose.prod.yml"
BRANCH="${DEPLOY_BRANCH:-main}"

cd "$(dirname "$0")"

echo "==> SkyCarWash deploy"

# --- Preconditions --------------------------------------------------------------
if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker is not installed. See DEPLOYMENT.md step 1." >&2
  exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
  echo "ERROR: 'docker compose' plugin not available. See DEPLOYMENT.md step 1." >&2
  exit 1
fi
if [ ! -f .env ]; then
  echo "ERROR: .env not found. Run: cp .env.example .env  then edit the secrets." >&2
  exit 1
fi

# --- Pull latest code -----------------------------------------------------------
if [ -d .git ]; then
  echo "==> Fetching latest code (branch: $BRANCH)"
  git fetch origin "$BRANCH"
  git checkout "$BRANCH"
  git reset --hard "origin/$BRANCH"
else
  echo "==> Not a git checkout, skipping code pull."
fi

# --- Build & start --------------------------------------------------------------
echo "==> Building images (this can take a few minutes on first run)"
docker compose -f "$COMPOSE_FILE" build

echo "==> Starting stack"
docker compose -f "$COMPOSE_FILE" up -d

# --- Wait for backend health ----------------------------------------------------
echo "==> Waiting for backend to become healthy..."
for i in $(seq 1 30); do
  status=$(docker inspect --format '{{ .State.Health.Status }}' \
            "$(docker compose -f "$COMPOSE_FILE" ps -q backend)" 2>/dev/null || echo "starting")
  if [ "$status" = "healthy" ]; then
    echo "    backend is healthy."
    break
  fi
  echo "    backend status: $status ($i/30)"
  sleep 10
done

echo
echo "==> Container status:"
docker compose -f "$COMPOSE_FILE" ps

echo
echo "==> Done. App should be reachable at: http://161.97.183.139"
echo "    Default login — phone: +22600000000   password: Admin1234!"
echo "    >>> Change this password immediately after first login. <<<"
