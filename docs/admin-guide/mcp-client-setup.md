# MCP Client Setup

ShipFlow acts as an MCP server, allowing AI editors (Claude Code, Cursor, Claude Desktop) to query and mutate your project data directly.

::: tip Full guide
See [`MCP_CLIENT_SETUP.md`](https://github.com/farzad-sedaghatbin/ShipFlow/blob/main/MCP_CLIENT_SETUP.md) in the repository for the complete reference including all tool signatures.
:::

## Enable the MCP server

Add to your environment:

```bash
MCP_SERVER_ENABLED=true
MCP_SERVER_WRITE_ENABLED=true   # optional — enables write tools
```

## Generate an API key

1. Go to Organization Settings → API Keys
2. Create a new key with scope `READ` (or `WRITE` for write tools)
3. Copy the key — it is shown only once

## Configure Claude Code

Add to your `~/.claude/settings.json` or project `.claude/settings.json`:

```json
{
  "mcpServers": {
    "shipflow": {
      "type": "http",
      "url": "http://localhost:8080/mcp",
      "headers": {
        "Authorization": "Bearer YOUR_API_KEY"
      }
    }
  }
}
```

## Available tools

### Read tools (10)
`list_projects`, `get_project`, `get_cycles`, `get_cycle`, `get_tasks`, `get_task`, `get_pitches`, `get_pitch_detail`, `get_betting_candidates`, `get_blockers`

### Write tools (5, requires WRITE key + `MCP_SERVER_WRITE_ENABLED=true`)
`create_task`, `create_pitch`, `update_pitch_status`, `update_task_status`, `add_comment`

### Wise Architecture tools (3)
`wise_architecture_list_analyses`, `wise_architecture_get_files`, `wise_architecture_analyze`
