# Workflow Automations Guide

Workflow Automations let you define trigger-based rules that react automatically to events in your project — no code required. Available from the **Automations** page in the sidebar (⚡ icon).

---

## What are Workflow Automations?

An automation rule has two parts:
- **Trigger** — the event that starts the rule (e.g. a task is completed, a cycle starts, scope creep is detected)
- **Action** — what happens when the trigger fires (e.g. notify the team, send a webhook, add a comment)

Rules run asynchronously in the background and never slow down the action that triggered them.

---

## Getting Started

### Who can manage automations?
Project Managers, Admins, and users with the **PRODUCT** role can create, edit, enable/disable, and delete automation rules. Developers and Viewers can view the rules and execution history but cannot modify them.

### Creating your first rule

1. Navigate to **Automations** in the sidebar (⚡)
2. Make sure a project is selected at the top
3. Click **"Browse Templates"** to install a pre-built rule in one click, or click **"New Rule"** to build a custom rule from scratch
4. Choose a **Trigger** (what event fires the rule)
5. Choose an **Action** (what should happen)
6. Optionally fill in action configuration (e.g. a webhook URL or a message template)
7. Click **Save**

### Installing from a template

ShipFlow ships 20 built-in templates grouped into four categories:

| Category | Examples |
|----------|---------|
| **Tasks** | Notify team when a task is completed; notify assignee when a task is assigned |
| **Shape Up** | Notify members when a cycle starts; alert when appetite is exceeded; detect scope creep |
| **Automation** | Send a webhook on any task status change; create a follow-up task on completion |
| **Notifications** | Notify on comment added; notify when betting table is locked |

Click **"Browse Templates"** → search or filter by category → click **"Use Template"** next to the one you want.

---

## Trigger Types

| Trigger | When it fires |
|---------|---------------|
| **Task Created** | A new task is added to the project |
| **Task Status Changed** | A task moves to any new status |
| **Task Assigned** | A task is assigned to a user |
| **Task Completed** | A task status changes to DONE/COMPLETED |
| **Pitch Created** | A new pitch is added |
| **Pitch Status Changed** | A pitch moves between IDEA, DRAFT, SHAPED, BET, etc. |
| **Cycle Started** | A cycle transitions to IN_PROGRESS |
| **Cycle Ended** | A cycle is marked COMPLETED or CANCELLED |
| **Cycle Status Changed** | Any cycle status transition |
| **Comment Added** | A comment is posted on any task or pitch |
| **Betting Table Locked** | The betting table is locked for the cycle *(Shape Up only)* |
| **Hill Chart Moved** | Any scope dot is dragged on the hill chart *(Shape Up only)* |
| **Appetite Exceeded** | Actual hours exceed the pitch appetite *(Shape Up only)* |
| **Scope Creep Detected** | New tasks are added to a cycle after it has started *(Shape Up only)* |

---

## Action Types

| Action | What it does |
|--------|-------------|
| **Notify Assignee** | Sends an in-app notification to the task's assignee |
| **Notify Project Members** | Sends an in-app notification to all project members |
| **Send Webhook** | POSTs the event payload to a URL you provide |
| **Send Email** | Sends an email notification (requires SMTP configured in Org Settings) |
| **Add Comment** | Posts an automatic comment on the triggering task or pitch |
| **Change Task Status** | Updates the task's status to a specified value |
| **Create Task** | Creates a new task in the project |

### Message templates

Action configs support `{{key}}` placeholders that are replaced with event data at runtime. Available placeholders depend on the trigger:

- `{{taskName}}` — name of the triggering task
- `{{assignee}}` — username of the assignee
- `{{status}}` — new status value
- `{{projectName}}` — project name
- `{{cycleName}}` — cycle name (cycle triggers)
- `{{pitchName}}` — pitch name (pitch triggers)

Example webhook message body: `"Task {{taskName}} was completed by {{assignee}} in project {{projectName}}"`

---

## Managing Rules

### Enable / disable a rule
Each rule has a toggle switch. Disabling a rule pauses it without deleting it — useful during testing or maintenance windows.

### Edit a rule
Click **Edit** on any rule card to open the rule form. You can change the name, trigger type, action type, and configuration.

### Delete a rule
Click the trash icon. Rules are soft-deleted (recoverable by an admin) — they disappear from the list immediately.

---

## Execution History

Every time an automation fires, a log entry is created with:
- **Status**: SUCCESS, FAILURE, or SKIPPED
- **Trigger data**: the raw event payload
- **Result message**: what the action did (or why it failed)
- **Timestamp**: when it ran

View logs two ways:
1. **Per-rule** — click the ⏱ (history) icon on any rule card to open a slide-over with that rule's last 50 runs
2. **Project-wide** — click the **Execution History** tab at the top of the Automations page to see all runs across all rules in the project

---

## Common Questions

**Why isn't my automation firing?**
- Check that the rule is **enabled** (toggle is on)
- Make sure the correct **project is selected** — rules are project-scoped
- Check the Execution History for FAILURE or SKIPPED entries — they include the reason

**Can I test a rule without waiting for the trigger to happen naturally?**
Not directly from the UI. The easiest workaround is to perform the triggering action manually (e.g. change a task status to DONE to test a Task Completed trigger) and then check the Execution History.

**What happens if a webhook action fails?**
The automation logs a FAILURE entry with the HTTP status code and error message. It does not retry automatically — fix the webhook URL and the next real trigger will try again.

**Can rules run in a chain (automation triggering another automation)?**
No — an action taken by an automation (e.g. "Create Task") does not itself trigger other automations. This prevents infinite loops.

**Who can see automation rules?**
All project members can view the Rules list and Execution History. Only Project Managers and Admins can create, edit, enable/disable, or delete rules.
