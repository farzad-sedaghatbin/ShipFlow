Scaffold a new ShipFlow MCP server tool. Tool name and description: $ARGUMENTS

Follow this workflow — show the plan first, then implement:

**Plan phase** (before writing any code):
1. Identify the correct `*McpTools.java` class in `backend/src/main/java/com/github/farzadsedaghatbin/shipflow/service/mcp/server/tools/` based on the domain (Task, Pitch, Cycle, Project, Comment, WiseArchitecture).
2. Classify as READ or WRITE tool.
3. Identify which existing service method(s) to delegate to (never bypass the service layer).
4. Confirm the plan with a summary before proceeding.

**Implementation** (after plan is confirmed):
1. Add `static Map<String, Object> get{ToolName}Definition()` and the instance method to the correct `*McpTools.java`.
2. Register in `McpToolDispatcher.java` — add to `READ_TOOLS` or `WRITE_TOOLS` map.
3. For write tools: verify `properties.isWriteEnabled()` is checked before dispatching.
4. Add a unit test in `McpToolDispatcherTest` (no Spring context needed).
5. Add a row to the tool reference table in `MCP_CLIENT_SETUP.md`.

After implementation, run `cd backend && ./mvnw test -pl . -Dtest=McpToolDispatcherTest` to verify the new test passes.
