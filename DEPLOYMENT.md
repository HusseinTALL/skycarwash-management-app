# Auto-deploy to Contabo on every push

Push to `main` → GitHub runs CI → if it's green, GitHub SSHes into your Contabo
server, pulls the new code, and rebuilds the Docker stack. Zero manual steps
after the one-time setup below.

```
git push  ──►  GitHub Actions "CI" (tests+build)  ──►  "Deploy" (SSH)  ──►  Contabo rebuilds
```

The moving parts (all committed to the repo):

| File                        | Role                                                        |
|-----------------------------|-------------------------------------------------------------|
| `.github/workflows/deploy.yml` | Runs after CI passes on `main`, SSHes to the server      |
| `deploy/deploy.sh`          | Rebuilds & restarts the stack on the server                 |
| `docker-compose.prod.yml`   | Production stack (DB + backend internal, frontend on :80)   |
| `deploy/nginx.prod.conf`    | Serves the SPA and proxies `/api` + `/ws` to the backend    |
| `.env.example`              | Template for the server's secrets (`.env` is git-ignored)   |

---

## One-time server setup (on Contabo)

SSH into your server as a non-root sudo user (e.g. `deploy`).

### 1. Install Docker + Compose

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER"        # log out/in afterwards so it takes effect
docker compose version                 # confirm the plugin is present
```

### 2. Open the firewall

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp                 # if you add HTTPS later
sudo ufw enable
```

### 3. Clone the repo and create the secrets file

```bash
git clone https://github.com/HusseinTALL/skycarwash-management-app.git
cd skycarwash-management-app
cp .env.example .env
nano .env                              # set POSTGRES_PASSWORD, JWT_SECRET, CORS_ORIGINS
```

> Generate a strong JWT secret with `openssl rand -base64 48`.
> **Private repo?** Clone over SSH and add a read-only *deploy key*
> (`ssh-keygen -t ed25519 -f ~/.ssh/gh_deploy`; add the `.pub` under the repo's
> **Settings → Deploy keys**), so the server can `git fetch` without a password.

### 4. First deploy by hand (verify it works)

```bash
bash deploy/deploy.sh
```

Visit `http://YOUR_SERVER_IP` — the app should load and log in. On first boot,
Flyway creates the schema and seeds the default manager
(`+22600000000` / `Admin1234!`). To fill demo data, see `backend/SEEDING.md`.

---

## Wire up the GitHub → server SSH key

On the server, create a dedicated key **for GitHub Actions** (no passphrase):

```bash
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/gh_actions -N ""
cat ~/.ssh/gh_actions.pub >> ~/.ssh/authorized_keys
cat ~/.ssh/gh_actions            # copy this PRIVATE key for the secret below
```

In GitHub → **Settings → Secrets and variables → Actions → New repository secret**,
add:

| Secret              | Value                                                    |
|---------------------|----------------------------------------------------------|
| `CONTABO_HOST`      | Server IP or hostname                                    |
| `CONTABO_USER`      | SSH user (e.g. `deploy`)                                 |
| `CONTABO_SSH_KEY`   | The **private** key printed above (whole file)          |
| `DEPLOY_PATH`       | Path to the clone, e.g. `/home/deploy/skycarwash-management-app` |
| `CONTABO_SSH_PORT`  | *(optional)* SSH port if not 22                          |

That's it. **Push to `main`** (or merge a PR) and watch the **Actions** tab:
`CI` runs, then `Deploy` connects and rebuilds. Subsequent pushes redeploy
automatically.

---

## Notes & options

- **What gets deployed:** the deploy job fast-forwards the server to the exact
  commit CI validated, then runs `docker compose -f docker-compose.prod.yml up
  -d --build`. Only changed images rebuild; Postgres data persists in the
  `postgres_data` volume.
- **HTTPS / domain:** point your domain's A record at the server. For TLS, the
  simplest add-on is Caddy or an nginx + certbot container in front of port 80
  — happy to set that up next if you want it.
- **Build resources:** the backend image builds with Maven on the server. If
  your Contabo plan is small (≤2 GB RAM), tell me and I'll switch the pipeline
  to build images in GitHub Actions and have the server only *pull* them
  (lighter on the box, needs a container registry).
- **Rollback:** `git reset --hard <old-sha> && bash deploy/deploy.sh` on the
  server redeploys a previous commit.
