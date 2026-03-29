# ShipFlow — VS Code Developer Guide

A complete setup guide for contributors running ShipFlow locally with VS Code.

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java (Temurin JDK) | 21 | https://adoptium.net |
| Node.js | 18 LTS | https://nodejs.org |
| Maven | 3.9+ | bundled via `./mvnw` |
| Docker Desktop | latest | https://docker.com |
| VS Code | latest | https://code.visualstudio.com |

---

## 1 — Open the Workspace

Open the **repository root** in VS Code (not `backend/` or `frontend/` individually):

```bash
code shapeup-tracker/
```

VS Code will prompt:
- **"Install recommended extensions?"** → click **Install All**
- **"Do you trust the authors?"** → Yes

The extension list is in `.vscode/extensions.json`.

---

## 2 — Install Recommended Extensions

If the prompt doesn't appear:

1. Open the Command Palette (`Cmd+Shift+P` / `Ctrl+Shift+P`)
2. Run `Extensions: Show Recommended Extensions`
3. Click the cloud icon to install all workspace recommendations

### Extension groups

**Java / Spring Boot**
- Java Extension Pack (Red Hat + Microsoft)
- Spring Boot Tools (VMware) — shows live bean/property data in the editor
- Spring Boot Dashboard — run/debug Spring apps from the sidebar

**Frontend**
- ESLint + Prettier — auto-format on save
- Tailwind CSS IntelliSense — class completion and hover docs
- ES7 React Snippets — `rafce`, `useState` shorthand

**Database**
- SQLTools + PostgreSQL driver — query the DB from VS Code

**API Testing**
- REST Client — run `.http` files directly in the editor

**Git**
- GitLens — inline blame, history, and PR reviews
- Git Graph — visual branch/commit history

**AI Assistants** (pick one or both)
- GitHub Copilot
- Claude for VS Code (if available)

---

## 3 — Configure the Java Extension

The Spring Boot Dashboard needs to know your Java 21 installation.

1. Open Settings (`Cmd+,` / `Ctrl+,`)
2. Search `java.home`
3. Set it to your JDK 21 path, e.g.:

```json
"java.jdk.home": "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
```

Or let VS Code auto-detect: `Java: Configure Java Runtime` from the Command Palette.

---

## 4 — Start Infrastructure (Docker)

```bash
docker compose up -d
```

This starts:
- **PostgreSQL** on port `5432` (database: `shipflow`, user: `shipflow`, password: `shipflow`)
- **Redis** on port `6379`

Verify:
```bash
docker compose ps
```

---

## 5 — Environment Variables

Copy the dev template:

```bash
cp backend/.env.example backend/.env.dev   # if it exists, otherwise create it
```

Minimum required for local dev:

```bash
# backend/.env.dev  (or set in your shell profile)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/shipflow
SPRING_DATASOURCE_USERNAME=shipflow
SPRING_DATASOURCE_PASSWORD=shipflow
SPRING_REDIS_HOST=localhost
JWT_SECRET=local-dev-secret-change-in-prod

# LLM (choose one)
OLLAMA_BASE_URL=http://localhost:11434    # local Ollama
# OPENAI_API_KEY=sk-...                  # OpenAI

# MCP Server — optional, see section 8
MCP_SERVER_ENABLED=false
```

---

## 6 — Run the Application

### Option A — Spring Boot Dashboard (recommended)

1. Click the **Spring Boot Dashboard** icon in the Activity Bar (leaf icon)
2. Expand `shipflow-backend`
3. Click the ▶ Run button (or the debug bug icon for debug mode)

The dashboard shows live bean count, active profiles, and port.

### Option B — Launch Configurations

Use the Run & Debug panel (`Cmd+Shift+D` / `Ctrl+Shift+D`):

| Configuration | What it starts |
|---------------|---------------|
| `ShipFlow Backend (dev)` | Backend only, `dev` profile |
| `ShipFlow Backend (MCP enabled)` | Backend with MCP server on (`MCP_SERVER_ENABLED=true`) |
| `ShipFlow Frontend (Chrome)` | Frontend dev server, opens Chrome |
| `Full Stack (Backend + Frontend)` | Both at once |
| `Full Stack + MCP Server` | Both + MCP server enabled |

### Option C — Terminal

```bash
# Terminal 1 — backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Terminal 2 — frontend
cd frontend
npm install
npm run dev
```

| Service | URL |
|---------|-----|
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Frontend | http://localhost:5173 |
| MCP Server (if enabled) | http://localhost:8080/mcp/sse |

---

## 7 — Running Tests

### Backend

```bash
cd backend

# All tests
./mvnw verify

# Single test class
./mvnw test -Dtest=CycleServiceTest

# With coverage report (opens in browser)
./mvnw verify
open target/site/jacoco/index.html
```

Coverage gate: **80% line coverage** enforced by JaCoCo. PRs must pass.

### Frontend

```bash
cd frontend

# All tests (watch mode)
npm test

# Single run (CI mode)
npm run test:ci

# Coverage report
npm run test:coverage
```

Or use the **Vitest Explorer** extension: click the flask icon in the Activity Bar to run/debug individual tests.

### Formatting

```bash
cd backend
./mvnw spotless:check    # fail if not formatted
./mvnw spotless:apply    # auto-fix
```

The ESLint + Prettier extensions auto-format frontend files on save if you have:

```json
// .vscode/settings.json (already configured)
"editor.formatOnSave": true
```

---

## 8 — MCP Server (Optional — Self-Hosted Instances)

> ShipFlow's MCP server is **opt-in**. You do not need it to run a fully functional instance.
> Enable it only if you want AI tools (Claude Code, Cursor, etc.) to connect directly to your ShipFlow.

