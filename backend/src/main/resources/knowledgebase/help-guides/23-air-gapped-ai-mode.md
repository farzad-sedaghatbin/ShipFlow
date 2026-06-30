# Air-Gapped AI Mode

Air-gapped mode is a hard guarantee that ShipFlow's AI and integration stack makes **no external network calls**. When it is enabled, ShipFlow only uses a **local LLM (Ollama)** and refuses to talk to any cloud AI provider or external MCP server. This is intended for regulated and isolated deployments (finance, government, defence, healthcare) where data must never leave your network.

## What it enforces

When `AIR_GAPPED_MODE=true`, ShipFlow validates the configuration **at startup** and **refuses to start** if anything would cause external egress:

1. **LLM provider must be local.** The active `AI_PROVIDER` must be `ollama`. Any cloud provider (`openai`, `anthropic`, `runpod`, `google`, `azure_openai`) fails startup with a clear message telling you to switch to `ollama`.
2. **No external MCP clients.** If any outbound MCP client (GitHub, Figma, Notion, Confluence, SharePoint) is enabled, startup fails and names the offending client(s).
3. **Ollama URL must be local/private.** The Ollama base URL host must be loopback (`localhost`, `127.0.0.0/8`, `::1`), an RFC-1918 private range, or an in-cluster hostname (e.g. `*.svc.cluster.local`). An obviously public `https://` URL fails startup.
4. **Ollama reachability preflight.** ShipFlow pings the Ollama endpoint at boot. If it is unreachable it logs a **warning** (it does not fail — Ollama may still be starting).

A runtime guard also prevents an administrator from switching to a cloud LLM provider while air-gapped mode is on.

## Enabling it

Set these environment variables (Docker Compose, Helm `values.yaml`, or your process environment):

```bash
AIR_GAPPED_MODE=true
AI_PROVIDER=ollama
APP_AI_OLLAMA_BASE_URL=http://ollama:11434   # must be local / in-cluster
APP_AI_OLLAMA_MODEL=mistral:instruct
# Ensure all external MCP clients are off:
MCP_GITHUB_ENABLED=false
MCP_FIGMA_ENABLED=false
```

With Docker Compose, the bundled Ollama overlay gives you a local model out of the box:

```bash
docker compose -f docker-compose.yml -f docker-compose.ollama.yml up -d
```

## Checking the status

- **In the app:** when air-gapped mode is active, an **"Air-gapped"** badge (shield icon) appears in the top header bar.
- **Via API:** `GET /api/system/air-gapped` returns the current status:

```json
{
  "enabled": true,
  "activeProvider": "ollama",
  "activeProviderLocal": true,
  "ollamaBaseUrl": "http://ollama:11434",
  "ollamaReachable": true,
  "externalMcpEnabled": []
}
```

`externalMcpEnabled` is empty whenever air-gapped mode is enforced.

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| App won't start, log says provider requires egress | `AI_PROVIDER` is a cloud provider | Set `AI_PROVIDER=ollama` |
| App won't start, log names an MCP client | An external MCP client is enabled | Set its `MCP_*_ENABLED=false` |
| App won't start, log says Ollama URL is public | `APP_AI_OLLAMA_BASE_URL` points to a public host | Use a local/in-cluster URL |
| Badge shows but AI calls fail | Ollama not reachable yet (preflight warned) | Start Ollama / pull the model |

Air-gapped mode pairs with the self-hosting story (see the Helm chart and the self-hosting guide): a fully on-premise deployment with zero data egress, including AI.
