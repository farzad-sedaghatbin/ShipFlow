# GitHub Integration Guide

ShipFlow now integrates with GitHub to automatically link commits, pull requests, and branches to tasks and pitches. This enables automatic task closure when PRs are merged and provides visibility into development activity.

## Features

- **Auto-linking**: Automatically links commits and PRs to tasks when they mention task/pitch IDs
- **Auto-close tasks**: Automatically closes tasks when PRs with "closes #123" keywords are merged
- **Real-time updates**: Webhook integration provides immediate updates as commits and PRs are created
- **Visual tracking**: See all related GitHub activity directly in task and pitch detail pages

## Setup Methods

ShipFlow supports **two methods** for GitHub integration:

| Method | Best For | Setup Effort | Repository Access | Webhooks |
|--------|----------|--------------|-------------------|----------|
| **GitHub App Installation** (Recommended) | Organizations with 50+ repositories | One-time authorization | All repos at once | ✅ Automatic |
| **Manual Repository Registration** | Small projects or specific repositories | Per-repository setup | Selected repos only | ❌ Manual |

---

## Method 1: GitHub App Installation (Recommended for Organizations)

The GitHub App method provides **organization-wide access** through a single OAuth consent flow. This is the recommended approach for organizations with many repositories.

### Benefits

- ✅ **One-Time Setup**: Authorize once, access all repositories
- ✅ **Automatic Discovery**: New repositories are automatically accessible
- ✅ **Automatic Webhooks**: GitHub App automatically configures webhooks for ALL repositories - no manual setup needed!
- ✅ **Better Security**: Uses GitHub's official App authentication with short-lived tokens
- ✅ **Bulk Sync**: Sync 50+ repositories in seconds
- ✅ **Future Repos**: New repositories created in the organization are automatically tracked

### How Webhooks Work with GitHub App

When you install the GitHub App:
1. **GitHub automatically sends webhook events** to your ShipFlow instance for ALL repositories
2. **No manual webhook configuration** is required per repository
3. Push, Pull Request, Branch Create/Delete events are automatically captured
4. New repositories added to your organization automatically receive webhook coverage

---

## 🏢 Creating Your Own GitHub App (Required for Self-Hosted)

> **Important**: Since ShipFlow is open-source, each company/organization running their own instance must create their own GitHub App. This ensures your data stays private and you have full control over the integration.

### Prerequisites

- Admin access to your GitHub organization
- Your ShipFlow instance URL (must be publicly accessible for webhooks)
- SSL certificate (HTTPS required for production)

### Step 1: Create a GitHub App

1. **Navigate to GitHub App Creation**:
   - For **Organization**: Go to `https://github.com/organizations/YOUR-ORG/settings/apps/new`
   - For **Personal Account**: Go to `https://github.com/settings/apps/new`

2. **Fill in Basic Information**:

   | Field | Value | Example |
   |-------|-------|---------|
   | **GitHub App name** | Unique name for your app | `mycompany-shipflow` |
   | **Description** | Brief description | `ShipFlow integration for MyCompany` |
   | **Homepage URL** | Your ShipFlow instance URL | `https://shipflow.mycompany.com` |

3. **Configure Callback URL** (for OAuth flow):
   ```
   https://YOUR-SHIPFLOW-DOMAIN/api/github/app/callback
   ```
   Example: `https://shipflow.mycompany.com/api/github/app/callback`

4. **Configure Webhook Settings**:
   
   | Field | Value |
   |-------|-------|
   | **Active** | ✅ Checked |
   | **Webhook URL** | `https://YOUR-SHIPFLOW-DOMAIN/api/github/webhook` |
   | **Webhook secret** | Generate with: `openssl rand -hex 32` |

5. **Set Repository Permissions**:

   | Permission | Access Level | Purpose |
   |------------|--------------|---------|
   | **Contents** | Read | Read commit information |
   | **Metadata** | Read | Read repository metadata |
   | **Pull requests** | Read & Write | Track PRs, auto-close tasks |
   | **Issues** | Read & Write | (Optional) Link issues to tasks |
   | **Webhooks** | Read & Write | Receive repository events |

