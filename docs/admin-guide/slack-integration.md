# Slack Integration

ShipFlow can send notifications to Slack channels when key events occur.

::: tip Full guide
See [`SLACK_INTEGRATION_GUIDE.md`](https://github.com/farzad-sedaghatbin/ShipFlow/blob/main/SLACK_INTEGRATION_GUIDE.md) in the repository for the complete setup guide.
:::

## Supported events

- Task assigned
- @mention in a comment
- Pitch status changed
- Cycle started / completed

## Setup

1. Create a Slack app at https://api.slack.com/apps
2. Add the **Incoming Webhooks** feature and create a webhook URL for your channel
3. In ShipFlow → Organization Settings → Integrations → Slack, paste the webhook URL
4. Select which events trigger notifications
5. Click **Test Notification** to verify the connection

## Per-user opt-out

Users can disable Slack notifications for themselves via their profile settings (Profile → Notifications).
