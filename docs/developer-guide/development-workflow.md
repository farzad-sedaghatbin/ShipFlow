# Development Workflow

::: tip Full guide
See [`DEVELOPMENT_WORKFLOW.md`](https://github.com/farzad-sedaghatbin/ShipFlow/blob/main/DEVELOPMENT_WORKFLOW.md) in the repository for the complete workflow guide.
:::

## Local stack

```bash
# Start Postgres + Redis
docker compose up -d postgres redis

# Backend (hot-reload)
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend (Vite HMR)
cd frontend && npm run dev
```

## Running tests

```bash
# Backend — compile + test (matches CI)
cd backend && ./mvnw test

# Backend — compile + test + coverage report (run locally before PRs)
cd backend && ./mvnw verify

# Frontend — unit tests
cd frontend && npm test

# Frontend — E2E (requires running dev stack)
cd frontend && npm run test:e2e
```

## Database migrations

- Files live in `backend/src/main/resources/db/migration/`
- Naming: `V{YYYY}_{MM}_{DD}_{seq}__{description}.sql`
- **Never edit an existing migration** — always add a new file
- H2 is used for tests; Postgres for dev/prod

## Formatting

```bash
# Check formatting (runs in CI)
cd backend && ./mvnw spotless:check

# Auto-fix formatting
cd backend && ./mvnw spotless:apply
```

## CI pipeline

GitHub Actions runs on every push and PR:
1. Spotless format check
2. Backend tests (`./mvnw test`)
3. Frontend tests (`npm test -- --run`)
4. Playwright E2E tests (on canonical repo only)
5. Docker build verification (on PRs)
