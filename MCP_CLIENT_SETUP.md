# ShipFlow MCP Client Setup Guide

Connect AI coding assistants to your ShipFlow instance so they can query projects, cycles,
pitches, tasks, and more — directly from your editor.

---

## Before You Start

### Is the MCP server enabled on your instance?

ShipFlow's MCP server is **opt-in**. The instance owner must enable it.

- **Self-hosted**: set `MCP_SERVER_ENABLED=true` when starting the backend (see below)
- **shipflow.dev**: check the Admin panel → Integrations → MCP Server

If you are the instance owner, see [Enabling the MCP Server](#enabling-the-mcp-server) below.

### Generate an API Key

All MCP clients authenticate with a ShipFlow API key.

1. Log in to ShipFlow
2. Go to **Settings → API Keys**
3. Click **Create Key**, give it a name like `claude-code-local`
4. Copy the key — you will not see it again

> Treat API keys like passwords. Do not commit them to git.

---

## Claude Code

Add to `.claude/settings.json` in your project or user settings:

```json
{
  "mcpServers": {
    "shipflow": {
      "type": "sse",
      "url": "https://your-shipflow-instance.example.com/mcp/sse",
      "headers": {
        "Authorization": "Bearer sf_live_xxxxxxxxxxxxxxxxxxxx"
      }
    }
  }
}
```

**For local development** (backend on localhost):

```json
{
  "mcpServers": {
    "shipflow": {
      "type": "sse",
      "url": "http://localhost:8080/mcp/sse",
      "headers": {
        "Authorization": "Bearer sf_dev_xxxxxxxxxxxxxxxxxxxx"
      }
    }
  }
}
```

After saving, restart Claude Code or run `/mcp` to verify the connection.

**Example prompts once connected:**
```
What tasks are blocking the current cycle?
List all pitched ideas waiting for the betting table.
Create a task "Fix password reset flow" in Cycle 12 under the Auth Revamp pitch.
Show me the hill chart status for the active cycle.
Summarize the retrospective from the last cycle.
```

---

## Claude Desktop

Edit the Claude Desktop config file:

| OS | Path |
|----|------|
| macOS | `~/Library/Application Support/Claude/claude_desktop_config.json` |
| Windows | `%APPDATA%\Claude\claude_desktop_config.json` |
| Linux | `~/.config/Claude/claude_desktop_config.json` |

```json
{
  "mcpServers": {
    "shipflow": {
      "type": "sse",
      "url": "https://your-shipflow-instance.example.com/mcp/sse",
      "headers": {
        "Authorization": "Bearer sf_live_xxxxxxxxxxxxxxxxxxxx"
      }
    }
  }
}
```

Restart Claude Desktop after saving. You will see **ShipFlow** appear in the tools panel on the left.

---

## Cursor

Create or edit `.cursor/mcp.json` in your project root:

```json
{
  "mcpServers": {
    "shipflow": {
      "url": "https://your-shipflow-instance.example.com/mcp/sse",
      "headers": {
        "Authorization": "Bearer sf_live_xxxxxxxxxxxxxxxxxxxx"
      }
    }
  }
}
```

Or configure globally in Cursor settings → **MCP** → Add Server.

After saving, Cursor will show ShipFlow tools in the Composer tool panel. Restart Cursor if the
server does not appear.

---

## Windsurf (Codeium)

Edit `~/.codeium/windsurf/mcp_config.json`:

```json
{
  "mcpServers": {
    "shipflow": {
      "serverUrl": "https://your-shipflow-instance.example.com/mcp/sse",
      "headers": {
        "Authorization": "Bearer sf_live_xxxxxxxxxxxxxxxxxxxx"
      }
    }
  }
}
```

---

## Generic HTTP+SSE Client

Any MCP client that supports HTTP+SSE transport can connect.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/mcp/sse` | GET | SSE stream — open this to establish the MCP session |
| `/mcp/messages` | POST | Send JSON-RPC 2.0 requests |
| `/mcp/health` | GET | Server health check (no auth required) |

Connection headers:
```
Authorization: Bearer <api-key>
Accept: text/event-stream        (for /mcp/sse)
Content-Type: application/json   (for /mcp/messages)
```

---

## Available MCP Tools

Once connected, your AI assistant has access to these tools:

### Read Tools

| Tool | What it returns |
|------|----------------|
| `list_projects` | All projects in the organization |
| `get_project` | Single project details |
| `get_cycles` | Cycles for a project (filter by status) |
| `get_cycle_detail` | Cycle with its pitches, tasks, and hill chart |
| `get_pitches` | Pitches (filter by status: IDEA, SHAPED, PITCHED…) |
| `get_pitch_detail` | Full pitch: problem, solution, risks, no-gos |
| `get_tasks` | Tasks for a cycle, pitch, or scope |
| `get_task_detail` | Task with dependencies and comments |
| `get_blockers` | Tasks that are blocking other tasks |
| `get_hill_chart` | Hill chart scope positions for a cycle |
| `get_betting_table` | Current betting table candidates |
| `get_retrospective` | Retrospective entries for a cycle |
| `get_release` | Release details and linked cycles |
| `get_initiative` | Initiative with its epics |
| `search_all` | Full-text search across all entities |

### Write Tools

| Tool | What it does |
|------|-------------|
| `create_task` | Create a task in a cycle |
| `update_task_status` | Change task status (TODO, IN_PROGRESS, DONE…) |
| `add_comment` | Add a comment to any entity |
| `create_pitch` | Draft a new pitch |
| `update_hill_chart` | Move a scope position on the hill chart |
| `log_retrospective_entry` | Add a retrospective item |

> Write tools require your API key to have the `mcp_write` permission.
> Read-only keys can only call read tools.

### Prompt Templates

| Prompt | What it does |
|--------|-------------|
| `analyze_pitch_risks` | AI risk analysis for a pitch |
| `generate_test_cases` | Generate QA test cases from a pitch |
| `summarize_cycle` | Stakeholder-ready cycle summary |
| `wise_architecture_advice` | Technical implementation advice |
| `retrospective_summary` | Summarize a retrospective |

---

## Verifying the Connection

### From Claude Code

```
/mcp
```

You should see `shipflow` listed with a green status and the available tools count.

### From the terminal

```bash
curl -N \
  -H "Authorization: Bearer sf_live_xxx" \
  -H "Accept: text/event-stream" \
  http://localhost:8080/mcp/sse
```

You should see an SSE stream starting with `event: endpoint`.

### Health check (no auth)

```bash
curl http://localhost:8080/mcp/health
# {"status":"UP","mcpServer":{"enabled":true,"tools":21}}
```

---

## Enabling the MCP Server

> This section is for **instance owners / self-hosters** who want to turn the MCP server on.
> If you are a developer connecting to an already-running instance, skip this section.

The MCP server is disabled by default. To enable it, set the following environment variable
before starting the backend:

```bash
MCP_SERVER_ENABLED=true
```

### Docker Compose (production / self-hosted)

```yaml
# docker-compose.yml
services:
  shipflow-backend:
    image: ghcr.io/farzad-sedaghatbin/shipflow:latest
    environment:
      MCP_SERVER_ENABLED: "true"
      MCP_SERVER_BASE_PATH: "/mcp"          # optional, default: /mcp
      MCP_SERVER_WRITE_ENABLED: "true"      # optional: allow write tools
      # ... other env vars
```

### Spring Boot (local dev)

```bash
MCP_SERVER_ENABLED=true ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Or use the **`ShipFlow Backend (MCP enabled)`** launch config in VS Code.

### application.properties

```properties
# Opt-in: MCP server is disabled by default
mcp.server.enabled=${MCP_SERVER_ENABLED:false}
mcp.server.base-path=${MCP_SERVER_BASE_PATH:/mcp}
mcp.server.write-enabled=${MCP_SERVER_WRITE_ENABLED:false}
```

### When NOT to enable the MCP server

- You do not use AI coding assistants
- You are running a minimal/embedded instance with limited resources
- Your network policy does not allow SSE connections to the backend
- You are on a shared/public instance where you cannot control who gets API keys

The MCP server adds an SSE endpoint but does not affect any other ShipFlow functionality.
You can enable or disable it at any time with a restart.

---

## Troubleshooting

### "Connection refused" or "Network error"

- Confirm the backend is running: `curl http://localhost:8080/api/health`
- Confirm MCP is enabled: `curl http://localhost:8080/mcp/health`
- Check `MCP_SERVER_ENABLED=true` in your environment

### "401 Unauthorized"

- Verify the API key is correct (copy-paste, no trailing spaces)
- Confirm the key has not been revoked (Settings → API Keys)
- Ensure the `Authorization` header format is `Bearer <key>` (with the `Bearer ` prefix)

### "403 Forbidden" on write tools

- Your API key may be read-only — regenerate with `mcp_write` scope enabled
- Check your ShipFlow role: write tools require `DEVELOPER` role or above

### Tools not appearing in Claude Code

- Run `/mcp` to see the server status
- Restart Claude Code after editing `settings.json`
- Check for JSON syntax errors in `settings.json`

### SSE connection drops after a few seconds

- This is normal for some proxies/load balancers that timeout idle connections
- Configure your reverse proxy to increase SSE timeout (nginx: `proxy_read_timeout 3600s;`)
- The MCP client will reconnect automatically

---

## Security Considerations

- **Use HTTPS in production.** Never send API keys over plain HTTP on a public network.
- **Create separate keys per tool.** One key for Claude Code, one for Claude Desktop, etc.
- **Read-only by default.** Only enable write tools if you need them.
- **Rotate keys regularly.** Revoke and regenerate API keys periodically.
- **Do not share keys.** Each developer should have their own key.
- **Key scope**: keys are scoped to your user account and can only access data your account can access.

---

## Related Documentation

| File | Topic |
|------|-------|
| `MCP_SERVER_MILESTONE.md` | Implementation plan and gap analysis |
| `CLAUDE.md` | Claude Code project guide |
| `VSCODE_GUIDE.md` | Full VS Code developer setup |
| `ENVIRONMENT_SETUP.md` | Local dev environment |
| `SECURITY.md` | Security considerations |
| `API_CONTRACT_GENERATION.md` | REST API documentation |