6. **Subscribe to Events** (check these boxes):
   - ✅ **Push** - Track commits
   - ✅ **Pull request** - Track PRs, auto-close tasks on merge
   - ✅ **Create** - Track branch/tag creation
   - ✅ **Delete** - Track branch/tag deletion

7. **Set "Where can this GitHub App be installed?"**:
   - Choose **"Only on this account"** for private use
   - Choose **"Any account"** if you want to share across organizations

8. Click **"Create GitHub App"**

### Step 2: Note Your App Credentials

After creation, you'll see your app settings page. **Copy these values**:

| Credential | Where to Find | Example |
|------------|---------------|---------|
| **App ID** | Top of the page, next to app name | `123456` |
| **App Name (slug)** | From URL: `github.com/apps/{APP-NAME}` | `mycompany-shipflow` |
| **Client ID** | In "About" section | `Iv1.a1b2c3d4e5f6g7h8` |
| **Client Secret** | Click "Generate a new client secret" | `abc123...` (copy immediately, shown once!) |

### Step 3: Generate Private Key

1. Scroll down to **"Private keys"** section
2. Click **"Generate a private key"**
3. A `.pem` file will be downloaded automatically
4. **Keep this file secure!** It's used to authenticate as your app

**Reading the Private Key**:
```bash
# Option 1: Read from file and export as environment variable
export GITHUB_APP_PRIVATE_KEY=$(cat /path/to/your-app.private-key.pem)

# Option 2: Base64 encode for easier storage
cat /path/to/your-app.private-key.pem | base64 > private-key-base64.txt
```

### Step 4: Configure ShipFlow Environment

Create or update your `.env` file with these values:

```bash
# ============================================
# GitHub App Configuration (REQUIRED)
# ============================================

# App ID - numeric ID from your GitHub App settings page
GITHUB_APP_ID=123456

# App Name - the slug from your app's URL (github.com/apps/{this-part})
GITHUB_APP_NAME=mycompany-shipflow

# Private Key - the full PEM content from the downloaded .pem file
# Option A: Direct PEM content (include the BEGIN/END markers)
GITHUB_APP_PRIVATE_KEY="-----BEGIN RSA PRIVATE KEY-----
MIIEowIBAAKCAQEA...
...your key content...
-----END RSA PRIVATE KEY-----"

# Option B: If using base64 encoded key (you'll need to decode in your startup script)
# GITHUB_APP_PRIVATE_KEY_BASE64=LS0tLS1CRUdJTi...

# Client ID - from the "About" section of your GitHub App
GITHUB_APP_CLIENT_ID=Iv1.a1b2c3d4e5f6g7h8

# Client Secret - generated in your GitHub App settings
GITHUB_APP_CLIENT_SECRET=your_client_secret_here

# Webhook Secret - the secret you set when creating the app
GITHUB_APP_WEBHOOK_SECRET=your_webhook_secret_here

# ============================================
# Application Configuration (OPTIONAL)
# ============================================

# Your ShipFlow instance's public URL (OPTIONAL - auto-detected from request)
# Only set this if auto-detection fails (complex proxy setups)
# APP_BASE_URL=https://shipflow.mycompany.com

# Enable GitHub integration
GITHUB_ENABLED=true
```

### Step 5: Verify Configuration

After starting ShipFlow, verify the configuration:

```bash
# Check GitHub App status via API
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  https://shipflow.mycompany.com/api/github/app/status
```

Expected response:
```json
{
  "configured": true,
  "appId": true,
  "appName": "mycompany-shipflow",
  "privateKeyConfigured": true,
  "clientIdConfigured": true,
  "webhookSecretConfigured": true,
  "installationUrl": "https://github.com/apps/mycompany-shipflow/installations/new",
  "totalInstallations": 0,
  "totalRepositories": 0
}
```

### Step 6: Install the App on Your Organization

**Option A: From ShipFlow UI (Recommended)**
1. Go to Settings → Integrations → GitHub
2. Click "Connect GitHub Organization"
3. You'll be redirected to GitHub
4. Select your organization
5. Choose repository access:
   - **All repositories**: Access all current and future repos (recommended)
   - **Selected repositories**: Pick specific repos
