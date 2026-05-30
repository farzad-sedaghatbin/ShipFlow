# ShipFlow MCP Client Setup Guide

Connect AI coding assistants to your ShipFlow instance so they can query projects, cycles,
pitches, tasks, and more — directly from your editor.

---

## Before You Start

### Is the MCP server enabled on your instance?

ShipFlow's MCP server is **opt-in**. The instance owner must enable it.

- **From the UI (recommended, no restart)**: an admin opens **Integrations → MCP → "MCP Server" tab** and toggles **Enable MCP server** on. This DB-backed setting overrides the environment default and takes effect immediately.
- **Self-hosted via environment**: set `MCP_SERVER_ENABLED=true` when starting the backend (see below)
- **shipflow.dev**: check Integrations → MCP → "MCP Server" tab

If you are the instance owner, see [Enabling the MCP Server](#enabling-the-mcp-server) below.

### Generate an API Key

All MCP clients authenticate with a ShipFlow API key.

1. Log in to ShipFlow
2. Go to **Integrations → MCP → "API Keys" tab**
3. Click **Create Key**, give it a name like `claude-code-local`, pick scopes (READ / WRITE / ADMIN) and an optional expiry
4. Copy the `sf_…` key — you will not see it again

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
Analyze the "Push Notifications" pitch using repositories 1 and 3, then implement it.
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

### Read Tools (v0.7.0 — 12 tools)

| Tool | What it returns |
|------|----------------|
| `list_projects` | All accessible projects (id, name, key, type, activeCycleCount) |
| `get_project` | Single project details |
| `get_cycles` | Cycles for a project with phase and dates |
| `get_cycle` | Cycle detail including scope list |
| `get_tasks` | Tasks for a cycle or project — **cycleId or projectId required** |
| `get_task` | Full task detail including blocked-by relationships |
| `get_blockers` | Tasks that are currently blocked within a cycle or project |
| `get_pitches` | Pitches for a project (filterable by status) |
| `get_pitch_detail` | Full pitch: problem, solution, risks, no-gos, **Figma wireframe URLs** |
| `get_betting_candidates` | Shaped pitches ready for the betting table |
| `wise_architecture_list_analyses` | Past Wise Architecture analyses for the current user (filterable by pitchId) |
| `wise_architecture_get_files` | Retrieve generated Markdown implementation guides for a past analysis |
| `get_work_context` | **Full relationship graph** for a pitch or cycle in one call — cycle, pitches, tasks, blockers, hill-chart scopes, and retrospective summaries (provide `pitchId` or `cycleId`) |

### Write Tools (v0.9.0 S18 — 7 tools, requires `MCP_SERVER_WRITE_ENABLED=true` + WRITE-scoped key)

| Tool | What it does |
|------|-------------|
| `create_task` | Create a task in a cycle (cycleId, title required; optional: description, assigneeUsername, priority) |
| `update_task_status` | Change task status (TODO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED) |
| `create_pitch` | Create a new pitch in IDEA status (title required; optional: problemStatement, appetiteDays) |
| `update_pitch_status` | Move a pitch to IDEA, DRAFT, SHAPED, or PENDING |
| `add_comment` | Add a comment to a TASK or BUG_REPORT (entityType, entityId, content required) |
| `wise_architecture_analyze` | Run a Wise Architecture analysis and return agent-ready Markdown guides |
| `create_scope` | Create a Hill Chart scope for a pitch (pitchId, title required; optional: description, progress) |

### Wise Architecture Tools (v0.9.0)

The Wise Architecture tools let AI agents generate and retrieve implementation guides for pitches
without opening the ShipFlow UI.

**End-to-end agent workflow:**

```
# Step 1 — find the pitch
get_pitches(projectId: 5)

# Step 2 — run Wise Architecture (auto-detects stacks if selectedStacks omitted)
wise_architecture_analyze(
  pitchId: 42,
  repositoryIds: [1, 3],          # ShipFlow-connected repo IDs
  selectedStacks: ["BACKEND_JAVA", "WEB_REACT"]   # optional
)

# Returns: list of Markdown files (architecture-overview.md,
#          java-implementation-guide.md, react-implementation-guide.md,
#          api-design.md, implementation-plan.md)

# Step 3 — read the files and implement!
```

**Retrieving a past analysis:**

```
wise_architecture_list_analyses(pitchId: 42)
# → [{ conversationId: "abc-...", techStacks: ["BACKEND_JAVA", "WEB_REACT"], ... }]

wise_architecture_get_files(conversationId: "abc-...")
# → list of Markdown files
```

**Work context graph workflow:**

```
# One call replaces: get_cycle + get_pitches + get_tasks + get_blockers

# By pitch — full graph scoped to that pitch
get_work_context(pitchId: 42)
# → { cycle, pitch, pitches, tasks, taskStatusCounts, blockers, hillChartScopes, retrospectives }

# By cycle — full graph for all pitches in the cycle
get_work_context(cycleId: 5)
# → { cycle, pitches: [...], tasks: [...], taskStatusCounts: { TODO: 3, IN_PROGRESS: 2, ... },
#     blockers: [...], hillChartScopes: [...], retrospectives: [...] }
```

> **Planned tools** (future releases): `search_all`, `get_initiative`, `get_betting_table`.
> See [MCP_SERVER_MILESTONE.md](MCP_SERVER_MILESTONE.md) for the full roadmap.

### Prompt Templates

Prompt templates are planned for a future release and are not yet available.

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

The MCP server is disabled by default.

**The quickest way to enable it is from the UI** — no restart, no env editing: an admin opens
**Integrations → MCP → "MCP Server" tab** and flips **Enable MCP server** on (and optionally
**Enable write tools**). This DB-backed runtime toggle overrides the environment default below.

For headless / infrastructure-as-code deployments, set the following environment variable before
starting the backend instead:

```bash
MCP_SERVER_ENABLED=true
```

> If no admin has touched the UI toggle, this environment value is used as the default.

### Docker Compose (production / self-hosted)

```yaml
# docker-compose.yml
services:
  shipflow-backend:
    image: ghcr.io/farzad-sedaghatbin/shipflow:latest
    environment:
      MCP_SERVER_ENABLED: "true"
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
mcp.server.write-enabled=${MCP_SERVER_WRITE_ENABLED:false}
# Note: MCP endpoints are served under the fixed /mcp path. No base-path config needed.
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
