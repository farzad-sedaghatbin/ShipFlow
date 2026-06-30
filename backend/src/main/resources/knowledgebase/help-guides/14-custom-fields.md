# Custom Fields

Custom Fields let your team attach extra metadata to Tasks, Pitches, and Bug Reports — without touching the database schema. Use them to track things like Priority Tier, Target QA Date, Story Points (team-specific), or any other structured data your workflow needs.

## Creating a Custom Field

1. Go to **Settings → Custom Fields** (admin sidebar).
2. Pick the entity type the field applies to: **Tasks**, **Pitches**, or **Bug Reports**.
3. Click **Create Field** and fill in:
   - **Name** — shown as the field label in every task/pitch/bug detail view.
   - **Type** — one of: Text, Number, Date, Single Select, Multi-Select, Checkbox, or URL.
   - **Scope** — leave blank for an org-wide field (visible on all projects) or pick a specific project.
   - **Required** — when checked, the field must be filled before saving entity values.
   - **Options** (for Select/Multi-Select) — type an option and press Enter; click × to remove.
4. Click **Save**. The field immediately appears on every relevant detail page.

## Editing a Field

Click the **pencil icon** on any field card. You can change the name, description, options, sort order, and required flag. The **field type** and **entity type** are locked after creation — delete and recreate if you need a different type.

## Deleting a Field

Click the **trash icon** and confirm. This permanently removes the field definition **and all values** that users have saved for it. This action cannot be undone.

## Filling in Field Values

Open any Task, Pitch, or Bug Report detail. Scroll to the **Custom Fields** section. Fill in the fields and click **Save**. Values are saved per-entity; different tasks can have different values for the same field.

## Field Types

| Type | Input | Storage |
|------|-------|---------|
| Text | Plain text box | Raw string |
| Number | Numeric input | Decimal string (e.g. `"42.5"`) |
| Date | Date picker | ISO date (e.g. `"2026-07-15"`) |
| URL | URL input with link icon | Raw string |
| Checkbox | Toggle switch | `"true"` or `"false"` |
| Select | Dropdown | Selected option string |
| Multi-Select | Multi-chip dropdown | JSON array (e.g. `["A","B"]`) |

## Permissions

| Role | Definitions | Values |
|------|------------|--------|
| ADMIN | Create, edit, delete (org-wide + project-scoped) | Read + set |
| MANAGER | Create, edit, delete (project-scoped only) | Read + set |
| MEMBER | — | Read + set |
| READONLY | — | Read only |

## API

All endpoints live under `/api/custom-fields`.

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/definitions?entityType=TASK&projectId=1` | List definitions |
| `POST` | `/definitions` | Create definition |
| `PUT` | `/definitions/{id}` | Update definition |
| `DELETE` | `/definitions/{id}` | Delete definition (cascades values) |
| `GET` | `/values?entityType=TASK&entityId=42` | Get values for an entity |
| `PUT` | `/values` | Upsert a single value |
| `PUT` | `/values/bulk` | Bulk upsert values |
