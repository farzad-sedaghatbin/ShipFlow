# Contributing

Thank you for your interest in contributing to ShipFlow!

::: tip Full guide
See [`CONTRIBUTING.md`](https://github.com/farzad-sedaghatbin/ShipFlow/blob/main/CONTRIBUTING.md) in the repository for the complete contribution guide including branch naming, PR templates, and code review expectations.
:::

## Quick start

1. Fork the repository
2. Create a branch: `git checkout -b feature/my-feature`
3. Make your changes following the [coding conventions](#coding-conventions)
4. Run tests: `./mvnw verify` (backend) and `npm test` (frontend)
5. Open a PR targeting `main`

## Branch naming

| Prefix | Use for |
|--------|---------|
| `feat/` | New features |
| `fix/` | Bug fixes |
| `docs/` | Documentation only |
| `chore/` | Build, deps, config |
| `refactor/` | Code restructuring |
| `test/` | Tests only |

## Coding conventions

### Backend (Java)
- Formatting: Spotless (run `./mvnw spotless:apply` before committing)
- Layering: Controller → Service → Repository — never skip layers
- DTOs at every controller boundary — never expose entities directly
- Soft delete only (`deletedAt` timestamp) — never hard-delete user data

### Frontend (TypeScript)
- React Query for server state, React Context for global UI state
- Tailwind CSS utility classes — no custom CSS unless absolutely necessary
- All user-facing strings through `i18next` — add keys to both `en.json` and `fa.json`

## Coverage gate

JaCoCo enforces **≥ 80% line coverage** on the backend. PRs that drop below this threshold will fail CI.