6. Click "Install"
7. You'll be redirected back to ShipFlow with all repositories synced automatically!

**Option B: From GitHub Directly**
1. Go to `https://github.com/apps/YOUR-APP-NAME/installations/new`
2. Select your organization
3. Choose repository access and click "Install"
4. Go to ShipFlow Settings → Integrations → GitHub and click "Sync"

---

## 📋 Quick Reference: All Configuration Variables

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `GITHUB_APP_ID` | ✅ Yes | Numeric App ID | `123456` |
| `GITHUB_APP_NAME` | ✅ Yes | App slug from URL | `mycompany-shipflow` |
| `GITHUB_APP_PRIVATE_KEY` | ✅ Yes | Full PEM private key | `-----BEGIN RSA...` |
| `GITHUB_APP_CLIENT_ID` | ✅ Yes | OAuth Client ID | `Iv1.abc123` |
| `GITHUB_APP_CLIENT_SECRET` | ✅ Yes | OAuth Client Secret | `secret123` |
| `GITHUB_APP_WEBHOOK_SECRET` | ✅ Yes | Webhook verification | `hex-string` |
| `APP_BASE_URL` | Optional | Auto-detected from request | `https://shipflow.example.com` |
Check that all required environment variables are set:
```bash
# Verify variables are loaded
echo $GITHUB_APP_ID
echo $GITHUB_APP_NAME
echo $GITHUB_APP_CLIENT_ID
```

### "Invalid state" error during OAuth

- The authorization link expired (10-minute timeout)
- Start the authorization flow again from ShipFlow

### Webhooks not receiving events

1. Check webhook URL is publicly accessible
2. Verify SSL certificate is valid
3. Check GitHub webhook delivery logs:
   - Go to your GitHub App settings → Advanced → Recent Deliveries
4. Verify webhook secret matches

### "Bad credentials" error

- Private key may be incorrectly formatted
- Ensure the full PEM content including `-----BEGIN RSA PRIVATE KEY-----` markers
- Check for extra whitespace or line breaks

---

## Method 2: Manual Repository Registration (Original Method)

For small projects or when you only need specific repositories, you can manually register each repository.

> ⚠️ **Note**: With manual registration, you must configure webhooks for EACH repository individually. For organizations with many repositories, use the GitHub App method instead.

### 1. Register a Repository

1. Navigate to the GitHub Integration settings (typically in Admin settings)
2. Click "Add Repository"
3. Fill in the repository details:
   - **Owner**: GitHub username or organization (e.g., `mycompany`)
   - **Name**: Repository name (e.g., `my-project`)
   - **URL**: Optional, full GitHub URL
   - **Default Branch**: Usually `main` or `master`

### 2. Configure GitHub Webhook (Manual - Required for each repository)

1. Go to your GitHub repository → Settings → Webhooks
2. Click "Add webhook"
3. Set the Payload URL to: `https://your-domain.com/api/github/webhook`
4. Set Content type to: `application/json`
5. Optional: Set a webhook secret for security (recommended)
6. Select events to trigger the webhook:
   - ✅ Push events
   - ✅ Pull requests
   - ✅ Branch or tag creation
   - ✅ Branch or tag deletion
7. Click "Add webhook"

### 3. Optional: Configure Webhook Secret

For production use, it's recommended to set a webhook secret:

1. Generate a secure random string (e.g., using `openssl rand -hex 32`)
2. Set it in your GitHub webhook configuration
3. Add it to your repository configuration in ShipFlow
4. Or set globally via environment variable: `GITHUB_WEBHOOK_SECRET`

### 4. Optional: Add Personal Access Token

For private repositories or enhanced API access:

1. Generate a GitHub Personal Access Token with `repo` scope
2. Add it to your repository configuration in ShipFlow
3. Or set globally via environment variable: `GITHUB_OAUTH_CLIENT_SECRET`

## Usage

### Auto-linking Commits to Tasks

Reference tasks in your commit messages using any of these formats:

**Jira-style (Recommended)**:
```
PROJ-42: Fixed the login bug
MYAPP-123: Added new feature
TASK-456: Updated documentation
```

