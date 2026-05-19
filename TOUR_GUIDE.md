# ShipFlow Onboarding Tour Guide

This file is the **single source of truth** for the interactive onboarding tour.

> **Rule**: Whenever a UI element targeted by the tour is moved, renamed, or removed,
> you MUST update both `TourContext.tsx` and this document in the **same PR**.
> The CI pipeline will not catch a broken tour step — only this doc + manual testing will.

---

## Architecture Overview

| File | Responsibility |
|------|---------------|
| `frontend/src/contexts/TourContext.tsx` | All 21 tour steps, driver.js config, navigation, skip-confirm |
| `frontend/src/components/WelcomeTourDialog.tsx` | Auto-shown welcome modal for first-time users |
| `frontend/src/styles/tour.css` | Custom dark-theme styling for driver.js popovers — imported from `TourContext.tsx` |
| `frontend/src/components/Layout.tsx` | Renders `<WelcomeTourDialog>`, exposes "Start Tour" / "Restart Tour" buttons in sidebar + topbar |

**Library**: [driver.js](https://driverjs.com/) v1.4.0 — installed as `driver.js` in `frontend/package.json`.

**How it's triggered**:
1. **Auto** — `WelcomeTourDialog` appears 1500 ms after first login (guarded by `localStorage.shipflow_welcome_shown`).
2. **Manual** — "Start Tour" / "Restart Tour" button in the left sidebar (desktop) and top navbar (mobile).
3. **Programmatic** — call `useTour().startTour()` from any component.

---

## localStorage Keys

| Key | Value | Purpose |
|-----|-------|---------|
| `shipflow_tour_completed` | `"true"` | Set when tour finishes or user confirms skip. Hides welcome dialog on next visit. Changes sidebar button label to "Restart Tour". |
| `shipflow_welcome_shown` | `"true"` | Set when welcome dialog is dismissed (either "Start" or "Skip"). Prevents the dialog from showing again. |

**Reset the tour for testing** (browser console):
```js
localStorage.removeItem('shipflow_tour_completed');
localStorage.removeItem('shipflow_welcome_shown');
location.reload();
```

Or call `useTour().resetTour()` from a component — this removes `shipflow_tour_completed` only.

---

## Step Inventory

Each step has a `data-tour` attribute on its target element. The table below is the canonical mapping.

> Column **"Selector file"** is the source file that owns the `data-tour="…"` attribute.
> If that element changes (wrapping div added, component extracted, etc.) you must update the attribute there AND verify the tour step still highlights correctly.

| # | Step Title | `data-tour` selector | Selector file | Route |
|---|-----------|---------------------|---------------|-------|
| 1 | Welcome to ShipFlow! | `sidebar` | `Layout.tsx:279` | `/` |
| 2 | Projects | `projects-menu` | `Layout.tsx:100` (navItem `tourId`) | `/` |
| 3 | Create Your First Project | `new-project-btn` | `Projects.tsx` | `/projects` |
| 4 | Project Created! | `project-card` | `Projects.tsx` (first card, `index === 0`) | `/projects` |
| 5 | Cycles | `cycles-menu` | `Layout.tsx:101` (navItem `tourId`) | `/projects` |
| 6 | Create a Cycle | `new-cycle-btn` | `CycleList.tsx:157` | `/cycles` |
| 7 | Your Cycle | `cycle-card` | `CycleList.tsx:225` (first card, `index === 0`) | `/cycles` |
| 8 | Pitch Board | `pitches-menu` | `Layout.tsx:112` (navItem `tourId`) | `/cycles` |
| 9 | Create a Pitch | `new-pitch-btn` | `PitchBoard.tsx:608` | `/pitches` |
| 10 | Kanban Board | `pitch-board` | `PitchBoard.tsx:661` / `665` | `/pitches` |
| 11 | Betting Table (nav) | `betting-menu` | `Layout.tsx:113` (navItem `tourId`) | `/pitches` |
| 12 | Plan Your Cycle | `betting-table` | `BettingTable.tsx:475` | `/betting` |
| 13 | Health Overview (nav) | `health-menu` | `Layout.tsx:114` (navItem `tourId`) | `/betting` |
| 14 | Hill Chart | `hill-chart-section` | `HealthOverview.tsx:115` | `/health` |
| 15 | Retrospectives | `retros-menu` | `Layout.tsx:115` (navItem `tourId`) | `/retros` |
| 16 | Reports | `reports-menu` | `Layout.tsx:117` (navItem `tourId`) | `/reports` |
| 17 | Meetings | `meetings-menu` | `Layout.tsx:147` (navItem `tourId`) | `/meetings` |
| 18 | Backlog | `backlog-menu` | `Layout.tsx:354` | `/backlog` |
| 19 | Work Logs | `worklogs-menu` | `Layout.tsx:367` | `/time/logs` |
| 20 | Sprint Planning | `sprint-planning-board` | `SprintPlanningPage.tsx` (two-column board container) | `/sprint-planning` — **Conditional: only included in the tour when the active project has `projectType === 'SCRUM'`**. `getTourSteps()` in `TourContext.tsx` filters this step out for non-SCRUM projects (which would otherwise be redirected to `/backlog`, breaking the tour). |
| 21 | Project Selector | `project-selector` | `Layout.tsx:537` | `/health` |
| 22 | You're All Set! | `user-menu` | `Layout.tsx:639` | `/health` |

### How `navItem` tourIds work

Sidebar nav items in `Layout.tsx` receive their `data-tour` attribute dynamically:

```tsx
// Layout.tsx ~line 180
<NavItem data-tour={item.tourId} ... />
```

The `tourId` is declared in the nav item arrays near the top of `Layout.tsx`:

```ts
{ textKey: 'nav.projects', icon: Folder, path: '/projects', tourId: 'projects-menu' },
```

If you **rename or remove a nav item**, update its `tourId` there, and update the corresponding step in `TourContext.tsx`.

---

## Navigation Behaviour

The tour navigates between routes automatically. The flow is:

1. User clicks **Next →**
2. `onNextClick` increments `currentStepIndex`
3. If the next step has a different `route`, the driver calls `navigate(route)` and then `moveNext()` after a 600 ms delay
4. A `useEffect` on `location.pathname` watches for **manual** navigation — if the user navigates to a route that is **not** part of the tour, the tour is destroyed. Navigation between routes that are still used by other tour steps does **not** stop the tour.

**Important**: Steps 3 and 4 share the same route (`/projects`). This is intentional — step 3 navigates to `/projects` and highlights the button, then step 4 stays on the same page and highlights the card.

**Known behaviour — same-route transitions take ~600 ms**: The `onNextClick` and `onPrevClick` handlers capture `location.pathname` in a closure at the time the driver is created. For steps that share a route, the stale closure value may differ from the actual current pathname, causing the handler to call `navigate()` (a no-op) and wait 600 ms before advancing. This has no visible side-effect beyond a short delay and is acceptable for the current release.

---

## Demo Data Dependency

Steps 4 and 7 target `[data-tour="project-card"]` and `[data-tour="cycle-card"]` respectively.
These selectors only match the **first** card (`index === 0`). They depend on demo data being present.

`SampleDataInitializer.java` seeds two projects and two cycles on startup (dev/demo profile).
If you're running the tour on a **blank instance with no data**, steps 4 and 7 will skip
(driver.js falls back to a full-screen popover when the element is not found).

To make the tour fully work end-to-end on a blank instance:
- Complete steps 3 (create project) and 6 (create cycle) manually before clicking Next on step 4 and 7
- Or rely on the seeded demo data (recommended for demos)

---

## Adding a New Tour Step

1. Add a `data-tour="my-feature"` attribute to the element in the relevant component.
2. Add a new `TourStep` entry in the `getTourSteps()` array in `TourContext.tsx`, with the correct `route`.
3. Add a row to the **Step Inventory** table above.
4. If the step navigates to a new route, verify the 600 ms navigation delay is sufficient for the target page to render. Increase if necessary.
5. Update `CHANGELOG.md` (tour step count changed).

---

## Removing or Renaming a Step

1. Remove or update the `data-tour` attribute from the element.
2. Remove or update the step in `getTourSteps()`.
3. Update the Step Inventory table — adjust step numbers for all rows below the change.
4. Run the tour manually end-to-end to verify it still transitions smoothly.

---

## Changing a Targeted Element

If you refactor a component and the element that owns a `data-tour` attribute moves:

1. Move the `data-tour` attribute to the new location.
2. Verify the `side` and `align` values in the TourContext step still make visual sense (e.g., a step that was `side: 'right'` on a sidebar item might need to become `side: 'bottom'` if it moves to a topbar).
3. Update the **Selector file** column in this document.

---

## Disabling the Tour for a Specific User

Set `shipflow_tour_completed = "true"` in `localStorage`. The welcome dialog will not appear and the sidebar button will show "Restart Tour" instead of "Start Tour".

---

## Testing the Tour

**Quick reset** (browser console):
```js
localStorage.clear(); location.reload();
```

**Step-by-step checklist**:
- [ ] Welcome dialog appears ~1.5 s after first login
- [ ] "Start Tour" button launches step 1 (sidebar highlighted)
- [ ] Each "Next →" advances the step and highlights the correct element
- [ ] Steps 3, 6, 9 navigate to `/projects`, `/cycles`, `/pitches` correctly
- [ ] Steps 12, 14 navigate to `/betting`, `/health` correctly
- [ ] Clicking the ✕ close button shows the skip confirmation dialog
- [ ] Confirming skip marks tour as complete (`shipflow_tour_completed = "true"`)
- [ ] Cancelling skip resumes the tour on the current step
- [ ] Tour completes on step 21 and sets `shipflow_tour_completed = "true"`
- [ ] After completion, sidebar button shows "Restart Tour"
- [ ] Clicking "Restart Tour" starts from step 1 again

---

## i18n

Tour button labels (Start Tour / Restart Tour) are in `en.json` / `fa.json` under:

```json
"layout": {
  "startTour": "Start Tour",
  "restartTour": "Restart Tour"
}
```

Skip confirmation dialog strings are under:

```json
"tour": {
  "skipTitle": "...",
  "skipDescription": "...",
  "skip": "...",
  "continue": "..."
}
```

Welcome dialog strings are under `"welcomeTour"`.

Step titles and descriptions are **hardcoded** in `TourContext.tsx` (not i18n keys). If you need to internationalise them, extract them to `en.json` / `fa.json` and use `t()`.

---

## Relationship to In-App Help Guides

The tour gives a **first-run surface-level overview**. The in-app help guides (`/help`) provide **deep-dive reference docs** per feature. They are complementary:

- Tour → "Here's where everything lives, click Next to explore"
- Help guides → "Here's how the Betting Table works in detail"

The `/help` page has a nav item with `tourId: 'help-menu'` (available in Layout.tsx for future tour steps if needed).

---

*Last updated: 2026-05-17 — step 20 (Sprint Planning) is now conditionally filtered to SCRUM projects only (PR #285 round-5 fix)*
