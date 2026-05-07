---
globs: frontend/**/*.{ts,tsx}
---

# ShipFlow React/TypeScript Rules

**State management**: React Query for all server state. React Context (Auth, Project, Theme, Toast, Tour) for global UI state. Do not introduce Redux or Zustand.

**API calls**: Use typed service files in `frontend/src/services/`. Never call `fetch` or `axios` directly inside components or hooks.

**Forms**: React Hook Form + Zod validation. No uncontrolled inputs for complex forms.

**Styling**: Tailwind CSS 4 utility classes. Radix UI primitives for interactive elements (dialogs, selects, dropdowns). No inline `style={}` for layout.

**i18n — mandatory for every user-facing string**: Use `useTranslation()` / `t('key')`. Add the key to BOTH:
- `frontend/src/i18n/locales/en.json`
- `frontend/src/i18n/locales/fa.json`

Missing a locale breaks Farsi/RTL support. Run `npm run validate:i18n` to check.

**Component placement**: Reusable components → `components/`. Page-level → `pages/`. Custom hooks → `hooks/`.

**Public pages sync**: `ReleaseNotes.tsx` and `PublicRoadmap.tsx` must always match. Adding a feature to one requires updating the other in the same PR.

**Tour selectors**: If you add or move a UI element with a `data-tour="..."` attribute, update `TourContext.tsx` and the Step Inventory table in `TOUR_GUIDE.md`.