**Traditional style**:
```
Task #123: Fixed the login bug
TASK #456: Added new feature
task #789: Updated documentation
#T123: Quick fix
```

The commit will automatically be linked to the referenced task(s). You can mix both styles in the same message.

### Auto-linking Pull Requests to Tasks

Reference tasks in PR titles or descriptions:

**Jira-style (Recommended)**:
```
PROJ-42: Implement user authentication
[MYAPP-123] Add new dashboard feature
```

**Traditional style**:
```
Fixes Task #123 - Implement user authentication
[Task #456] Add reporting module
```

### Auto-closing Tasks

Use closing keywords in PR titles or descriptions:

**Jira-style (Recommended)**:
```
Closes PROJ-42
Fixes MYAPP-123
Resolves TASK-456
```

**Traditional style**:
```
Closes #123
Fixes #456
Resolves #789
Close Task #101
Fix task #202
Resolve TASK #303
```

When the PR is merged, the referenced tasks will automatically be marked as DONE.

### Linking to Pitches

Reference pitches using similar syntax:

```
Pitch #5: Implemented new dashboard feature
PITCH #12: Added reporting capabilities
#P7: Updated UI design
```

## Keyword Patterns

The system recognizes these patterns:

### Task References (Jira-style - Recommended)
- `PROJ-42` (any 2-10 uppercase letters + number)
- `MYAPP-123`
- `TASK-456`
- Works in any context: commits, PR titles, PR descriptions

### Task References (Traditional style)
- `Task #123` (case-insensitive)
- `TASK #123`
- `task #123`
- `#T123`

### Pitch References
- `Pitch #5`
- `PITCH #5`
- `pitch #5`
- `#P5`

### Closing Keywords (Both styles supported)
- Jira-style: `closes PROJ-42`, `fixes MYAPP-123`, `resolves TASK-456`
- Traditional: `closes #123`, `fixes #456`, `resolves #789`
- Works with: `close`, `fix`, `resolve` (and their plural forms)

## Configuration Options

### Application Properties

```properties
# Enable/disable GitHub integration
app.github.enabled=true

# Auto-link commits/PRs to tasks when keywords are detected
app.github.auto-link.enabled=true

# Auto-close tasks when PR is merged with closing keywords
app.github.auto-close-tasks.enabled=true

# Global webhook secret (can be overridden per repository)
app.github.webhook-secret=${GITHUB_WEBHOOK_SECRET:}

# GitHub OAuth credentials for private repositories
app.github.oauth.client-id=${GITHUB_OAUTH_CLIENT_ID:}
app.github.oauth.client-secret=${GITHUB_OAUTH_CLIENT_SECRET:}
```

### Environment Variables

```bash
# Set in .env or environment
GITHUB_ENABLED=true
GITHUB_AUTO_LINK_ENABLED=true
GITHUB_AUTO_CLOSE_TASKS_ENABLED=true
GITHUB_WEBHOOK_SECRET=your-secure-secret-here
GITHUB_OAUTH_CLIENT_ID=your-github-app-client-id
GITHUB_OAUTH_CLIENT_SECRET=your-github-app-client-secret
```

## Viewing GitHub Activity

### Task Detail Page

When viewing a task, you'll see a "GitHub Activity" card showing:
- All linked commits with messages, authors, and timestamps
- All linked pull requests with status (open, merged, closed)
- All linked branches
- Auto-linked vs. manually linked indicators

### Pitch Detail Page

Similar to tasks, pitch pages show all related GitHub activity.

## API Endpoints

### GitHub App Endpoints (Organization-Wide Access)

#### Get Configuration Status
```
GET /api/github/app/status
```
Returns GitHub App configuration status and statistics.

#### Initiate OAuth Authorization
```
POST /api/github/app/authorize
```
Generates authorization URL for installing the GitHub App.

#### OAuth Callback
```
GET /api/github/app/callback
```
Handles OAuth callback from GitHub after app installation.

#### List Installations
```
GET /api/github/app/installations
```
Returns all active GitHub App installations.

#### Sync All Repositories
```
POST /api/github/app/sync-all
```
Bulk syncs repositories from all active installations.

#### Remove Installation
```
DELETE /api/github/app/installations/{id}
```
Deactivates a GitHub App installation.

