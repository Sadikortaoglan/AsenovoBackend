# Asenovo Production Deployment

Single-server production MVP for Hetzner:

- Docker Compose runs PostgreSQL, Spring Boot backend, React/Vite frontend, and Nginx.
- Nginx is the only public app container and routes `/api` to Spring Boot and `/` to the frontend.
- PostgreSQL and backend are only reachable on the private Docker network.
- Logs use Docker log rotation and named volumes for Nginx/Spring file logs.

## File Structure

```text
AsenovoBackend/
  Dockerfile
  deploy/
    production/
      docker-compose.yml
      README.md
      env/
        .env.prod.example
      frontend/
        Dockerfile
      nginx/
        nginx.conf
        conf.d/
          asenovo.conf
      firewall/
        ufw-setup.sh
      fail2ban/
        jail.local
        asenovo-nginx-4xx.conf
AsenovoWeb/
  package.json
  src/
```

## DNS

Point these records to the Hetzner server public IP:

```text
asenovo.com      A      YOUR_SERVER_IP
www.asenovo.com  A      YOUR_SERVER_IP
app.asenovo.com  A      YOUR_SERVER_IP
api.asenovo.com  A      YOUR_SERVER_IP
*.asenovo.com    A      YOUR_SERVER_IP
```

For wildcard TLS with Let's Encrypt, use DNS validation through your DNS provider. HTTP validation cannot issue `*.asenovo.com`.

## Server Bootstrap

Run on a fresh Ubuntu server:

```bash
sudo apt update
sudo apt install -y ca-certificates curl git ufw fail2ban
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo tee /etc/apt/keyrings/docker.asc >/dev/null
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"
```

Log out and back in after adding your user to the Docker group.

## Firewall

```bash
cd /opt/asenovo/AsenovoBackend
sudo sh deploy/production/firewall/ufw-setup.sh
```

Only SSH, HTTP, and HTTPS are opened.

## App Deploy

Clone the backend and frontend as sibling directories:

```bash
sudo mkdir -p /opt/asenovo
sudo chown "$USER":"$USER" /opt/asenovo
cd /opt/asenovo
git clone YOUR_BACKEND_REPO_URL AsenovoBackend
git clone YOUR_FRONTEND_REPO_URL AsenovoWeb
cd AsenovoBackend/deploy/production
cp env/.env.prod.example .env.prod
```

Edit `.env.prod` and replace all `REPLACE_...` secrets:

```bash
openssl rand -base64 48
```

Build and start:

```bash
cd /opt/asenovo/AsenovoBackend/deploy/production
docker compose --env-file .env.prod up -d --build postgres backend frontend
```

## TLS

If you use a normal certificate without wildcard support, issue it before starting the `nginx` service:

```bash
docker run --rm \
  -v asenovo_letsencrypt:/etc/letsencrypt \
  -p 80:80 \
  certbot/certbot certonly --standalone \
  -d asenovo.com -d www.asenovo.com -d app.asenovo.com -d api.asenovo.com \
  --email admin@asenovo.com --agree-tos --no-eff-email
```

For `*.asenovo.com`, use Certbot DNS validation for your DNS provider, then store the issued certificate in the `asenovo_letsencrypt` volume under `/etc/letsencrypt/live/asenovo.com`.

Start Nginx after certificates exist:

```bash
docker compose --env-file .env.prod up -d nginx
docker compose ps
```

Renewal can be run manually for MVP:

```bash
docker compose --env-file .env.prod stop nginx
docker run --rm \
  -v asenovo_letsencrypt:/etc/letsencrypt \
  -p 80:80 \
  certbot/certbot renew --standalone
docker compose --env-file .env.prod up -d nginx
```

## Fail2ban

Install the jail and custom filter on the host:

```bash
sudo cp deploy/production/fail2ban/asenovo-nginx-4xx.conf /etc/fail2ban/filter.d/asenovo-nginx-4xx.conf
sudo cp deploy/production/fail2ban/jail.local /etc/fail2ban/jail.d/asenovo.local
sudo systemctl enable --now fail2ban
sudo systemctl restart fail2ban
sudo fail2ban-client status
```

The Compose project name is pinned to `asenovo`, so the Nginx log volume path used by fail2ban is stable.

## Health Checks

```bash
curl -i https://asenovo.com/healthz
curl -i https://api.asenovo.com/api/health
docker compose --env-file .env.prod ps
docker compose --env-file .env.prod logs --tail=100 nginx
docker compose --env-file .env.prod logs --tail=100 backend
```

## Backups

Minimum viable PostgreSQL backup:

```bash
mkdir -p /opt/asenovo/backups
docker compose --env-file .env.prod exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' | gzip > /opt/asenovo/backups/asenovo-$(date +%F-%H%M).sql.gz
```

Move backups off the server regularly.

## Scaling Notes

This setup is intentionally simple. For low traffic, keep one backend container and PostgreSQL on the same server. Next practical upgrades are:

- Add scheduled off-server backups.
- Move TLS renewal to a systemd timer.
- Increase backend replicas only after tenant provisioning and file uploads are verified as stateless or moved to object storage.
- Move PostgreSQL to a managed or separate database server when CPU, RAM, or backup windows become limiting.
