# Environment Variables Setup

This document describes how to configure environment variables for ShipFlow development and deployment.

## AI Provider Configuration

ShipFlow uses a pluggable LLM provider system that supports multiple AI backends. Configure your preferred provider using the `AI_PROVIDER` environment variable.

### Supported Providers

| Provider | Config Value | Best For | API Key Required |
|----------|-------------|----------|------------------|
| Ollama | `ollama` | Local development, privacy-first | No |
| RunPod | `runpod` | Production with GPU scaling | Yes |
| OpenAI | `openai` | High-quality responses, complex tasks | Yes |
| Anthropic | `anthropic` | Best for code analysis & long-context reasoning | Yes |
| Google | `google` | Coming soon | Yes |

## Vector Store Configuration

ShipFlow uses a pluggable vector store system for RAG (Retrieval-Augmented Generation). Configure your preferred provider using the `QA_VECTORSTORE_PROVIDER` environment variable.

### Supported Vector Stores

| Provider | Config Value | Best For | API Key Required |
|----------|-------------|----------|------------------|
| **Qdrant** | `qdrant` | **Production (recommended)** | Yes (recommended) |
| In-Memory | `in-memory` | Development/Testing | No |
| ChromaDB | `chroma` | Small deployments | No |
| Milvus | `milvus` | Large-scale (coming soon) | Yes |
| Pinecone | `pinecone` | Managed cloud (coming soon) | Yes |

### Qdrant Configuration (Production Recommended)

Qdrant is the recommended vector database for production deployments due to its high performance, advanced filtering, and enterprise features.

```bash
# Vector store provider
QA_VECTORSTORE_PROVIDER=qdrant

# Qdrant connection settings
QDRANT_HOST=localhost          # or qdrant for Docker
QDRANT_PORT=6334               # gRPC port (default)
QDRANT_API_KEY=your-secure-api-key  # Strongly recommended for production

# Collection settings
QA_VECTORSTORE_COLLECTION=shipflow_knowledge
QA_VECTORSTORE_DIMENSION=384   # Matches all-MiniLM-L6-v2 embedding model
```

### In-Memory Configuration (Development)

For local development, the in-memory store requires no external dependencies:

```bash
QA_VECTORSTORE_PROVIDER=in-memory
# No additional configuration needed
# Note: Data is NOT persistent - lost on restart
```

### ChromaDB Configuration (Alternative)

For users who prefer ChromaDB:

```bash
QA_VECTORSTORE_PROVIDER=chroma
CHROMADB_URL=http://localhost:8000
QA_VECTORSTORE_COLLECTION=shipflow_knowledge
```

## Local Development

### Option 1: Ollama (Recommended for Development)

1. **Create a `.env` file:**
   ```bash
   # Create .env file in project root
   cat > .env << 'EOF'
   AI_PROVIDER=ollama
   OLLAMA_BASE_URL=http://localhost:11434
   OLLAMA_MODEL=mistral:instruct
   EOF
   ```

2. **Install and start Ollama:**
   ```bash
   brew install ollama
   ollama pull mistral:instruct
   ollama serve
   ```

### Option 2: OpenAI ChatGPT (Recommended for Production)

1. **Get an API key from OpenAI:**
   - Go to https://platform.openai.com/api-keys
   - Create a new API key

2. **Create a `.env` file:**
   ```bash
   cat > .env << 'EOF'
   AI_PROVIDER=openai
   OPENAI_API_KEY=sk-your-api-key-here
   OPENAI_MODEL=gpt-4.1-mini
   # Current recommended default in this guide.
   # If you see older examples elsewhere in the repo using gpt-4o or
   # gpt-4-turbo-preview, treat those as legacy references.
   # Optional: gpt-4.1 for more complex tasks
   EOF
   ```

### Option 3: RunPod (Cloud GPU)

1. **Copy the example file:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` with your RunPod credentials:**
   ```bash
   # The .env file is git-ignored and safe for local credentials
   nano .env
   ```

3. **Required Variables:**
   - `RUNPOD_BASE_URL` - Your RunPod endpoint URL
   - `RUNPOD_API_KEY` - Your RunPod API key
   - `RUNPOD_MODEL` - Model name (default: mistral:instruct)
   - Other optional configurations (see `.env.example`)

## Common AI Configuration

These settings apply to all providers:

```bash
# Temperature (0.0-1.0, lower = more deterministic)
AI_TEMPERATURE=0.3

