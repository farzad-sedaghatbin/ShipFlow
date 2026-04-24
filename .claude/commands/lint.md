Run all ShipFlow lint and validation checks. Argument: $ARGUMENTS (check | fix). Default: check.

Run these checks in order:

1. **Backend build + tests**: `cd backend && ./mvnw verify`
   - Report any build errors or test failures.

2. **i18n validation**: `cd frontend && npm run validate:i18n`
   - Report any keys missing from `en.json` or `fa.json`, grouped by locale.

3. **TypeScript type check**: `cd frontend && npx tsc --noEmit`
   - Report any type errors with file + line.

After running, summarize: whether the backend build is clean, whether i18n is clean, and whether TypeScript compiled without errors.
