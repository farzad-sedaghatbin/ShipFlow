# Project Type Architecture

ShipFlow supports **three** project methodologies. This doc describes the current
state of the art — how project type is modeled, and how the UI adapts to it.

> **History note**: this doc originally covered only `SHAPE_UP`/`KANBAN` (the
> v0.x design). `SCRUM` shipped in v1.1.0 and was never folded in here — this
> version replaces that stale content. See the CHANGELOG's `[Unreleased]`
> entry (context-aware dashboard/navigation work) for the session that fixed
> the drift this staleness caused.

---

## The three project types

| Type | Concept | Primary cadence view | Shape-Up-only features |
|------|---------|----------------------|-------------------------|
| `SHAPE_UP` | 6-week cycles, betting table | Cycles | Pitches, Betting, Hill Charts, Cooldown |
| `SCRUM` | Sprints (same underlying `Cycle` entity, relabeled), story points | Sprints | — (no Pitches/Betting; has Sprint Planning) |
| `KANBAN` | Continuous flow board | Backlog board | — (no cycle concept exposed to users at all) |

Backend enum: `com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType`
(`SHAPE_UP`, `KANBAN`, `SCRUM`). Frontend: `frontend/src/types/index.ts`'s
`ProjectType` string union (same three values). `Project.projectType` defaults
to `SHAPE_UP` for backward compatibility when not specified.

A Kanban project gets an automatically-created, permanently-hidden "Continuous
Flow" `Cycle` row (`ProjectService.create()`) so the rest of the data model
(tasks belong to a cycle) doesn't need a special case — but no UI ever shows
it. Scrum sprints and Shape Up cycles are the same `Cycle` entity; Scrum adds
`sprintGoal` and reports progress via `taskCount` (stories = Tasks), while
Shape Up reports via `pitchCount` (Pitches). This distinction matters: **a
Scrum "story" is a `Task` entity, not a `Pitch` entity** — several bugs (see
below) came from code that assumed "cycle progress" always means "pitch
progress."

---

## The capability-config pattern

Prior to the work described in this doc's history note, nav items, mobile
tabs, quick links, and dashboard content were each hand-coded per surface,
independently, with no shared source of truth. They drifted: the mobile
bottom nav (`MobileBottomNav.tsx`) never checked project type at all and kept
showing "Cycles"/"Pitches" tabs to Kanban-only orgs, long after the desktop
sidebar (`Layout.tsx`) had grown real Kanban/Scrum gating.

**The fix**: `frontend/src/config/projectTypeCapabilities.ts` is now the
single source of truth for "what does this project type support":

```ts
export interface ProjectTypeCapabilities {
  projectType: ProjectType | null;
  hasCycles: boolean;   // Shape Up + Scrum
  hasPitches: boolean;  // Shape Up only
  isScrum: boolean;
  nav: { mainItems, workspaceItems, showWorkspace, workspaceSectionTitleKey,
         workspaceGroupTitleKey, showSprintPlanning, promoteReportsTopLevel };
  mobile: { primaryTabs: NavItemConfig[] };
  quickLinkIds: QuickLinkId[];
  shortcutIds: ShortcutId[];
  dashboard: { showActiveCyclesStat, showTotalPitchesStat, showCompletedStat,
               showInProgressStat, overviewWidgetTypes };
  defaultWidgetTypes: string[];
}

export const PROJECT_TYPE_CAPABILITIES: Record<ProjectType, ProjectTypeCapabilities>;
export function resolveOrgCapabilities(orgProjectTypes: ProjectType[]): ProjectTypeCapabilities;
export function resolveCapabilities(currentProjectType: ProjectType | null, orgProjectTypes: ProjectType[]): ProjectTypeCapabilities;
```

**Consumers** (all read `capabilities` from `useProject()` rather than
re-deriving their own project-type checks):
- `Layout.tsx` (desktop sidebar) — `capabilities.nav.*`
- `MobileBottomNav.tsx` (bottom tabs + "More" drawer) — `capabilities.nav.*` and `capabilities.mobile.primaryTabs`
- `QuickLinks.tsx` / `useKeyboardShortcuts.ts` — `capabilities.quickLinkIds` / `shortcutIds`
- `Dashboard.tsx` (stat cards) / `DashboardTabs.tsx` (Overview tab) — `capabilities.dashboard.*`
- `DashboardCustomizer.tsx` — `capabilities.defaultWidgetTypes` (via `resolveOrgCapabilities`, not the current-project capabilities — widget preferences are per-user, not per-project)

**Backend mirror**: `DashboardWidgetService.java` has its own small,
hand-mirrored version of the same widget-applicability logic (`GENERIC_WIDGETS`
/ `CYCLE_WIDGETS` / `SHAPE_UP_ONLY_WIDGETS`, `resolveDefaultWidgetTypesForDeployment()`),
used only when seeding a brand-new user's default widget rows. If you change
one side, check the other — there's no shared package between frontend and
backend, so this is a deliberate, documented duplication, not a service call.

