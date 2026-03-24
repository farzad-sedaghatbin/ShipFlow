# ShipFlow MCP Server — Milestone Plan

## Vision

Turn ShipFlow into an **MCP (Model Context Protocol) server** so that AI coding assistants —
Claude Code, Cursor, GitHub Copilot, and custom agents — can query and drive ShipFlow directly.

> A developer types: *"What tasks are blocking Cycle 12?"* or *"Create a task for the auth bug under the Login pitch"*
> — and their AI assistant does it through ShipFlow's MCP server without leaving the editor.

---

## Current State

ShipFlow is already an **MCP client** (consuming external servers):

| File | Role |
|------|------|
| `service/mcp/McpClientService.java` | Interface for consuming MCP servers |
| `service/mcp/GitHubMcpProvider.java` | Reads code from GitHub MCP |
| `service/mcp/FigmaMcpProvider.java` | Reads designs from Figma MCP |
| `service/mcp/McpConfig.java` | Config for consumed MCP servers |

**What does NOT exist yet** — the server side:
- No MCP server transport (SSE or stdio)
- No `@Tool`-annotated methods exposed as MCP tools
- No MCP resource definitions for ShipFlow entities
- No MCP-specific authentication flow
- No `claude_desktop_config.json` or client setup guide

---

## Gap Analysis

### GAP 1 — MCP Server Transport Layer

**What's missing**: ShipFlow has no SSE endpoint or stdio adapter. MCP requires one of:
- **HTTP + SSE** transport (server-sent events on `/mcp/sse`, JSON-RPC on `/mcp/messages`)
- **stdio** transport (for local process-based MCP servers)

**Impact**: Blocking — no transport = no MCP server.

**Plan**:
- Add `spring-ai-mcp-server-spring-boot-starter` to `pom.xml`
- Configure SSE transport at `/mcp` base path
- Keep separate from existing REST API (`/api/`)

---

### GAP 2 — No MCP Tool Definitions

**What's missing**: No methods annotated with `@Tool` (Spring AI MCP) or equivalent, exposing
ShipFlow domain operations to MCP clients.

**Tools needed** (priority order):

#### Read Tools (no side effects)
| Tool Name | Description | Backed by |
|-----------|-------------|-----------|
| `get_cycles` | List cycles for a project | `CycleService` |
| `get_cycle_detail` | Get cycle with pitches, tasks | `CycleService` |
| `get_pitches` | List pitches with status filter | `PitchService` |
| `get_pitch_detail` | Full pitch (problem, solution, risks…) | `PitchService` |
| `get_tasks` | List tasks for a cycle/pitch/scope | `TaskService` |
| `get_task_detail` | Task with dependencies and comments | `TaskService` |
| `get_hill_chart` | Hill chart state for a cycle | `HillChartService` |
| `get_betting_table` | Current betting table | `BettingTableService` |
| `get_project` | Project metadata | `ProjectService` |
| `list_projects` | All projects in organization | `ProjectService` |
| `get_blockers` | Tasks blocking other tasks | `TaskDependencyService` |
| `get_retrospective` | Retrospective entries for a cycle | `RetrospectiveService` |
| `search_all` | Full-text search across entities | `GlobalSearchService` |
| `get_release` | Release details and linked cycles | `ReleaseService` |
| `get_initiative` | Initiative with epics | `InitiativeService` |

#### Write Tools (mutating)
| Tool Name | Description | Backed by |
|-----------|-------------|-----------|
| `create_task` | Create a task in a cycle | `TaskService` |
| `update_task_status` | Change task status (TODO→IN_PROGRESS…) | `TaskService` |
| `add_comment` | Add a comment to any entity | `CommentService` |
| `create_pitch` | Draft a new pitch | `PitchService` |
| `update_hill_chart` | Move a scope on the hill chart | `HillChartService` |
| `log_retrospective_entry` | Add a retrospective item | `RetrospectiveService` |

**Impact**: Core value — without tools, MCP server is useless.

---

### GAP 3 — No MCP Resource Definitions

**What's missing**: MCP supports *resources* (static or templated URIs representing data) separately
from tools. ShipFlow entities are natural resources.

**Resources to define**:

```
shipflow://org/{orgId}/projects           → list of projects
shipflow://project/{projectId}/cycles     → cycles list
shipflow://cycle/{cycleId}                → cycle detail
shipflow://pitch/{pitchId}                → pitch detail
shipflow://task/{taskId}                  → task detail
shipflow://cycle/{cycleId}/hillchart      → hill chart state
shipflow://cycle/{cycleId}/betting-table  → betting table
```

**Impact**: Medium — resources improve discoverability and AI context quality.

---

### GAP 4 — MCP Authentication

**What's missing**: ShipFlow uses JWT bearer tokens for its REST API. MCP clients connect via SSE
and need a way to authenticate. The existing API key infrastructure (`ApiKeyService`) exists but
is not wired to an MCP transport.

