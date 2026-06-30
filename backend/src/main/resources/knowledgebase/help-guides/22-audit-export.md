# Audit Trail Export

ShipFlow records a full change history for key entities using Hibernate Envers. Admins can export that audit trail for compliance, security review, or offline analysis.

# What's Audited

Change history is tracked for **tasks**, **bug reports**, **pitches**, and **test cases**. Each revision captures who made the change, when, the change type (created / modified / deleted), and the before/after values of the audited fields.

# Exporting the Audit Trail

The export is **admin-only**. Open **Organization Settings → Audit Export**, then:

1. Choose the **entity type** — Task, Bug Report, Pitch, Test Case, or **All**.
2. Optionally set a **From** and **To** date to limit the range (inclusive). Leave blank for the full history.
3. Pick a **format** — **CSV** (spreadsheet-friendly) or **JSON** (machine-readable).
4. Click **Export**. The file downloads to your browser.

## CSV columns

`entityType, entityId, revision, timestamp, modifiedBy, changeType, field, oldValue, newValue`

There is **one row per changed field** — a single revision that changed three fields produces three rows. Deletions produce a single row with no field values. CSV output is RFC-4180 quoted and guarded against spreadsheet formula injection.

# API

The same data is available programmatically (admin auth required):

```
GET /api/audit/export?entityType=task&format=csv&from=2026-01-01&to=2026-06-30
```

- `entityType`: `task` | `bug` | `pitch` | `testcase` | `all` (default `all`)
- `format`: `csv` | `json` (default `csv`)
- `from`, `to`: ISO dates `YYYY-MM-DD`, inclusive (optional)

The endpoint returns HTTP 400 for an unknown entity type or when `from` is later than `to`.

# Notes

- Audit data is never hard-deleted — soft-deleted records still appear in the trail with a `DELETED` change type.
- The `modifiedBy` column shows the user's display name where it can be resolved, otherwise the stored username (`system` for automated changes).
