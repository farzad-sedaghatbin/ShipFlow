# Environment Variables Setup

This document describes how to configure environment variables for ShipFlow development and deployment.

## Local Development

1. **Copy the example file:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` with your credentials:**
   ```bash
   # The .env file is git-ignored and safe for local credentials
   nano .env
   ```

3. **Required Variables:**
   - `RUNPOD_BASE_URL` - Your RunPod endpoint URL
   - `RUNPOD_API_KEY` - Your RunPod API key
   - Other optional configurations (see `.env.example`)

## Testing

Tests use mock credentials defined in `backend/src/test/resources/application-test.properties`. No real API keys are needed for tests.

## Production Deployment

Set environment variables through your deployment platform:

### Docker
```bash
docker run -e RUNPOD_API_KEY=your-key -e RUNPOD_BASE_URL=your-url ...
```

### Kubernetes
Use ConfigMaps and Secrets:
```yaml
env:
  - name: RUNPOD_API_KEY
    valueFrom:
      secretKeyRef:
        name: shipflow-secrets
        key: runpod-api-key
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

## Alternative: Ollama (Local AI)

If you want to avoid external API dependencies, use Ollama:

```bash
# In your .env file
AI_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=mistral:instruct
```

Then install and start Ollama:
```bash
brew install ollama
ollama pull mistral
ollama serve
```
