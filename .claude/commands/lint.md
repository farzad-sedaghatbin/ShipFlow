Run all ShipFlow linters. Argument: $ARGUMENTS (check | fix). Default: fix.

Run these three checks in order:

1. **Spotless (backend Java formatter)**
   - `fix`: `cd backend && ./mvnw spotless:apply`
   - `check`: `cd backend && ./mvnw spotless:check`

2. **i18n validation** (always runs): `cd frontend && npm run validate:i18n`
   - Report any keys missing from `en.json` or `fa.json`, grouped by locale.

3. **TypeScript type check** (always runs): `cd frontend && npx tsc --noEmit`
   - Report any type errors with file + line.

After running, summarize: how many Spotless violations were fixed (or found if check-only), whether i18n is clean, and whether TypeScript compiled without errors.