### The org-level aggregate — the actual root-cause fix

`ProjectContext.tsx` exposes:

```ts
orgProjectTypes: ProjectType[];        // distinct projectType values across `projects`
capabilities: ProjectTypeCapabilities; // resolveCapabilities(currentProjectType, orgProjectTypes)
```

The previous design's `isShapeUpProject` returns `true` whenever
`currentProject === null` ("All Projects" — the default landing state for any
new user), "for legacy compatibility." Nothing checked what project types the
org actually had, so **an org whose only projects were Kanban still got
Shape-Up nav/dashboard/quick-links by default**, because every "All Projects"
code path assumed Shape Up. `capabilities` fixes this: in "All Projects" mode
it resolves via `resolveOrgCapabilities(orgProjectTypes)` — a Kanban-only org's
`orgProjectTypes = ['KANBAN']` correctly resolves to Kanban capabilities. A
mixed-type org resolves to the richest type present (Shape Up ⊃ Scrum ⊃
Kanban in nav/feature surface) so no active project's features are hidden; a
brand-new org with zero projects yet falls back to the minimal Kanban-shaped
baseline, never to Shape Up.

`isShapeUpProject`/`isStrictlyShapeUp`/`currentProjectType` still exist on
`ProjectContext` (kept for now — they had zero consumers left after this work,
so removing them was unnecessary risk) but new code should use `capabilities`
instead.

---

## Adding a new project-type-gated UI surface

1. Check `frontend/src/config/projectTypeCapabilities.ts` first — if what
   you need is "does this project type have X," it likely already exists or
   is a one-line addition to the shared interface + all three per-type
   objects (`shapeUp`, `scrum`, `kanban`).
2. Only reach for a page-local `isKanbanProject`/`isScrumProject` check
   (from `useProject()`) when the check is genuinely local to one file — the
   value of the shared config is de-duplication across ≥2 files. The
   codebase still has ~20 such ad hoc checks (`BacklogHeader.tsx`,
   `Reports.tsx`, `WorkLogsPage.tsx`, `TestCasesPage.tsx`, `CycleDetail.tsx`,
   `SprintPlanningPage.tsx`, etc.) that predate `projectTypeCapabilities.ts`
   and haven't been migrated — migrate opportunistically when you're already
   touching one of those files for unrelated work, not as a standalone sweep.
3. **Before assuming "cycle progress" or "cycle content" applies uniformly**:
   check whether the underlying data is Pitch-based (Shape-Up-only) or
   Task-based (works for Scrum's "stories" too, and Kanban). `CycleProgressWidget.tsx`
   had exactly this bug — it filtered to `SHAPE_UP` cycles only because its
   progress numbers came from `pitchService`, not `taskService`. It now
   branches per cycle: Pitch-based counts for Shape Up, Task-based ("stories",
   `t.status === 'DONE'` via `taskService.getMy`) for Scrum — use this as the
   reference pattern for any future cycle-scoped widget.
4. Mirror any backend-relevant capability logic in
   `DashboardWidgetService.java` if it affects what gets seeded/offered —
   see "Backend mirror" above.

## Reproducing an org-type-pure fixture locally

The normal dev database is always mixed-type by design — `SampleDataInitializer`
seeds one Shape Up project (MBA) and one Kanban project (DVP), and the dev
profile also enables `ScrumDemoInitializer`'s Scrum project (MAS). A mixed org
can **never** exercise the "All Projects mode defaults to Shape Up regardless
of what the org actually has" bug class (see the CHANGELOG's `[Unreleased]`
entry) — `resolveOrgCapabilities` correctly resolves to Shape Up whenever a
Shape Up project exists anywhere, so that's not a bug in a mixed DB.

To reproduce a genuinely Kanban-only org (the exact scenario that surfaced
that bug — a white-label deployment with no Shape Up/Scrum projects at all),
start against a **fresh** database with:
```
app.sample-data.enabled=false
app.scrum-demo.auto-create=false
app.kanban-demo.auto-create=true
```
`KanbanDemoInitializer` (new, `@Order(4)`, off everywhere by default including
the dev profile — see its javadoc) then seeds a single Kanban project
("Customer Support — Kanban Demo", key `SUP`) with a hidden Continuous Flow
cycle and a handful of tasks. For the **Scrum-only** equivalent, no new code
was needed: `app.sample-data.enabled=false` with the existing
`app.scrum-demo.auto-create=true` already produces a Scrum-only org, since
`ScrumDemoInitializer` falls back to whatever user exists (the
always-present `admin`, created unconditionally by `DefaultAdminInitializer`)
when its preferred demo user (`sara`) is absent.

## Known gaps (tracked, not yet done)

- The ~20 ad hoc per-project-type checks listed in step 2 above are not
  migrated to `capabilities` — this is deliberate (avoiding a large,
  low-value mechanical sweep), not an oversight, but worth revisiting file by
  file as each is next touched.
