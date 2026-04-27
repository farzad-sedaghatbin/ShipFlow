# Quick Start (Docker)

Get ShipFlow running locally in under 5 minutes using Docker Compose.

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (includes Compose)

## 1. Clone and configure

```bash
git clone https://github.com/farzad-sedaghatbin/ShipFlow.git
cd ShipFlow
cp .env.example .env   # edit values as needed
```

Key variables in `.env`:

| Variable | Default | Description |
|----------|---------|-------------|
| `APP_JWT_SECRET` | *(required)* | Min 32-char secret — **change before first run** |
| `SPRING_DATASOURCE_PASSWORD` | `shipflow_secret` | Postgres password |
| `SPRING_DATA_REDIS_PASSWORD` | `changeme` | Redis password |
| `AI_PROVIDER` | `ollama` | LLM provider (`ollama` \| `openai` \| `runpod`) |
| `OPENAI_API_KEY` | — | Required only if `AI_PROVIDER=openai` |

## 2. Start all services

```bash
docker compose up -d
```

This starts:
- **PostgreSQL 16** on port 5432
- **Redis 7** on port 6379
- **ShipFlow backend** on port 8080
- **ShipFlow frontend** on port 3000

## 3. Open the app

Navigate to **http://localhost:3000**

Default credentials: `admin` / `admin123`

::: warning Change the default password
Log in, go to your profile, and change the admin password immediately.
:::

## Pulling a specific release

```bash
docker pull ghcr.io/farzad-sedaghatbin/shipflow:0.9.0
```

## Stopping

```bash
docker compose down          # keep data
docker compose down -v       # also remove volumes (wipes database)
```
