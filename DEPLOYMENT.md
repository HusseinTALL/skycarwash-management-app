# SkyCarWash — Deployment Guide (Contabo)

Target server: **`root@161.97.183.139`** · Access: **http://161.97.183.139**

This deploys the whole stack (PostgreSQL + Spring Boot backend + Vue PWA behind
nginx) with Docker Compose. The frontend nginx is the single public entry point
on port 80 and proxies `/api` and `/ws` to the backend — one origin, no CORS.

> These steps run **on the server**. Everything is copy-paste. Run them from
> your own machine's terminal (they start with `ssh`).

---

## 1. Install Docker on the server (first time only)

```bash
ssh root@161.97.183.139

# Docker Engine + Compose plugin (official convenience script)
curl -fsSL https://get.docker.com | sh
docker compose version   # should print v2.x
```

---

## 2. Get the code onto the server

```bash
# still on the server
mkdir -p /opt && cd /opt
git clone https://github.com/HusseinTALL/skycarwash-management-app.git
cd skycarwash-management-app
git checkout claude/project-assessment-kr2hmk
```

> Private repo? Use a deploy key or a personal access token in the clone URL:
> `git clone https://<TOKEN>@github.com/HusseinTALL/skycarwash-management-app.git`

---

## 3. Configure secrets

```bash
cp .env.example .env

# generate strong secrets
echo "POSTGRES_PASSWORD=$(openssl rand -base64 24)"
echo "JWT_SECRET=$(openssl rand -base64 48)"

nano .env   # paste the generated values, save
```

`.env` is git-ignored — it never leaves the server.

---

## 4. Open the firewall (if `ufw` is active)

```bash
ufw allow OpenSSH
ufw allow 80/tcp
# ufw allow 443/tcp   # only when you add HTTPS (see §7)
ufw --force enable
ufw status
```

Postgres (5432) and the backend (8080) are **not** exposed publicly — the
backend port is bound to `127.0.0.1` for local debugging only.

---

## 5. Deploy

```bash
./deploy.sh
```

This builds the images, starts the stack, waits for the backend health check,
and prints container status. First build takes a few minutes (Maven + npm).

When it finishes, open **http://161.97.183.139**.

**Default login:** phone `+22600000000` · password `Admin1234!`
**→ Change this password immediately after first login.**

---

## 6. Day-to-day operations

```bash
cd /opt/skycarwash-management-app

# redeploy after new commits are pushed
./deploy.sh

# logs
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f frontend

# stop / start
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d

# database backup
docker compose -f docker-compose.prod.yml exec postgres \
  pg_dump -U skycarwash skycarwash > backup_$(date +%F).sql
```

---

## 7. Adding a domain + HTTPS (recommended for production)

Login over plain HTTP sends the JWT/credentials unencrypted. Once you have a
domain, add TLS. Easiest path with the current setup:

1. Point an **A record** for your domain at `161.97.183.139`.
2. Put **Caddy** in front (auto Let's Encrypt) or add a certbot + nginx `443`
   server block. Then:
   - open `443/tcp` in `ufw`,
   - set `CORS_ORIGINS=https://your-domain` in `.env`,
   - rebuild: `./deploy.sh`.

Ask and this guide can be extended with a ready-to-run Caddy service in the
compose file.

---

## Troubleshooting

| Symptom | Check |
|---|---|
| `backend status: unhealthy` | `docker compose -f docker-compose.prod.yml logs backend` — usually DB creds or `JWT_SECRET` too short (min 32 chars). |
| Site loads but API calls 502 | Backend not healthy yet; wait, or check backend logs. |
| Login fails | Confirm you're using `+22600000000` / `Admin1234!` on first run. |
| Port 80 already in use | Another web server holds it: `ss -tlnp \| grep ':80 '`. Either free it (`systemctl disable --now apache2` / `nginx`) or set `HTTP_PORT=8090` in `.env` and re-run `./deploy.sh` (then `ufw allow 8090/tcp`). |
