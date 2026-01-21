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
| Anthropic | `anthropic` | Coming soon | Yes |
| Google | `google` | Coming soon | Yes |

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
   OPENAI_MODEL=gpt-4o-mini
   # Optional: gpt-4o for more complex tasks
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
OPENAI_MODEL=gpt-4o-mini          # or gpt-4o, gpt-3.5-turbo
OPENAI_TIMEOUT=120
OPENAI_ORG_ID=                     # Optional: Organization ID
OPENAI_BASE_URL=                   # Optional: For Azure OpenAI or proxies
```

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
