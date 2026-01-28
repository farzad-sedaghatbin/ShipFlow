# GitHub Integration Guide

ShipFlow now integrates with GitHub to automatically link commits, pull requests, and branches to tasks and pitches. This enables automatic task closure when PRs are merged and provides visibility into development activity.

## Features

- **Auto-linking**: Automatically links commits and PRs to tasks when they mention task/pitch IDs
- **Auto-close tasks**: Automatically closes tasks when PRs with "closes #123" keywords are merged
- **Real-time updates**: Webhook integration provides immediate updates as commits and PRs are created
- **Visual tracking**: See all related GitHub activity directly in task and pitch detail pages

## Setup Instructions

### 1. Register a Repository

1. Navigate to the GitHub Integration settings (typically in Admin settings)
2. Click "Add Repository"
3. Fill in the repository details:
   - **Owner**: GitHub username or organization (e.g., `mycompany`)
   - **Name**: Repository name (e.g., `my-project`)
   - **URL**: Optional, full GitHub URL
   - **Default Branch**: Usually `main` or `master`

### 2. Configure GitHub Webhook

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

### Get Task GitHub Links
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

1. Register the repository in ShipFlow
2. Historic commits won't be automatically imported
3. New commits and PRs will be tracked from webhook registration onward
4. Optionally implement a one-time import script if needed

## Future Enhancements

Potential future improvements:
- Import historic commits and PRs
- GitHub App integration (instead of webhooks)
- Two-way sync (create GitHub issues from tasks)
- Branch protection rules integration
- Code review tracking
- CI/CD status integration
- GitHub Actions workflow status
