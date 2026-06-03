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

### Read Tools

| Tool | What it returns |
|------|----------------|
| `whoami` | Identity of the authenticated MCP caller — username, role, userId, **personId** (use this for `assigneeId` filters), fullName, email |
| `list_projects` | All accessible projects (id, name, key, type, activeCycleCount) |
| `get_project` | Single project details |
| `get_cycles` | Cycles for a project with phase and dates |
| `get_cycle` | Cycle detail including scope list |
| `get_tasks` | Tasks filtered by **any combination** of `cycleId`, `projectId`, `pitchId`, `assigneeId`, or `mine: true`. At least one scope required. |
| `get_task` | Full task detail including blocked-by relationships |
| `get_blockers` | Tasks that are currently blocked within a cycle, project, pitch, or for a given assignee/mine |
| `get_test_cases` | Test cases (acceptance criteria) linked to a task, pitch, or cycle — preconditions, steps, expectedResult |
| `get_test_case` | Single test case by ID |
| `get_test_runs` | Execution history of a test case — status, notes, actualResult, linked bug |
| `get_bug_reports` | Bug reports linked to a task, pitch, or cycle — severity, status, repro steps |
| `get_bug_report` | Single bug report by ID |
| `get_pitches` | Pitches for a project (filterable by status) |
| `get_pitch_detail` | Full pitch: problem, solution, risks, no-gos, **Figma wireframe URLs** |
| `get_betting_candidates` | Shaped pitches ready for the betting table |
| `wise_architecture_list_analyses` | Past Wise Architecture analyses for the current user (filterable by pitchId) |
| `wise_architecture_get_files` | Retrieve generated Markdown implementation guides for a past analysis |
| `get_work_context` | **Full relationship graph** for a pitch or cycle in one call — cycle, pitches, tasks, blockers, hill-chart scopes, and retrospective summaries (provide `pitchId`, `cycleId`, or `taskId` — `taskId` resolves to the task's parent pitch or cycle) |
| `get_task_context` | **Task-rooted aggregator for coding agents** — given a single `taskId`, returns the task (with dependency graph and subtasks), its parent pitch (Shape Up fields + `wireframeLinks`), parent cycle, sibling tasks under the same pitch, and a server-generated `hints` array (Figma URL guidance, blocked-by detail, thin-context warnings). Use this instead of stitching `get_task` + `get_pitch_detail` + `get_tasks` when the goal is "implement this task". |

### Write Tools (v0.9.0 S18 — 7 tools, requires `MCP_SERVER_WRITE_ENABLED=true` + WRITE-scoped key)

| Tool | What it does |
|------|-------------|
| `create_task` | Create a task in a cycle (cycleId, title required; optional: description, pitchId, **parentTaskId** for subtasks, assigneeUsername, priority) |
| `update_task_status` | Change task status (TODO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED) |
| `update_task_assignee` | Reassign an existing task — by `assigneeUsername`, `assigneeId`, or `mine: true`; or clear with `unassign: true` |
| `create_pitch` | Create a new pitch in IDEA status (title required; optional: problemStatement, appetiteDays) |
| `update_pitch_status` | Move a pitch to IDEA, DRAFT, SHAPED, or PENDING |
| `add_comment` | Add a comment to a TASK or BUG_REPORT (entityType, entityId, content required) |
| `wise_architecture_analyze` | Run a Wise Architecture analysis and return agent-ready Markdown guides |
| `create_scope` | Create a Hill Chart scope for a pitch (pitchId, title required; optional: description, progress) |
| `record_test_run` | Record the result of executing a test case — status (PASSED/FAILED/BLOCKED/SKIPPED/PENDING/RUNNING), notes, actualResult, buildVersion, environment |
| `update_bug_status` | Update a bug report's status (OPEN, IN_PROGRESS, RESOLVED, VERIFIED, CLOSED, REOPENED, WONT_FIX, DUPLICATE) and optional resolution text |

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

# From a task — get_work_context resolves the task's parent pitch/cycle
get_work_context(taskId: 8)
```

**Task context workflow (for coding agents):**

When an agent is asked to implement a specific task, `get_task_context` is the canonical single
call. It carries everything `get_task` + `get_pitch_detail` + `get_tasks` would, plus a `hints`
array that tells the agent how to use the payload.

```
get_task_context(taskId: 8)
# → {
#     task:    { id, title, description, status, blockingTasks: [...], blockedByTasks: [...],
#                children: [...], pitchId, ... },
#     pitch:   { problemStatement, solution, rabbitHoles, risks, noGos, wireframeLinks, ... },
#     cycle:   { id, name, projectName, ... },
#     siblings:            [ ...other tasks under the same pitch ],
#     siblingTotalCount:   7,
#     siblingsTruncated:   false,
#     siblingStatusCounts: { TODO: 3, IN_PROGRESS: 2, DONE: 2 },
#     hints: [
#       "pitch.wireframeLinks is present — fetch the design via a Figma MCP before implementing.",
#       "task is BLOCKED by 2 task(s) — resolve dependencies before starting; see task.blockedByTasks for IDs."
#     ]
#   }

# Cap siblings for very large pitches (default 50, max 200)
get_task_context(taskId: 8, siblingLimit: 20)

# Skip siblings if the agent only needs the task + pitch
get_task_context(taskId: 8, includeSiblings: false)
```

> **Figma boundary**: `pitch.wireframeLinks` is a URL, not the design. ShipFlow's MCP hands the
> pointer to the agent — turning that URL into design context still requires a Figma-capable tool
> (Figma MCP, browser extension, etc.) alongside ShipFlow's MCP. The `hints` array surfaces this
> explicitly whenever `wireframeLinks` is populated.

**"My work" workflow:**

```
# One-shot — no whoami needed
get_tasks(mine: true, cycleId: 5)
# → tasks assigned to the authenticated caller in cycle 5

# Combined filters compose
get_tasks(mine: true, pitchId: 10)         # my tasks under pitch 10
get_tasks(assigneeId: 42, projectId: 1)    # someone else's tasks in a project

# Explicit identity lookup (when the agent needs to display "I'm working as ...")
whoami()
# → { userId, username, email, role, personId, fullName }

# Claim an unassigned task — no separate whoami call needed
update_task_assignee(taskId: 8, mine: true)

# Reassign to a specific teammate
update_task_assignee(taskId: 8, assigneeUsername: "alice")

# Release a task you can't take
update_task_assignee(taskId: 8, unassign: true)
```

**QA / verification workflow:**

```
# Step 1 — read the acceptance criteria attached to the task
get_test_cases(taskId: 8)
# → [{ id, title, preconditions, steps, expectedResult, priority, status, ... }]

# Step 2 — implement against those criteria, then record the outcome
record_test_run(
  testCaseId: 1,
  status: "PASSED",
  notes: "Verified click event reaches analytics endpoint",
  buildVersion: "1.2.0-rc1",
  environment: "staging"
)

# Step 3 — check past runs (e.g. before re-running a flaky test)
get_test_runs(testCaseId: 1)
```

**Bug triage workflow:**

```
# Read all bugs on a task before implementing a fix
get_bug_reports(taskId: 8)
# → [{ bugKey, title, severity, status, stepsToReproduce, expectedBehavior, actualBehavior, ... }]

# After fixing, update status (the resolvedAt timestamp is stamped automatically for RESOLVED/VERIFIED/CLOSED)
update_bug_status(bugReportId: 1, status: "RESOLVED", resolution: "Fixed in commit abc123")
```

**Subtask workflow:**

```
# Create a parent task...
create_task(cycleId: 5, title: "Implement click tracking", pitchId: 10)
# → { id: 8, ... }

# ...then add subtasks underneath
create_task(cycleId: 5, parentTaskId: 8, title: "Wire frontend dispatcher")
create_task(cycleId: 5, parentTaskId: 8, title: "Add backend ingestion endpoint")
# Subtasks appear in get_task_context(taskId: 8).task.children
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
