# MCP Usage Report

The MCP Usage Report gives admins a full picture of how external MCP clients (Claude Code, Cursor, Claude Desktop, etc.) are using the ShipFlow API.

## Accessing the report

1. Go to **Integrations → MCP** in the sidebar.
2. Open the **MCP Server** tab.
3. Click **View Usage Report** (visible to ADMIN users only).

The report is at `/app/integrations/mcp-usage`.

## Summary cards

At the top of the page, six cards show at-a-glance stats:

| Card | What it shows |
|------|--------------|
| **Total Calls** | All `tools/call` invocations ever recorded |
| **Success Rate** | % of calls that completed without error |
| **Failures** | Count of failed calls |
| **Active Users** | Distinct users who called a tool in the last 30 days |
| **Unique Tools** | How many distinct tool names have been called |
| **Successful Calls** | Absolute count of successful calls |

## Tabs

### Timeline
A 30-day area chart showing daily call volume (total and successful). Useful for spotting usage spikes or drops.

### By User
Table ranking every user by total MCP calls with their success rate and last-active timestamp. Color-coded success rate: green ≥ 90%, yellow ≥ 70%, red < 70%.

### By Tool
Horizontal bar chart + table showing call counts per tool. Use this to see which tools are most-used and which have high error rates.

### Recent Logs
The last 50 tool calls in reverse chronological order, showing: time, username, tool name, success/error badge (with the error message on hover), API key prefix (first 8 chars), and call duration in milliseconds.

## Refreshing data

Click **Refresh** in the top-right corner to reload all panels. Data is updated in real time — every tool call is recorded asynchronously so it never slows down the MCP response.

## Permissions

Only users with the **ADMIN** role can access this page. Regular users and API keys without `ADMIN` scope will receive a 403 Forbidden response from the backend endpoints.

## API endpoints (for scripting)

All endpoints require a JWT or API key with `ADMIN` scope:

```
GET /api/admin/mcp/usage/summary
GET /api/admin/mcp/usage/by-user
GET /api/admin/mcp/usage/by-tool
GET /api/admin/mcp/usage/timeline?days=30
GET /api/admin/mcp/usage/recent?limit=50
```
