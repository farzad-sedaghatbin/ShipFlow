# Microsoft Teams Integration Guide

## Overview

ShipFlow includes comprehensive Microsoft Teams integration, allowing you to receive real-time notifications about important events directly in your Teams channels. The integration supports both traditional Teams incoming webhooks and modern Power Automate flows with automatic flow type detection and optimized payload formats.

## Features

- **Flexible Integration Types**: Support for traditional webhooks and Power Automate flows (post to channel or create thread)
- **Automatic Flow Detection**: Smart detection of webhook type for optimal payload formatting
- **Tenant Configuration**: Connect your Teams tenant using webhook URLs or Power Automate flows
- **Channel-Specific Notifications**: Configure different notification preferences and flow types for different Teams channels
- **Event Types Supported**:
  - Task assignments
  - Task completions
  - Tasks becoming blocked
  - Pitch shaping events
  - Cycle phase changes (start, cooldown)
  - Betting completion
  - Sprint starts

## Flow Types

ShipFlow supports three different integration methods, each optimized for specific use cases:

### 1. Traditional Webhook
- **Best for**: Standard Teams setups with webhook support
- **URL Format**: `https://outlook.office.com/webhook/...`
- **Features**: Direct integration, fastest setup
- **Payload Format**: Adaptive Card format compatible with Teams

### 2. Power Automate (Post to Channel)
- **Best for**: Modern Teams environments, enterprise setups
- **URL Format**: `https://...powerplatform.com/.../invoke?...`
- **Features**: Posts messages directly to channel feed
- **Payload Format**: Optimized for Power Automate flows with rich formatting

### 3. Power Automate (Create Thread)
- **Best for**: Organized discussions, threaded conversations
- **URL Format**: `https://...powerplatform.com/.../invoke?...` (with thread-specific flow)
- **Features**: Creates new conversation threads for each notification type
- **Payload Format**: Structured for threaded discussions

> **Note**: ShipFlow automatically detects the flow type based on your webhook URL and applies the appropriate message format for optimal compatibility.

## Setup Instructions

### Option 1: Traditional Teams Incoming Webhook (Recommended if available)

#### 1. Create a Teams Incoming Webhook

1. Go to your Teams channel where you want to receive notifications
2. Click the **three dots (...)** next to the channel name
3. Select **Workflows** (or **Connectors** in older Teams versions)
4. Look for **"Incoming Webhook"** and click **Add**
5. Click **Configure**
6. Give it a name like **"ShipFlow Notifications"**
7. Optionally upload an icon (you can use your company logo)
8. Click **Create**
9. Copy the **Webhook URL** (it should look like `https://outlook.office.com/webhook/...`)

#### 2. Configure ShipFlow

1. Log in to ShipFlow as an administrator
2. Navigate to **Teams Integration** from the sidebar menu
3. Click **Configure Tenant**
4. Fill in the following details:
   - **Tenant Name**: A friendly name for your Teams tenant (e.g., "Company Teams")
   - **Webhook URL**: Paste the webhook URL from step 1
   - **Default Channel** (optional): The default channel name for notifications
   - **Enable Teams Integration**: Toggle to enable/disable notifications
5. Click **Save**

### Option 2: Power Automate Flow (For Modern Teams/Enterprise)

If traditional webhooks are not available (common in newer Teams or enterprise environments), you can use Power Automate:

#### 1. Create a Power Automate Flow