### Manual Repository Endpoints

#### Get Task GitHub Links
```
GET /api/github/tasks/{taskId}/links
```

Returns all commits, PRs, and branches linked to the task.

### Get Pitch GitHub Links
```
GET /api/github/pitches/{pitchId}/links
```

Returns all commits, PRs, and branches linked to the pitch.

### Register Repository
```
POST /api/github/repositories
```

Register a new GitHub repository for webhook integration.

### Webhook Endpoint
```
POST /api/github/webhook
```

Receives GitHub webhook events (push, pull_request, create, delete).

## Database Schema

The integration creates these tables:

### GitHub App Tables
- `github_app_installations`: GitHub App installation data (org-wide access)

### Repository & Activity Tables
- `github_repositories`: Registered repositories
- `github_commits`: Commit information
- `github_pull_requests`: PR information
- `github_branches`: Branch information
- `task_github_links`: Links between tasks and GitHub items
- `pitch_github_links`: Links between pitches and GitHub items
- `github_webhook_events`: Audit log of webhook events
- `github_configuration`: Repository-specific configuration

## Troubleshooting

### Webhooks not working

1. Check that the webhook URL is correct
2. Verify webhook events are selected (Push, Pull requests, etc.)
3. Check webhook delivery logs in GitHub
4. Check application logs for webhook processing errors
5. Verify webhook secret matches if configured

### Auto-linking not working

1. Ensure `app.github.auto-link.enabled=true`
2. Check that task/pitch IDs are valid
3. Verify keyword format matches the patterns above
4. Check application logs for parsing errors

### Tasks not auto-closing

1. Ensure `app.github.auto-close-tasks.enabled=true`
2. Verify PR is actually merged (not just closed)
3. Check that closing keywords are used correctly
4. Verify task exists and is in a closeable state

### GitHub links not showing in UI

1. Verify the task/pitch has linked GitHub items
2. Check browser console for API errors
3. Ensure frontend is building correctly
4. Check network tab for failed API requests

## Security Best Practices

1. **Always use webhook secrets** in production
2. Use HTTPS for webhook URLs
3. Rotate secrets periodically
4. Use Personal Access Tokens with minimal required scopes
5. Store secrets in environment variables, not in code
6. Monitor webhook event logs for suspicious activity
### Example: Jira-style workflow

1. Create a task: "Implement user login feature" (ID: 42)

2. Create a feature branch:
   ```bash
   git checkout -b feature/PROJ-42-user-login
   ```

3. Make commits referencing the task:
   ```bash
   git commit -m "PROJ-42: Add login form UI"
   git commit -m "PROJ-42: Implement authentication logic"
   git commit -m "PROJ-42: Add session management"
   ```

4. Create a pull request:
   ```
   Title: PROJ-42: Implement user login feature
   Description: Closes PROJ-42
   
   This PR implements the user login feature including:
   - Login form UI
   - Authentication backend
   - Session management
   ```

5. When the PR is merged:
   - All commits are automatically linked to Task #42
   - The PR is automatically linked to Task #42
   - Task #42 is automatically marked as DONE

### Alternative: Traditional Style

You can also use the traditional style:
```bash
git commit -m "Task #42: Add login form UI"
```

PR title: `Closes #42 - Implement user login feature`

Both styles work and can be mixed in the same project.

## Migration from Existing Projects

If you have an existing repository with commits:

1. Register the repository in ShipFlow (via GitHub App or manually)
2. Historic commits won't be automatically imported
3. New commits and PRs will be tracked from webhook registration onward
4. Optionally implement a one-time import script if needed

**Migrating from Manual to GitHub App**:
1. Install the GitHub App on your organization
2. Existing manually-registered repositories will continue to work
3. New repositories will be automatically added via GitHub App
4. Optionally remove manual registrations after verifying GitHub App sync

## Future Enhancements

Potential future improvements:
- Import historic commits and PRs
- ~~GitHub App integration (instead of webhooks)~~ ✅ **Implemented!**
- Two-way sync (create GitHub issues from tasks)
- Branch protection rules integration
- Code review tracking
- CI/CD status integration
- GitHub Actions workflow status

