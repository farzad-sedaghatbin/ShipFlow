# MCP Server Tools

ShipFlow exposes 18 MCP tools across three categories.

::: tip Setup guide
See [MCP Client Setup](/admin-guide/mcp-client-setup) for how to connect your AI editor.
:::

## Read tools (10)

| Tool | Description |
|------|-------------|
| `list_projects` | List all accessible projects |
| `get_project` | Get project details by ID |
| `get_cycles` | List cycles for a project |
| `get_cycle` | Get cycle details and tasks |
| `get_tasks` | List tasks with optional filters |
| `get_task` | Get full task detail |
| `get_pitches` | List pitches with optional status filter |
| `get_pitch_detail` | Get pitch with scopes, tasks, and work logs |
| `get_betting_candidates` | List shaped pitches available for betting |
| `get_blockers` | List tasks and pitches currently blocked |

## Write tools (5)

Require a WRITE-scoped API key and `MCP_SERVER_WRITE_ENABLED=true`.

| Tool | Description |
|------|-------------|
| `create_task` | Create a task in a cycle |
| `create_pitch` | Create a new pitch (IDEA status) |
| `update_pitch_status` | Advance a pitch through IDEA → DRAFT → SHAPED → PENDING |
| `update_task_status` | Change a task's status |
| `add_comment` | Add a comment to a task or bug report |

## Wise Architecture tools (3)

| Tool | Description |
|------|-------------|
| `wise_architecture_list_analyses` | List past analyses (optionally filter by pitchId) |
| `wise_architecture_get_files` | Get generated Markdown files for a conversation |
| `wise_architecture_analyze` | Run a full analysis for a pitch + repositories |
