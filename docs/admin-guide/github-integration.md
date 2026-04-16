# GitHub Integration

ShipFlow integrates with GitHub for repository context in the Wise Architecture AI feature and for inbound webhooks.

::: tip Full guide
See [`GITHUB_INTEGRATION_GUIDE.md`](https://github.com/farzad-sedaghatbin/ShipFlow/blob/main/GITHUB_INTEGRATION_GUIDE.md) in the repository for the complete setup guide.
:::

## What's supported

- **Repository context** — Wise Architecture AI reads your codebase structure via the GitHub MCP provider to generate architecture advice grounded in your actual tech stack.
- **Inbound webhooks** — Receive push/PR events to trigger automations.
- **OAuth login** — Optionally allow users to sign in with GitHub.

## Quick setup

1. Create a GitHub OAuth App at https://github.com/settings/developers
2. Set callback URL to `http://your-shipflow-host/api/auth/github/callback`
3. Add to your environment:
   ```bash
   GITHUB_CLIENT_ID=your_client_id
   GITHUB_CLIENT_SECRET=your_client_secret
   ```
4. In Organization Settings → Integrations → GitHub, connect your repositories.

## MCP GitHub provider

The GitHub MCP provider is used internally by Wise Architecture. Configure it in Organization Settings → AI → GitHub Repositories.
