# Slack Integration Guide

## Overview

The ShapeUp Tracker now includes Slack integration, allowing you to receive real-time notifications about important events directly in your Slack workspace.

## Features

- **Workspace Configuration**: Connect your Slack workspace using webhook URLs
- **Channel-Specific Notifications**: Configure different notification preferences for different Slack channels
- **Event Types Supported**:
  - Task assignments
  - Task completions
  - Tasks becoming blocked
  - Pitch shaping events
  - Cycle phase changes
  - Betting completion
  - Sprint starts

## Setup Instructions

### 1. Create a Slack Incoming Webhook

1. Go to your Slack workspace settings
2. Navigate to **Apps** → **Manage** → **Custom Integrations**
3. Select **Incoming Webhooks**
4. Click **Add to Slack**
5. Choose the default channel where notifications will be posted
6. Click **Add Incoming WebHooks integration**
7. Copy the **Webhook URL** (it looks like `https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX`)

### 2. Configure ShapeUp Tracker

1. Log in to ShapeUp Tracker as an administrator
2. Navigate to **Admin** → **Slack Integration** from the sidebar menu
3. Click **Configure Workspace**
4. Fill in the following details:
   - **Workspace Name**: A friendly name for your Slack workspace
   - **Webhook URL**: Paste the webhook URL from step 1
   - **Default Channel** (optional): The default channel for notifications (e.g., `general`)
   - **Enable Slack Integration**: Toggle to enable/disable notifications
5. Click **Save**

### 3. Configure Channel Notifications (Optional)

You can configure notification preferences for specific channels:

1. Go to the **Channel Notifications** tab
2. Click **Add Channel**
3. Specify:
   - **Channel Name**: The Slack channel name (without the #)
   - **Channel-Specific Webhook URL** (optional): If you want to use a different webhook for this channel
   - **Notification Preferences**: Toggle which events trigger notifications in this channel
4. Click **Save**

### 4. Test the Integration

1. In the **Workspace Configuration** tab, click the **Send** button next to your configuration
2. Enter a test message and channel (optional)
3. Click **Send Test**
4. Check your Slack channel for the test notification

## Channel Configuration Options

For each Slack channel, you can enable/disable the following notification types:

| Notification Type | Description |
|------------------|-------------|
| **Task Assigned** | Notifies when a task is assigned to a team member |
| **Task Completed** | Notifies when a task is marked as done |
| **Task Blocked** | Notifies when a task becomes blocked |
| **Pitch Shaped** | Notifies when a new pitch is shaped |
| **Cycle Started** | Notifies when a new cycle begins |
| **Cycle Cooldown** | Notifies when a cycle enters cooldown phase |
| **Betting Completed** | Notifies when betting round is completed |
| **Sprint Started** | Notifies when a sprint starts |

## Notification Message Format

Notifications are sent to Slack with formatted messages:

- **Task Assigned**: `📋 Task Assigned - [Task Title] has been assigned to [Username]`
- **Task Completed**: `✅ Task Completed - [Task Title] has been marked as done by [Username]`
- **Task Blocked**: `⚠️ Task Blocked - [Task Title] is now blocked (Assigned to: [Username])`

## API Endpoints

For programmatic access, the following REST API endpoints are available:

### Configuration Management
- `POST /api/slack/configurations` - Create or update workspace configuration
- `GET /api/slack/configurations` - Get all configurations
- `GET /api/slack/configurations/active` - Get active configuration
- `DELETE /api/slack/configurations/{configId}` - Delete configuration

### Channel Configuration
- `POST /api/slack/configurations/{configId}/channels` - Create channel config
- `GET /api/slack/configurations/{configId}/channels` - Get channel configs
- `DELETE /api/slack/channels/{channelConfigId}` - Delete channel config

### Testing
- `POST /api/slack/configurations/{configId}/test` - Send test notification

### History
- `GET /api/slack/configurations/{configId}/history` - Get notification history

## Permissions

The following permissions are required to manage Slack integration:

- **Configure Workspace**: ADMIN or MANAGER role
- **Configure Channels**: ADMIN or MANAGER role
- **Send Test Notifications**: ADMIN or MANAGER role
- **View Configurations**: Any authenticated user
- **Delete Configurations**: ADMIN role only

## Troubleshooting

### Notifications not appearing in Slack

1. **Check workspace is enabled**: Ensure the "Enable Slack Integration" toggle is ON
2. **Verify webhook URL**: Make sure the webhook URL is correct and hasn't been regenerated
3. **Check channel configuration**: If using channel-specific settings, ensure the notification type is enabled
4. **Test the connection**: Use the "Send Test" feature to verify connectivity
5. **Check notification history**: View the notification history to see if messages are being sent but failing

### Notification sent to wrong channel

- If you specified a default channel in the workspace configuration, it will be used
- Channel-specific webhooks override the default webhook
- The channel name should not include the `#` symbol

### Permission errors

- Only users with ADMIN or MANAGER roles can configure Slack integration
- Contact your system administrator if you need access

## Security Considerations

- **Webhook URLs are sensitive**: Treat webhook URLs like passwords. Anyone with access to the URL can post to your Slack channel
- **Use HTTPS**: The Slack webhook URL uses HTTPS, ensuring encrypted communication
- **Audit logging**: All notifications are logged in the notification history for auditing purposes
- **Access control**: Only administrators and managers can configure the integration

## Database Schema

The Slack integration uses the following database tables:

- `slack_configuration`: Stores workspace configuration
- `slack_channel_config`: Stores channel-specific notification preferences
- `slack_notification_history`: Audit log of all sent notifications

## Support

For issues or questions about Slack integration:

1. Check the notification history for error messages
2. Verify your Slack webhook is still active
3. Ensure you're using the correct API version
4. Contact your system administrator

## Future Enhancements

Planned features for future releases:

- Interactive Slack messages with action buttons
- Two-way integration (respond to Slack messages)
- Rich formatting with attachments and blocks
- User mention support
- Thread support for related notifications
- Slack OAuth integration (instead of webhooks)
