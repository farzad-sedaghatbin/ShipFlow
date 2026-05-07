Scaffold a new ShipFlow MCP server tool. Tool name and description: $ARGUMENTS

Follow this workflow — show the plan first, then implement:

**Plan phase** (before writing any code):
1. Locate the current MCP tool implementation files in the backend — search for tool handler classes, a dispatcher or router, and any MCP setup docs. Do not assume specific file names; discover them from the codebase.
2. Identify which domain class handles the relevant feature (Task, Pitch, Cycle, Project, Comment, etc.) based on the current implementation.
3. Classify as READ or WRITE tool.
4. Identify which existing service method(s) to delegate to — never bypass the service layer or call repositories directly.
5. Confirm the plan by naming the exact files you will edit before writing any code.

**Implementation** (after plan is confirmed):
1. Add the tool method to the correct existing tool class following the existing code pattern.
2. Register it in the current dispatcher/router — add to the READ or WRITE tool collection.
3. For write tools: verify the existing write-enabled guard is checked before dispatching.
4. Add or update a unit test in the current MCP dispatcher test file (no Spring context needed).
5. Update any MCP client/setup documentation that exists; if none exists yet, add a note in the appropriate docs file.

After implementation, run the correct backend test command for the dispatcher test and include the command in your summary.