# Maximum tokens in response
AI_MAX_TOKENS=2048
```

## Provider-Specific Configuration

### Ollama Configuration
```bash
AI_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=mistral:instruct
OLLAMA_TIMEOUT=180
```

### OpenAI Configuration
```bash
AI_PROVIDER=openai
OPENAI_API_KEY=sk-your-key
OPENAI_MODEL=gpt-4.1-mini          # or gpt-4.1 (flagship), gpt-4.1-nano (fastest)
OPENAI_TIMEOUT=120
OPENAI_ORG_ID=                     # Optional: Organization ID
OPENAI_BASE_URL=                   # Optional: For Azure OpenAI or proxies
```

### Anthropic Configuration
```bash
APP_AI_PROVIDER=anthropic
APP_AI_ANTHROPIC_API_KEY=sk-ant-your-key-here   # preferred — consistent with APP_AI_* convention
# ANTHROPIC_API_KEY=sk-ant-your-key-here        # also accepted as fallback
APP_AI_ANTHROPIC_MODEL=claude-haiku-4-5-20251001   # see model table below
APP_AI_ANTHROPIC_TIMEOUT=120
```
Get an API key at: https://console.anthropic.com/settings/keys

Check which models your key can access:
```bash
curl https://api.anthropic.com/v1/models -H "X-Api-Key: $APP_AI_ANTHROPIC_API_KEY"
```

> ⚠️ **Common mistake**: `APP_ANTHROPIC_API_KEY` (missing the `AI` segment) is **wrong** — it maps to
> `app.anthropic.api-key`, not `app.ai.anthropic.api-key`. Always use `APP_AI_ANTHROPIC_API_KEY`.

> ⚠️ **Model mismatch**: Newer API keys (created 2025+) only expose Claude 4.x models. Older Claude 3.x
> IDs like `claude-3-5-haiku-20241022` return `not_found_error` on these keys. Use the models table below.

| Model | Best for | Notes |
|-------|----------|-------|
| `claude-haiku-4-5-20251001` | **Default** — all ShipFlow AI features | Fastest, cheapest |
| `claude-sonnet-4-5-20250929` | Wise Architecture, risk analysis | Better reasoning |
| `claude-sonnet-4-6` | Best quality/cost balance | Latest Sonnet |
| `claude-opus-4-6` | Maximum capability | Premium pricing |

### RunPod Configuration
```bash
AI_PROVIDER=runpod
RUNPOD_BASE_URL=https://api.runpod.ai
RUNPOD_API_KEY=your-api-key
RUNPOD_MODEL=mistral:instruct
RUNPOD_TIMEOUT=180
RUNPOD_POLL_INTERVAL=2
```

## Testing

Tests use mock credentials defined in `backend/src/test/resources/application-test.properties`. No real API keys are needed for tests.

## Production Deployment

Set environment variables through your deployment platform:

### Docker
```bash
docker run -e AI_PROVIDER=openai -e OPENAI_API_KEY=your-key ...
```

### Kubernetes
Use ConfigMaps and Secrets:
```yaml
env:
  - name: AI_PROVIDER
    value: "openai"
  - name: OPENAI_API_KEY
    valueFrom:
      secretKeyRef:
        name: shipflow-secrets
        key: openai-api-key
```

### Cloud Platforms
- **AWS**: Use Parameter Store or Secrets Manager
- **Heroku**: Use Config Vars
- **Azure**: Use App Settings

## Security Notes

⚠️ **NEVER commit the `.env` file to git!**

- `.env` is in `.gitignore` to prevent accidental commits
- Use `.env.example` as a template for documentation
- Real credentials should only exist in:
  - Local `.env` files (developers)
  - CI/CD secrets (automated testing)
  - Platform environment variables (production)

## Adding New Providers

To add support for a new LLM provider:

1. Add the provider type to `LLMProviderType` enum
2. Create a new class implementing `LLMProvider` interface
3. Add configuration properties in `application.properties`
4. Register the provider with Spring `@Component` annotation

See the [LLM Plugin Architecture](backend/src/main/java/com/github/farzadsedaghatbin/shipflow/config/llm/README.md) for details.

## Adding New Vector Store Providers

To add support for a new vector store provider:

1. Add the provider type to `VectorStoreProviderType` enum
2. Create a new class implementing `VectorStoreProvider` interface
3. Add the LangChain4j dependency to `pom.xml`
4. Register the provider with Spring `@Component` annotation

See the [RAG Architecture](RAG_ARCHITECTURE.md) for details on the vector store plugin system.

## Feature Flags

ShipFlow uses Spring Boot property-based feature flags. Set in `application-dev.properties` or via environment variables.

| Flag | Default | Description |
|------|---------|-------------|
| `app.features.qa.enabled` | `true` | Enable QA test-case generation from pitches |
| `app.features.wise-architecture.enabled` | `true` | Enable AI Wise Architecture advisor |
| `app.features.rag.enabled` | `true` | Enable RAG document Q&A |
| `app.features.risk-analysis.enabled` | `true` | Enable AI pitch risk scoring |
| `app.features.ai-cache.enabled` | `true` | Enable Redis-backed AI response cache |
| `app.features.import.csv.enabled` | `true` | Enable CSV import (Jira/Linear/Asana/Generic) |
| `app.features.import.linear.enabled` | `true` | Enable Linear API import via OAuth2 |
| `app.features.import.jira.enabled` | `true` | Enable Jira API import via Atlassian OAuth 2.0 |
| `app.features.notifications.sse.enabled` | `true` | Enable Server-Sent Events real-time notifications |
| `app.features.notifications.email.enabled` | `false` | Enable email notifications (requires SMTP config) |
| `app.features.bulk-operations.enabled` | `true` | Enable bulk task operations |
| `app.features.file-attachments.enabled` | `true` | Enable file attachments on tasks |
| `app.features.csv-export.enabled` | `true` | Enable CSV export from backlog |
| `app.features.saved-filters.enabled` | `true` | Enable saved filter views |
| `app.features.work-log.enabled` | `true` | Enable work log timer |
| `mcp.server.enabled` | `false` | Expose ShipFlow as an MCP server for AI editors |
| `mcp.server.write-enabled` | `false` | Allow write tools via MCP server (requires `mcp.server.enabled`) |
| `mcp.github.enabled` | `false` | Enable GitHub MCP client for Wise Architecture |
| `mcp.figma.enabled` | `false` | Enable Figma MCP client for Wise Architecture |
| `app.ai.cache.ttl-minutes` | `60` | AI cache TTL in minutes |
| `app.rate-limit.trusted-proxies` | `127.0.0.1,::1` | Comma-separated trusted proxy IPs for `X-Forwarded-For` |
| `app.demo-mode.enabled` (`APP_DEMO_MODE`) | `false` | Shows a "Use admin / admin123" hint on the login page. Leave off for real production instances — enable only on a deliberately public demo deployment (e.g. shipflow.dev) |

To disable a flag at runtime, set it to `false` in your `application-dev.properties` or as a `SPRING_APPLICATION_JSON` environment variable in production.
