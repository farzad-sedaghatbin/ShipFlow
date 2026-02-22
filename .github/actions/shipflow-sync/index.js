// ShipFlow Sync – GitHub Action
// Interacts with the ShipFlow Public REST API to update tasks from CI/CD pipelines.
//
// Usage in a workflow:
//   - uses: ./.github/actions/shipflow-sync
//     with:
//       shipflow-url: ${{ secrets.SHIPFLOW_URL }}
//       api-key: ${{ secrets.SHIPFLOW_API_KEY }}
//       action: update-task-status
//       task-id: '42'
//       status: DONE
//       comment: 'Closed by CI after deploy succeeded'

const core = require('@actions/core');

async function run() {
  try {
    const baseUrl = core.getInput('shipflow-url', { required: true }).replace(/\/+$/, '');
    const apiKey = core.getInput('api-key', { required: true });
    const action = core.getInput('action', { required: true });
    const taskId = core.getInput('task-id');
    const status = core.getInput('status');
    const comment = core.getInput('comment');
    const cycleId = core.getInput('cycle-id');

    const headers = {
      'X-API-Key': apiKey,
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };

    let url;
    let method = 'GET';
    let body = null;

    switch (action) {
      case 'update-task-status':
        if (!taskId) throw new Error('task-id is required for update-task-status');
        if (!status) throw new Error('status is required for update-task-status');
        url = `${baseUrl}/api/v1/public/tasks/${taskId}/status`;
        method = 'PATCH';
        body = JSON.stringify({ status, comment: comment || undefined });
        break;

      case 'get-task':
        if (!taskId) throw new Error('task-id is required for get-task');
        url = `${baseUrl}/api/v1/public/tasks/${taskId}`;
        break;

      case 'list-tasks':
        url = `${baseUrl}/api/v1/public/tasks`;
        if (cycleId) url += `?cycleId=${cycleId}`;
        break;

      default:
        throw new Error(`Unknown action: ${action}`);
    }

    core.info(`ShipFlow API: ${method} ${url}`);

    const response = await fetch(url, { method, headers, body });
    const responseText = await response.text();

    if (!response.ok) {
      core.setFailed(`ShipFlow API returned ${response.status}: ${responseText}`);
      return;
    }

    core.info(`ShipFlow API response (${response.status}): ${responseText.substring(0, 500)}`);
    core.setOutput('result', responseText);
  } catch (error) {
    core.setFailed(`ShipFlow Sync failed: ${error.message}`);
  }
}

run();
