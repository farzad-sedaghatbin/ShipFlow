Run ShipFlow tests. Argument: $ARGUMENTS (backend | frontend | all | e2e | coverage). Default: all.

- **backend**: `cd backend && ./mvnw test` — show a summary of any failing tests with their first error message.
- **frontend**: `cd frontend && npm test -- --run` — show failure summary.
- **coverage**: `cd backend && ./mvnw verify` — report JaCoCo line coverage %. Flag if below the 80% gate.
- **e2e**: `cd frontend && npm run test:e2e` — note this requires both backend (port 8080) and frontend (port 3000) dev servers to be running first.
- **all** (default): Run backend tests, then frontend tests, then report combined pass/fail status.

After running, if there are failures show the failing test names and first error for each. Offer to investigate and fix the root cause.