### Should I enable the MCP server?

| Scenario | Enable MCP? |
|----------|-------------|
| Using ShipFlow as a team tool, no AI editor integration needed | No |
| Developing or testing the MCP server feature | Yes |
| Want Claude Code / Cursor to query your ShipFlow | Yes |
| Running a minimal/lightweight instance | No |
| Contributing to MCP feature development | Yes |

### Enable for development

Set the environment variable before starting the backend:

```bash
MCP_SERVER_ENABLED=true
```

Or use the `ShipFlow Backend (MCP enabled)` launch config.

### Connect Claude Code to your local instance

Add to `.claude/settings.json` (project or user level):

```json
{
  "mcpServers": {
    "shipflow-local": {
      "type": "sse",
      "url": "http://localhost:8080/mcp/sse",
      "headers": {
        "Authorization": "Bearer <your-shipflow-api-key>"
      }
    }
  }
}
```

Generate an API key: ShipFlow UI → Settings → API Keys → Create Key.

### Connect Cursor to your local instance

Create or edit `.cursor/mcp.json` in the project root:

```json
{
  "mcpServers": {
    "shipflow": {
      "url": "http://localhost:8080/mcp/sse",
      "headers": {
        "Authorization": "Bearer <your-api-key>"
      }
    }
  }
}
```

### Connect Claude Desktop

Edit `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS):

```json
{
  "mcpServers": {
    "shipflow": {
      "type": "sse",
      "url": "http://localhost:8080/mcp/sse",
      "headers": {
        "Authorization": "Bearer <your-api-key>"
      }
    }
  }
}
```

Restart Claude Desktop after saving.

See `MCP_SERVER_MILESTONE.md` for the full MCP implementation plan and available tools.

---

## 9 — Database Tools

### SQLTools (VS Code extension)

1. Click the SQLTools icon in the Activity Bar (database cylinder)
2. Add a new connection:
   - Driver: **PostgreSQL**
   - Server: `localhost`
   - Port: `5432`
   - Database: `shipflow`
   - Username: `shipflow`
   - Password: `shipflow`
3. Save and connect

You can now run queries, browse tables, and inspect data without leaving VS Code.

### Useful queries

```sql
-- Active cycles
SELECT id, name, status, start_date, end_date FROM cycles WHERE status = 'ACTIVE';

-- Tasks blocking others
SELECT t.id, t.title, td.dependency_type
FROM tasks t JOIN task_dependencies td ON t.id = td.task_id
WHERE td.dependency_type = 'BLOCKS';

-- Recent audit log
SELECT entity_name, entity_id, action, created_at, created_by
FROM audit_log ORDER BY created_at DESC LIMIT 50;
```

---

## 10 — REST Client (`.http` files)

The REST Client extension lets you call the API directly from `.http` files.
Create `scratch.http` in the project root (it's gitignored):

```http
### Login
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "password"
}

### List projects
GET http://localhost:8080/api/projects
Authorization: Bearer {{token}}

### Create task
POST http://localhost:8080/api/tasks
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "title": "Fix login redirect",
  "cycleId": "{{cycleId}}",
  "status": "TODO"
}
```

Click **Send Request** above each block to execute.

---

## 11 — Debugging Tips

### Backend won't start

- Check Docker containers are running: `docker compose ps`
- Check port 8080 is free: `lsof -i :8080`
- Check Java version: `java -version` (must be 21)
- Look for Flyway migration errors in the console (usually a `V*.sql` file issue)

### Frontend proxy errors

The Vite dev server proxies `/api` to `localhost:8080`. If you see CORS or 502 errors:
- Confirm the backend is running
- Check `frontend/vite.config.ts` proxy section

### JWT errors (401)

- Tokens expire — log in again
- Check `JWT_SECRET` matches between restarts

### Redis connection errors

```bash
docker compose restart redis
```

### Ollama not responding (LLM features)

```bash
ollama serve          # start if not running
ollama pull llama3    # pull a model
```

---

## 12 — Useful VS Code Shortcuts (project-specific)

| Action | Shortcut |
|--------|----------|
| Toggle Spring Boot Dashboard | Activity Bar → leaf icon |
| Run launch config | `F5` |
| Run without debug | `Ctrl+F5` |
| Java: Organize Imports | `Shift+Alt+O` |
| Format document | `Shift+Alt+F` |
| Open terminal | `Ctrl+\`` |
| Find file | `Cmd+P` / `Ctrl+P` |
| Find in files | `Cmd+Shift+F` / `Ctrl+Shift+F` |
| Go to definition | `F12` |
| Peek definition | `Alt+F12` |
| Rename symbol | `F2` |

---

## 13 — Project Structure Navigation

Useful VS Code tips for this codebase:

- **Find a controller**: `Ctrl+P` → type `CycleController`
- **Find all usages of a service**: right-click → `Find All References`
- **Jump to Spring bean**: Spring Boot Tools shows beans in the gutter; click to navigate
- **Browse DB schema**: SQLTools sidebar → expand `shipflow` database → Tables
- **Frontend component**: `Ctrl+P` → `HillChart` to find `HillChartPage.tsx` etc.

---

## 14 — Contributing

1. Fork the repo and clone your fork
2. Create a branch: `git checkout -b feat/your-feature`
3. Make changes, run `./mvnw spotless:apply && ./mvnw verify`
4. Push and open a PR against `main`
5. Fill in the PR template (`.github/PULL_REQUEST_TEMPLATE.md`)

See `CONTRIBUTING.md` for the full contribution guide.
