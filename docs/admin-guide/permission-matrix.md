# Permission Matrix

ShipFlow uses role-based access control (RBAC) with 6 built-in roles.

::: tip Full matrix
See [`PERMISSION_MATRIX.md`](https://github.com/farzad-sedaghatbin/ShipFlow/blob/main/PERMISSION_MATRIX.md) in the repository for the complete per-endpoint permission table.
:::

## Roles

| Role | Description |
|------|-------------|
| `ADMIN` | Full system access including organization settings, user management, API keys |
| `PROJECT_MANAGER` | Manage projects, cycles, betting table, pitches |
| `DEVELOPER` | Create and update tasks and scopes |
| `QA` | Manage test cases, test runs, and bug reports |
| `PRODUCT` | Manage pitches and roadmap items |
| `VIEWER` | Read-only access to all content |

## Assigning roles

Admins assign roles in **Organization Settings → Users**.

## API key scopes

API keys have their own scope separate from user roles:

| Scope | Capabilities |
|-------|-------------|
| `READ` | All GET endpoints |
| `WRITE` | Read + mutation endpoints |
| `ADMIN` | Full access including settings |