**Plan**:
- Reuse existing API key system: client sends `Authorization: Bearer <api-key>` on SSE connect
- MCP session bound to the API key's user/org context
- Scope down: read-only vs read-write API key types
- Add `MCP_SCOPE` permission in the existing RBAC matrix

**Impact**: Blocking for production use. Dev/local can run without auth for testing.

---

### GAP 5 — No Spring AI MCP Server Dependency

**What's missing**: The `pom.xml` does not include Spring AI or the MCP server starter.

```xml
<!-- MISSING from pom.xml -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-mcp-server-spring-boot-starter</artifactId>
  <version>1.0.0-M6</version>
</dependency>
```

LangChain4j (already in pom.xml at 0.35.0) does not provide an MCP server implementation.
Spring AI's MCP module does.

**Impact**: Blocking — no library = no `@Tool` annotations.

**Alternative**: Implement MCP protocol from scratch (JSON-RPC 2.0 over SSE). Feasible but high effort.

---

### GAP 6 — No MCP Capability Manifest

**What's missing**: MCP servers expose a `initialize` response listing their capabilities
(`tools`, `resources`, `prompts`). This does not exist.

**Plan**: Spring AI MCP starter auto-generates this from `@Tool` / `@McpResource` annotations.
Manual implementation otherwise requires a `McpServerCapabilities` bean.

---

### GAP 7 — No Prompt Templates

**What's missing**: MCP supports *prompts* — pre-built templates clients can invoke.
ShipFlow's AI features (risk analysis, test generation, Wise Architecture) are perfect candidates.

**Prompts to expose**:
| Prompt Name | Description |
|-------------|-------------|
| `analyze_pitch_risks` | Run AI risk analysis on a pitch |
| `generate_test_cases` | Generate QA test cases for a pitch |
| `summarize_cycle` | Generate a cycle summary for stakeholders |
| `wise_architecture_advice` | Get technical implementation advice |
| `retrospective_summary` | Summarize a retrospective |

---

### GAP 8 — No Integration Tests for MCP

**What's missing**: No test harness to:
- Start the MCP server in test mode
- Connect a test MCP client
- Assert tool call input/output

**Plan**: Use `McpClient` (Spring AI test utilities) or `mcp-sdk` (Node.js) for end-to-end tests.

---

### GAP 9 — No Client Configuration Guide

**What's missing**: No instructions for developers on how to connect Claude Code, Claude Desktop,
or Cursor to ShipFlow's MCP server.

**Plan**: Create `MCP_CLIENT_SETUP.md` with configs for:
- Claude Desktop (`claude_desktop_config.json`)
- Claude Code (`.claude/settings.json` MCP server entry)
- Cursor (`.cursor/mcp.json`)
- Generic HTTP+SSE client

---

### GAP 10 — No Developer-Facing ShipFlow MCP SDK / Wrapper

**What's missing**: A lightweight SDK or OpenAPI-generated client developers can use if they want
to build their own MCP server atop ShipFlow's REST API, without running Java.

**Plan**: Document the REST API as the "build your own MCP tool" path; provide example Node.js
and Python MCP server skeletons in a `examples/mcp-server/` directory.

---

## Proposed Architecture

```
External AI Client (Claude Code / Cursor / etc.)
        │
        │  MCP over HTTP+SSE
        ▼
┌────────────────────────────────────────────┐
│  ShipFlow MCP Server Layer                 │
│  ─────────────────────────────             │
│  McpServerController  (SSE endpoint)       │
│  McpToolsProvider     (@Tool methods)       │
│  McpResourceProvider  (@McpResource)        │
│  McpPromptsProvider   (prompt templates)    │
│  McpAuthFilter        (API key → JWT)       │
└────────────────────────────────────────────┘
        │
        │  Internal Java calls
        ▼
┌────────────────────────────────────────────┐
│  Existing Service Layer (unchanged)        │
│  CycleService / PitchService / TaskService │
│  RetrospectiveService / HillChartService   │
│  … 86 services total                       │
└────────────────────────────────────────────┘
        │
        ▼
┌────────────────────────────────────────────┐
│  PostgreSQL + Redis                        │
└────────────────────────────────────────────┘
```

Key design decisions:
- **MCP server is additive** — zero changes to existing REST API or service layer
- **MCP tools delegate to services** — no business logic in MCP layer
- **Auth is orthogonal** — API key filter translates to SecurityContext before tools execute
- **Read-only by default** — write tools require explicit `mcp_write` permission

---

## Implementation Roadmap

### Phase 1 — Foundation (Week 1–2)

- [ ] Add Spring AI MCP Server starter to `pom.xml`
- [ ] Configure SSE transport (`/mcp` base path)
- [ ] Wire API key authentication to MCP session
- [ ] Expose 3 read tools: `list_projects`, `get_cycles`, `get_tasks`
- [ ] Return valid JSON-RPC 2.0 responses
- [ ] Basic integration test with `McpClient`

