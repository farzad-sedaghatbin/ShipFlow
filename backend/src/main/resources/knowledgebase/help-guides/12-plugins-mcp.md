# Plugins & MCP Integration

## What is a plugin in ShipFlow?

A plugin is an extension that adds new capabilities to ShipFlow without changing the core codebase. Plugins follow the ShipFlow Plugin SPI (Service Provider Interface) and are registered automatically when present on the classpath as Spring beans.

ShipFlow supports three types of plugins:

- **Risk Calculator plugins** — custom logic to calculate risk scores for pitches, supplementing the built-in AI risk analysis.
- **Report Generator plugins** — produce custom report formats (PDF, CSV, custom) from cycle and project data.
- **Integration Provider plugins** — connect ShipFlow to external tools and services (sync tasks, send notifications, import data).

## How do I view and manage plugins?

Go to **Organization Settings → Plugins** tab. You will see all registered plugins grouped by type with their current status. Any ADMIN user can toggle a plugin on or off without restarting the server.

## What built-in plugins does ShipFlow include?

ShipFlow ships with two built-in plugins that are always available (each labeled "Preview" in the admin list):

1. **Deadline Pressure Risk** (risk) — scores pitches based on the gap between time consumed and task completion ratio. Flags high risk when you are more than 50% through the cycle appetite but less than 50% done with tasks.
2. **Cycle Health Summary Report** (report) — generates a plain-text cycle health summary you can download or share.

## How do I enable or disable a plugin?

1. Navigate to **Organization Settings → Plugins**.
2. Find the plugin in the list.
3. Toggle the switch next to it on or off.
4. The change takes effect immediately — no server restart required.
5. Only ADMIN users can change plugin settings.

## How do I build a custom plugin?

ShipFlow provides a real Plugin SDK in the `plugin-sdk/` directory of the repository: a
distributable `shipflow-plugin-api` jar with the SPI interfaces, and a `shipflow-plugin-archetype`
Maven archetype that scaffolds a new plugin project for you.

Plugins are Spring beans **compiled into the ShipFlow backend build** — there is no dynamic
"drop a JAR into a folder" loading. Adding a new plugin (or removing one) requires a rebuild and
restart of the backend; toggling an *already-registered* plugin on/off does not.

**Quick start:**

1. Generate a project from the archetype:
   ```bash
   mvn archetype:generate \
     -DarchetypeGroupId=com.github.farzadsedaghatbin.shipflow \
     -DarchetypeArtifactId=shipflow-plugin-archetype \
     -DarchetypeVersion=1.0.0 \
     -DgroupId=com.example \
     -DartifactId=my-shipflow-plugin \
     -DinteractiveMode=false
   ```
   (or copy `plugin-sdk/sample-plugin/` by hand as a starting point instead)
2. Implement one of the three plugin interfaces:
   - `RiskCalculatorPlugin` for custom risk scoring
   - `ReportGeneratorPlugin` for custom report formats
   - `IntegrationProviderPlugin` for external tool connections
3. Annotate your class with `@Component` so Spring auto-discovers it.
4. Add your generated plugin project as a Maven dependency of `backend/pom.xml`, then rebuild and restart ShipFlow.

**Example — Risk Calculator plugin:**

```java
@Component
public class MyRiskPlugin implements RiskCalculatorPlugin {
    @Override public String getPluginId() { return "my-risk-plugin"; }
    @Override public String getDisplayName() { return "My Custom Risk"; }

    @Override
    public Double calculateRisk(Map<String, Object> context) {
        Integer daysRemaining = (Integer) context.get("daysRemaining");
        return daysRemaining != null && daysRemaining < 3 ? 0.9 : 0.2;
    }
}
```

The plugin appears automatically in Organization Settings → Plugins the next time the application starts with your plugin dependency on the classpath. See `plugin-sdk/README.md` for the full walkthrough.

## What is MCP (Model Context Protocol)?

MCP is a standardized way for AI tools (Claude Code, Cursor, etc.) to query and act on ShipFlow data. ShipFlow acts both as an MCP client (reading from GitHub, Figma, Notion, Confluence, GitLab, Azure DevOps) and as an MCP server (letting AI tools read and write ShipFlow tasks and pitches).

## How do I configure MCP integrations?

Go to **MCP Integration** (in the main navigation under Integrations). There are tabs for:

- **GitHub** — enter a personal access token to give ShipFlow access to your repository for code context in Wise Architecture AI.
- **Figma** — enter a personal access token for design context.
- **Notion** — enter an internal integration token to read design docs and meeting notes from your Notion workspace.
- **Confluence** — enter an API token plus your Atlassian domain and default space key to read pages from Confluence Cloud.
- **MCP Server** — toggle the built-in ShipFlow MCP server on or off, and enable write tools.
- **API Keys** — create and revoke API keys used by external MCP clients to authenticate with ShipFlow.

GitLab and Azure DevOps MCP (below) have backend support but not yet their own tabs in this screen — their tokens are set via the organization settings API until that UI ships.

## How do I connect Claude Code or Cursor to ShipFlow via MCP?