1. Go to **Power Automate** (https://make.powerautomate.com)
2. Click **Create** → **Instant cloud flow**
3. Name it **"ShipFlow Teams Notifications"**
4. Select **"When an HTTP request is received"** as the trigger
5. Click **Create**

#### 2. Configure the HTTP Trigger

1. In the **"When an HTTP request is received"** step:
   - Set **Who can trigger the flow** to **"Anyone"** (this is crucial!)
   - Optionally, add this JSON schema for better structure:
   ```json
   {
       "type": "object",
       "properties": {
           "title": {"type": "string"},
           "message": {"type": "string"},
           "text": {"type": "string"},
           "notificationType": {"type": "string"},
           "timestamp": {"type": "string"},
           "source": {"type": "string"},
           "themeColor": {"type": "string"},
           "entityType": {"type": "string"},
           "entityId": {"type": "integer"}
       }
   }
   ```

#### 3. Add Teams Action

1. Click **New step**
2. Search for **"Post message in a chat or channel"**
3. Select your **Team** and **Channel**
4. Set **Message** to something like:
   ```
   **@{triggerBody()?['title']}**
   
   @{triggerBody()?['message']}
   
   _Source: @{triggerBody()?['source']} • Type: @{triggerBody()?['notificationType']} • @{triggerBody()?['timestamp']}_
   ```

#### 4. Save and Get the URL

1. Click **Save**
2. Go back to the **"When an HTTP request is received"** trigger
3. Copy the **HTTP POST URL** (it will look like `https://...powerplatform.com/.../invoke?...`)

#### 5. Configure ShipFlow

1. Log in to ShipFlow as an administrator
2. Navigate to **Teams Integration** from the sidebar menu
3. Click **Configure Tenant**
4. Fill in the following details:
   - **Tenant Name**: A friendly name (e.g., "Company Teams - Power Automate")
   - **Webhook URL**: Paste the Power Automate HTTP POST URL from step 4
   - **Default Channel** (optional): The channel name used in the flow
   - **Enable Teams Integration**: Toggle to enable/disable notifications
5. Click **Save**

## Testing the Integration

1. After configuring either option above, go to the **Teams Integration** page in ShipFlow
2. Find your configuration and click **Send Test Notification**
3. You should receive a test message in your Teams channel
4. If it doesn't work, check the troubleshooting section below

## Channel-Specific Notifications (Optional)

You can configure notification preferences for specific channels:

1. Go to the **Channel Notifications** tab in Teams Integration
2. Click **Add Channel**
3. Specify:
   - **Channel Name**: The Teams channel name
   - **Channel Webhook URL** (optional): A specific webhook for this channel
   - **Notification Preferences**: Choose which events to receive for this channel

## Supported Event Types

| Event Type | Description | Default Enabled |
|------------|-------------|-----------------|
| Task Assigned | When a task is assigned to someone | ✅ |
| Task Completed | When a task is marked as completed | ✅ |
| Task Blocked | When a task becomes blocked | ❌ |
| Pitch Shaped | When a new pitch is shaped | ✅ |
| Cycle Started | When a new cycle begins | ✅ |
| Cycle Cooldown | When a cycle enters cooldown phase | ✅ |
| Betting Completed | When betting phase is completed | ❌ |
| Sprint Started | When a sprint starts | ❌ |

## Troubleshooting

### "Connection reset by peer" Error
- This usually means you're using the wrong type of URL
- Make sure you're using a Teams webhook URL (`https://outlook.office.com/webhook/...`) or properly configured Power Automate flow

### "AuthorizationFailed" Error (Power Automate)
- Your Power Automate flow is not set to accept anonymous requests
- Edit your flow → HTTP trigger → Set "Who can trigger the flow" to "Anyone"

### "No suggested workflows" in Teams
- Your organization might have disabled traditional webhooks
- Use the Power Automate option instead
- Contact your IT admin about enabling Teams connectors

### Messages not appearing in Teams
- Check if the webhook URL is correct and hasn't expired
- For Power Automate: Check the flow run history for errors
- Verify the channel name matches exactly (case-sensitive)

### Test notification fails
- Check your internet connection
- Verify the webhook URL is complete and unmodified
- For Power Automate: Ensure the flow is turned on and published

## Advanced Configuration

### Custom Message Format (Power Automate)

If you're using Power Automate, you can customize the message format by modifying the Teams action in your flow. ShipFlow sends these fields:

- `title`: Notification title with emoji
- `message`: Main notification text
- `notificationType`: Type of event (TEST, TASK_ASSIGNED, etc.)
- `timestamp`: When the event occurred
- `source`: Always "ShipFlow"
- `themeColor`: Hex color code for visual theming
- `entityType`: Type of entity (Task, Pitch, Cycle, etc.)
- `entityId`: ID of the entity (optional)

### Multiple Channels

You can set up multiple Teams integrations for different channels or teams:

1. Create separate webhook/flow configurations
2. Add multiple tenant configurations in ShipFlow
3. Use channel-specific settings to route different notifications

## Security Considerations

- **Webhook URLs contain sensitive tokens** - treat them like passwords
- **Don't share webhook URLs** in public repositories or documentation
- **Regenerate webhooks periodically** for security
- **For Power Automate**: Only use "Anyone" access for dedicated notification flows
- **Monitor webhook usage** in Teams admin center or Power Automate

## Upgrade Guide (Existing Installations)

If you're upgrading from a previous version of ShipFlow, the Teams integration has been enhanced with flow type support:

### Database Migration
- The system automatically adds a `flow_type` column to existing Teams channel configurations
- Existing configurations are set to `WEBHOOK` type by default
- No action required - your existing webhooks will continue working

### New Features Available After Upgrade
1. **Flow Type Selection**: Choose between webhook, Power Automate post, or Power Automate thread
2. **Enhanced UI**: Updated setup guide with clear instructions for both integration types
3. **Smart Detection**: Automatic flow type detection based on URL format
4. **Improved Error Messages**: Better guidance when setup issues occur

### Recommended Actions After Upgrade
1. **Review Existing Configurations**: Check your current Teams integrations in the UI
2. **Set Appropriate Flow Types**: Update flow types if you're using Power Automate
3. **Test Notifications**: Send test messages to ensure everything works correctly
4. **Update Documentation**: Share the new setup instructions with your team

## User Mention Configuration (v0.9.0+)

Teams notifications can @mention the relevant user in channel messages. When a task is assigned, blocked, completed, or a user is mentioned in a comment, the notification will tag the assignee directly in Teams.

### Setup

1. Each user opens **Profile** in ShipFlow
2. In the **Notification IDs** section, enter their **Teams User ID**
3. Click the save button

### What to Enter as Teams User ID

Enter your **Microsoft email address** (User Principal Name), e.g., `john.doe@company.com`. This is the same email you use to sign into Microsoft Teams.

### How It Works

- When a notification targets a specific user, ShipFlow looks up their Teams User ID from the `notification_user_mapping` table
- If found, the message includes an `<at>` tag that Teams renders as an @mention in Adaptive Cards
- If no mapping exists, the notification is sent without a mention (graceful fallback)

### API

```
GET    /api/users/me/notification-mappings
PUT    /api/users/me/notification-mappings   { "providerName": "teams", "externalUserId": "john@company.com" }
DELETE /api/users/me/notification-mappings/teams
```

## Plugin Architecture (v0.9.0+)

All notification providers (Slack, Teams, future integrations) implement the `NotificationProvider` interface. Adding a new provider (e.g., Discord) requires only:

1. A `@Service` class implementing `NotificationProvider`
2. Implement `getProviderName()`, `sendNotification()`, `isActive()`, and optionally `resolveUserMention()`
3. Spring auto-discovers the bean — zero changes to existing code

## FAQ

**Q: Can I use both traditional webhooks and Power Automate?**
A: Yes, you can configure multiple tenants with different approaches.

**Q: Why do I see Power Platform URLs instead of Teams webhooks?**
A: Microsoft is migrating Teams integrations to Power Automate. This is normal for newer/enterprise Teams environments.

**Q: Can I customize the notification appearance?**
A: Yes, especially with Power Automate flows where you have full control over the message format.

**Q: How many notifications will I receive?**
A: This depends on your team's activity and which event types you enable. You can fine-tune this in the channel notification settings.

**Q: Can I disable notifications temporarily?**
A: Yes, use the "Enable Teams Integration" toggle in the tenant configuration.