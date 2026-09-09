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

## Future direction: configurable methodology structure (design analysis, not a plan)

A tester's feedback (2026-09) asked for something categorically different from
the gap above: not de-duplicating "does this type have X" checks, but letting
an org **customize** the structure/vocabulary at project-creation time instead
of picking one of exactly three fixed presets. The concrete complaint was
Scrum projects inheriting Shape Up vocabulary and structure in places that
were never properly gated — the AI task-suggestion dialog's "Pitch" source
badge and `taskCategory` values (`PITCH_SCOPE`/`DEBT_IMPROVEMENT` — there is
no Scrum-native task category at all, so a Scrum project's own opportunistic
work is filed under a Shape-Up-named bucket), the `cycles.searchCycles`
placeholder leaking onto the Sprints page, `cycleDetailPage.cycleNotes`
showing on Sprint Detail, etc. Several of these were fixed as straightforward
gating bugs in the same session this section was written (see CHANGELOG's
`[Unreleased]` entries around 2026-09-09) — those were real bugs (a capability
check that should have existed and didn't), not evidence that the underlying
three-preset model itself is wrong. This section is about the model question
that's left over once the bugs are fixed: **should ShipFlow's three fixed
methodology presets become configurable?**

This is a real, larger product/architecture decision — evaluated here, not
decided. No implementation should start from this section alone; treat it as
the input to a dedicated Plan Mode session, per this repo's normal workflow
for anything above trivial scope.

### Option A — per-project capability toggles (data-driven `capabilities`)

Instead of resolving `ProjectTypeCapabilities` from a fixed
`Record<ProjectType, ...>` keyed by a 3-value enum, store the capability set
(or a delta from a preset) per project and resolve it from data. An org could
then compose something between the current three presets — e.g. Scrum
structure with Shape Up's Pitch concept layered on top for teams that shape
work before committing it to a sprint.

**Cost**: large and cross-cutting. `ProjectType` is not just a UI-gating enum
— it's read directly in backend authorization/query logic (`hasPitches`,
category defaults in `TaskService.bulkCreate`, `AirGappedModeValidator`-style
type checks, etc.) and in every frontend consumer listed above. Making
capabilities *data* rather than a compile-time constant means every one of
those call sites needs to load project-scoped configuration instead of
switching on a fixed enum value, and the "three known-good combinations"
guarantee disappears — testing now has to cover a combinatorial capability
space instead of three fixed points. This is genuinely a multi-session,
foundational change, not a feature addition.

### Option B — terminology/label overrides only (no structural change)

Keep the three fixed structural presets exactly as they are today — they
encode real, different data-model shapes (Pitch vs. no-Pitch, cycle-as-sprint
vs. cycle-as-shape-up-cycle) that aren't arbitrary. Add a much narrower
per-org or per-project **label override** layer: an admin can rename what a
Pitch/Cycle/Sprint/Task is called in the UI (e.g. "Pitch" → "Feature",
"Sprint" → "Iteration") without touching which capabilities are structurally
available. Implementation sketch: a small `labelOverrides: Record<string,
string>` resolved alongside `capabilities` in `useProject()`, consulted by
`t()` call sites for the handful of methodology-noun keys, falling back to
the existing i18n string when no override is set. This directly answers the
"wrong vocabulary" half of the original complaint without touching structure,
authorization, or the data model at all.

**Cost**: small and additive. New per-org/per-project settings storage (a
JSON column is enough — no new entity), a resolution layer parallel to the
existing `capabilities` one, and updating call sites for the ~10-15 literal
methodology-noun strings to go through it. No backend authorization logic
changes, no new combinatorial testing surface.

### Option C — do nothing further; keep closing gaps opportunistically

Treat the concrete complaints as already substantially addressed (this
session's gating fixes, the new Scrum-native reports/board/AI-parity work)
and the remaining ~20-item ad hoc-check list as already tracked above. Defer
"real" customizability indefinitely, revisited only if it's independently
requested by more than one org — the same demand-gating discipline this
repo's roadmap already applies to full CRDT wiki co-editing (see `CLAUDE.md`'s
future-milestones table: "demand-gated, skip if still no multi-user usage").

### Recommendation

**Option C for now, Option B if and when this is requested again.** A single
tester's suggestion, on a repo whose own roadmap explicitly demand-gates
comparable speculative asks elsewhere, doesn't justify Option A's blast
radius, and most of the concrete pain reported so far turned out to be
ordinary missing-gate bugs rather than a structural limitation — the presets
being fixed wasn't actually what was making Scrum feel like Shape Up in a
trenchcoat, missing/wrong gating was. If this comes up again independently,
Option B is the right-sized answer: it fixes the vocabulary complaint
directly, stays additive, and doesn't put the "three known-good
combinations" property at risk. Option A stays on the table only if multiple
orgs need genuinely different *structure* (not just different words) inside
one project — nothing gathered so far indicates that's the actual need.
