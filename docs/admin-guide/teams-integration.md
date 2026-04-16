# Microsoft Teams Integration

ShipFlow can send notifications to Microsoft Teams channels via Incoming Webhooks.

::: tip Full guide
See [`TEAMS_INTEGRATION_GUIDE.md`](https://github.com/farzad-sedaghatbin/ShipFlow/blob/main/TEAMS_INTEGRATION_GUIDE.md) in the repository for the complete setup guide.
:::

## Supported events

- Task assigned
- @mention in a comment
- Pitch status changed
- Cycle started / completed

## Setup

1. In Microsoft Teams, navigate to the channel you want to receive notifications
2. Click **⋯ → Connectors → Incoming Webhook → Configure**
3. Give it a name (e.g. "ShipFlow") and copy the webhook URL
4. In ShipFlow → Organization Settings → Integrations → Microsoft Teams, paste the webhook URL
5. Select which events trigger notifications
6. Click **Test Notification** to verify

## Troubleshooting

If notifications are not delivered:
- Verify the webhook URL is correct and has not been rotated in Teams
- Check that the Teams channel connector is still active (Teams sometimes disables inactive connectors)
- Review ShipFlow logs for HTTP errors when posting to the webhook
