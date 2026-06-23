# Import & Migration

ShipFlow can import your existing work from other tools so you don't have to start from scratch. You can import from a **CSV file** (Jira, Linear, Asana, or a generic spreadsheet), pull live data through the **Jira API** or **Linear API**, or import test cases from a **Zephyr Scale** export.

Open the importer at **Import** (`/import`). You can also reach it from **Organization Settings → Import Data**. Importing creates a brand-new project, so it requires **Admin** or **Manager** (Project Manager) permissions.

## Supported Sources

| Source | What it brings in |
|--------|-------------------|
| **Jira CSV** | Issues → tasks (bugs → bug reports), epics, sprints → cycles, story points |
| **Linear CSV** | Issues → tasks, projects → epics, cycles, estimates |
| **Asana CSV** | Tasks, sections → status, assignees, due dates, tags |
| **Generic CSV** | Any spreadsheet — maps common columns (title/name, status, priority, assignee, type) |
| **Jira API** | Live pull of issues, sprints, and epics via Atlassian OAuth |
| **Linear API** | Live pull of issues, cycles, and projects via Linear OAuth |
| **Zephyr Scale (.xlsx)** | Test cases (imported as Test Cases, not tasks) |

## How to Import a CSV File

1. Go to **Import** in the sidebar.
2. Stay on the **CSV** tab.
3. **Upload** your file — drag and drop, or click to browse (`.csv` only).
4. Enter a **Project Name** (e.g. "Migrated from Jira"). A new project is created with this name.
5. Leave **Source Format** on **Auto-detect** (recommended), or pick Jira / Linear / Asana / Generic explicitly.
6. Click **Start Import**.
7. When it finishes, the **Done** screen shows **Total Rows**, **Imported**, and **Failed** counts. Expand **Error Details** to see any rows that were skipped.
8. Click **View Projects** to open your new project, or **Import Another File** to start over.

Auto-detect inspects the file's columns. Content-based detection wins over your manual hint — a well-formed Jira export is recognized as Jira even if you picked something else.

## How to Import Live from Jira (API)

The Jira API import requires an admin to have configured the Atlassian OAuth app (`JIRA_OAUTH_CLIENT_ID` / `JIRA_OAUTH_CLIENT_SECRET`). If it isn't configured, the Jira tab tells you so.

1. Open the **Jira** tab on the Import page.
2. Click **Connect Jira** and approve access in the Atlassian login window.
3. After redirect, click **Choose project…** and select the Jira project to import.
4. Enter a **New project name** and pick a **Project type** (Kanban or Scrum — defaults to Kanban).
5. Click **Import from Jira**.
6. Use **Disconnect** to revoke the connection or **Change project** to pick a different one.

Sub-tasks are excluded; closed/past sprints are imported as cycles already in their cooldown phase.

## How to Import Live from Linear (API)

Requires `LINEAR_OAUTH_CLIENT_ID` / `LINEAR_OAUTH_CLIENT_SECRET` to be configured.

1. Open the **Linear** tab.
2. Click **Connect Linear** and approve access.
3. Click **Choose team…** and select your Linear team.
4. Enter a **New project name**, pick **Kanban** or **Scrum**, and click **Import from Linear**.

## How to Import Test Cases (Zephyr Scale)

1. Open the **Zephyr** tab.
2. Upload your `.xlsx` (or `.xls`) export and click **Import Test Cases**.
3. Review the per-row report (use **Failures only** to focus on errors).
4. Optionally **link** the imported test cases to a pitch or a task: pick a pitch and/or enter a Task ID, then click **Link … test cases**.

Linking test cases additionally allows the **QA** role, not just Admin/Manager.

## What Gets Created

- Imports always create a **new project** (CSV imports are always **Kanban**; API imports let you choose Kanban or Scrum).
- **Issues / tasks** map to ShipFlow tasks. Status and priority are normalized (e.g. Done/Resolved/Closed → Done; Highest/Critical/High → High).
- **Bug-type issues** (type "Bug", "Defect", "Error", etc.) become **Bug Reports** instead of tasks, with severity mapped from priority.
- **Epics** become ShipFlow Epics; **sprints/cycles** become ShipFlow cycles.
- **Story points / estimates** carry over to tasks.
- **Assignees** are matched to existing people by name or email — people are **not** auto-created, so add your team first if you want assignments to stick.

## Import History

Every import is recorded as a job with its file name, source, status, and row counts. Admins can see all import jobs; everyone else sees their own.

## Troubleshooting

**"Linear/Jira OAuth app is not configured."** An admin must set the OAuth client ID and secret environment variables before API imports work. CSV import always works without OAuth.

**Some rows show as Failed.** A row is skipped (not the whole import) when it's missing a required field — the title/summary for Jira/Linear/Asana. Open **Error Details** to see which rows and why.

**Assignees didn't import.** People are matched by name or email against existing ShipFlow users. Add the team members first, then re-import, or assign manually afterward.

**Columns look wrong / a column is missing.** Files exported from Excel sometimes include a hidden byte-order mark or duplicate column names — ShipFlow strips the mark and detects duplicate headers automatically, but if detection is off, choose the exact **Source Format** instead of Auto-detect.

**My dates didn't import.** Supported date formats are `YYYY-MM-DD`, `MM/DD/YYYY`, and `DD/MM/YYYY`. Other formats are left blank.
