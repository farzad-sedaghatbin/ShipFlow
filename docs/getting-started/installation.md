# Installation

ShipFlow can be run via Docker Compose (recommended) or built from source for development.

## Option A — Docker Compose (recommended)

See [Quick Start](./quick-start) for the fastest path.

## Option B — Build from source

### Prerequisites

| Tool | Version |
|------|---------|
| Java (Temurin) | 21 |
| Node.js | 18 LTS |
| Maven | 3.9+ |
| Docker | any recent version (for Postgres + Redis) |

### Steps

```bash
# 1. Start infrastructure
docker compose up -d postgres redis

# 2. Backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Frontend (new terminal)
cd frontend
npm install
npm run dev
```

- Backend: http://localhost:8080
- Frontend: http://localhost:3000
- Swagger UI: http://localhost:8080/swagger-ui.html

## Database migrations

Flyway runs automatically on startup. No manual migration steps needed.

## First login

Default credentials: `admin` / `admin123`

See [Environment Setup](./environment-setup) for all configuration options.
