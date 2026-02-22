## ShipFlow Sync – GitHub Action

A reusable GitHub Action that interacts with the ShipFlow Public REST API.  
Use it to update task statuses, fetch task details, or list tasks from your CI/CD pipelines.

### Quick Start

```yaml
name: Deploy & Update ShipFlow
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # ... your build & deploy steps ...

      # Mark a ShipFlow task as DONE after successful deploy
      - name: Update ShipFlow task
        uses: ./.github/actions/shipflow-sync
        with:
          shipflow-url: ${{ secrets.SHIPFLOW_URL }}
          api-key: ${{ secrets.SHIPFLOW_API_KEY }}
          action: update-task-status
          task-id: '42'
          status: DONE
          comment: 'Auto-closed by CI after deploy to production'
```

### Inputs

| Input          | Required | Description |
|----------------|----------|-------------|
| `shipflow-url` | Yes      | Base URL of your ShipFlow instance |
| `api-key`      | Yes      | ShipFlow API key (use GitHub Secrets) |
| `action`       | Yes      | `update-task-status`, `get-task`, or `list-tasks` |
| `task-id`      | Depends  | Required for `update-task-status` and `get-task` |
| `status`       | Depends  | Required for `update-task-status`. Values: `BACKLOG`, `TODO`, `IN_PROGRESS`, `BLOCKED`, `IN_REVIEW`, `DONE`, `CANCELLED` |
| `comment`      | No       | Comment for the status change |
| `cycle-id`     | No       | Filter for `list-tasks` |

### Outputs

| Output   | Description |
|----------|-------------|
| `result` | JSON response from the ShipFlow API |

### Examples

#### Update task on PR merge
```yaml
- name: Move task to IN_REVIEW
  if: github.event_name == 'pull_request' && github.event.action == 'opened'
  uses: ./.github/actions/shipflow-sync
  with:
    shipflow-url: ${{ secrets.SHIPFLOW_URL }}
    api-key: ${{ secrets.SHIPFLOW_API_KEY }}
    action: update-task-status
    task-id: '123'
    status: IN_REVIEW
    comment: 'PR opened: ${{ github.event.pull_request.html_url }}'
```

#### Fetch task details
```yaml
- name: Get task info
  id: task
  uses: ./.github/actions/shipflow-sync
  with:
    shipflow-url: ${{ secrets.SHIPFLOW_URL }}
    api-key: ${{ secrets.SHIPFLOW_API_KEY }}
    action: get-task
    task-id: '42'

- name: Print task
  run: echo '${{ steps.task.outputs.result }}'
```

### Generating an API Key

1. Log in to ShipFlow
2. Go to **Settings → API Keys**
3. Click **Create API Key**
4. Copy the key (shown only once) and add it as a GitHub Secret named `SHIPFLOW_API_KEY`
