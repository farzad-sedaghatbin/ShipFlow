Run the mandatory end-of-session checklist for ShipFlow before creating the PR.

For the current session's changes, check each item below and report status as ✅ done / ⚠️ needs attention / — not applicable:

1. **CHANGELOG.md** — Is there an entry under `[Unreleased]` for work done this session?
2. **README.md** — If a user-visible feature was added, is it in the Features list? Were screenshots refreshed?
3. **COMPETITOR_ANALYSIS.md** — Does this close a gap vs Linear/Jira/Asana that should be noted?
4. **ReleaseNotes.tsx** — Is there a highlight card at `frontend/src/pages/ReleaseNotes.tsx`?
5. **PublicRoadmap.tsx** — Is `ReleaseNotes.tsx` in sync with `PublicRoadmap.tsx`? (Same milestones, titles, statuses.)
6. **i18n** — Were all user-facing strings added to BOTH `frontend/src/i18n/locales/en.json` AND `fa.json`? Run: `cd frontend && npm run validate:i18n`
7. **Tests** — Do tests pass and does JaCoCo show ≥ 80% line coverage? Run: `cd backend && ./mvnw verify`
8. **SampleDataInitializer.java** — Does the demo seed data reflect the new feature?
9. **TourContext.tsx** — If the UI layout changed, are `data-tour` selectors still valid? Is the Step Inventory in `TOUR_GUIDE.md` updated?
10. **Help guides** — If any `*_GUIDE.md` references changed UI, is it updated?
11. **Backend build** — Does `cd backend && ./mvnw verify` pass cleanly with no build or test failures?
12. **PR branch** — Is the branch named with `feat/fix/chore/refactor/test/docs` prefix, targeting `main`?

After checking, list anything that needs attention and offer to fix it.
