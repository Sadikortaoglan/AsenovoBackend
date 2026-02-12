# AWS EC2 Deployment Guide

Complete guide for deploying Sara Elevator Backend API on AWS EC2.

## 📋 Prerequisites

- AWS EC2 instance (Ubuntu 22.04 LTS recommended)
- SSH access to EC2 instance
- Domain name (optional, for production)
- Security Group configured (see below)

## 🔐 Security Group Configuration

Configure your EC2 Security Group to allow:

| Port | Protocol | Source | Description |
|------|----------|--------|-------------|
| 22 | TCP | Your IP | SSH access |
| 80 | TCP | 0.0.0.0/0 | HTTP (if using reverse proxy) |
| 443 | TCP | 0.0.0.0/0 | HTTPS (if using reverse proxy) |
| 8080 | TCP | Your IP / 0.0.0.0/0 | API (restrict in production) |

## 🚀 Step-by-Step Deployment

### Step 1: Connect to EC2 Instance

```bash
ssh -i your-key.pem ubuntu@your-ec2-ip
```

### Step 2: Update System

```bash
sudo apt update
sudo apt upgrade -y
```

### Step 3: Install Docker

```bash
# Install dependencies
sudo apt install -y apt-transport-https ca-certificates curl software-properties-common

# Add Docker's official GPG key
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# Add Docker repository
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Start Docker
sudo systemctl start docker
sudo systemctl enable docker

# Add current user to docker group (optional, to run without sudo)
sudo usermod -aG docker $USER
# Log out and log back in for this to take effect
```

### Step 4: Verify Docker Installation

```bash
docker --version
docker compose version
```

### Step 5: Clone Repository

```bash
# Install Git if not already installed
sudo apt install -y git

# Clone repository
cd /opt
sudo git clone https://github.com/Sadikortaoglan/SarAsansorBackend.git
sudo chown -R $USER:$USER SarAsansorBackend
cd SarAsansorBackend/backend
```

### Step 6: Create Environment File

```bash
nano .env
```

Add the following (replace with your values):

```env
# Database
POSTGRES_DB=sara_asansor
POSTGRES_USER=sara_asansor
POSTGRES_PASSWORD=YOUR_SECURE_PASSWORD_HERE

# JWT Secret (generate a secure random string, minimum 32 characters)
JWT_SECRET=YOUR_JWT_SECRET_KEY_MINIMUM_32_CHARACTERS_LONG

# File Storage (local or s3)
FILE_STORAGE_TYPE=local
FILE_STORAGE_LOCAL_DIR=/app/uploads

# Optional: AWS S3 Configuration
# FILE_STORAGE_TYPE=s3
# AWS_ENDPOINT=https://s3.amazonaws.com
# AWS_ACCESS_KEY=your-access-key
# AWS_SECRET_KEY=your-secret-key
# AWS_BUCKET=sara-asansor-files
# AWS_REGION=us-east-1
```

Generate a secure JWT secret:

```bash
openssl rand -base64 32
```

Save the file (Ctrl+X, then Y, then Enter).

### Step 7: Create Required Directories

```bash
mkdir -p uploads logs
chmod 755 uploads logs
```

### Step 8: Build and Start Services

```bash
# Build and start with docker compose
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

### Step 9: Verify Deployment

```bash
# Check container status
docker compose -f docker-compose.prod.yml ps

# Check logs
docker compose -f docker-compose.prod.yml logs -f app

# Check health endpoint
curl http://localhost:8080/api/health
```

Expected response:

```json
{
  "status": "UP",
  "timestamp": "2024-01-01T12:00:00",
  "service": "sara-asansor-api"
}
```

### Step 10: Test API

```bash
# Test login endpoint
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "patron",
    "password": "password"
  }'
```

### Step 11: Configure Firewall (UFW)

```bash
# Install UFW if not installed
sudo apt install -y ufw

# Allow SSH
sudo ufw allow 22/tcp

# Allow API port (adjust based on your setup)
sudo ufw allow 8080/tcp

# If using reverse proxy, allow HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Enable firewall
sudo ufw enable

# Check status
sudo ufw status
```

## 🔄 Management Commands

### View Logs

```bash
# Application logs
docker compose -f docker-compose.prod.yml logs -f app

# Database logs
docker compose -f docker-compose.prod.yml logs -f postgres

# All logs
docker compose -f docker-compose.prod.yml logs -f
```

### Restart Services

```bash
# Restart all services
docker compose -f docker-compose.prod.yml restart