**Definition of done**: Claude Desktop can connect and list projects.

### Phase 2 — Core Read Tools (Week 3–4)

- [ ] All 15 read tools listed in GAP 2
- [ ] MCP resource URIs for project, cycle, pitch, task
- [ ] Tool input validation (Jakarta Bean Validation)
- [ ] Error responses follow MCP error spec
- [ ] Test coverage ≥ 80% for MCP layer

**Definition of done**: Claude Code can answer "what's in Cycle 12?" from inside the editor.

### Phase 3 — Write Tools + Prompts (Week 5–6)

- [ ] 6 write tools (create_task, update_task_status, add_comment, …)
- [ ] Prompt templates for AI features
- [ ] Permission enforcement (`mcp_write` scope)
- [ ] Audit log entries for MCP mutations (Envers)
- [ ] Rate limiting on write tools

**Definition of done**: Claude Code can create a task via chat.

### Phase 4 — Developer Experience (Week 7)

- [ ] `MCP_CLIENT_SETUP.md` with configs for Claude Desktop, Claude Code, Cursor
- [ ] `examples/mcp-server/` — Node.js skeleton using ShipFlow REST API
- [ ] OpenAPI spec updated with MCP-specific endpoints
- [ ] In-app MCP status page (extend existing `McpStatusDTO`)

---

## File Creation Plan

When implementing, create files in this order:

```
backend/src/main/java/.../
├── mcp/server/
│   ├── McpServerConfig.java          # Spring AI MCP server bean configuration
│   ├── McpAuthFilter.java            # API key → SecurityContext
│   ├── ShipFlowMcpToolsProvider.java # All @Tool methods
│   ├── ShipFlowMcpResourceProvider.java # @McpResource definitions
│   └── ShipFlowMcpPromptsProvider.java  # Prompt templates
└── dto/mcp/
    ├── McpCycleDTO.java              # Compact DTOs for MCP responses
    ├── McpPitchDTO.java
    ├── McpTaskDTO.java
    └── McpProjectDTO.java

backend/src/test/java/.../mcp/
    └── McpServerIntegrationTest.java # End-to-end MCP tool tests

backend/src/main/resources/
    └── application.properties        # mcp.server.enabled, mcp.server.base-path additions
```

---

## MCP Tool Example (Target State)

```java
@Component
public class ShipFlowMcpToolsProvider {

    private final CycleService cycleService;
    private final TaskService taskService;

    @Tool(description = "List all cycles for a project. Returns cycle id, name, status, start/end dates.")
    public List<McpCycleDTO> getCycles(
            @ToolParam(description = "Project UUID") String projectId,
            @ToolParam(description = "Filter by status: ACTIVE, COMPLETED, UPCOMING") String status) {

        return cycleService.getCyclesForProject(UUID.fromString(projectId), status)
                .stream()
                .map(McpCycleDTO::from)
                .toList();
    }

    @Tool(description = "Create a task in a cycle. Returns the created task id.")
    public McpTaskDTO createTask(
            @ToolParam(description = "Cycle UUID") String cycleId,
            @ToolParam(description = "Task title") String title,
            @ToolParam(description = "Optional pitch UUID to link") String pitchId) {

        // Auth check delegated to Spring Security — method is @PreAuthorize protected
        var task = taskService.createTask(cycleId, title, pitchId);
        return McpTaskDTO.from(task);
    }
}
```

---

## Claude Code MCP Config (Target State)

Once the server is running, developers add to `.claude/settings.json`:

```json
{
  "mcpServers": {
    "shipflow": {
      "type": "sse",
      "url": "http://localhost:8080/mcp/sse",
      "headers": {
        "Authorization": "Bearer <your-shipflow-api-key>"
      }
    }
  }
}
```

Then in Claude Code chat:
```
> What pitches are in the betting table for project X?
> Create a task "Fix login redirect" under Cycle 12 linked to pitch "Auth Revamp"
> Show me the hill chart for the current cycle
```

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Tools exposed | ≥ 15 read + 6 write |
| MCP layer test coverage | ≥ 80% |
| Tool call latency (p99) | < 500ms |
| Auth overhead | < 10ms |
| Claude Desktop connection | Verified working |
| Claude Code connection | Verified working |

---

## Related Files

- `CLAUDE.md` — Claude Code setup and coding conventions
- `ENVIRONMENT_SETUP.md` — local dev environment
- `PERMISSION_MATRIX.md` — RBAC roles (MCP scope to be added)
- `API_CONTRACT_GENERATION.md` — OpenAPI docs (MCP endpoints to be added)
- `WISE_ARCHITECTURE.md` — AI feature that will get a prompt template
- `service/mcp/McpClientService.java` — existing MCP client interface (do not confuse with server)
