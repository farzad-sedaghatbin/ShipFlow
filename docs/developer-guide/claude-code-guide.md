# Claude Code Guide

This page describes how AI coding assistants (Claude Code, Cursor, etc.) should work in this repository.

::: tip Source of truth
[`CLAUDE.md`](https://github.com/farzad-sedaghatbin/ShipFlow/blob/main/CLAUDE.md) in the repository root is the authoritative guide. Read it before making any changes.
:::

## Common tasks

### Add a new REST endpoint

1. Create/update DTO in `dto/<feature>/`
2. Add method to the relevant `*Service`
3. Add method to the relevant `*Controller` with `@PreAuthorize`
4. Add Flyway migration if schema changed
5. Write a service test in `src/test/`
6. Run `./mvnw spotless:apply && ./mvnw verify`

### Add a new MCP tool

1. Add the tool method to the relevant `*McpTools` class in `service/mcp/server/tools/`
2. Register it in `McpToolDispatcher` — add to `READ_TOOLS` or `WRITE_TOOLS` map
3. Map to the existing service layer — **never bypass it**
4. Add unit tests in `McpToolDispatcherTest`
5. Update `MCP_CLIENT_SETUP.md` tool reference table

## End-of-session checklist

Every session must complete before creating a PR:

1. `CHANGELOG.md` — add entry under `[Unreleased]`
2. `README.md` — update if user-visible feature added
3. `frontend/src/pages/ReleaseNotes.tsx` — add highlight card
4. `en.json` + `fa.json` — add i18n keys
5. Tests — ≥ 80% JaCoCo line coverage
6. `SampleDataInitializer.java` — add demo data for new features
7. Run `./mvnw spotless:apply && ./mvnw verify` and `npm test`
