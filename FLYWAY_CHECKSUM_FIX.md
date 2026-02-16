# Flyway Migration Checksum Mismatch - Resolution Guide

## Issue
If you encounter Flyway checksum mismatch errors for migrations `V2026_02_14_0003` and `V2026_02_14_0004`, this is expected after recent updates where these migrations were converted to no-ops.

## Error Message
```
Migration checksum mismatch for migration version 2026.02.14.0003
-> Applied to database : 1203553530
-> Resolved locally    : 1427956216
```

## Resolution Options

### Option 1: Flyway Repair (Recommended for Development)
Run the Flyway repair command to update the schema history checksums:

```bash
cd backend
./mvnw flyway:repair
```

This will update the checksums in the `flyway_schema_history` table to match the current migration files.

### Option 2: Fresh Database (For Local Development)
If you're working with a local development database and don't need to preserve data:

1. Drop and recreate your database
2. Run the application - Flyway will apply all migrations fresh

```sql
DROP DATABASE IF EXISTS shipflow;
CREATE DATABASE shipflow;
```

### Option 3: Production/Staging Environments
For production or staging environments:

1. **DO NOT** drop the database
2. Run `flyway:repair` to fix the checksums
3. Deploy the new migration `V93__remove_scope_from_bug_reports.sql`
4. The new migration will properly remove the `scope_id` column from `bug_reports`

## What Changed?

### Migrations Modified
- `V2026_02_14_0003__fix_task_status_enum.sql` - Converted to no-op (functionality moved to V88)
- `V2026_02_14_0004__fix_completed_task_status.sql` - Converted to no-op (functionality moved to V88)

### New Migration
- `V93__remove_scope_from_bug_reports.sql` - Removes redundant `scope_id` column from `bug_reports` table

## Why Was This Change Made?

Tasks are now integrated with scopes through a bidirectional relationship. The `scope_id` field in `bug_reports` became redundant since traceability is provided through:
- `bug_reports.task_id` → `tasks.scope_id` → `hill_chart_points`

This simplifies the data model while maintaining full traceability.

## Prevention

To avoid checksum mismatches in the future:
- **NEVER** modify migration files that have already been applied to any database
- Always create new migrations for schema changes
- Use Flyway's `validate` command before deploying: `./mvnw flyway:validate`