1. Go to **MCP Integration → MCP Server** and enable the server.
2. Go to **MCP Integration → API Keys** and create a key with the READ (and optionally WRITE) scope.
3. In your Claude Code or Cursor MCP config, point the client at the ShipFlow SSE endpoint shown on the MCP Server tab.
4. Use the API key as a bearer token in the `Authorization` header.
5. Changes take effect immediately — no restart needed.

## How do I connect claude.ai (hosted chat / free tier) to ShipFlow via MCP?

claude.ai's connector settings don't let you set a custom `Authorization` header, and its "Add custom connector" feature speaks the newer Streamable HTTP MCP transport (a single endpoint) rather than the older SSE transport ShipFlow's other clients use. ShipFlow supports both: paste one URL, no headers.

1. Go to **MCP Integration → MCP Server** and enable the server.
2. Go to **MCP Integration → API Keys** and create a key — pick the scope you actually want claude.ai to have: `READ` if it should only look things up, `WRITE`/`ADMIN` if it should also create/update. This connection grants exactly the scope the key was created with; it is not automatically downgraded to read-only.
3. When you create the key, ShipFlow shows a ready-to-copy **Connector URL** alongside the raw key (in the same one-time reveal dialog) — it looks like `https://your-instance/mcp/<api-key>` (no `/sse` suffix — that's a different, older transport and claude.ai won't reliably fall back to it).
4. In claude.ai, go to **Settings → Connectors → Add custom connector** and paste that URL.

Because the key sits in the URL, it can be logged by proxies or browser history, and unlike the header-based method this connection carries the key's full scope — a leaked URL for a `WRITE`/`ADMIN` key means whoever has it can act as you. Use a dedicated, short-lived key just for this connector rather than reusing a broadly-scoped one, treat the URL itself like a secret, and revoke the key immediately if you suspect it leaked. When your client supports custom headers, prefer the standard `Authorization: Bearer <api-key>` method instead — this URL-based method exists specifically for hosted clients (like claude.ai) that can't.

## Can an AI tool view image attachments on a bug?

Yes. Call `get_bug_attachments` (by `bugKey` or `bugReportId`) to list a bug's files — each one has an `isImage` flag. For image attachments (PNG, JPEG, GIF, WebP) pass the attachment `id` to `download_bug_attachment`, which returns the image itself so the AI client (e.g. Claude Code) can view a design mockup or screenshot directly. PDFs and documents aren't returned as images — read their `extractedText` from `get_bug_attachments` instead. Images over 8 MB are not returned inline.

## How do I set up a Notion MCP connection?

1. Create a Notion internal integration at notion.so/my-integrations.
2. Share the pages or databases you want ShipFlow to read with the integration.
3. Copy the integration token (starts with `secret_`).
4. Go to **MCP Integration → Notion**, paste the token, and click Save.
5. ShipFlow AI features can now pull content from those Notion pages as context.

## How do I set up a Confluence MCP connection?

1. Create a Confluence API token at id.atlassian.com/manage-profile/security/api-tokens.
2. Go to **MCP Integration → Confluence**.
3. Enter your API token, your Atlassian domain (e.g. `yourcompany.atlassian.net`), and the default space key (e.g. `ENG`).
4. Click Save. ShipFlow AI features can now read pages from that Confluence space.

## How do I set up a GitLab MCP connection?

1. Ask a self-hosting administrator to set `MCP_GITLAB_ENABLED=true` and `MCP_GITLAB_SERVER_URL` (your GitLab instance, e.g. `https://gitlab.com` or your self-hosted URL) on the ShipFlow backend, then restart it.
2. Create a GitLab Personal Access Token with `read_repository` (and `read_api` for search) scope.
3. Until a dedicated GitLab tab ships in this screen, an admin sets the token via the organization-settings API (the `gitlabAccessToken` field on the same PATCH endpoint used by Org Settings) — ask your administrator or a developer to set it for you.
4. Once configured, ShipFlow AI features (like Wise Architecture) can read files from your GitLab project by its numeric project ID or its `namespace/project` path.

## How do I set up an Azure DevOps MCP connection?

1. Ask a self-hosting administrator to set `MCP_AZURE_DEVOPS_ENABLED=true` and `MCP_AZURE_DEVOPS_SERVER_URL` on the ShipFlow backend, then restart it. This works against both Azure DevOps Services (`dev.azure.com`) and a self-hosted Azure DevOps Server, since the organization/project/repository are supplied per request rather than baked into the URL.
2. Create an Azure DevOps Personal Access Token with **Code (Read)** scope.
3. Until a dedicated Azure DevOps tab ships in this screen, an admin sets the token via the organization-settings API (the `azureDevOpsAccessToken` field on the same PATCH endpoint used by Org Settings) — ask your administrator or a developer to set it for you.
4. Once configured, ShipFlow AI features (like Wise Architecture) can read files from your Azure Repos repository by organization, project, and repository name.

## Can I share ShipFlow links in Slack or iMessage with rich previews?

Yes. Every task, pitch, and cycle detail page has a **Copy shareable link** button (link icon near the page title). Copying that link and pasting it into Slack, iMessage, WhatsApp, or Notion will unfurl a preview showing the item title and description.

If you paste the direct app URL (e.g. from the browser bar), ShipFlow also handles that automatically for known social platforms.
