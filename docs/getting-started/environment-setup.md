# Environment Setup

Full reference for all environment variables.

::: tip Source of truth
This page mirrors [`ENVIRONMENT_SETUP.md`](https://github.com/farzad-sedaghatbin/ShipFlow/blob/main/ENVIRONMENT_SETUP.md) in the repository root. If they differ, the repo file is authoritative.
:::

## Core

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | ✅ | `jdbc:postgresql://localhost:5432/shipflowdb` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | ✅ | `shipflow` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | ✅ | `shipflow_secret` | DB password |
| `SPRING_DATA_REDIS_HOST` | ✅ | `localhost` | Redis host |
| `SPRING_DATA_REDIS_PORT` | | `6379` | Redis port |
| `SPRING_DATA_REDIS_PASSWORD` | | `changeme` | Redis password |
| `APP_JWT_SECRET` | ✅ | — | JWT signing secret — **must be ≥ 32 chars and changed in production** |

## AI / LLM

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `AI_PROVIDER` | | `ollama` | Active LLM provider: `ollama` \| `openai` \| `runpod` |
| `OLLAMA_BASE_URL` | | `http://localhost:11434` | Ollama server URL |
| `OPENAI_API_KEY` | if openai | — | OpenAI API key |
| `OPENAI_MODEL` | | `gpt-4o` | OpenAI model ID |
| `RUNPOD_API_KEY` | if runpod | — | RunPod API key |

## Vector Store (RAG)

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `QA_VECTORSTORE_PROVIDER` | | `in-memory` | `in-memory` \| `qdrant` \| `chroma` |
| `QDRANT_HOST` | if qdrant | `localhost` | Qdrant host |
| `QDRANT_PORT` | if qdrant | `6334` | Qdrant gRPC port (note: 6333 is Qdrant's HTTP port; 6334 is gRPC) |
| `QDRANT_API_KEY` | | — | Qdrant cloud API key (optional for self-hosted) |

## Email Notifications (SMTP)

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SMTP_HOST` | | — | SMTP server hostname. Leave unset to disable email. |
| `SMTP_PORT` | | `587` | SMTP port |
| `SMTP_USERNAME` | | — | SMTP username |
| `SMTP_PASSWORD` | | — | SMTP password (**env var only — never stored in DB**) |
| `SMTP_FROM` | | — | From address (e.g. `noreply@yourcompany.com`) |

## MCP Server

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `MCP_SERVER_ENABLED` | | `false` | Enable the ShipFlow MCP server |
| `MCP_SERVER_WRITE_ENABLED` | | `false` | Enable write tools (requires `MCP_SERVER_ENABLED=true`) |

## Profiles

| Profile | Use case |
|---------|----------|
| `dev` | Local development — PostgreSQL, relaxed CORS, debug logging (H2 is used for unit tests only) |
| `prod` | Production — strict CORS, secret validation on startup, no Swagger |