# Restart only app
docker compose -f docker-compose.prod.yml restart app

# Restart only database
docker compose -f docker-compose.prod.yml restart postgres
```

### Stop Services

```bash
docker compose -f docker-compose.prod.yml stop
```

### Start Services

```bash
docker compose -f docker-compose.prod.yml start
```

### Update Application

```bash
cd /opt/SarAsansorBackend/backend
git pull origin main
docker compose -f docker-compose.prod.yml up -d --build app
```

### View Container Status

```bash
docker compose -f docker-compose.prod.yml ps
```

### Access Database

```bash
# Connect to PostgreSQL
docker compose -f docker-compose.prod.yml exec postgres psql -U sara_asansor -d sara_asansor
```

### Backup Database

```bash
# Create backup
docker compose -f docker-compose.prod.yml exec postgres pg_dump -U sara_asansor sara_asansor > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore backup
docker compose -f docker-compose.prod.yml exec -T postgres psql -U sara_asansor sara_asansor < backup_20240101_120000.sql
```

## 🌐 Reverse Proxy (Optional, Recommended)

For production, use Nginx as a reverse proxy:

### Install Nginx

```bash
sudo apt install -y nginx
```

### Configure Nginx

```bash
sudo nano /etc/nginx/sites-available/sara-asansor-api
```

Add:

```nginx
server {
    listen 80;
    server_name your-domain.com;  # Replace with your domain

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /health {
        proxy_pass http://localhost:8080/api/health;
        access_log off;
    }
}
```

Enable site:

```bash
sudo ln -s /etc/nginx/sites-available/sara-asansor-api /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### SSL with Let's Encrypt (Optional)

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

## 📊 Monitoring

### Health Check

```bash
# Check health endpoint
curl http://localhost:8080/api/health

# Set up monitoring script (cron job)
*/5 * * * * curl -f http://localhost:8080/api/health || echo "API is down" | mail -s "API Alert" admin@example.com
```

### Resource Usage

```bash
# Container stats
docker stats

# Disk usage
df -h
docker system df
```

## 🔒 Security Recommendations

1. **Change default passwords** in `.env` file
2. **Use strong JWT secret** (minimum 32 characters)
3. **Restrict Security Group** to specific IPs in production
4. **Enable HTTPS** using reverse proxy with SSL
5. **Regular updates**: `sudo apt update && sudo apt upgrade`
6. **Firewall**: Use UFW to restrict ports
7. **Regular backups**: Set up automated database backups
8. **Monitor logs**: Check logs regularly for errors

## 🐛 Troubleshooting

### Application won't start

```bash
# Check logs
docker compose -f docker-compose.prod.yml logs app

# Check database connection
docker compose -f docker-compose.prod.yml exec app ping postgres
```

### Database connection errors

```bash
# Verify database is running
docker compose -f docker-compose.prod.yml ps postgres

# Check database logs
docker compose -f docker-compose.prod.yml logs postgres

# Test database connection
docker compose -f docker-compose.prod.yml exec postgres psql -U sara_asansor -d sara_asansor -c "SELECT 1;"
```

### Port already in use

```bash
# Check what's using port 8080
sudo lsof -i :8080

# Kill process or change port in docker-compose.prod.yml
```

### Permission errors

```bash
# Fix uploads directory permissions
sudo chown -R $USER:$USER uploads logs
chmod 755 uploads logs
```

## 📝 Quick Reference

```bash
# Start
docker compose -f docker-compose.prod.yml --env-file .env up -d

# Stop
docker compose -f docker-compose.prod.yml stop

# Restart
docker compose -f docker-compose.prod.yml restart

# Logs
docker compose -f docker-compose.prod.yml logs -f

# Update
git pull && docker compose -f docker-compose.prod.yml up -d --build app
```

## 🎯 API Endpoints

After deployment:

- **API Base**: `http://your-ec2-ip:8080/api`
- **Health Check**: `http://your-ec2-ip:8080/api/health`
- **Swagger UI**: `http://your-ec2-ip:8080/api/swagger-ui.html`
- **API Docs**: `http://your-ec2-ip:8080/api/api-docs`

If using reverse proxy:

- **API Base**: `http://your-domain.com/api`
- **Health Check**: `http://your-domain.com/api/health`
- **Swagger UI**: `http://your-domain.com/api/swagger-ui.html`

