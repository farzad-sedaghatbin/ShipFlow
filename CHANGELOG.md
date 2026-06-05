# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- **SSO Frontend (S33)**: Full identity-provider management UI for admins under Organization Settings → SSO tab. Supports OIDC and SAML 2.0 providers via a form dialog with provider-type-conditional fields (client ID/secret/discovery URL for OIDC; entity ID/SSO URL/certificate for SAML 2.0). Enabled/Disabled toggle and Enforce SSO toggle (with a destructive warning). Edit/Delete actions via Radix DropdownMenu. Admin CRUD routes backed by `ssoService.ts`.
- **SSO Login buttons**: The Login page now queries `GET /api/sso/providers` on mount; if any enabled identity providers exist a "or sign in with" divider and one button per provider are shown. Clicking initiates the SSO flow via `GET /api/sso/initiate/{idpId}` and redirects the browser to the IdP.
- **SSO Callback page** (`/sso-callback`): Public route that processes the JWT from `?token=` query param returned by the backend after a successful IdP redirect, stores it in AuthContext, and navigates to the intended deep-link (`?redirect=`) or `/dashboard`.
- **S37a — Deep-link routing review**: Confirmed all detail routes (`/pitches/:id`, `/backlog/:taskId`, `/cycles/:cycleId`, etc.) are flat top-level registrations that work as direct deep links. ProjectContext bootstraps from localStorage and re-hydrates from the API; pages that require a project use a `<ProjectRequiredDialog>` guard rather than redirect, preserving the URL. No routing changes required; review comment added to `App.tsx`.

## [1.3.0] - 2026-06-05

### Added
- **MCP Server Runtime Toggle**: Enable or disable the built-in MCP server from Integrations → MCP — a DB-backed toggle that overrides the `MCP_SERVER_ENABLED` environment default and takes effect immediately, no restart required.
- **MCP Write-Tools Toggle**: Separately allow connected AI tools to create and update data; write tools are automatically disabled whenever the server is off.
- **API Key Management UI**: Create, list, and revoke API keys from a new "API Keys" tab. Keys support READ / WRITE / ADMIN scopes and an optional expiry date. The raw key (`sf_…`) is shown **once** at creation with a copy button and a clear "copy it now — it won't be shown again" warning.
- **Admin API Key Oversight**: Admins see all API keys across the entire organization — who created each one and when — and can revoke any user's key directly from the MCP Integration → API Keys tab. New backend endpoints `GET /api/api-keys/admin` and `DELETE /api/api-keys/admin/{keyId}` are guarded by `@PreAuthorize("hasRole('ADMIN')")`.

### Fixed
- **QA test cases not showing on `/qa/test-cases`**: Three bugs combined to hide all test cases from the list. (1) Race condition: `loadTestCases` and `loadCyclesAndPitches` fired concurrently on page mount; the project-scoped filter ran with `cycles = []` (stale closure), making `projectPitchIds` an empty Set and filtering out every record. After cycles loaded, the list never re-evaluated because `cycles`/`pitches` were absent from the effect dependency array. Fix: moved the project filter out of the async function and into a reactive `useMemo` so it re-evaluates whenever cycles or pitches finish loading. (2) Test cases created without a pitch association were always hidden when a project was selected (`tc.pitchId && ...` short-circuited); changed condition to `!tc.pitchId || projectPitchIds.has(tc.pitchId)` so unassigned test cases are visible. (3) The `findWithFilters` JPQL query in `TestCaseRepository` was missing `AND tc.deletedAt IS NULL`, causing soft-deleted records to reappear whenever any filter was active.

### Added
- **Admin API key oversight**: Admins can now see all API keys across the entire organization in the MCP Integration → API Keys tab, including who created each key and when. A new "Created by" column is shown for admins, and admins can revoke any user's key directly. New backend endpoints `GET /api/api-keys/admin` and `DELETE /api/api-keys/admin/{keyId}` are guarded by `@PreAuthorize("hasRole('ADMIN')")`.

### Improved
- **AI Risk Advisor now uses full project context**: Risk analysis previously saw only the single pitch in isolation. Two layers of enrichment added:
  - *Layer 1 — Structured DB context*: Each risk analysis now includes the pitch's epic goals and status, initiative/roadmap alignment, sibling pitches in the same epic (with same-team warnings), all other pitches sharing the cycle, and the last 3 risk history snapshots showing trend direction. Same data injected into the Risk Q&A advisor prompts.
  - *Layer 2 — Enriched vector store*: Pitch embeddings now include problem statement, proposed solution, rabbit holes, known risks, no-gos, epic name, and initiative name — making semantic similarity search far more meaningful. Epic embeddings now include per-pitch appetite, cycle assignment, and problem summary. After every AI risk analysis, a `Risk Summary` knowledge item is stored (risk level, score, insights, recommendations) so future analyses can retrieve historical patterns for similar pitches.

### Fixed
- **Chat Q&A no longer hallucinates cycle/pitch data**: The "Ask about your active cycles" AI chat was returning wrong pitches and confusing cycle display names with database IDs (e.g. a cycle named "Cycle 7" with db id 9 caused the AI to say "there is no Cycle 7"). Four root causes fixed in `QAService`:
  - *Structured entity context*: For entity-scoped questions (e.g. contextType=cycle), the actual pitches, dates, and phase are now loaded from the DB and injected as `=== STRUCTURED DATA (primary source) ===` before vector search results. The LLM no longer guesses at relational facts.
  - *ID leak removed*: The system prompt no longer appends `(ID: <n>)` to entity headers. Users and models should never see or reason about internal primary keys.
  - *Anti-hallucination guardrails*: System prompt now includes 5 strict grounding rules — answer only from provided context, never invent entities, never fabricate missing data, treat cycle names as display labels not IDs, and state uncertainty explicitly.
  - *cycleId sync*: When `contextType=cycle` and `contextId` is set but `cycleId` is null, they are now synced so the metadata-based vector filter fires correctly for every cycle-scoped question.

## [1.2.1] - 2026-06-03

### Added
- **`update_task_assignee` MCP write tool**: Reassign an existing task to a person (by `assigneeUsername`, `assigneeId`, or `mine: true`) or clear the assignee with `unassign: true`. Previously the only mutation on an existing task was `update_task_status` — agents who needed to reassign had to delete and recreate. Now an agent can answer "find the unassigned bug-fix task in this cycle and take it" in two MCP calls (`get_tasks` + `update_task_assignee`).
- **MCP tools for the QA domain — test cases and test runs**: New read tools `get_test_cases(taskId | pitchId | cycleId)`, `get_test_case(testCaseId)`, `get_test_runs(testCaseId)` surface acceptance criteria (preconditions, steps, expectedResult) and execution history that were previously invisible to MCP clients despite a full TestCase/TestRun domain existing in the backend. New write tool `record_test_run(testCaseId, status, notes, actualResult, buildVersion, environment)` lets an agent record PASSED/FAILED/BLOCKED/SKIPPED outcomes after verifying. `McpTaskDetailDTO` now carries `testCaseCount`, and `get_task_context` hints prompt the agent to call `get_test_cases` whenever a task has linked criteria.
- **MCP tools for the bug-tracking domain**: New read tools `get_bug_reports(taskId | pitchId | cycleId)` and `get_bug_report(bugReportId)` plus write tool `update_bug_status(bugReportId, status, resolution)`. Closes the asymmetry where `add_comment` already accepted `entityType: BUG_REPORT` but no MCP tool could read bugs. `McpTaskDetailDTO` gains `bugReportCount`; `get_task_context` hints surface linked bugs.
- **`whoami` MCP tool — caller identity**: Returns `userId`, `username`, `email`, `role`, `personId`, and `fullName` of the authenticated MCP user. Unblocks the natural "tasks assigned to me" phrasing — agents can resolve the caller's `personId` once and pass it to filters.
- **Filters on `get_tasks`: `pitchId`, `assigneeId`, `mine`**: `get_tasks` previously only accepted `cycleId` or `projectId`, forcing client-side filtering. New params compose with existing scopes (e.g. `cycleId + mine` returns the caller's tasks within that cycle) and `mine: true` resolves to the authenticated user's `personId` without a separate `whoami` round-trip.
- **`create_task` accepts `parentTaskId` — subtasks via MCP**: The `create_task` write tool now takes an optional `parentTaskId`, so an agent can break a task into subtasks programmatically. The created child appears under the parent in `get_task_context.task.children`.
- **`get_task_context` MCP tool — task-rooted context aggregator for coding agents**: New tool that takes a single `taskId` and returns everything an AI agent needs to implement that task in one call — the task itself (with full dependency graph and subtasks), its parent pitch (Shape Up problem/solution/rabbit-holes/risks/no-gos plus `wireframeLinks`), the parent cycle, sibling tasks under the same pitch (capped at 50, max 200), and a server-generated `hints` array (e.g. "pitch.wireframeLinks is present — fetch the design via a Figma MCP", "task is BLOCKED by 2 task(s)", "pitch.solution is empty"). Closes the gap where `get_work_context` required a `pitchId`/`cycleId` the agent didn't have when starting from a task. `get_work_context` now also accepts `taskId` as a third entry point and resolves to the task's parent pitch (or cycle, for Kanban tasks). `McpTaskDTO` also gains a `pitchId` field so the manual `get_task` → `get_pitch_detail` flow no longer requires parsing `pitchTitle`.

- **MCP server admin UI**: Admins can enable/disable the built-in MCP server at runtime from Integrations → MCP → "MCP Server" tab. This is a DB-backed toggle that overrides the `MCP_SERVER_ENABLED` environment default and **takes effect immediately — no restart required**. A second toggle controls MCP write tools (disabled while the server is off). The tab also shows live status plus the SSE URL and bearer-auth instructions for Claude Code.
- **API key management UI**: Create, list, and revoke API keys from the new "API Keys" tab. Keys support READ / WRITE / ADMIN scopes and an optional expiry date. The raw key (`sf_…`) is shown **once** at creation with a copy button and a clear "copy it now — it won't be shown again" warning. (The API-key REST endpoints at `/api/api-keys` already existed; this adds the management UI and a runtime MCP-enablement service, `McpServerSettingsService`.)

### Fixed
- **Document (and task attachment) downloads failing after server migration**: The `docker-compose.yml` had no volume for the uploads directory, so all uploaded files lived inside the container's ephemeral layer and were lost on every `docker compose up --build` or server migration. Added a `uploads_data` named volume mounted at `/app/uploads` and set `UPLOAD_DIR=/app/uploads` in the app service. **To recover existing files**: copy the `uploads/` directory from the old server into the new Docker volume (`docker cp` or `docker run --rm -v uploads_data:/data ...`).
- **Document drop zone scrolled page to top on click**: Clicking the "Drag and drop documents here" area on pitches (and meetings/cycles) caused the page to jump to the top before sometimes opening the file picker. The `<input type="file">` inside the `<label>` drop zone had `class="sr-only"`, which makes it a 1×1 px `position:absolute` element. When the label forwards its click event to that input, the browser scroll-into-view behavior fired and scrolled the `<main>` scroll container back to `scrollTop:0`. Changing the class to `hidden` (`display:none`) prevents the scroll entirely while still allowing the label-to-input click forwarding to open the file picker.
- **Task attachment download returned 401**: The download button on a task's attachments list was a plain `<a href>` pointing at the API path. Because the JWT lives in localStorage and is only attached by the axios request interceptor, the browser sent no `Authorization` header and the backend rejected the request. The button now fetches the file as a blob through the authenticated axios client and triggers the browser save from an object URL.
- **Betting board crashed on pitches without an appetite**: A `SHAPED` pitch with no appetite (`appetiteDays = null`) made the betting board throw HTTP 500s — drag-and-drop placement NPE'd in `BettingTableService.canPitchFitInSlot`, and recording a decision violated the `betting_decisions.requested_appetite_days` NOT NULL constraint. Root cause was `PitchService.updatePitch` flipping a pitch to `SHAPED`+ without the appetite check that `markAsShaped`/`validatePitchForStatus` enforce. Now: `updatePitch` validates appetite for `SHAPED`+ (prevents appetite-less betting candidates), the drag preview returns a clean "doesn't fit" instead of NPE-ing, and both placement and betting decisions reject appetite-less pitches with a clear 400 (`error.betting.pitch.no.appetite`, en + fa) instead of a 500.
- **Hill chart "failed to save point" on pre-cycle pitches**: Adding a scope to a pitch that isn't bet into a cycle yet (board/shaping) threw `"Pitch must have a cycle to create a linked task"`. The scope now saves standalone and the auto-linked task is only created once the pitch has a cycle.
- **Pitch tasks now always appear on the hill chart (Scope-Task Bridge)**: Creating a task from a pitch was sending `createScopeAutomatically: false`, so no scope was created. Removed the client-side flag entirely — scope auto-creation is now fully backend-owned (`hasPitch && isRootTask && noExistingScope`) and cannot be suppressed by any client.
- **Auto-created scope position reflects task status**: A scope auto-created for a task was pinned at position 0 regardless of the task's status, so a task added as `IN_PROGRESS`/`DONE` showed no progress on the hill. The initial position is now derived from the task's status.
- **Misleading DOR/DOD meeting badges**: Badges showed green even when checklist items were unchecked (an untouched all-optional checklist counted as "ready"). Badges now show a `completed/total` count and only turn green when all required items are done with real engagement; partial completion shows amber, untouched shows outline. Applied on both the pitch detail meetings card and the Meeting List table.
- **Roadmap not showing pitches under orphan epics**: Epics not attached to an initiative rendered an expand control but never listed their pitches. The orphan-epics section now renders pitch rows when expanded, matching the initiative-grouped section.
- **MEMBER users got 403 on all bug and test-case writes (stale role names)**: `QATestManagementController` `@PreAuthorize` annotations referenced roles that no longer exist (`QA`, `DEVELOPER`, `TEAM_LEAD`, `PROJECT_MANAGER`). Spring never matched them, so every `MEMBER` user got silently rejected. Updated to the current four-role model (`MEMBER`, `MANAGER`, `ADMIN`).
- **Notes NPE on pitches without a cycle**: `NoteService.setRelatedIds` called `pitch.getCycle().getId()` unconditionally. Pitches in IDEA, DRAFT, or SHAPED status have no cycle assigned, so creating a note on any such pitch threw `NullPointerException → 500`. Added null guard before accessing cycle/team.
- **Saving wireframe links blocked for PENDING/ACTIVE pitches**: `PitchService.updatePitch` ran the full status validation on every PUT, including in-place Shape Up field edits. If `appetiteDays` was null, saving wireframe links, solution, or any other field on a PENDING/ACTIVE pitch returned 400. Validation is now only applied when the status actually changes.
- **Document upload: silent failure on unsupported file types**: `DocumentDropZone` silently dropped files outside the allowed list (PDF/DOCX/DOC/TXT/MD) with no feedback. Now shows a 4-second error banner listing the rejected filenames and allowed types.
- **Work logs date range filter**: Added server-side `fromDate`/`toDate` filters to the times/logs page. Both ends must be set; setting only one is ignored. Filter works across all pages (not just the current paginated page). Date filter inputs appear in both the My Logs and Team Logs filter bars.
- **Change Role dialog showed `{{username}}` literally**: `UserManagement.tsx` was appending the username as a separate JSX node rather than passing it as an i18n interpolation variable. The Farsi translation was also missing the `{{username}}` placeholder.
- **`update_task_assignee` MCP tool**: New write tool allowing agents to reassign or unassign existing tasks — by `assigneeUsername`, `assigneeId`, `mine: true`, or `unassign: true`.

## [1.2.0] - 2026-05-24

### Added
- **Jira API import — Atlassian OAuth 2.0 + REST API (v1.2.0 S30)**: Connect ShipFlow to Jira Cloud via Atlassian OAuth 2.0 (3-legged) and import issues, sprints, and epics directly. Jira Epics → ShipFlow Epics, Sprints → Cycles, Issues → Tasks with priority and status preserved. Atlassian Document Format descriptions extracted to plain text. Cloud workspace auto-detected via accessible-resources. 19 new i18n keys, Jira tab on the import page alongside CSV and Linear. New endpoints: `GET|POST /api/import/jira/authorize`, `GET /api/import/jira/callback`, `GET /api/import/jira/projects`, `DELETE /api/import/jira/disconnect`, `POST /api/import/jira`.
- **Linear API import — OAuth2 + GraphQL (v1.2.0 S29)**: Connect ShipFlow to Linear via OAuth2 and import issues, cycles, and projects directly — no CSV export needed. Cycles → ShipFlow Cycles, Projects → Epics, Issues → Tasks (priority and state mapped). Target project type is Kanban or Scrum (user's choice). OAuth flow: `POST /api/import/linear/authorize` → redirect to Linear → `GET /api/import/linear/callback`. Team picker, connection status, and one-click disconnect. New endpoints: `GET /api/import/linear/status`, `GET /api/import/linear/teams`, `POST /api/import/linear/team`, `DELETE /api/import/linear/disconnect`, `POST /api/import/linear`.
- **CSV import — full stack (v1.2.0)**: Import tasks and projects from Jira, Linear, Asana, or any generic CSV directly into ShipFlow. Backend auto-detects format from column headers and maps rows to Tasks, Epics, and Cycles inside a new Kanban project. Frontend: new `/import` page with 3-step stepper (Upload → Importing → Done), drag-and-drop file zone, project name input, format selector, stats summary, and per-row error log. "Import Data" nav link added to sidebar. API: `POST /api/import/csv`, `GET /api/import`, `GET /api/import/{id}`.

### Fixed
- **Stop timer directly from task detail page**: The "Running" timer button on `TaskDetailPage` was a dead indicator — clicking it did nothing and users had to scroll to the floating `TimerWidget` to stop the timer. The button now shows a live elapsed clock (`HH:MM:SS`) and opens a "Stop & Log Work" dialog right on the task, with the same note + rounded-hours flow as the global widget.
- **Sprint cards showing "0 stories"**: `CycleDTO.pitchCount` counts pitches (Shape Up concept), which is always 0 for Scrum projects. Added `taskCount` to `CycleDTO` populated from `TaskRepository.countByCycleId()`. `CycleList.tsx` now shows `taskCount` for Scrum projects and `pitchCount` for Shape Up.
- **"Shaping & Building" badge on active Scrum sprints**: Active sprint cards now show "Active Sprint" badge instead of the Shape Up phase label. Completed past sprints show "Completed" instead of "inactive".
- **Breadcrumb shows "Cycles" and CTA says "New Cycle" in Scrum projects**: `Breadcrumbs.tsx` now reads `isScrumProject` from context and swaps `/cycles` labels to "Sprints", "New Sprint", and "Sprint #N" throughout. `CycleList.tsx` CTA button also switches to "New Sprint" for Scrum projects.
- **Scrum terminology in Project Detail page**: `ProjectDetail.tsx` now shows "Scrum" as the project type badge (not "Shape Up"), "Active Sprints" / "Completed Sprints" in stats, "Sprints" as the section header, "+ New Sprint" as the CTA, and hides the pitch count on sprint cards. `RecentActivityFeed` and `CycleViewDialog` also suppress pitch count for Scrum projects.
- **Clicking a project in the Projects list now switches the project selector**: `handleCardClick`, `handleViewDetails`, and `handleViewCycles` in `Projects.tsx` all call `selectProject()` before navigating, so the toolbar project selector updates immediately when the user opens a project.

## [1.1.0] - 2026-05-20

### Added
- **Sprint Planning moved into Sprint Tools nav group**: Sprint Planning link is now the first item inside the collapsible "Sprint Tools" group in the sidebar for Scrum projects, contextually grouped with Health, Retrospectives, and Reports instead of floating under Work Management.
- **Product Backlog seeded with Sprint-4 candidate tasks**: `ScrumDemoInitializer` now seeds 5 backlog tasks (no sprint assigned) — offline mode, biometric login, profile photo upload, PDF export, admin analytics — so the Sprint Planning board shows a populated Product Backlog out of the box. Includes a back-fill path for databases that already have sprint tasks but 0 backlog tasks.
- **Create Task from Pitch**: "Create Task" button added to the Tasks section of PitchDetail. Opens a dialog pre-filled with the pitch's cycle (if assigned), allowing title, description, priority, status, and cycle selection. Created task is linked to the pitch via `pitchId` and categorised as `PITCH_SCOPE`.
- **PM Report widget rows are now clickable**: Table rows in PM Report widgets (Unshaped Pitches, Stale Bugs, High-Priority Tasks, At-Risk Epics, Overdue Tasks) are now clickable and navigate to the relevant detail page — pitches → `/pitches/:id`, tasks → `/backlog/:id`, bugs → `/qa/bug-reports/:id`, epics → `/epics/:id`. First column is highlighted as a link.
- **PM Report widgets respect current project**: All five PM Report widget data sources now filter by the currently selected project. Pitches and epics use project-scoped API endpoints; tasks and bugs filter client-side by `projectId` present in the API response.

### Fixed
- **Backend startup crash on platforms without AVX2/native tokenizer**: `QAConfig.embeddingModel()` now catches `Throwable` when `AllMiniLmL6V2EmbeddingModel` fails to initialize (e.g. unsupported CPU flavor in Docker) and returns a no-op model so the application starts; QA endpoints will throw `UnsupportedOperationException` with a clear message instead of blocking the entire Spring context.
- **Meeting checklist items always showing as checked**: `PitchMeetingsSection` view dialog rendered `<Checkbox checked disabled />` which defaulted to `checked={true}` regardless of the actual item state. Fixed to `checked={item.isCompleted}`; uncompleted items are now visually dimmed to distinguish them from completed ones.
- **Reports page showing cycles from all projects**: `Reports.tsx` was calling `getMyCycles()` regardless of the selected project. Now calls `cycleService.getByProject(projectId)` when a project is active so the cycle dropdown only shows cycles belonging to that project.
- **Reports page pitch rows are now clickable**: Clicking a row in the pitch details table navigates to `/pitches/:id`.
- **Burndown chart lines invisible**: `BurndownChart` used `hsl(var(--primary))` and `hsl(var(--muted-foreground))` as Recharts stroke colors. SVG elements don't inherit CSS custom properties, so both lines rendered transparent. Fixed to explicit hex colors — remaining line is now red (`#ef4444`) and ideal line is slate (`#94a3b8` dashed).
- **Velocity chart colors**: Planned and completed bars were nearly identical dark shades in dark mode. Planned bars are now indigo (`#6366f1`) and completed bars are green (`#22c55e`) for clear visual differentiation.
- **Velocity chart scope label**: Added "All Sprints · Project Overview" subtitle to clarify the chart shows project-wide data, distinguishing it from the adjacent per-sprint Burndown tab.
- **Burndown diagnostics**: `BurndownService` now logs cycle ID, task count, and story-pointed task count on every call, plus a WARN when it returns an empty series — makes startup seeding issues diagnosable from logs without needing a debugger.
- **Sprint terminology — sidebar, CycleList, CycleDetail, CycleForm, HealthOverview, CycleHealthSummary**: All Scrum projects now show sprint-specific labels (Sprints, Sprint Tools, Sprint Progress, Sprint Goal, etc.) instead of Shape Up terminology (Cycles, Cycle Tools, Pitches, Betting Table) throughout the app.
- **Sprint planning crash (`S.reduce is not a function`)**: Added `GET /api/tasks/cycle/{id}/all` endpoint returning a plain `List<TaskDTO>` (not paginated) so the Sprint Planning board can load sprint tasks without hitting the page-only endpoint.
- **PageImpl serialization warning**: Added `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` to suppress the startup warning and stabilise paginated response format (`$.page.totalElements`, `$.page.number`, etc.).
- **`sprintPlanning.totalPoints` untranslated**: i18next pluralisation requires `count` as the interpolation variable — added `count` alongside `points` in both `t()` calls.
- **`chk_project_type` constraint blocking Scrum project insert**: Added Flyway migration `V104` to drop and recreate the constraint to include `SCRUM` alongside `SHAPE_UP` and `KANBAN`.
- **Burndown empty for seeded sprints**: `ScrumDemoInitializer` now detects when MAS cycles exist with 0 tasks (partial prior commit) and back-fills all 10 sprint tasks with correct `completedAt` values.

### Added (core features, shipped 2026-05-19)
- **Scrum mode**: New `SCRUM` project type alongside Shape Up and Kanban. Adds story points on tasks, sprint goal and actual velocity on cycles, Sprint Planning page with two-column product/sprint backlog drag-drop, Burndown chart (remaining vs ideal), and Velocity chart (planned vs completed per sprint). New API endpoints: `GET /api/cycles/{id}/burndown` and `GET /api/projects/{id}/velocity`. Demo data now seeds a "Mobile App — Scrum Demo" project with three sprints (two completed with velocity 13/16, one active) and Fibonacci-pointed tasks so the burndown and velocity charts render out of the box.
- **Explicit team-to-cycle assignment for betting**: Introduced a `cycle_teams` join table so teams can be assigned to a cycle independently of pitch assignments. This unblocks the "Generate Slots" flow in the Betting Table — previously it failed silently when no pitches had yet been assigned. Teams can now be added/removed directly from the Betting Table page using the new **Teams for this Cycle** panel. REST API: `GET/POST/DELETE /api/cycles/{id}/teams[/{teamId}]`.
- **Inline pitch title editing**: Pitch names can now be edited directly from the PitchDetail header and the SortablePitchList (EpicDetailPage). Click-to-edit with Enter/blur to save, Escape to cancel.
- **Interactive roadmap timeline**: Drag-to-move and drag-to-resize timeline bars on the Roadmap page to adjust epic and initiative dates. Empty-state "Set dates" button creates a default 2-week range. Progress percentages and status-colored dots shown on bars.
- **Backend date validation**: `PATCH /epics/{id}/dates` and `PATCH /initiatives/{id}/dates` now validate that startDate ≤ endDate, returning 400 if invalid. `@DateTimeFormat(iso = DATE)` annotations added for explicit date parsing.
- **Project Manager report template**: New "Project Manager" dashboard template with 5 purpose-built widgets — unshaped pitches, stale bugs (3+ days unresolved), high-priority tasks, at-risk epics, and overdue tasks. Available from the template gallery in the Reports page.
- **New widget data sources**: Five new data source types for custom dashboards — `UNSHAPED_PITCHES`, `STALE_BUGS`, `HIGH_PRIORITY_TASKS`, `AT_RISK_EPICS`, and `OVERDUE_TASKS`. Usable in any custom report board.

### Fixed (core release, 2026-05-19)
- **Release form navigation**: After creating or editing a release, the form now correctly navigates to `/releases-management/:id` instead of the non-existent `/releases/:id`.
- **Report board View button**: Clicking "View" on a custom report board now correctly navigates to the dashboard view instead of redirecting to the main dashboard.
- **Startup crash (`tasks_aud` missing column)**: Added Flyway migration `V102` to add `project_id` to the Hibernate Envers `tasks_aud` audit table, fixing a schema-validation crash on startup when `Task.project` is audited.

### Changed
- **Test suite: 100% pass rule added to `CLAUDE.md`**: Infrastructure changes (serialization mode, renamed fields) can silently break unrelated tests. All 11 affected integration test assertions (`$.pageable` → `$.page`, `$.totalElements` → `$.page.totalElements`) were updated; `./mvnw verify` now exits with 0 failures.

## [1.0.0] - 2026-04-21

### Added
- **MCP `get_work_context` — relationship graph tool**: New read tool that returns the full context for a pitch or cycle in a single call — cycle metadata, pitch details (problem/solution/risks/wireframes), all tasks with per-status counts, blockers, hill-chart scope positions (0–100), and retrospective summaries.
  - Replaces the need to chain `get_cycle` + `get_pitches` + `get_tasks` + `get_blockers` separately.
  - Accepts either `pitchId` (scoped to that pitch and its parent cycle) or `cycleId` (full cycle graph).
  - New `McpWorkContextDTO` with nested `McpHillChartScopeDTO` and `McpRetroSummaryDTO` types.
  - `MCP_CLIENT_SETUP.md` updated with tool reference and usage example.
  - 5 new unit tests in `McpToolDispatcherTest`.
- **Anthropic Claude provider**: `langchain4j-anthropic` integrated as a first-class LLM provider.
  Set `AI_PROVIDER=anthropic` with `ANTHROPIC_API_KEY` to use Claude models.
  Default: `claude-3-5-haiku-20241022` (cost-efficient). Recommended for Wise Architecture: `claude-3-5-sonnet-20241022`.
- **Static Blog system**: Public `/blog` route with markdown posts served from `frontend/public/blog/`.
  Index fetched from `/blog/index.json`; posts parsed from frontmatter. Includes three launch articles.
  Blog link added to Landing page CTA and footer nav.
- **Markdown rendering fixed**: `@tailwindcss/typography` plugin enabled in Tailwind v4 so
  `prose` classes correctly style headings, bold, lists, code blocks in blog posts.
- **Blog nav link**: "Blog" added alongside Competitors / What's New / Roadmap in landing header and footer.

### Fixed
- **JWT secret configurable via env var** (`JWT_SECRET`): app no longer hardcodes the secret in production.
- **CORS production config**: `CORS_ALLOWED_ORIGINS` env var properly threaded through `docker-compose.yml`;
  exact origins and wildcard patterns handled separately to prevent Spring Security `IllegalArgumentException`.
- **SSE `AccessDeniedException`**: Spring Security 6 `AuthorizationFilter` now permits `ASYNC` and `ERROR`
  dispatcher types, fixing `403` errors during SSE notification push.
- **Anthropic model IDs corrected** and async job polling exempted from AI rate limiting.

### Changed
- Default OpenAI model updated from retired `gpt-4o-mini` to `gpt-4.1-mini`.

## [1.0.0-rc1] - 2026-04-14

### Focus
Stabilization — no new features. Bug fixes, documentation, community setup, and final release engineering.

### Added
- **VitePress Documentation Site (S27)**: Full docs site at `farzad-sedaghatbin.github.io/ShipFlow/` built with VitePress.
  - 5 sections: Getting Started, User Guide, Admin Guide, Developer Guide, API Reference
  - 20 content pages covering installation, environment setup, Shape Up workflow, Kanban, hill charts, all integrations (GitHub, Slack, Teams), MCP client setup, Redis, permission matrix, REST API, MCP tools, webhooks, contributing, architecture
  - `npm run docs:build` / `docs:dev` / `docs:preview` scripts at repo root
  - Docs build step added to CI `build-and-test` job
- **GOVERNANCE.md (S29)**: Documents project status (solo maintainer), decision-making process, contributor path, response time expectations, and security vulnerability reporting.
- **GitHub Discussion Templates (S29)**: `.github/DISCUSSION_TEMPLATE/support.yml` and `ideas.yml` for structured community support and feature requests.

### Changed
- `README.md`: pinned docker pull example to `0.9.0`; added docs site link in header
- `CHANGELOG.md`: added `[1.0.0-rc1]` upcoming section

## [0.9.0] - 2026-04-14

### Added
- **Wise Architecture: Pitch Scope & Task Context (v0.9.0)**: The LLM now receives a summary of existing hill-chart scopes and tasks already defined on the pitch before generating implementation steps.
  - `WiseArchitectureService.extractPitchProgressContext()` queries `HillChartPointRepository` and `TaskRepository` for existing work on the pitch
  - Scope names, position phase (`figuring-out` 0–49 / `executing` 50–100), and description included; root-level tasks included with title, status, and estimate hours (~50–120 tokens)
  - LLM explicitly instructed to avoid duplicating captured work and to reference existing scopes as dependency anchors
  - `ContextSourcesDTO` gains `hasPitchProgressContext` flag surfaced in API response and frontend context warnings
  - `TechnicalSolutionGeneratorService.generateStackSolution()` updated to accept a 10th `pitchProgressContext` parameter injected into the prompt under a dedicated "Current Pitch Progress" section

- **Wise Architecture: MCP Tools (v0.9.0)**: Three new MCP server tools let AI coding agents trigger analyses and retrieve Markdown guides without opening the UI.
  - `wise_architecture_list_analyses` (read) — list past analyses for the current user, optionally filtered by `pitchId`; returns `conversationId`, `pitchTitle`, `techStacks`, `createdAt`, `messageCount`
  - `wise_architecture_get_files` (read) — retrieve all generated Markdown files for a conversation by `conversationId`
  - `wise_architecture_analyze` (write, WRITE-scoped key required) — run a full analysis for a pitch + repositories; `selectedStacks` is optional and auto-detected (confidence ≥ 50 %) when omitted; returns all `GeneratedMarkdownFile` objects in a single response
  - `WiseArchitectureHistoryService.getGeneratedFilesByConversationId(String)` added for MCP look-ups by conversation UUID
  - `MCP_CLIENT_SETUP.md` updated with tool reference table and end-to-end agent workflow example
  - `WISE_ARCHITECTURE.md` updated with Pitch Progress Context and MCP Tools sections

### Tests
- **`WiseArchitectureServiceTest`**: Updated all `generateStackSolution` mocks from 9 to 10 arguments; new `PitchProgressContext` nested class with 5 tests covering context flag truthiness, LLM arg content, and scope phase labelling
- **`WiseArchitectureMcpToolsTest`** (new, 26 tests): Full unit-test coverage of all three MCP tools — auth guards, pagination, `pitchId` filtering, size cap, auto-detect fallback, low-confidence fallback, unknown stack type, missing required args, empty results, `cross-stack` null handling, mixed Integer/Long IDs
  - `frontend/e2e/auth.spec.ts` — login, logout, invalid credentials, protected-route redirect, remember-username (S19)
  - `frontend/e2e/projects.spec.ts` — create Shape Up project, create Kanban project, sidebar adapts per project type, project selector (S20)
  - `frontend/e2e/pitch-lifecycle.spec.ts` — pitch board loads, create IDEA pitch, advance to DRAFT, shaped pitch visible in betting candidates (S21)
  - `frontend/e2e/hill-chart.spec.ts` — hill chart renders canvas or empty state, drag scope dot and verify reload persists (S22)
  - `frontend/e2e/tasks.spec.ts` — create task from backlog, open detail, change status, add @mention comment, notification bell, Cmd+K global search (S23)
  - `frontend/e2e/helpers.ts` — shared `login()`, `logout()`, `waitForApp()` utilities
  - `frontend/playwright.config.ts` — Chromium, `localhost:3000`, screenshots + traces on failure
  - `frontend/package.json` — `test:e2e`, `test:e2e:ui`, `test:e2e:report` scripts
  - `.github/workflows/ci.yml` — `e2e-tests` job (runs after `build-and-test`, spins up Postgres + Redis, starts backend + frontend, runs Playwright, uploads HTML report as artifact)

### Refactored
- **PitchDetail Decomposition (S26)**: `PitchDetail.tsx` reduced from 1615 lines to 609 lines. JSX sections extracted into `src/components/pitchDetail/`: `PitchHeader` (title, status, action buttons), `PitchStatsRow` (4 stat cards), `PitchTeamCapacity` (team capacity card), `PitchShapingSection` (Shape Up narrative editor with AI extraction), `PitchDocumentsSection` (document drop zone), `PitchTasksSection` (task list), `PitchWorkLogsSection` (work log list + create dialog), `PitchMeetingsSection` (meeting list + create/view dialogs). All state and handlers remain in `PitchDetail.tsx`.
- **OrganizationSettings Decomposition (S25)**: `OrganizationSettings.tsx` reduced from 1740 lines to 271 lines. Each of the 10 settings tabs is now a focused, self-contained component under `src/components/organizationSettings/`: `GeneralSettingsTab`, `CycleSettingsTab`, `RiskSettingsTab`, `WeightsSettingsTab`, `ColorsSettingsTab`, `BugSettingsTab`, `CategoriesSettingsTab`, `MeetingsSettingsTab`, `FeaturesSettingsTab`, `EmailSettingsTab`. The page component is now a pure coordinator: loads settings, manages save/reset, and assembles the tab router.
- **BacklogPage Decomposition (S24)**: `BacklogPage.tsx` reduced from ~2320 lines to ~170 lines by extracting all state and logic into a dedicated `useBacklogPage` custom hook. Each sub-component (`BacklogHeader`, `BacklogFilters`, `BacklogStatistics`, `BacklogTaskTable`, `BacklogTaskDialog`, `BacklogViewDialog`, `BacklogDeleteDialog`) is now fully self-contained. `BacklogHeader` renders the CSV export button. `BacklogTaskTable` supports optional multi-select with `BulkActionBar`. Loading guard uses `BacklogSkeleton` for both initial load and project-switch transitions.

### Added
- **SMTP Email Notifications (S17)**: Pluggable email notification service wired into task assignment and @mention events.
  - `IEmailNotificationService` interface with two implementations: `EmailNotificationService` (real SMTP/Thymeleaf, active when `SMTP_HOST` is set) and `NoOpEmailNotificationService` (stub, auto-registered otherwise — app starts without mail config)
  - Three Thymeleaf HTML templates: `task-assigned`, `mentioned-in-comment`, `pitch-status-changed` (ShipFlow purple `#7c3aed` branding)
  - `DashboardNotificationService` wires email send on task assignment and @mention events (after existing Slack notification)
  - `V2026_04_05_0003` Flyway migration adds six SMTP columns to `organization_settings` (SMTP password is env-var only — never stored in DB)
  - `POST /api/admin/settings/test-email` endpoint — sends test task-assigned email to logged-in admin
  - Frontend **Email** tab added to Organization Settings page with SMTP host/port/username/from/TLS controls and **Send Test Email** button
  - `spring-boot-starter-mail` and `spring-boot-starter-thymeleaf` added to `pom.xml`
  - i18n keys `emailSettings.*` added to `en.json` and `fa.json`
  - 8 unit tests in `EmailNotificationServiceTest` (no-op stub, SMTP enabled/disabled, each send method, exception safety)

- **MCP Phase 2 Write Tools (S18)**: The ShipFlow MCP server is now fully bidirectional. AI editors (Claude Code, Cursor) can create and mutate data in addition to querying it.
  - `create_task(cycleId, title, description?, assigneeUsername?, priority?)` — creates a task in a cycle; resolves assignee by username automatically
  - `update_task_status(taskId, status)` — already existed in Phase 1, now grouped with Phase 2 write tools
  - `create_pitch(title, problemStatement?, appetiteDays?)` — creates a new pitch in IDEA status
  - `update_pitch_status(pitchId, status)` — advances a pitch through IDEA → DRAFT → SHAPED → PENDING
  - `add_comment(entityType, entityId, content)` — adds a comment to a TASK or BUG_REPORT as the API key's owner
  - All write tools require `MCP_SERVER_WRITE_ENABLED=true` and a WRITE-scoped API key
  - `MCP_CLIENT_SETUP.md` updated with all Phase 2 tool descriptions
  - 5 new unit tests in `McpToolDispatcherTest` (14 tests total)

- **Real-Time Notifications via SSE (S16)**: Replaced 30-second polling with instant Server-Sent Events push for the notification center.
  - `GET /api/notifications/stream` — long-lived `text/event-stream` endpoint; each authenticated user holds one active SSE connection
  - `NotificationSseManager` — thread-safe `ConcurrentHashMap`-backed emitter registry; one stream per user, auto-cleans on completion/timeout/error
  - `DashboardNotificationService.createNotification()` now pushes each new notification to the user's live SSE stream immediately after persisting it
  - `useNotificationStream` React hook — opens the stream via `fetch` (supports `Authorization` header for JWT), parses SSE frames, and invokes a callback on each `notification` event; falls back gracefully if stream fails (single reconnect after 5 s)
  - `NotificationCenter.tsx` updated: SSE hook triggers instant badge refresh; 30 s polling replaced with 60 s fallback interval so the UI continues to work even if the SSE stream is unavailable
  - i18n keys `notificationStream.streamConnected` and `notificationStream.streamReconnecting` added to `en.json` and `fa.json`
  - Unit tests for `NotificationSseManager`: subscribe lifecycle, active-count tracking, duplicate-user replacement, stale-emitter cleanup

- **Saved Filter Views — Backend + Frontend (S14 + S15)**: Users can save, load, and manage named filter presets for the task backlog on a per-user, per-project basis.
  - `POST /api/projects/{projectId}/saved-views` — create a named view with a serialised filter state (status, priority, assignee, sort, search, etc.)
  - `GET /api/projects/{projectId}/saved-views` — list all saved views for the current user within a project
  - `PUT /api/projects/{projectId}/saved-views/{id}` — rename or update filter state
  - `DELETE /api/projects/{projectId}/saved-views/{id}` — remove a view
  - `PATCH /api/projects/{projectId}/saved-views/{id}/default` — mark a view as the default (automatically unsets the previous default)
  - Flyway migration `V2026_04_05_0001__add_saved_views.sql` adds the `saved_views` table with a JSONB `filters` column and a unique constraint on `(user_id, project_id, name)`
  - 4 sample saved views seeded via `SampleDataInitializer` (admin + sara users)
  - **`SavedViewsDropdown`** component added to the BacklogPage filter bar — Bookmark icon button opens a Radix DropdownMenu with the list of saved views, inline name-input to save current filters, star icon to set as default view (automatically replaces the previous default), trash icon to delete
  - **Auto-apply default view** on page load — if the user has a default saved view and no explicit URL filters are active, the filters are applied automatically
  - `savedViewService.ts` typed service wrapping the five REST endpoints; parses the `filters` JSON string in the service layer before returning to components
  - i18n keys `savedViews.*` extended with `saveCurrentFilters`, `clearFilters`, `confirmDelete`, `enterName` in both `en.json` and `fa.json`
  - Unit tests for `savedViewService` cover all five CRUD operations

## [0.8.0] - 2026-04-05 - Core Product + Hardening

### Added
- **AI Q&A / RAG hardening (S13.1)**: Five gaps in the AI-powered Q&A feature fixed before v0.8.0 release.
  - **Entity disambiguation**: `"Cycle 5"` now resolves to the cycle *named* "Cycle 5" first (exact case-insensitive DB name lookup), falling back to numeric ID only when no name match exists. Prevents the common case of landing on the wrong cycle because its DB id happened to match the number in the question.
  - **Partial-match protection**: `"Cycle 5"` no longer incorrectly resolves to `"Cycle 50"` via a partial `LIKE` hit. Exact name wins; partial is only used for multi-result vector-search scoping.
  - **Cache bypass for multi-turn sessions**: Q&A response cache is skipped when a `conversationId` is present so conversation-history-aware answers are never served from or stored in the generic cache.
  - **ConversationId persisted across navigation**: `QAPanel` stores `conversationId` in `sessionStorage` keyed by `(contextType, contextId)`, restoring the same conversation when the user navigates away and returns.
  - **Conversation context evolves across turns**: New `ConversationManager.updateContext()` called after each entity resolution, so `getMostRecentContext()` always reflects the most recently discussed entity rather than only the one from conversation creation.
  - **Clarification turns saved**: The ambiguity-check early-exit path now saves a conversation turn and propagates `conversationId` in the response, maintaining history through clarification exchanges.
  - **Cache-hit responses carry conversationId**: Conversation creation moved before the cache check, so even a cached first answer hands the client a `conversationId` to continue as a multi-turn session.
  - 4 new unit tests in `QAServiceEntityResolutionTest` covering: named cycle preferred over ID, `"Cycle 50"` not matching `"Cycle 5"`, caller-supplied `cycleId` preserved, and `setCycleId` null-guard.

- **Interactive onboarding tour (S13)**: Full 21-step product tour powered by driver.js v1.4.0, wired into the existing `TourContext` architecture.
  - **WelcomeTourDialog** auto-appears 1500 ms after first login and gives users a choice to start the tour or skip it.
  - **21 steps** walk through: sidebar orientation → projects → cycles → pitch board → betting table → health/hill chart → retrospectives → reports → meetings → backlog → work logs → project selector → user menu.
  - Tour navigates automatically between routes (`/projects`, `/cycles`, `/pitches`, `/betting`, `/health`) with a 600 ms render delay before advancing.
  - Skip confirmation dialog prevents accidental tour exit; user must confirm before the tour stops mid-run.
  - Tour completion and welcome-shown state are persisted in `localStorage`. After completion the sidebar button changes to "Restart Tour".
  - Custom dark-theme CSS (`tour.css`) — imported from `TourContext.tsx` — provides animated highlight ring, gradient popover, and responsive mobile layout.
  - Steps 4 and 7 (project card / cycle card) work with the seeded demo data from `SampleDataInitializer`; on blank instances driver.js gracefully falls back to a full-screen popover.
  - `TOUR_GUIDE.md` created — the single source of truth for all 21 steps: selector, source file, route, and a maintenance contract that every future UI PR must follow.
  - `CLAUDE.md` checklists updated to point to `TourContext.tsx` and `TOUR_GUIDE.md` (was pointing to non-existent `src/tours/`).
  - **Bug fixes (post-review)**:
    - `tour.css` was never imported — custom dark-theme popover styling, pulse animation, and responsive layout were silently missing. Fixed by adding `import '../styles/tour.css'` to `TourContext.tsx`.
    - `useEffect` that guarded against manual navigation had a logic error: it destroyed the tour on navigation to ANY valid tour route (not just invalid ones), causing the tour to stop on the first step transition. Fixed to only destroy when navigating to a completely unknown route.
    - Navigation delay increased 400 ms → 600 ms so pages have sufficient time to render before the next step highlights.
    - Welcome dialog delay increased 1 000 ms → 1 500 ms so it never appears over a still-loading dashboard.
    - `pendingDestroyRef` was not cleared on `TourProvider` unmount — fixed to prevent stale ref holding a destroyed driver instance.
- **CSV export for task backlog (S12)**: One-click export of the current task list (with active filters applied) as a UTF-8 CSV file.
  - **Backend**: New `TaskService.exportTasksCsv()` accepts either `projectId` or `cycleId` (not both) plus all filter parameters. Uses `Pageable.unpaged()` to fetch all matching tasks in a single query; the cycle-scoped path also filters out soft-deleted tasks. Columns: `ID, Title, Status, Priority, Assignee, Pitch, Cycle, Estimate(h), Actual(h), Tags, Created, Updated`. Tags are stored as a plain string and included as-is. `csvEscape()` handles RFC-4180 quoting and prefixes formula-injection characters (`=`, `+`, `-`, `@`) with a single quote to prevent spreadsheet apps from executing them. Exposed via `GET /api/tasks/export` secured with `BACKLOG READ` permission; returns 400 if both or neither scope param is provided.
  - **Frontend**: Download icon button added to the BacklogPage header toolbar (between the view-mode toggle and the New Task button). Passes either `cycleId` or `projectId` (matching the API contract) with the current filter state, builds a `Blob`, and triggers a browser download. The button shows a spinner while the download is in progress.
  - 8 unit tests in `TaskCsvExportServiceTest` cover: header row, per-task row format, CSV escaping, assignee/pitch names, tags field, cycle-scoped query routing, empty result, and null-field safety.
  - i18n keys added to `en.json` and `fa.json`.
- **@mention notifications (S11)**: Writing `@Name` in any comment now triggers an in-app notification for the mentioned user.
  - `CommentService.processMentions()` parses `@Name` and `@"Full Name"` patterns after every comment save, looks up matching users via `userRepository.findByPersonNameIn()`, and calls `DashboardNotificationService.notifyCommentMention()` for each match.
  - Self-mentions and mentions of unknown users are silently skipped.
  - The `NotificationCenter` now renders a distinct `MessageSquare` (violet) icon for `COMMENT_MENTION` notifications instead of the generic INFO icon.
  - 7 new unit tests in `DashboardNotificationServiceTest` cover: notification content, username fallback when person profile is absent, self-skip, null-author/null-mentionee guards, long-comment truncation (>100 chars), and BUG_REPORT entity URL generation.
- **Bulk task operations (S09 + S10)**: Multi-select tasks in the backlog list view and apply bulk actions in one click.
  - **Backend**: New `BulkAction` enum (`ASSIGN`, `CHANGE_STATUS`, `CHANGE_PRIORITY`, `ADD_TAG`, `DELETE`), `BulkTaskUpdateRequest` / `BulkUpdateResult` DTOs, `TaskService.bulkUpdate()` (single `@Transactional`, cross-project validation, per-task error collection), and `POST /api/tasks/bulk-update` endpoint secured with `BACKLOG UPDATE` permission. 13 unit tests cover every action plus cross-project rejection and soft-delete filtering.
  - **Frontend**: Checkbox column added to the backlog list table (select-all in header, per-row checkboxes). When ≥ 1 task is selected a sticky `BulkActionBar` appears at the bottom of the viewport with: Assign To (person picker), Change Status, Change Priority, Add Tag (inline input), Delete (with confirm dialog), and Clear. After a successful bulk action the list auto-refreshes and selection is cleared. i18n keys added to `en.json` and `fa.json`.

### Changed
- **Spring Boot upgraded 3.2.1 → 3.4.7**: Pulls in Spring Framework 6.2, Hibernate 6.6, Spring Security 6.4, and Spring Data 3.4. Zero application-code changes required — all 1849 existing tests pass.
- **springdoc-openapi upgraded 2.3.0 → 2.8.6**: Keeps Swagger UI compatible with the new Spring MVC auto-configuration in Spring Boot 3.4.

### Added
- **Rate limiting (Bucket4j)**: Per-IP token-bucket rate limits on sensitive API paths — `/api/auth/**` (10 req/min), `/api/search/**` (30 req/min), `/api/wise-architecture/**` and `/api/risk/**` (5 req/min). Excess requests receive HTTP 429 with JSON `{"error":"Too many requests","retryAfter":60}` and a `Retry-After` header.
- **Security response headers**: Content-Security-Policy, `X-Content-Type-Options: nosniff`, HSTS (`max-age=31536000; includeSubDomains`), `Referrer-Policy: strict-origin-when-cross-origin`, and `Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=(), usb=()` added to all responses via Spring Security's headers DSL.
- **Production secret validation** (`StartupSecretValidator`): On startup in the `prod` profile, ShipFlow now fails fast with a clear error message if `app.jwt.secret` or `spring.data.redis.password` is still set to the default development value. Prevents accidental production deployment with insecure credentials.
- **Docker GHCR CI/CD** (`.github/workflows/docker.yml`): Pushing a `v*.*.*` tag now automatically builds the multi-stage Docker image and publishes `ghcr.io/farzad-sedaghatbin/shipflow:{version}` and `:latest` to GitHub Container Registry using `GITHUB_TOKEN` (no secrets required). Docker Buildx layer caching is enabled for fast builds.
- **Docker build check on PRs**: `ci.yml` now includes a `docker-build-check` job that builds (but does not push) the Docker image on every PR to catch `Dockerfile` regressions early.
- **React.lazy code splitting**: All 50+ page components in `App.tsx` are now loaded with `React.lazy()` + `<Suspense>`.
- **File attachments on tasks (S07 + S08)**: Tasks now support file attachments end-to-end.
  - **Backend**: New `task_attachments` table (Flyway `V2026_03_30_0001`), `TaskAttachment` JPA entity, `TaskAttachmentService` (upload / list / download / delete), and 4 new controller endpoints — `POST /api/tasks/{id}/attachments`, `GET /api/tasks/{id}/attachments`, `GET /api/tasks/{id}/attachments/{aid}/download`, `DELETE /api/tasks/{id}/attachments/{aid}`. Only the uploader or ADMIN may delete. `TaskDTO` now includes the `attachments` list.
  - **Frontend**: New `TaskAttachments.tsx` component with drag-and-drop upload zone, upload progress bar, per-file download and delete buttons, and collapsible section header. Added to `TaskDetailPage`. `taskAttachmentService.ts` handles all API calls. i18n keys added to `en.json` and `fa.json`.

### Added
- **`CODE_OF_CONDUCT.md`**: Community code of conduct adapted from Contributor Covenant v2.1 — defines expected behavior, enforcement guidelines, and contact for reporting violations.
- **`ROADMAP.md`**: Root-level roadmap document pointing to the live `/roadmap` page on shipflow.dev with the full v0.8.0 session table and beyond-v0.8.0 milestones.

### Changed
- **Version bump to 0.8.0**: `backend/pom.xml` version `0.6.2 → 0.8.0`, `frontend/package.json` version `0.6.2 → 0.8.0`.
- **Java version aligned to 21**: `pom.xml` `<java.version>17</java.version>` → `21` (matches CI and runtime).
- **CORS prod config fixed**: `application-prod.properties` default allowed-origin updated from `ship.somedayy.com` to `shipflow.dev`.

### Added
- **Public Roadmap Page** (`/roadmap`): New public page (no auth required) showing upcoming milestones, recently shipped releases, and long-term vision. Accessible from the landing page nav alongside Competitors and Release Notes.
  - "What's Coming" section with v0.8.0 (In Progress), v0.9.0 (Planned), v1.0.0 (Planned) phase cards
  - "Recently Shipped" section sourced from CHANGELOG (v0.7.0, v0.6.2, v0.6.1)
  - "Long-Term Vision" section (Scrum mode, automations, custom fields, mobile/PWA)
  - "Roadmap" nav button added to Landing.tsx
  - i18n keys added to en.json and fa.json

### Changed
- **Demo seed data refreshed** — replaced outdated sample data with realistic 2026 demo content:
  - Two projects: "Mobile Banking App" (Shape Up) and "DevOps Platform" (Kanban)
  - Five demo users: `admin/admin123`, `sara/demo123` (Manager), `ali/demo123` (Member), `mina/demo123` (Member), `viewer/demo123` (Read-only)
  - 7 pitches across the full Shape Up lifecycle (IDEA → DONE), hill chart positions, and work logs
  - 16 tasks for the active MBA cycle and 21 Kanban tasks covering all 7 columns
  - Bug reports (CRITICAL / MAJOR / MINOR), test cases, two retrospectives with votes, team assignments
  - Wise Architecture conversation history, roadmap initiatives, epics, and releases

## [0.7.0] - 2026-03-24 - MCP Server (AI Editor Integration)

### Added
- **ShipFlow as MCP Server (opt-in)**: Expose ShipFlow data directly to AI coding assistants — Claude Code, Cursor, Claude Desktop, GitHub Copilot, or any MCP-compatible client
  - Disabled by default; enable with `MCP_SERVER_ENABLED=true` so self-hosters decide when to activate it
  - Transport: HTTP + Server-Sent Events (SSE) at `GET /mcp/sse` + `POST /mcp/messages` (JSON-RPC 2.0)
  - Public health endpoint `GET /mcp/health` for status checks and readiness probes
- **10 read tools** available to any connected AI client:
  - `list_projects` / `get_project` — browse projects and metadata
  - `get_cycles` / `get_cycle` — cycle details, scope lists, and hill-chart positions
  - `get_tasks` / `get_task` / `get_blockers` — task data with dependency graph
  - `get_pitches` / `get_pitch_detail` — full Shape Up pitch including wireframe (Figma) links
  - `get_betting_candidates` — shaped pitches ready for the betting table
- **1 write tool** (requires `MCP_SERVER_WRITE_ENABLED=true`):
  - `update_task_status` — change task status from the editor without opening the UI
- **Pitch → Figma chain**: `get_pitch_detail` returns `wireframeLinks` (Figma URLs) so AI editors can immediately call Figma MCP for design context — enabling a full code-to-design-to-implementation loop
- **MCP API Key auth** (`McpAuthFilter`): `Authorization: Bearer <api-key>` on all `/mcp/**` endpoints; ties into existing `ApiKeyService` and `ApiKeyScope` (READ / WRITE / ADMIN)
- **Session management** (`McpSessionManager`): UUID-keyed SSE emitter registry — streams JSON-RPC responses async after HTTP 202 Accepted
- All MCP beans use `@ConditionalOnProperty(mcp.server.enabled)` — zero performance or startup impact when the server is disabled
- **`McpServerProperties`** config class (`mcp.server.*`) with environment variable overrides (`MCP_SERVER_ENABLED`, `MCP_SERVER_WRITE_ENABLED`, `MCP_SERVER_NAME`)
- **VS Code guide** (`VSCODE_GUIDE.md`): step-by-step instructions for Claude Code, Cursor, and GitHub Copilot integration
- **MCP client setup guide** (`MCP_CLIENT_SETUP.md`): configuration snippets for Claude Desktop, Claude Code CLI, Cursor, and VS Code MCP extension

### Technical Notes
- 9 unit tests added (`McpToolDispatcherTest`) covering initialize handshake, tools/list, tools/call routing, write-tool gating, Figma URL presence, and tool schema completeness
- No Spring context required for MCP unit tests — all tool logic is plain Java

## [0.6.2] - 2026-02-26 - Multi-Layer Caching & Performance

### Added
- **HTTP ETag / 304 Caching**: `ShallowEtagHeaderFilter` computes response ETags; browsers and API clients receive `304 Not Modified` when resources are unchanged, eliminating redundant payload transfers
- **Cache-Control headers**: All API responses include `no-cache, must-revalidate` to force client revalidation on each request
- **Spring Service-Layer Caching** — `@Cacheable` / `@CacheEvict` annotations on eight domain services with per-domain TTLs:
  - `PermissionService` (10 min), `ProjectService` (5 min), `CycleService` (5 min)
  - `TeamService` (10 min), `TagService` (10 min), `PersonService` (10 min)
  - `UserService` (5 min), `RoadmapService` (2 min)
- **Redis distributed cache** via `spring.cache.type=redis` in production — shared across multiple instances, survives restarts; automatic failover to in-memory if Redis is unavailable
- **In-memory fallback** (`SimpleCacheConfig`) via `spring.cache.type=simple` for development and tests — zero infrastructure overhead
- **Axios ETag interceptor**: stores `ETag` header per endpoint URL; replays `If-None-Match` on subsequent GETs; 304 responses return the in-memory cached body transparently to callers
- **React Query staleTime tuning** — per-domain constants in `queryClient.ts`:
  - Tasks (30 s), Entities — cycles/pitches/teams (5 min), Reference — tags/people/permissions (10 min), User profile (1 min), Analytics / roadmap (10 min)
- **Dashboard widgets migrated to React Query**: `OverdueTasksWidget`, `BlockedTasksWidget`, `MyTasksWidget`, `TeamWorkloadWidget`, `UpcomingDeadlinesWidget`, `CycleProgressWidget`, and `Dashboard.tsx` now use `useQuery` / `useQueries` — eliminates redundant fetch-on-mount loops, shares cached data across widget mounts, and enables automatic background refetch
- **`useCurrentUser` hook**: background-synced current-user query with 1-minute staleTime, replacing direct auth-context reads in components that only need user data

### Fixed
- **CycleService compile error**: Removed malformed `/**` Javadoc fragment inserted before `@CacheEvict` on `closeCycle()`, which caused the compiler to treat the annotation and method signature as Javadoc body, resulting in a `cannot find symbol: method closeCycle(Long)` error in `CycleController`
- **UserService `findByUsername` caching incompatibility**: Removed `@Cacheable` from `findByUsername()` — this auth-critical method is called inside `@SpringBootTest + @Transactional` integration tests where `ConcurrentMapCacheManager` persists across DB rollbacks, causing stale user IDs and 500 errors in `DashboardWidgetControllerIntegrationTest`, `DashboardNotificationControllerIntegrationTest`, and `BettingDecisionControllerIntegrationTest`
- **Hardcoded English strings in dashboard widgets**: `MyTasksWidget`, `TeamWorkloadWidget`, and `CycleProgressWidget` now use `t('widgets.*')` i18n keys instead of literal English strings ("My Tasks", "Loading...", "Completion Rate", "Work Progress", "Time Progress", "Behind schedule", "Teams", "No teams available", "No active cycles")

### Technical Notes
- All 1820 backend tests pass (0 failures, 0 errors, 4 skipped) after fixes
- `@Cacheable` was intentionally NOT added to `findByUsername()` because it serves authentication-path lookups that must always reflect the live DB state



### Added
- **Global Search (Cmd+K)**: Project-scoped instant search across all entities from the top bar
  - PostgreSQL trigram (`pg_trgm`) indexes on task titles, bug report titles/keys, pitch titles, and epic names
  - `GlobalSearchService` — UNION ALL native query with trigram similarity scoring and exact key matching
  - `GlobalSearchController` — `GET /api/search/global?q=&projectId=&limit=` (any authenticated user)
  - `GlobalSearchCommand` — cmdk-powered command palette with Cmd+K / Ctrl+K keyboard shortcut
  - Grouped results by entity type (Task, Subtask, Bug Report, Pitch, Epic) with icons and score-based ranking
  - Debounced 300ms search with loading, empty state, and minimum-chars feedback
  - Disabled when "All Projects" is selected — requires a specific project context
  - Bug report deep-link route (`/qa/bug-reports/:id`) with `BugReportDetailPage`
  - Flyway migration `V2026_02_26_0001` for `pg_trgm` extension and GIN trigram indexes
  - i18n keys added (English and Persian)
- **Inbound Webhook Admin UI**: DB-backed provider configuration through "Integrations → Inbound Webhooks" — no environment variables required
  - Create, edit, enable/disable, and delete webhook provider configs via the admin page
  - HMAC signature validation (HmacSHA256, HmacSHA1, HmacSHA512) configured per provider through the UI
  - Auto-generated, copyable webhook URL displayed for each provider
  - `InboundWebhookConfig` JPA entity + Flyway migration `V98`
  - `InboundWebhookConfigService` — upsert by provider name, secret masking, toggle enabled, webhook URL builder
  - `InboundWebhookConfigController` — REST API at `GET/POST/PATCH/DELETE /api/inbound-webhooks/configurations`; ADMIN/MANAGER gated
  - `GenericInboundWebhookHandler` — DB-driven fallback handler; `InboundWebhookRouter` falls through to it when no code-level handler exists
  - `InboundWebhooksIntegration.tsx` frontend admin page with full CRUD dialog, enable/disable toggle, copy-URL button
  - Navigation entry under Integrations sidebar (`integrations/inbound-webhooks`)
  - i18n keys added (English and Persian)

## [0.6.1] - 2026-02-25 - Markdown Editor, Project Selection Dialog, Expanded Color Palette & Pitch Prioritization

### Added
- **Pitch Prioritization inside Epics**: Drag-and-drop reordering and priority labels for pitches within an epic
  - `PATCH /api/pitches/reorder`, `PATCH /api/epics/reorder`, `PATCH /api/initiatives/reorder` endpoints accept ordered `{id, sortOrder}` lists
  - `BusinessValue` enum (`HIGH`, `MEDIUM`, `LOW`) wired to Pitch, Epic, and Initiative entities
  - Color-coded `PriorityBadge` component: red = HIGH, amber = MEDIUM, green = LOW
  - Inline priority selector on each pitch row in the epic detail list
  - Optimistic UI updates with rollback on API error
  - `SortablePitchList` component built on `@dnd-kit/sortable` v10 with drag handles, keyboard navigation, and `closestCenter` collision detection
  - `PitchRepository.findByEpicIdNotDeleted` now sorts by `sort_order ASC, id ASC`
  - Flyway migration `V2026_02_25_0002`: adds `sort_order` and `priority` columns to `pitches`, `epics`, and `initiatives` tables with covering indexes
- **Release Version Badge on Pitch Cards**: Target release version now visible on PitchBoard and epic pitch lists
  - Shows `v{version}` badge (Tag icon) wherever pitch cards are rendered — both mobile and desktop paths in PitchBoard
  - Reads the existing `targetReleaseVersion` field already present in `PitchDTO`
- **Priority Sort Option in PitchBoard**: Sort pitches by priority (HIGH → MEDIUM → LOW → unset)
  - Added `sortPriority` sort option to the Sort By dropdown
- **i18n**: Added `priority.{high,medium,low,set,label}`, `pitches.reorderError`, `pitches.priorityUpdated`, `pitchBoard.sortPriority` keys to English and Persian locale files
- **Permission Matrix — New Resources**: Added 13 new resource types to the RBAC permission matrix covering all recent features
  - Backend `ResourceType` enum extended with `BACKLOG`, `WORKLOG`, `MEETING`, `METRIC`, `TEST_CASE`, `INTEGRATION`, `WISE_ARCHITECTURE`
  - Frontend `permissionService.ts` updated with 10 new types (`INITIATIVE`, `EPIC`, `RELEASE`, `BACKLOG`, `WORKLOG`, `MEETING`, `METRIC`, `TEST_CASE`, `INTEGRATION`, `WISE_ARCHITECTURE`) including labels
  - Flyway migration `V2026_02_25_0001` inserts default permissions for all new resources
  - `PERMISSION_MATRIX.md` documentation updated
- **Pitch Hill Chart — Scope Summary**: Single-pitch hill chart now displays a scope summary section below the chart
  - Each scope shows name, hill position percentage, description, and linked tasks with status badges
  - Tasks loaded via new `GET /api/tasks/pitch/{pitchId}` endpoint
  - i18n keys added (English and Persian)
- **Pitch Detail — Tasks Section & Compact Work Logs**: Pitch detail page now includes a tasks section and a streamlined work log widget
  - New tasks card shows task title, status badge, assignee, and priority for all tasks in the pitch
  - Work log display reduced from 20 to 5 entries with a "View all work logs" link to the Work Logs page
  - Backend endpoint `GET /api/tasks/pitch/{pitchId}` added (`TaskController` + `TaskService`)
  - i18n keys added (English and Persian)
- **Work Logs Page — Filters**: Added filter dropdowns to the Work Logs page for both My Logs and Team Logs tabs
  - Filter by person (Team Logs tab), pitch, and task with client-side filtering
  - Clear Filters button to reset all active filters
  - Filters reset automatically when the active project changes
  - i18n keys added (English and Persian)
- **Markdown Editor for Descriptions**: All description fields now support Markdown editing with live preview
  - Write/Preview tab toggle with monospace editing and rendered markdown preview
  - New `MarkdownEditor` component wraps the existing `Markdown` renderer (react-markdown + GFM)
  - Form pages updated: EpicForm, InitiativeForm, BugReportModal, PitchBoard (create), TaskDetail (edit)
  - View/detail pages render descriptions as rich Markdown: EpicDetail, InitiativeDetail, BugViewDialog, TaskDetail (task + subtasks), PitchDetail (description, problemStatement, solution, rabbitHoles, risks, noGos)
  - i18n keys added (English and Persian)
- **Project Selection Dialog**: Modal popup when navigating to pages that require a specific project
  - Replaces the previous empty-state card (users often mistook it for a blank page)
  - `ProjectRequiredDialog` component shows a project list with avatar, name, and key
  - Cannot be dismissed — forces users to select a project before continuing
  - Applied to: EpicList, InitiativeList, ReleaseList, Roadmap, RetroList
  - i18n keys added (English and Persian)
- **Expanded Color Palette for Epics & Initiatives**: 42 colors organized by hue groups
  - Expanded from 10 to 42 colors (7 hue groups × 6 each: Reds, Oranges, Greens, Teals, Blues, Purples, Neutrals)
  - Improved UX: flex-wrap layout, smaller circles, hover scale effect, ring indicator on selected

### Changed
- **Sidebar Menu Reorganization**: Reordered sidebar navigation for better workflow alignment
  - Roadmap & Planning moved up (after Work Management) for quicker access
  - Organization (People/Teams) moved down (after Help, before Administration) since it's less frequently used

### Fixed
- **Cycle Cache Invalidation on Date Change**: Changing a cycle's start or end date now properly invalidates risk analysis caches
  - `CycleService.updateCycle()` detects date changes and invalidates both the cycle cache and all associated pitch caches via `RiskAnalysisService`
  - Prevents stale risk advisory data after cycle duration adjustments


## [0.6.0] - 2026-02-22 - Provider Abstractions, Release Traceability & Inbound Webhooks

### Theme
> "Pluggable integrations, full release traceability, and context-aware help."

This release introduces **pluggable VCS and Notification provider interfaces**, **generic inbound webhook infrastructure**, **AI-powered Help Search**, **Public API with scoped API keys**, **separated Dashboards and Reports routes**, and **enhanced Release tracking** with filters and cockpit views across Backlog, Bug Reports, and Release Detail pages.

### Added
- **VCS Provider Abstraction**
  - New `VCSProvider` interface defining a standard contract for version-control integrations
    - `getProviderName()`, `processCommit()`, `processPullRequest()`, `getTaskLinks()`, `getPitchLinks()`
  - `GitHubIntegrationService` refactored to implement `VCSProvider`
  - New `processCommitAndReturn` / `processPullRequestAndReturn` convenience methods that return the persisted entity
  - Provider interface enables future integrations (GitLab, Bitbucket) without changing core logic
- **Notification Provider Abstraction**
  - New `NotificationProvider` interface defining a standard contract for messaging integrations
    - `getProviderName()`, `sendNotification()`, `isActive()`
  - `SlackIntegrationService` refactored to implement `NotificationProvider`
  - Provider interface enables future integrations (Discord, PagerDuty) without changing notification routing
- **Release Filters on Bug Reports Page**
  - Combobox filter to narrow bug reports by target release
  - Client-side filtering with badge count and clear-filter support
- **Release Filter on Backlog Page**
  - Target Release filter in the task filter bar
  - Integrates with existing status, assignee, and search filters
- **Target Release Field on Bug Report Modal**
  - New Combobox field for assigning bugs to a target release
  - Loads available releases from release service
- **Enhanced Release Detail Cockpit**
  - Task breakdown showing count and status distribution per release
  - Bug breakdown showing count and severity distribution per release
  - Slipped bugs warning section highlighting bugs that missed the release
- **New i18n Keys**
  - Added translation keys for release filters, target release field, breakdown labels, and slipped bugs section (en locale)
- **Generic Inbound Webhook Infrastructure**
  - `InboundWebhookHandler` interface — 4-method contract (`getProviderName`, `validateSignature`, `handle`, `isActive`)
  - `InboundWebhookRouter` service — auto-discovers handler beans, O(1) dispatch, signature validation, full lifecycle management
  - `InboundWebhookController` — vendor-agnostic `POST /api/inbound/{provider}` and `GET /api/inbound` (list active providers)
  - Auto-detects event type from common headers (X-Event-Type, X-GitHub-Event, X-Intercom-Event, X-PagerDuty-Event, X-GitLab-Event, X-Linear-Event)
  - Status-to-HTTP mapping: success→200, unknown provider→404, invalid signature→401, inactive→503
  - SecurityConfig updated: `/api/inbound/**` → `permitAll` (handlers validate signatures themselves)
  - Full test suite: interface contract test + router unit tests (14 tests)
  - Implement `InboundWebhookHandler` as a `@Component` to add any new provider — zero changes to existing code
- **AI-Powered Help Search**
  - Ask "how do I…" questions in the Help Guides and get guardrailed AI answers
  - Dedicated `HelpGuideAIService` + `HelpGuideController` (fully separated from business Q&A)
  - Vector store retrieval via `EmbeddingStore`/`EmbeddingModel` — only top-5 relevant chunks included per prompt for token efficiency
  - 10 markdown knowledge base files auto-loaded and embedded at startup from `classpath:knowledgebase/help-guides/`
  - Guardrailed system prompt restricts answers to ShipFlow documentation only
  - Frontend `HelpSearch` component with suggested questions, follow-up chips, and markdown rendering
  - New `helpGuideService.ts` frontend API client (separate from `qaService.ts`)
  - i18n support (English + Persian) for all search UI elements
- **Expanded Help Guides**: Added 4 new technical guides
  - Export Data — Cycle summaries and data portability
  - Webhooks — Incoming/outgoing event integrations
  - Public API — REST API, OpenAPI, Personal Access Tokens
  - MCP Server — Model Context Protocol integrations
- **Expanded Webhooks Guide**: Complete rewrite with inbound webhook documentation, event headers reference, and provider architecture diagram
- **Updated Knowledge Base**: Inbound webhook docs added to `06-technical-features.md` for AI help search; new suggested help questions added
- **Public API & API Key Management**
  - `PublicTaskController` — paginated task listing and status update via `PATCH /api/v1/public/tasks/{id}/status`
  - `ApiKeyController` — create, list, and revoke scoped API keys (JWT-authenticated)
  - `WebhookController` — create, list, toggle, and delete outgoing webhook subscriptions (JWT-authenticated)
  - `ApiKeyAuthenticationFilter` — authenticates `X-API-Key` header on `/api/v1/public/**`; enforces `READ`/`WRITE`/`ADMIN` scopes (mutating methods require WRITE or ADMIN); SecurityContext authorities derived from key scopes, not full user authorities
  - `UpdateTaskStatusRequest` `comment` field now threaded through `TaskService.updateTaskStatus` and logged
  - `SecurityException` → 403 handler added to `ApiKeyController` and `WebhookController`
  - `IllegalArgumentException` → 404 handler added to `WebhookController`

### Security
- Silenced bot-probe multipart flood errors; blocked `/goform/` router exploits
- Excluded `/api/` paths from suspicious-path regex to prevent false-positive 403s
- Hardened against common bot/scanner probe patterns

### Changed
- **Separated Dashboards and Reports Routes**
  - Dashboards now live under `/dashboards` (previously shared `/reports` path)
  - Reports remain at `/reports` (cycle reports, analytics)
  - Updated `Layout.tsx` navigation with dedicated Dashboards menu item
  - Updated `DashboardSwitcher.tsx` links to use `/dashboards/` prefix
  - Fixed `DashboardManager.tsx` static route handling

## [0.5.3] - 2026-02-21 - Wise AI & Strategic Planning

### Theme
> "Context-aware AI meets strategic roadmap planning."

This release introduces **Wise Architecture** with multi-source context integration (team skills, Figma designs, GitHub code, roadmap relationships) and a comprehensive **Roadmap & Release Planning** system for strategic product management.

### Added
- **Wise Architecture Structured Solutions (Phase 1 Overhaul)**
  - **Architecture Detail Breakdown**: Solutions now include structured components, API contracts, data models, and config changes instead of generic text
  - **Enriched Implementation Steps**: Steps include sub-tasks with acceptance criteria, method signatures, step dependencies, and files to create/modify
  - **Enriched Reusable Services**: Services show specific methods to call, import statements, and usage instructions
  - **Enriched Libraries**: Recommendations show version, docs URL, and "in project" badge
  - **Project Convention Pre-pass**: Lightweight LLM call analyzes code context to detect naming conventions and patterns before generating solutions
  - **Cross-Stack Coordination**: API contracts from previous stacks are passed to subsequent ones for interface consistency
  - **Risk Factors Section**: Dedicated UI section with warning icon
  - **Markdown Rendering**: Architecture overviews and step descriptions now rendered as markdown via `<Markdown>` component
  - **History Alignment**: Enriched markdown persisted to advice history with full structured detail (components, API contracts, data model, sub-tasks, method signatures)
  - **Follow-up Context**: Conversation service now passes components, API contracts, data model, and method signatures to the LLM for richer follow-up answers
  - **New i18n Keys**: Added 12 new translation keys for structured solution UI (components, apiContracts, dataModel, configChanges, etc.) in both en and fa locales
  - **New TypeScript Interfaces**: `ArchitectureDetail`, `ArchitectureComponent`, `ApiContract`, `DataModel`, `ConfigChange`, `SubTask`
  - **Updated DTOs**: `StackSolutionDTO` restructured with `ArchitectureDetailDTO` and nested component DTOs; `ImplementationStepDTO` enriched with subTasks, methodSignatures, dependsOnSteps; `ReusableServiceDTO` enriched with methodsToCall, importStatement
- **Wise Architecture Advice History**
  - **Conversation Persistence**: All AI-generated solutions are now saved for review and follow-up
    - `WiseArchitectureAdvice` entity storing full conversation threads
    - Conversation ID grouping for threaded discussions
    - Message type tracking: `INITIAL_SOLUTION` and `FOLLOW_UP`
  - **History API Endpoints**:
    - `GET /api/wise-architecture/history` - Paginated list of user's conversation summaries
    - `GET /api/wise-architecture/history/pitch/{pitchId}` - All conversations for a specific pitch
    - `GET /api/wise-architecture/history/conversation/{id}` - Full conversation thread
    - `GET /api/wise-architecture/history/{adviceId}` - Single advice entry
    - `POST /api/wise-architecture/history/{adviceId}/feedback` - Submit helpful/not helpful feedback
  - **Context Tracking**: Each advice entry records which context sources were used
    - Figma design context, GitHub code context, roadmap context flags
    - Processing time tracking in milliseconds
    - Tech stacks and repository IDs stored for reference
  - **Feedback System**: Users can mark advice as helpful/not helpful with optional text
  - **Database Migration**: New `wise_architecture_advice` table (V94)
- **Improved Figma MCP Integration**
  - **Node ID Extraction**: Properly extracts `node-id` parameter from Figma URLs
    - Parses both `node-id=1434-49411` and `node-id=1434:49411` formats
    - Uses `get_node` MCP tool for specific frame/component context
    - New `FigmaFileReference` record with fileKey and nodeId
- **Wise Architecture Async Processing & Performance**
  - **Async Job Infrastructure**: Long-running operations no longer timeout
    - New `AsyncWiseArchitectureService` with job-based execution pattern
    - `WiseArchitectureExecutor` runs on dedicated `aiTaskExecutor` thread pool
    - Request deduplication prevents duplicate jobs for same parameters
    - Automatic job cleanup after 30-minute TTL
    - New async endpoints: `POST /async/detect-stacks`, `POST /async/analyze`, `GET /jobs/{id}/status`
  - **Granular Progress Tracking**: Real-time progress updates with descriptive messages
    - 0-10% Initialization phase: "Loading repository information..."
    - 10-55% File listing phase: "Listing files for repository 1/3 (kixy-mobile)..."
    - 55-95% Detection/generation phase: "Detected 3 stacks: React Native, Node.js, Kotlin"
    - 95-100% Finalization phase: "Solution complete: 3 stacks, 48 hours, appetite passed"
  - **Performance Optimizations**:
    - File list caching with 10-minute TTL (avoids repeated MCP calls)
    - Pre-indexed file pattern matching by extension for O(1) lookups
    - Quick config file detection for common frameworks (package.json, pom.xml, etc.)
    - Parallel repository processing (up to 4 concurrent scans)
    - Batch file reads using CompletableFuture.allOf() for code context
  - **Frontend Polling**: Automatic progress tracking with visual feedback
    - 2-second polling interval with exponential backoff
    - Real-time progress bar and descriptive status messages
    - Graceful timeout handling after 10 minutes
    - Repository search with 300ms debouncing
    - Memoized computed values (filteredRepositories, stacksByCategory)
- **Wise Architecture Enhancements**
  - **Team Skills Integration**: AI solutions now consider team member skills when generating technical recommendations
    - Extracts unique skills from assigned team members (~25-35 tokens)
    - Suggests technologies matching team expertise for faster implementation
  - **Figma MCP Integration**: Analyze Figma design files linked in pitches for better solution generation
    - Extracts design context from wireframeLinks via Figma MCP server (~100-200 tokens)
    - Per-organization Figma access token storage in Organization Settings
    - Environment-variable driven MCP configuration (MCP_FIGMA_ENABLED, MCP_FIGMA_SERVER_URL)
  - **GitHub MCP Integration**: Repository code analysis via Model Context Protocol servers
    - GitHubMcpProvider with full HTTP implementation for file listing, reading, and searching
    - Batch file read support with fallback to individual reads
    - Automatic service discovery based on tech stack patterns
    - Graceful fallback to default patterns when MCP not configured
  - **MCP Infrastructure**: Complete HTTP client implementation for MCP servers
    - Dedicated `mcpRestTemplate` bean with 10s connect / 30s read timeouts
    - FigmaMcpProvider with page listing, node reading, and design context extraction
    - JSON-RPC style REST endpoints for MCP server communication
  - **Roadmap Context Integration**: AI solutions now consider Epic/Initiative relationships for extensibility
    - Extracts roadmap context from assigned Epic (name, status, description)
    - Includes parent Initiative information when available
    - Lists related pitches in the same Epic for cohesive design recommendations
    - Generates architecture suggestions optimized for future extension across related work
    - New `hasRoadmapContext` indicator in Context Sources DTO
  - **Context Availability Warnings**: Frontend displays alerts when context sources are missing
    - Shows which sources were used (code analysis, team skills, design context, roadmap context)
    - Warns users that recommendations may be less accurate without full context
    - New tip: "Assign pitches to epics to enable roadmap-aware recommendations"

- **Shape Up Workflow Improvements - Pre-Cycle Pitch States**
  - Pitches now support a true pre-cycle workflow per Shape Up methodology
  - **Pre-cycle statuses** (IDEA, DRAFT, SHAPED) no longer require cycle assignment
  - **Betting Candidates**: SHAPED pitches with `cycle=null` appear in betting table for selection
  - **Cycle Assignment**: Betting process assigns shaped pitches to cycles (SHAPED → PENDING transition)
  - New repository method `findBettingCandidates()` fetches SHAPED pitches without cycle
  - New endpoint `PUT /api/pitches/{id}/assign-cycle/{cycleId}` for betting table integration
  - Entity enhancements: `Pitch.isPreCycleStatus()`, `Pitch.isReadyForBetting()`
  - Full null-safety for DTOs when handling pre-cycle pitches
  - Database and service layer support for pitches throughout their complete lifecycle
- **Roadmap & Release Planning Feature**
  - **Initiatives**: Strategic themes spanning multiple quarters for high-level planning
    - Full CRUD operations with soft delete support
    - Status workflow: DRAFT → PLANNED → IN_PROGRESS → COMPLETED (with ON_HOLD, CANCELLED)
    - Color-coded visualization with sortable display order
    - Target date ranges for timeline planning
    - Owner assignment and project association
  - **Epics**: Feature grouping layer between initiatives and pitches
    - Optional parent initiative for strategic alignment
    - Independent lifecycle with same status options as initiatives
    - Link multiple pitches to track epic progress
    - Color customization for visual differentiation
  - **Releases**: Versioned delivery milestones
    - Version string support (e.g., "v2.4.0", "2026.Q2")
    - Status workflow: DRAFT → PLANNED → IN_PROGRESS → STAGING → RELEASED
    - Risk level tracking: LOW, MEDIUM, HIGH, CRITICAL
    - Many-to-many relationship with cycles (releases can span multiple cycles)
    - Release notes for documentation
  - **Roadmap Timeline View**: Stakeholder-friendly visualization
    - Timeline data aggregation across initiatives, epics, and releases
    - Progress calculation from linked pitches
    - Pitch status breakdown per epic/release
  - **Bug & Pitch Release Tracking**
    - `target_release_id` on pitches - "When will this be delivered?"
    - `target_release_id` and `fixed_in_release_id` on bugs - Track expected vs actual fix release
    - `target_release_id` on tasks for release scoping
  - Backend: 4 new enums, 3 new entities, migration V82, 8 DTOs, 3 repositories, 4 services, 4 controllers
  - Frontend: 4 services, 8 pages (Roadmap, Initiatives, Epics, Releases)
  - Navigation: New "Roadmap & Planning" menu group with Map, Target, Layers, PackageCheck icons
  - i18n: Full English and Persian translations for all roadmap features

- **Bug Report Attachments & Media Support**
  - Image and video attachment support for bug reports (JPG, JPEG, PNG, GIF, WEBP, SVG, MP4, WEBM, MOV, AVI)
  - Drag-and-drop upload interface with progress indicators and previews
  - **Attachments can be added during bug creation** - files are staged and uploaded after the bug is created
  - Gallery view with thumbnail previews for all attachments
  - Full-screen preview modal for viewing images and videos
  - Download functionality for all attachments
  - Backend API endpoints: `POST /api/documents/bug/{bugId}/attachment`, `GET /api/documents/bug/{bugId}/attachments`, `DELETE /api/documents/bug/attachment/{attachmentId}`
  - New `MediaAttachmentUpload` component with file validation and preview capabilities
  - Extended `DocumentService` with `uploadMediaAttachment()` method for handling media files (50MB limit, no text extraction)
  - Integrated into `BugReportModal` (upload during creation and editing) and `BugViewDialog` (attachment gallery display)
  - i18n support for attachment-related messages in English and Persian
  - Comprehensive integration tests (9 tests covering upload, retrieval, deletion, validation)
  - Seed data with 14 sample bug attachments across 7 bug reports demonstrating the feature

- **Scope-Task Auto-Bridge Integration**
  - Unified Scope and Task entities with automatic bidirectional synchronization
  - Creating a root task with a pitch automatically creates a linked hill chart scope
  - Creating a scope automatically creates a corresponding task for work assignment
  - Auto-progress feature: scope position automatically updates based on subtask completion (0-100%)
  - Manual override: dragging a scope on the hill chart disables auto-progress (user takes control)
  - Re-enable auto-progress: toggle to restore automatic position synchronization
  - New `ScopeProgressService` calculates positions from task completion percentages
  - New `ScopeProgressListener` listens to task status changes for real-time sync
  - Task status change events (`TaskStatusChangedEvent`) trigger scope progress updates
  - UI indicators show auto-progress status, linked tasks, and suggested positions
  - Hill chart displays ghost position when suggested differs from current
  - API endpoints: `PUT /api/hill-chart/{id}/auto-progress`, `GET /api/hill-chart/{id}/suggested-position`
  - Database migrations V73 and V79: add bidirectional linking columns (`linked_task_id`, `auto_progress_enabled` on hill_chart_points; `auto_created_scope_id` on tasks)
  - Database migration V80: fixes invalid `COMPLETED` status values to `DONE` (TaskStatus enum compatibility)
  - New fields in DTOs: `linkedTaskId`, `autoProgressEnabled`, `suggestedPosition`, `showOnHillChart`
  - Simplified UX: single workflow for creating trackable work items

- **Configurable Team Capacity & Budget Management**
  - Organization-wide default hours per day (default: 8) and working days per week (default: 5)
  - Team-level capacity overrides for working hours and days
  - Person-level capacity overrides for individual team members
  - Team assignment-level overrides for fine-grained control per pitch
  - Capacity inheritance hierarchy: Organization → Team → Person → Assignment (most specific wins)
  - Budget calculations now use team member capacity for accurate per-person budget tracking
  - Risk calculation enhanced to detect over-budget individual team members
  - New UI in Organization Settings for configuring default capacity
  - New UI in Teams page for team and assignment capacity overrides
  - PitchHealthDTO now includes team budget breakdown with member utilization

- **Dashboard Tabbed Layout Redesign**
  - Reorganized dashboard into 3 tabs: Overview, AI Insights, Activity
  - Reduces scrolling and improves content discoverability
  - Tab selection persisted to localStorage for user preference
  - Overview: Quick Links, Active Cycles, Cycle Summary
  - AI Insights: Hill Chart, AI Risk Advisory, Cycle Signals
  - Activity: Recent Pitches, Recent Activity

- **Extended Widget Management**
  - Added 6 new manageable widgets: Cycle Summary, Cycle Signals, AI Risk Advisory, Hill Chart, Active Cycles, Recent Pitches
  - All widgets now appear in show/hide customization panel
  - Widget visibility preferences saved per user

- **Auto-Regeneration for AI Narratives**
  - Automatic narrative regeneration on cycle status changes (SHIPPED, COMPLETED, COOLDOWN)
  - Auto-regeneration on pitch status changes (SHIPPED, DROPPED)
  - 60-minute debounce interval to prevent excessive regenerations
  - Event-driven architecture using Spring Events for decoupled processing
  - New event classes: `CycleStatusChangedEvent`, `PitchStatusChangedEvent`, `TaskCompletedEvent`

### Changed
- **Permission Control for Regenerate Buttons**
  - Regenerate buttons in CycleSummaryPanel now require ADMIN or MANAGER role
  - Uses PermissionGate component with REPORT:CREATE permission check
  - Aligns with existing permission matrix (REPORT write access for Admin/Manager only)

### Fixed
- **Risk Factors Not Rendering**: Fixed field name mismatch (`risks` vs `riskFactors`) in TypeScript types that caused risk factors to be silently dropped in the UI
- **Missing Context Source Flag**: Added `hasRoadmapContext` to frontend `ContextSources` interface to match backend DTO
- **React Native Detection**: Now correctly identifies React Native projects via `react-native` in package.json dependencies
- **Figma MCP 404 Handling**: Gracefully handles missing Figma pages instead of failing entire analysis
- **Frontend Null Safety**: Added null checks for missing `bestPractices` field in stack solutions
- **Meeting creation - project association for non-pitch meetings**
  - Added direct `project_id` column to meetings table (migration V89) to support project-level meetings without pitch association
  - Meetings can now be associated with a project directly, not just through pitch → cycle → project relationship
  - When creating a meeting without selecting a pitch, the current project is automatically assigned
  - Fixes issue where meetings created without a pitch wouldn't appear when filtering by project (e.g., standalone standups in Kanban projects)
  - Backend: Added `projectId` field to `CreateMeetingRequest`, updated `MeetingService` to handle project assignment
  - Frontend: Automatically passes `currentProject.id` when creating meetings without a pitch
  - Migration V89 backfills existing meetings with project data from their pitch associations

- **Retrospectives API graceful handling for disabled features**
  - `/api/retros/project/{projectId}` and `/api/retros/cycle/{cycleId}` now return empty lists instead of 500 errors when retrospectives are disabled for a project (e.g., Kanban projects)
  - Meeting List page was calling retros endpoint during data refresh to populate retrospective dropdown, causing errors in Kanban projects
  - Updated backend service layer (`RetroCrudService`) to gracefully handle disabled features
  - Updated 3 tests to reflect non-blocking behavior for feature availability checks
  - Removed frontend error handling workaround now that backend handles this properly

- **ActOnRetroItemsDialog checkbox double-toggle bug**
  - Fixed issue where clicking checkbox triggered both onCheckedChange and parent div onClick
  - Removed redundant onCheckedChange handler to prevent state toggling twice
  - Checkbox selection now works correctly in retrospective action dialog

- **Backend test failures from dashboard changes**
  - Added missing ApplicationEventPublisher mock to CycleServiceTest
  - Added missing EntityManager mock to DashboardWidgetServiceTest
  - All 1,475 backend tests now passing

### API Contract Alignment & Code Quality (2026-02-09)

- **P0: Cooldown Activity API Contract Alignment**
  - **Status Enum Mismatch**: Unified `CooldownActivityStatus` enum between frontend and backend
    - Removed `CANCELLED` from frontend (was never in backend)
    - Frontend now uses: `PLANNED`, `IN_PROGRESS`, `COMPLETED`, `SKIPPED`, `BLOCKED`
    - Matches backend enum exactly for proper API communication
  - **Summary DTO Field Mismatch**: Updated frontend `CooldownSummaryDTO` interface
    - Added `blockedCount` and `skippedCount` fields (were missing)
    - Removed non-existent `cancelledCount` field
    - Added `cycleName`, `activities`, `countByType`, `countByStatus` fields for full compatibility
  - **Assignee Field Name**: Updated frontend to use `assigneeUsername` (matching backend)
    - Changed `assigneeName` → `assigneeUsername` in `CooldownActivityDTO`
    - Updated `CooldownActivities.tsx` table to display correct field

- **P0: i18n Duplicate Key Detection**
  - **Fixed Duplicate Keys**: Renamed `cooldownActivity.title` to avoid collision
    - `cooldownActivity.pageTitle` for page heading ("Cooldown Activities")
    - `cooldownActivity.title` for form field label ("Title")
    - Updated `CooldownActivities.tsx` to use `pageTitle` for page heading
  - **CI Protection**: Added duplicate key detection to `validate-i18n.js`
    - New `checkDuplicateKeys()` method with text-based JSON parsing
    - Detects same-level duplicate keys that would be silently overwritten
    - Reports exact line numbers of first and second occurrence
    - Duplicate keys now cause CI validation to **fail** (exit code 1)

- **P0: Separate Create/Update DTOs for Cooldown API**
  - Created `UpdateCooldownActivityRequest.java` with partial update support
    - All fields optional (null = no change)
    - Added `clearAssignee` flag for explicit unassignment
    - Added `clearRelatedPitch` flag for explicit pitch unlinking
    - Includes `status` field for status changes
  - Updated `CooldownActivityController.java` PUT endpoint
  - Enhanced `CooldownActivityService.updateActivity()` with:
    - Null-safe partial updates (only modifies provided fields)
    - Automatic timestamp handling for status transitions
    - Explicit clear flag support for nullable relationships

### Changed
- **Cooldown Activity UI Improvements**
  - Updated status badge colors for `SKIPPED` (amber) and `BLOCKED` (red)
  - Added appropriate icons: `SkipForward` for skipped, `Ban` for blocked
  - Status filter dropdown now shows all five status options
  - Status select in edit dialog shows correct localized labels

- **i18n Translations**
  - Added translations for new status values: `skipped`, `blocked`
  - Persian translations: `رد شده` (skipped), `مسدود شده` (blocked)
  - Fixed i18n key structure to prevent duplicate key issues

- **P1: Cooldown UI Refactoring (Container/Presentational Pattern)**
  - Decomposed monolithic `CooldownActivities.tsx` (433 lines) into focused components
  - Created 5 new presentational components:
    - `cooldownActivityUtils.tsx` (58 lines) - Shared utility functions for icons and badge colors
    - `CooldownSummaryCards.tsx` (70 lines) - Metrics dashboard cards
    - `CooldownActivityFilters.tsx` (73 lines) - Type/status filter dropdowns
    - `CooldownActivityTable.tsx` (129 lines) - Data table with edit/delete actions
    - `CooldownActivitiesView.tsx` (144 lines) - Pure presentational component
  - Refactored `CooldownActivities.tsx` (118 lines) - Container with business logic only
  - **Benefits**: 72% code reduction, better separation of concerns, easier testing

- **P1: React Query Migration for Cooldown Activities**
  - Migrated from manual `useState`/`useEffect` to React Query hooks
  - Created custom hooks in `hooks/useCooldownActivities.ts`:
    - `useCooldownActivities()` - Fetch activities with automatic caching
    - `useCooldownSummary()` - Fetch summary statistics
    - `useCreateCooldownActivity()` - Create mutation with optimistic updates
    - `useUpdateCooldownActivity()` - Update mutation with cache invalidation
    - `useDeleteCooldownActivity()` - Delete mutation with automatic refetch
  - Updated `CooldownActivities.tsx` and `CooldownActivityDialog.tsx` to use new hooks
  - **Benefits**: Automatic background refetching, better error handling, reduced loading state management

- **P1: RetroService.java Already Decomposed** ✅
  - Verified facade pattern implementation with specialized services:
    - `RetroCrudService.java` (222 lines) - CRUD operations
    - `RetroItemService.java` (249 lines) - Item management
    - `RetroActionService.java` (139 lines) - Action tracking
    - `RetroConversionService.java` (243 lines) - Pitch conversion
  - `RetroService.java` (163 lines) acts as lightweight coordinator
  - **Benefits**: Single Responsibility Principle, focused testing, easier maintenance

- **P1: API Contract Generation Infrastructure**
  - Installed OpenAPI TypeScript code generation tools:
    - `openapi-typescript` v7.12.0 - Type definition generator
    - `openapi-typescript-codegen` v0.30.0 - API client generator
  - Created `frontend/generate-api-types.sh` script:
    - Downloads OpenAPI spec from running backend (`/v3/api-docs`)
    - Generates TypeScript types to `src/types/api-schema.d.ts`
    - Generates type-safe API client to `src/api/generated/`
  - Added `npm run generate:api` script to package.json
  - Created comprehensive documentation: `API_CONTRACT_GENERATION.md`
  - **Benefits**: Prevents future contract mismatches, compile-time type safety, auto-completion

- **P2: Large File Decomposition Recommendations**
  - Identified top refactoring candidates:
    - **Frontend**: BacklogPage.tsx (2,013 lines), OrganizationSettings.tsx (1,510 lines), PitchDetail.tsx (1,314 lines)
    - **Backend**: QAService.java (1,215 lines), RiskAnalysisService.java (1,064 lines), PitchHealthService.java (981 lines)
  - Created `P2_REFACTORING_RECOMMENDATIONS.md` with detailed decomposition plans
  - Extracted `constants/backlogConstants.ts` from BacklogPage.tsx (first step)
  - **Benefits**: Roadmap for future maintainability improvements, reduced technical debt

### Test Coverage
- **CooldownActivityServiceTest**: Extended with 6 new test cases
  - `shouldNotModifyFieldsNotProvided` - verifies partial update behavior
  - `shouldUpdateStatusWithTimestamp` - validates startedAt/completedAt handling
  - `shouldClearAssigneeWhenFlagSet` - tests explicit unassignment
  - `shouldAssignNewUser` - tests assignee update
  - `shouldClearRelatedPitchWhenFlagSet` - tests explicit pitch unlinking
  - All 23 tests passing (18 existing + 5 new)

- **Layout.test.tsx**: Added 4 new tests
  - Version display from package.json
  - Format validation (v{major}.{minor}.{patch})
  - Children rendering
  - Navigation components presence

### Technical Details
- **Frontend Contract Changes** ([cooldownActivityService.ts](frontend/src/services/cooldownActivityService.ts)):
  - `CooldownActivityStatus`: Added `SKIPPED`, `BLOCKED`; removed `CANCELLED`
  - `CooldownActivityDTO`: Added backend-aligned fields
  - `UpdateCooldownActivityRequest`: Added `clearAssignee`, `clearRelatedPitch`, `priority`, `notes`
  - `CooldownSummaryDTO`: Full alignment with backend fields

- **Backend Contract Changes**:
  - New `UpdateCooldownActivityRequest.java` DTO
  - `CooldownActivityService.updateActivity()` signature changed
  - Partial update pattern: null = keep existing value

- **UI Components**
  - `ActOnRetroItemsDialog.tsx` - Flexible action selection dialog
  - `RadioGroup.tsx` - Radio button group component for UI controls
  - Action type selection with visual descriptions
  - Success confirmations for all action types

- **Seed Data**
  - Added comprehensive examples of acted-on retrospective items demonstrating all three action types
  - Example pitch converted from retro items: "Infrastructure Improvements for Safer & More Thoughtful Development"
  - Example tasks converted from ACTION items: Feature flag setup and performance testing integration
  - Example marked-only items with detailed notes explaining decisions

- **Test Coverage**
  - Frontend: `ActOnRetroItemsDialog.test.tsx` with 8 test cases covering all action workflows
  - Backend: `RetroServiceTest` with 7 new test cases for `markActedOn` and `convertToPitchDraft` methods
  - Coverage for edge cases, validation, and error handling

### Changed
- **Mobile Responsiveness**
  - Updated WorkLogsPage, BugReportsPage, TestCasesPage, BacklogPage, MeetingList, RetroList, and Teams pages to show skeletons during project switching
  - Converted fixed-width components to responsive: WorkLogForm, PitchDetail, BettingTable selects now use `w-full sm:w-[Xpx]` pattern
  - KanbanBoard columns now use responsive widths with scroll snap for mobile navigation
  - DashboardGrid automatically adjusts column count based on screen size

- Replaced `ConvertRetroToPitchDialog` with more flexible `ActOnRetroItemsDialog`
- Updated `RetroBoard` to use new action dialog
- Made retro-to-pitch conversion optional rather than automatic
- Enhanced translation keys in en.json and fa.json for new dialog options

### Fixed
- **UX Improvements**
  - Missing loading indicators when switching projects - now shows page-specific skeletons
  - Data appearing to "flash" during project changes - smooth skeleton transitions prevent layout shift
  - Fixed-width components breaking mobile layouts
  - Horizontal overflow on mobile for select components and drag overlays

- Translation inconsistencies between en.json and fa.json
- Added missing Persian translation placeholders for new features

## [0.5.0] - 2026-02-08 - Insight, Not Metrics

### Theme
> "Help teams think, not measure velocity."

This release introduces **decision support signals** that replace vanity metrics with actionable insights. Instead of tracking velocity or story points, ShipFlow now surfaces patterns that help teams improve their process.

### Added
- **Cycle Signals - Decision Support from Historical Data**
  - **Appetite Accuracy Signal**: Track how well appetite estimates match reality
    - Per-cycle ratio analysis (actual/appetite)
    - Linear regression for trend detection (IMPROVING, DECLINING, STABLE)
    - Contextual interpretation and recommendations
  - **Shaping Quality Signal**: Detect shaping health patterns
    - Aggregated shaping health evaluation based on existing shaping data
    - Highlights cycles that may warrant closer review
    - Quality classification (EXCELLENT, GOOD, NEEDS_ATTENTION, POOR, INSUFFICIENT_DATA)
  - **Risk Prediction Signal**: Measure risk prediction accuracy
    - Compare predicted vs actual risk outcomes
    - Correlation strength indicator (STRONG, MODERATE, WEAK)
    - Calibration guidance for future estimates
  - **Retro Follow-Through Signal**: Track action item completion rates
    - Per-retrospective follow-through analysis
    - Cross-cycle trending for systemic issues
    - Pending action item surfacing

- **Narrative Cycle Summaries**
  - **AI-Powered Narratives**: Generate cycle summaries with LangChain4j
    - What We Bet On: Committed pitches and appetites
    - What Shipped: Completed work with outcomes
    - What We Cut: Descoped items with rationale
    - Surprises: Unexpected discoveries and lessons
  - **Template Fallback**: Structured summaries when AI is unavailable
  - **Markdown Export**: Download cycle summaries for stakeholders
  - **Regeneration**: Re-generate narratives as cycle evolves

- **Enhanced Retrospectives 2.0**
  - **Action Tracking**: "Did we act on this?" checkbox for retro items
    - Boolean `actedOn` status with notes
    - Timestamp and user attribution for actions
    - Visual indicators in retro board
  - **Tag-Based Correlation**: Link retro items to pitches via tags
    - Shared tags between RetroItem and Pitch entities
    - "Link to Future Bet" suggestions
    - Cross-cycle learning patterns
  - **Action Statistics**: Follow-through metrics per retrospective
    - Total action items vs acted-on count
    - Follow-through rate percentage
    - Pending actions dashboard

- **Health Score Dashboard**
  - Combined signal health score (0-100)
  - Holistic view of team process health
  - Trend indicators across all signal types

- **API Endpoints**
  - `GET /api/signals/project/{projectId}` - Project-level signals
  - `GET /api/signals/cycle/{cycleId}` - Cycle-specific signals
  - `GET /api/narratives/cycle/{cycleId}/summary` - Full cycle summary
  - `POST /api/narratives/cycle/{cycleId}/regenerate` - Regenerate narratives
  - `GET /api/narratives/cycle/{cycleId}/export/markdown` - Export as Markdown
  - `GET /api/tags/project/{projectId}` - Tag management
  - `POST /api/retros/items/{itemId}/acted-on` - Mark action items
  - `GET /api/retros/{retroId}/action-stats` - Action statistics

- **Frontend Components**
  - `SignalCards.tsx` - Individual signal visualization cards
  - `CycleSignalsPanel.tsx` - Combined signals overview
  - `CycleSummaryPanel.tsx` - Narrative display with regeneration
  - Signal and narrative services for API integration

### Changed
- **EnhancedCycleReportDTO**: Now includes signals and narrative summary
- **RetroItemDTO**: Extended with action tracking fields
- **ReportService**: Integrates signals and narratives into enhanced reports

### Database
- **V72**: Added insights and narrative tables
  - `tags` - Project-scoped tags for cross-entity correlation
  - `retro_item_tags` - Many-to-many for RetroItem-Tag
  - `pitch_tags` - Many-to-many for Pitch-Tag
  - `cycle_narratives` - AI/template generated narratives
  - `cycle_signal_cache` - Cached signal calculations
  - Added `acted_on`, `acted_on_notes`, `acted_on_at`, `acted_on_by_id` to `retro_items`

### Philosophy
This release embodies Shape Up's principle that **appetite is not an estimate**. Instead of measuring whether teams "hit their targets," signals help teams understand:
- Are we getting better at shaping?
- Do our risk predictions match reality?
- Are we learning from retrospectives?

These signals inform better betting decisions, not performance evaluations.

## [0.4.1] - 2026-02-07 - Bug Fixes & Improvements

### Fixed
- **Meeting List Sorting**: Fixed meeting list to show newest meetings first in pitch detail view
- **Meeting Type Display**: Fixed case-sensitive UUID matching for meeting type display
- **Meeting Type Names**: Display meeting type names instead of UUIDs throughout the application
- **Test Suite Improvements**: Fixed backend and frontend tests for improved reliability

## [0.4.0] - 2026-02-05 - Cycle & Betting Excellence

### Added
- **Betting Decision Tracking**
  - **BettingDecision Entity**: Record commit/reject/defer/needs-shaping decisions for pitches
  - **Decision History**: Full audit trail of betting decisions across cycles
  - **Commitment Levels**: Track confidence (50-100%) for committed pitches
  - **Deferral Tracking**: Link deferred pitches to future cycles with reasons
  - **Decision Statistics**: View committed/rejected/deferred counts per cycle

- **Cooldown Activity Tracking**
  - **CooldownActivity Entity**: Track activities during Shape Up cooldown periods
  - **Activity Types**: Bug fixes, tech debt, research, experiments, training, documentation
  - **Activity Status**: Track pending, in-progress, completed, cancelled activities
  - **Cooldown Summary**: View activity counts and completion rates per cycle

- **Stagnation Detection & Notifications**
  - **Pitch Stagnation Detection**: Automatically detect pitches with no progress
  - **Stagnation Types**: HILL_CHART_STALLED, STUCK_AT_PEAK, NO_RECENT_WORK, COMPOUND_STAGNATION
  - **Severity Calculation**: LOW, MEDIUM, HIGH, CRITICAL based on days and cycle progress
  - **Dashboard Notifications**: Generate notifications for stagnating pitches
  - **Multi-Channel Integration**: Send critical stagnation alerts to configured external channels
    - Supports Slack, Microsoft Teams, or both simultaneously
    - Automatically detects which channels are configured and active
    - Gracefully handles cases where no external channels are configured
    - Sends notifications only to configured and enabled integrations

- **Frontend: Betting Decisions UI**
  - **BettingDecisionDialog**: Record betting decisions with rationale and options
  - **BettingDecisionBadge**: Display decision status with color-coded badges
  - **BettingTable Integration**: Show decisions on pitch cards with quick actions
  - **Cycle Summary**: Display decision counts in betting table header

- **Frontend: Hill Chart Narrative**
  - **ScopeNarrative Component**: Show scope progress with health indicators
  - **Stagnation Warnings**: Visual indicators for stalled and stuck-at-peak scopes
  - **Health Status**: Healthy, warning, critical, stalled status badges
  - **Contextual Narratives**: Phase-appropriate descriptions of scope progress

- **Async AI Advisor Pattern**
  - **Cache-First Optimization**: AI analysis returns instantly if cached, avoiding unnecessary async jobs
  - **Job-Based Async Execution**: Long-running AI operations execute in background with job tracking
  - **Dedicated Thread Pool**: `aiTaskExecutor` with configurable pool size (2-5 threads)
  - **Polling API**: Frontend polls for job completion with exponential backoff (1-5 seconds)
  - **Job Status Tracking**: PENDING → PROCESSING → COMPLETED/FAILED with error messages
  - **API Endpoints**: `/api/risk/async/pitch/{id}/analyze`, `/api/risk/async/cycle/{id}/analyze`
  - **Comprehensive Tests**: Unit tests for cache-first pattern, job lifecycle, and statistics

- **Semantic Search Improvements**
  - **Recency Boost**: Recently updated documents score higher in search results
    - 15% boost for documents updated within 7 days
    - 10% boost for documents updated within 30 days
    - 5% boost for documents updated within 90 days
  - **Keyword Fallback**: Database keyword search when semantic search returns no results

- **Phase Transition Validation**
  - Enforce betting decisions before cycle transition to BUILD phase
  - Validate all shaped pitches have decisions recorded
  - Prevent premature phase transitions

### Changed
- Enhanced PitchHealthService with comprehensive stagnation analysis
- Updated DashboardNotificationService to generate stagnation alerts
- Removed debug console.log statements from frontend code

### Database
- **V69**: Added `betting_decisions` table for decision tracking
- **V70**: Added `cooldown_activities` table for cooldown period tracking

### Added
- **Meeting View Mode with Smart Filtering**
  - **View-Only Dialog**: Click on meeting type badge to open read-only view showing only completed checklist items
  - **Backend Filtering**: New `/api/meetings/{id}/view` endpoint returns only completed DOR/DOD items
  - **Deleted Items Filter**: Organization Settings automatically filters out deleted DOR/DOD items when loading meeting types
  - **Consistent Behavior**: View mode works identically in both Meeting List and Pitch Detail pages
  - **Edit from View**: Seamlessly switch from view mode to edit mode with dedicated button
  - **Test Coverage**: Comprehensive unit and integration tests for all filtering logic
  - **Better UX**: View completed items without clutter, edit when needed with full context

- **Dynamic Meeting Types with DOR/DOD Checklists**
  - **Configurable Meeting Types**: Manage meeting types through Organization Settings instead of hardcoded enum
  - **DOR/DOD Checklists**: Each meeting type can have its own Definition of Ready (DOR) and Definition of Done (DOD) checklist items
  - **Per-Meeting Tracking**: When creating/editing meetings, check off DOR/DOD items as they are completed
  - **Auto-Ready Status**: DOR/DOD Ready status automatically calculated based on required item completion
  - **Default Meeting Types**: Initialized with 7 default types (SHAPING, BETTING, KICKOFF, STANDUP, DEMO, RETROSPECTIVE, HILL_CHART_REVIEW)
  - **Default Checklists**: Each default meeting type comes with sensible DOR/DOD checklist items
  - **Organization Settings UI**: New "Meeting Types" tab for managing types, colors, and checklist items
  - **Visual Checklist UI**: Interactive checkbox-based checklist in meeting creation/edit dialog
  - **Database Migration**: V68 adds `meeting_types_json` to organization_settings and `dor_items_json`/`dod_items_json` to meetings
  - **Backwards Compatible**: Existing meetings continue to work with legacy `dorReady`/`dodReady` boolean fields
  - **Internationalization**: Full i18n support (English/Persian) for new meeting type configuration

- **Jira-Style Activity Timeline**
  - **Embedded Activity View**: View change history directly inline without opening a dialog
  - **Bug View Dialog**: New tabbed interface with Details, Activity, and Comments tabs
  - **Task Detail Page**: Activity Timeline card showing complete change history
  - **Visual Timeline**: Timeline with colored dots (green=created, blue=modified, red=deleted)
  - **Relative Time Display**: Shows "5 minutes ago", "2 hours ago" for recent changes
  - **Field-Level Changes**: See exactly what changed with old → new value comparisons
  - **Reusable Component**: New `ActivityTimeline` component for consistent UX across entities
  - **Pagination Support**: Navigate through history with Previous/Next controls
  - **Internationalization**: Full i18n support (English/Persian) for activity labels

- **@Mention Support in Comments**
  - **User Mentions**: Type `@` to mention users in comments with autocomplete suggestions
  - **Person Name Search**: Search by person's display name (e.g., `@r.jahani`, `@"John Doe"`)
  - **Clickable Mentions**: Click on @mentions to view comprehensive user profile popover
  - **User Profile Popover**: Shows avatar, name, role, email, department, and skills
  - **Real-time Search**: Debounced user search as you type (150ms delay)
  - **Keyboard Navigation**: Arrow Up/Down to navigate, Enter to select, Escape to close
  - **Mention Highlighting**: Mentioned names displayed in primary color with hover underline
  - **Notification System**: Mentioned users receive in-app dashboard notifications
  - **Slack Integration**: Mention notifications sent to Slack channels
  - **Self-mention Prevention**: Users don't receive notifications for mentioning themselves
  - **API Endpoints**: `/api/comments/users/search` for autocomplete, `/api/comments/users/by-name` for profile lookup
  - **Internationalization**: Full i18n support (English/Persian) for mention UI

### Fixed
- **Pitch Count Accuracy in Cycle Display**
  - **Soft Delete Exclusion**: Cycle pitch count now correctly excludes deleted (soft-deleted) pitches
  - **Database Query Optimization**: Added `countByCycleIdNotDeleted()` method for efficient counting
  - **API Consistency**: All cycle endpoints (list, active, by-project) now return accurate pitch counts
  - **Test Coverage**: Comprehensive unit and integration tests for pitch count functionality
  - **Data Integrity**: Ensures UI displays reflect actual active pitch numbers

## [0.3.11] - 2026-02-03

### Added
- **Entity Change History with Hibernate Envers**
  - **Full Audit Trail**: Track all changes to Tasks, Bug Reports, Pitches, and Test Cases
  - **Selective Field Auditing**: Only audit important fields (status, priority, severity, assignee, title, description, etc.)
  - **User Attribution**: Every change records who made it and when
  - **Computed Diffs**: Server-side computation of field changes between revisions
  - **History API**: RESTful endpoints (`GET /api/tasks/{id}/history`, etc.) with pagination
  - **Timeline UI**: Interactive dialog showing change history with expandable revision details
  - **Field Change Display**: Visual old → new value display with color-coded badges
  - **Internationalization**: Full i18n support (English/Persian) for history labels and field names
  - **Database**: Automatic audit table creation via Hibernate Envers (`*_AUD` tables)
  - **Unit Tests**: Comprehensive tests for AuditService with mocked AuditReader

- **Direct Project Association for Bug Reports**
  - **Kanban Support**: Bug reports can now be directly associated with projects
  - **Flexible Workflow**: Support both Shape Up (cycle-based) and Kanban (project-only) methodologies
  - **Database Migration**: Automatic migration to handle nullable cycle field in bug reports
  - **UI Enhancement**: Add Bug button relocated to the right side with view toggle

- **Enhanced Task Management**
  - **Task Assignee Fields**: Added assignee and pair assignee fields to Task entity with full auditing
  - **Improved UI**: Better assignee selector with clean dropdown and avatars for bug reports
  - **Inline Status Changes**: Enhanced bug report inline status/severity changes to match task patterns

- **Kanban Board Improvements**
  - **Column Visibility Control**: Show/hide individual columns to reduce scrolling
  - **Quick Actions**: Show/hide all columns and hide optional columns functionality
  - **Session Storage**: Column visibility state persists per session
  - **Visual Indicators**: Display count of hidden columns
  - **Essential Columns**: Preserve TODO, IN_PROGRESS, DONE columns in hide optional action
  - **Internationalization**: Full i18n support for English and Persian languages

- **Modern Password Reset Modal**
  - **Enhanced UX**: Replaced HTTP basic auth prompt with modern modal interface
  - **Better Security**: Improved password validation for user creation
  - **Bug Kanban Board**: New specialized Kanban board component for bug management

- **Enhanced Comments System**
  - **Bug Reports Integration**: Added Comments section to BugReportsPage for enhanced user feedback
  - **Comprehensive UI**: Full commenting functionality integrated across entity detail pages

### Fixed
- **Permission System Stability**
  - **Infinite Loop Prevention**: Fixed infinite loops in usePermission hook and PermissionGate
  - **React Error #310**: Resolved hook count changes between renders
  - **Stable Dependencies**: Proper useEffect dependency management for permission checks
  - **Request Deduplication**: Prevent race conditions in permission loading
  - **ADMIN Permission**: Fixed SYSTEM:MANAGE permission check issue for admin users

- **Database Query Improvements**
  - **Null Cycle Handling**: Fixed JPQL query for direct project association in bug reports
  - **LEFT JOIN Optimization**: Use LEFT JOIN for nullable relationships in bug report filters
  - **Lazy Loading**: Resolved LazyInitializationException in audit queries
  - **Comprehensive Logging**: Added detailed logging to bug report filter API for debugging

- **Bug Report Enhancement**
  - **Assignee Field Alignment**: Fixed frontend assigneeId to match backend field naming
  - **User to Person Migration**: Changed bug report assignee from User to Person entity
  - **Optimistic Updates**: Restored optimistic updates for better UX after fixing underlying issues

- **Project Type Conversion**
  - **Auto Cycle Creation**: Automatically create default cycle when converting from Shape Up to Kanban
  - **Smooth Migration**: Seamless project methodology transitions with proper data setup

- **Audit System Enhancements**
  - **Serializable Entity**: Updated AuditRevisionEntity to implement Serializable
  - **Custom Fields**: Define custom revision fields for better audit tracking
  - **Team Association**: Added proper auditing for team association in Pitch entity

### Technical Improvements
- **Code Quality**: Improved permission hook architecture with better state management
- **Database Consistency**: Enhanced soft delete behavior across all services
- **Frontend Stability**: Resolved React hooks violations and rendering issues
- **API Robustness**: Better error handling and logging throughout the application

## [0.3.10] - 2026-02-02

### Added
- **Commenting System with Reactions**
  - **Full-Stack Implementation**: Complete backend and frontend commenting system for tasks and bug reports
  - **Emoji Reactions**: Support for 8 emoji reactions (👍, 👎, ❤️, 😄, 😮, 😢, 🚀, 👀)
  - **Toggle Reactions**: Click to add/remove your reaction; see aggregated counts per emoji
  - **CRUD Operations**: Create, read, update, and delete comments with permission checks
  - **Edit Tracking**: Comments show "edited" badge when modified
  - **Author Controls**: Only comment authors can edit; authors and admins can delete
  - **Comment Counts**: Display comment count in task/bug report lists
  - **Database Design**: New `comments` and `comment_reactions` tables with triggers for auto-counting
  - **Comprehensive Tests**: Full unit test coverage for CommentService

- **RTL (Right-to-Left) Text Detection**
  - **Automatic Detection**: Detects Arabic, Farsi, Hebrew, and other RTL scripts
  - **Dynamic Styling**: Bug titles automatically switch to RTL when content is in RTL languages
  - **Unicode Support**: Full support for RTL Unicode ranges

- **Bug Report Project Context Requirement**
  - **Context Enforcement**: Disabled bug creation when "All Projects" is selected
  - **User Guidance**: Tooltip explains users must select a specific project
  - **UX Improvement**: Prevents bugs without proper project context

- **Comprehensive Soft Delete Functionality**
  - **Safe Deletion**: Records are marked as deleted rather than permanently removed
  - **Audit Trail**: Complete tracking with deletion timestamp and user information
  - **Data Recovery**: Deleted items can be restored if needed
  - **Entity Support**: Available for pitches, tasks, and test cases
  - **Role-Based Permissions**: Deletion permissions based on user roles (ADMIN, MANAGER, MEMBER)
  - **Frontend Integration**: User-friendly delete buttons with confirmation dialogs
  - **API Compatibility**: Existing endpoints work unchanged, deleted items automatically excluded
  - **Performance Optimized**: Dedicated database indexes for efficient soft delete queries

- **Discover GitHub App Installations**
  - Added `syncedInstallations` field to `GitHubBulkSyncResultDTO` for tracking synced installations
  - Enables syncing GitHub Apps installed directly on GitHub (outside ShipFlow UI)

### Fixed
- **GitHub App PKCS#1 Private Key Support**
  - Handle PKCS#1 format (`-----BEGIN RSA PRIVATE KEY-----`) for GitHub App private keys
  - Automatic conversion from PKCS#1 to PKCS#8 format for Java compatibility
  - Fixes authentication failures when using GitHub-generated private keys

- **Flyway Out-of-Order Migrations**
  - Enable `spring.flyway.out-of-order=true` for hotfix branch compatibility
  - Allows migration scripts with older version numbers from hotfix branches
  - Prevents `FlywayValidateException` during version merges

- **TypeScript Build Errors**
  - Fixed frontend TypeScript compilation errors
  - Ensures clean builds in CI/CD pipelines

- **Soft Delete Lazy Initialization Issues**
  - Fixed `LazyInitializationException` in PitchRiskHistory queries
  - Eagerly fetch `deletedBy` relationship in all risk history repository queries
  - Added `deletedBy` to `@JsonIgnoreProperties` to prevent serialization issues

- **Soft Delete Service Consistency**
  - Unified soft delete behavior across all services
  - Fixed edge cases where deleted items might appear in results

- **AI Test Generation Enhancement**
  - Include all Shape Up methodology fields in test generation context
  - Problem Statement, Solution, Rabbit Holes, Risks, No-Gos, Wireframe Links
  - More comprehensive test cases based on complete pitch information

- **AI Risk Analysis Enhancement**
  - Include all Shape Up methodology fields in risk analysis context
  - Better risk factor identification based on Problem Statement and Solution
  - Improved "unclear requirements" detection considering all pitch fields
  - More accurate AI insights with complete project context

- **Document Knowledge Ingestion**
  - Simplified document ingestion to use existing `ingestDocument` method
  - Generate synthetic document IDs for pitch documents without database records
  - Deterministic ID generation using pitch ID and content hash

## [0.3.9] - 2026-02-01

### Fixed
- **Power Automate Teams Integration - URL Double-Encoding Issue**
  - Fixed `401 AuthorizationFailed` error when sending notifications via Power Automate
  - Root cause: RestTemplate was double-encoding URL query parameters (`sp=%2F` became `sp=%252F`)
  - Solution: Use `URI.create()` instead of String URL to prevent double-encoding
  - Restored Adaptive Card format required by Power Automate Teams flows
  - Updated unit tests for URI-based API

### Added
- **GitHub App OAuth UI for Organization-Wide Integration**
  - Updated GitHubRepositoryManager to show both OAuth and Manual options
  - Added GitHub App status check and conditional UI rendering
  - Added organization connection, sync, and removal functionality
  - Updated githubService with OAuth methods (getAppStatus, initiateOAuth, etc.)
  - Added English and Persian translations for GitHub App UI
  - GitHub App section only shows when backend is configured

- **GitHub App i18n Messages**
  - Moved GitHub App OAuth messages from SQL to properties files
  - Added messages to `messages.properties` (English)
  - Added messages to `messages_fa.properties` (Persian)

## [0.3.8] - 2026-02-01

### Added
- **GitHub App OAuth Integration (Organization-Wide Access)**
  - **Two Integration Methods**: Users can now choose between GitHub App or Manual registration
    - **GitHub App** (Recommended): Single OAuth consent grants access to all organization repositories
    - **Manual Registration**: Per-repository setup for smaller projects or specific repos
  - **GitHub App Benefits**:
    - Sync 50+ repositories with a single authorization
    - Automatic webhook configuration for ALL repositories (no manual setup per repo)
    - New repositories automatically tracked when "All repositories" is selected
    - Short-lived tokens with automatic refresh for better security
  - **New Endpoints**:
    - `GET /api/github/app/status` - Check GitHub App configuration status
    - `POST /api/github/app/authorize` - Initiate OAuth consent flow
    - `GET /api/github/app/callback` - Handle OAuth callback from GitHub
    - `GET /api/github/app/installations` - List all GitHub App installations
    - `POST /api/github/app/sync-all` - Bulk sync repositories from all installations
    - `DELETE /api/github/app/installations/{id}` - Remove an installation
  - **Database Migration**: V62 adds `github_app_installations` table
  - **Configuration Variables**:
    - `GITHUB_APP_ID` - App ID from GitHub App settings
    - `GITHUB_APP_NAME` - App slug from URL
    - `GITHUB_APP_PRIVATE_KEY` - PEM private key for JWT authentication
    - `GITHUB_APP_CLIENT_ID` - OAuth Client ID
    - `GITHUB_APP_CLIENT_SECRET` - OAuth Client Secret
    - `GITHUB_APP_WEBHOOK_SECRET` - Webhook verification secret
  - **Documentation**: Comprehensive setup guide in `GITHUB_INTEGRATION_GUIDE.md`
    - Step-by-step instructions for creating GitHub App
    - Self-hosted deployment guide (each company creates their own GitHub App)
    - Troubleshooting section for common issues
  - **Testing**: Unit tests for `GitHubAppOAuthService`

- **Microsoft Teams Integration: Flow Type Support**
  - **Multiple Integration Methods**: Support for 3 flow types
    - `WEBHOOK`: Traditional Teams incoming webhooks
    - `POWER_AUTOMATE_POST`: Power Automate flows that post to channel
    - `POWER_AUTOMATE_THREAD`: Power Automate flows that create conversation threads
  - **Smart Flow Detection**: Automatic URL-based flow type detection for optimal payload formatting
  - **Enhanced Database Schema**: Added `flow_type` column to `teams_channel_config` with migration V20241217001
  - **Backend Enhancements**:
    - Created `FlowType` enum for type safety
    - Updated `TeamsIntegrationService` with flow-type-aware payload building
    - Enhanced error handling with flow-type-specific guidance
    - Updated DTOs and request classes to support flow type selection
  - **Frontend Improvements**:
    - Flow type dropdown selector in channel configuration UI
    - Enhanced setup guide with dual-path instructions (webhook vs Power Automate)
    - Updated channel table to display flow type with color-coded badges
    - Complete Farsi translations for all new features
  - **Documentation**:
    - Updated `TEAMS_INTEGRATION_GUIDE.md` with flow type explanations and upgrade guide
    - Added security considerations and FAQ section
    - Database migration notes for existing installations
  - **Testing**: Comprehensive test coverage including flow type validation and payload formatting tests

## [0.3.7] - 2026-02-01

### Fixed
- **Frontend API Configuration**
  - Fixed hardcoded localhost URLs in GitHub, Teams, and Slack service clients
  - Services now properly use environment variables or relative URLs for API base URL
  - Ensures correct API routing in both development and production environments
  - Fixes potential deployment issues where API calls would fail in production

## [0.3.6] - 2026-02-01

### Fixed
- **Critical: Read-Only Transaction Error in Risk History**
  - Fixed `ERROR: cannot execute INSERT in a read-only transaction` when saving pitch risk history
  - Added explicit `readOnly=false` to `@Transactional` annotations in `RiskAnalysisService`
  - Refactored `RiskHistoryService` to use `EntityManager` directly with `REQUIRES_NEW` propagation
  - Re-fetch Pitch entity within new transaction to avoid detached entity issues
  - Added HikariCP `connection-init-sql` to ensure connections default to read-write mode
  - Fixes risk analysis history tracking in production environments

## [0.3.4] - 2026-01-28

### Fixed
- **Production Flyway Validation Fix**
  - Added `spring.flyway.ignore-missing-migrations=true` to production configuration
  - Prevents Flyway validation errors for seed migrations that were moved to `db/seed`
  - Fixes: `FlywayValidateException: Detected applied migration not resolved locally: 10`
  - Required for production deployments that have seed migrations in `flyway_schema_history`

## [0.3.3] - 2026-01-28

### Fixed
- **CRITICAL: Separate Seed Data from Production** (Breaking Change)
  - **Problem**: Seed/demo data was being inserted into production databases via Flyway migrations
  - **Solution**: Moved 11 seed data migrations from `db/migration` to `db/seed` folder
  - **Dev**: Loads both `db/migration` (schema) + `db/seed` (data) via Flyway
  - **Prod**: Loads ONLY `db/migration` (schema) - no seed data
  - **Cleanup**: Added `scripts/cleanup-production-seed-data.sql` to remove existing seed data from production
  - **Breaking**: Production Flyway history will show "missing" migrations (V10, V11, V17, V18, V20, V21, V25, V27, V30, V47, V56)
  
- **Seed Data PostgreSQL Compatibility**
  - Added `OVERRIDING SYSTEM VALUE` clause to all INSERT statements with explicit IDs
  - Fixed V11, V20, V21, V25 seed data migrations for PostgreSQL compatibility
  - Fixes: `ERROR: cannot insert a non-DEFAULT value into column "id"` when inserting seed data

## [0.3.2] - 2026-01-28

### Fixed
- **PostgreSQL Migration Compatibility** (Production Critical)
  - Converted all MySQL-specific `AUTO_INCREMENT` syntax to SQL standard `GENERATED ALWAYS AS IDENTITY`
  - Removed MySQL-specific `ON UPDATE CURRENT_TIMESTAMP` clauses (not supported in PostgreSQL/H2)
  - Replaced `CLOB` data type with `TEXT` for PostgreSQL compatibility
  - Updated 14 migration files: V1, V3, V9, V12, V14, V15, V22, V23, V43, V44, V45, V54, V59, V60
  - **Impact**: Migrations now fully support both H2 (development) and PostgreSQL (production)
  - Fixes: `ERROR: syntax error at or near "AUTO_INCREMENT"` in production deployments

## [0.3.1] - 2026-01-27

### Fixed
- **Critical Database Migration Fixes** (Production Blocking)
  - **V59 Migration** (Pitch Risk History):
    - Fixed H2-incompatible inline INDEX syntax - separated into individual CREATE INDEX statements
    - Fixed TEXT/CLOB type mismatch in `PitchRiskHistory` entity causing schema validation errors
    - Updated entity to use `@Lob` annotation for proper CLOB mapping
  - **V60 Migration** (User Projects):
    - Replaced MySQL-specific `ON DUPLICATE KEY UPDATE` with H2-compatible `MERGE INTO` statement
    - Ensures data migration works correctly across different database environments
  - These fixes ensure application starts successfully with H2 database in all environments

## [0.3.0] - 2026-01-27

### Added
- **Pluggable Vector Store Architecture**: Complete refactoring to pluggable vector database system
  - **Core Architecture**:
    - Created `VectorStoreProvider` interface for all provider implementations
    - Implemented `VectorStoreProviderFactory` with Spring auto-discovery for automatic provider registration
    - Added `VectorStoreProviderType` enum supporting: in-memory, qdrant, chroma, milvus (future), pinecone (future), weaviate (future)
    - Created `VectorStoreProviderConfig` builder pattern for flexible, provider-agnostic configuration
    - Default vector dimension: 384 (matches all-MiniLM-L6-v2 embedding model)
  - **Provider Implementations**:
    - **InMemoryVectorStoreProvider**: Development/testing (non-persistent, no dependencies)
    - **QdrantVectorStoreProvider**: Production recommended - high-performance Rust-based vector DB with API key auth
    - **ChromaVectorStoreProvider**: Alternative option for simpler deployments
  - **Configuration System**:
    - Updated `application.properties` with `app.qa.vectorstore.*` configuration section
    - Dev profile defaults to `in-memory` (no external dependencies)
    - Prod profile defaults to `qdrant` with API key authentication
    - Docker Compose updated with Qdrant service (replaces ChromaDB)
  - **Testing**:
    - Created 6 new test classes covering the vector store plugin system:
      - `VectorStoreProviderTypeTest`: Enum validation and config parsing (10 tests)
      - `VectorStoreProviderConfigTest`: Configuration builder, defaults, and extra params (16 tests)
      - `VectorStoreProviderFactoryTest`: Factory auto-discovery and store creation (13 tests)
      - `InMemoryVectorStoreProviderTest`: In-memory provider tests (9 tests)
      - `QdrantVectorStoreProviderTest`: Qdrant provider validation tests (11 tests)
      - `ChromaVectorStoreProviderTest`: ChromaDB provider tests (8 tests)
    - Total: 67 new unit tests for vector store architecture, 100% pass rate
  - **Documentation**:
    - Updated `RAG_ARCHITECTURE.md` with pluggable vector store section
    - Updated `ENVIRONMENT_SETUP.md` with vector store configuration guide
    - Updated `README.md` to highlight Qdrant as production recommendation

- **LLM Plugin Architecture**: Complete refactoring to pluggable AI provider system
  - **Core Architecture**:
    - Created `LLMProvider` interface for all provider implementations
    - Implemented `LLMProviderFactory` with Spring auto-discovery for automatic provider registration
    - Added `LLMProviderType` enum supporting: ollama, runpod, openai, anthropic (future), google (future), azure-openai (future)
    - Created `LLMProviderConfig` builder pattern for flexible, provider-agnostic configuration
    - Replaced monolithic `RunPodChatModel` class with modular provider implementations
  - **Provider Implementations**:
    - **OllamaLLMProvider**: Local/self-hosted AI (privacy-first, no API costs)
    - **OpenAILLMProvider**: Production-grade ChatGPT integration (gpt-4o, gpt-4o-mini, gpt-4-turbo, gpt-3.5-turbo)
    - **RunPodLLMProvider**: Cloud GPU serverless compute with async job polling
  - **Configuration System**:
    - Updated `application.properties` with provider-specific sections (ollama, openai, runpod)
    - Added `application-dev.properties` configuration for local development (defaults to Ollama)
    - Created `.env.example` with all provider configuration templates
    - Updated Docker Compose files to support all three providers via environment variables
  - **Testing**:
    - Created 4 new test classes covering the LLM plugin system:
      - `LLMProviderConfigTest`: Configuration builder and extra params (8 tests)
      - `LLMProviderFactoryTest`: Factory auto-discovery and model creation (13 tests)
      - `LLMProviderTypeTest`: Enum validation and config parsing (7 tests)
      - `OllamaLLMProviderTest`, `OpenAILLMProviderTest`, `RunPodLLMProviderTest`: Provider-specific tests (22 tests)
    - Total: 53 new unit tests, 100% pass rate, 94% instruction coverage on LLM package
    - All tests passing: 1018 total, 0 failures
  - **Developer Experience**:
    - Enhanced `start-dev.sh` with provider-specific validation (checks Ollama only when AI_PROVIDER=ollama)
    - Added provider display names and descriptive error messages
    - Created comprehensive README in `backend/src/main/java/.../llm/README.md` with:
      - Architecture overview and diagrams
      - Step-by-step guide for adding new providers
      - Configuration examples and usage patterns
  - **Extensibility**:
    - Framework ready for future providers (Anthropic Claude, Google Gemini, Azure OpenAI)
    - Provider plugins auto-discovered via Spring's component scanning
    - No code changes needed to switch providers (environment variable only)

### Fixed
- **Circuit Breaker Monitor**: Fixed URL parameter handling
  - Changed from `/cycles/:id/circuit-breaker` to `/cycles/:cycleId/circuit-breaker`
  - Updated `useParams` hook to correctly parse `cycleId` parameter
  - Resolved 404 errors when navigating to circuit breaker page from cycle detail
- **AI Cache Controller**: Added missing `AIConfig` dependency injection
  - Injected `AIConfig` to access provider information
  - Added `/api/cache/ai-provider` endpoint to display current AI provider and model
  - Shows provider display name (e.g., "OpenAI ChatGPT", "Ollama (Local)", "RunPod (Cloud GPU)")
- **Circuit Breaker Service**: Fixed axios import to use centralized API client
  - Changed from direct `axios` import to `api` from `./api`
  - Ensures all API requests use consistent base URL and interceptors
  - Maintains authentication headers across all circuit breaker requests
- **Translation Interpolation**: Fixed string interpolation in English and Persian translations
  - Changed `{count}` and `{threshold}` to `{{count}}` and `{{threshold}}` (i18next syntax)
  - Fixed circuit breaker and cycle detail pages showing raw placeholder strings
- **Flyway Configuration**: Changed `spring.flyway.clean-disabled` to `true` in `application-dev.properties`
  - Prevents accidental database wipes during development restarts
  - Preserves sample data and work logs across application restarts
  - Developers can still manually clean database when needed

### Changed
- **AI Risk Analysis Logging**: Enhanced logging with provider and timing information
  - Added provider name and model to AI generation logs ("🤖 Generating AI insights using provider: openai (model: gpt-4o-mini)")
  - Added execution time tracking for AI requests ("✅ AI response received in 2345ms")
  - Improved debugging and performance monitoring capabilities
- **Cycle Risk Overview Component**: Enhanced AI insights rendering
  - Changed from plain list items to Markdown rendering for insights and recommendations
  - Supports bold, italic, code blocks, and formatting in AI-generated content
  - Improves readability of structured AI responses
- **Cycle Detail Navigation**: Added Circuit Breaker button to cycle header
  - Placed ⚡ icon button next to Hill Chart and Edit buttons
  - Improves discoverability of circuit breaker feature
  - Consistent with Shape Up methodology's safety valve concept
- **Competitors Comparison Page**: Updated AI features description
  - Highlights pluggable LLM architecture with provider choice (Ollama, OpenAI, RunPod)
  - Emphasizes "Privacy-first or production-ready—your choice"
  - Clarifies deployment flexibility (local, cloud, or GPU-accelerated)
- **Circuit Breaker Guide**: Added "Re-pitching Killed Work" section
  - Documents how to learn from killed pitches and re-pitch smarter
  - Provides step-by-step guide and example scenario
  - Includes Shape Up wisdom: "The best teams kill pitches early and re-pitch smarter, not harder"
  - Updated access instructions to reflect new navigation (button in cycle header vs. separate link)

### Development
- **Database Migration**: Added V56 with circuit breaker test data
  - Populates Cycle 4 pitches with realistic work logs
  - Demonstrates overflow detection (116%, 120%, 85%, 55%, 118% of appetite)
  - Enables testing of circuit breaker functionality with real data
  - Updates pitch statuses to reflect active development (IN_PROGRESS, STARTED)

- **Project Type System**: Support for both Kanban and Shape Up methodologies
  - **Backend Implementation**:
    - New `ProjectType` enum (SHAPE_UP, KANBAN) with database migration V55
    - Automatic "Continuous Flow" cycle creation for Kanban projects
    - `ProjectDTO` and `CreateProjectRequest` include projectType field
    - Backward compatibility: All existing projects default to SHAPE_UP
    - Comprehensive unit tests in `ProjectTypeTest.java` (5 tests, 100% pass rate)
  - **Frontend Implementation**:
    - Project type selection in create/edit project dialogs
    - `useProject` context with `isKanbanProject` computed property
    - Conditional navigation: Cycles menu hidden for Kanban projects
    - Automatic view switching: Kanban projects default to board view
  - **Kanban-Specific Features**:
    - Pitch/scope fields hidden in task/bug/testcase forms
    - Cycle and pitch filters hidden in list views (BacklogPage, WorkLogsPage, TestCasesPage, BugReportsPage)
    - Terminology changes: "Feature Tasks" vs "Pitch Tasks" based on project type
    - Kanban board enhancements: subtask creation, timer start functionality
  - **Project-Based Filtering**:
    - All pages filter data by currently selected project
    - "All Projects" selection shows data from all projects
    - Consistent filtering across BacklogPage, WorkLogsPage, TestCasesPage, BugReportsPage, MeetingList
  - **Documentation**:
    - Comprehensive architecture doc: `PROJECT_TYPE_ARCHITECTURE.md`
    - Updated README.md with project type feature comparison
    - Test coverage documentation and implementation summary
    - New comprehensive guide: `ProjectTypesGuide.tsx` explaining both modes
    - Updated landing page with dual mode feature highlight
    - Updated competitor comparison page with Kanban support
  - **UI Consistency & Internationalization**:
    - Dashboard hides cycle/pitch widgets for Kanban projects
    - Reports page shows appropriate message for Kanban (no cycle-based reports)
    - Documentation guides updated with project type disclaimers
    - Complete Farsi translations for dual mode features
    - Help & Guides page includes Project Types guide

- **Farsi (Persian) Language Support**: Comprehensive RTL internationalization
  - **Complete Translation Coverage**: 3,650+ translation keys in Persian (fa.json)
    - All UI components, forms, navigation, and messages fully translated
    - Dashboard widgets, reports, meetings, and QA sections localized
    - Chart labels, tooltips, and data visualizations in Farsi
  - **RTL Layout Support**: Full right-to-left layout implementation
    - Tailwind CSS logical properties (me-, ms-, start-, end-) throughout application
    - React Grid Layout configured for RTL with proper direction handling
    - Dynamic text direction based on language selection (ltr/rtl)
    - Bidirectional text rendering for mixed content
  - **Responsive Grid Layouts**: Dynamic width calculation for RTL compatibility
    - Container-aware grid sizing using useRef and resize listeners
    - Prevents widget overflow in RTL mode
    - Proper grid positioning calculations for both LTR and RTL
  - **Language Switching**: Seamless language toggle in user interface
    - Persistent language preference in localStorage
    - Automatic direction and font changes
    - No page reload required for language switch

- **Microsoft Teams Integration**: Full integration with Microsoft Teams for real-time notifications
  - **Backend Features**:
    - Database tables for Teams configuration, channel settings, and notification history (V54 migration)
    - `TeamsConfiguration` entity for tenant-level settings with webhook URL
    - `TeamsChannelConfig` entity for channel-specific notification preferences
    - `TeamsNotificationHistory` entity for audit logging
    - `TeamsIntegrationService` for sending notifications using Adaptive Card format
    - `TeamsIntegrationController` with REST endpoints for configuration management
    - Support for 8 notification types: task assigned/completed/blocked, pitch shaped, cycle started/cooldown, betting completed, sprint started
    - Channel-specific notification filtering and test notification functionality
    - Color-coded notification cards based on event type
    - **Test Coverage**: 17 comprehensive unit tests for `TeamsIntegrationService` with Mockito (100% pass rate)
  - **Frontend Features**:
    - Teams Integration settings page at `/integrations/teams`
    - Tenant configuration UI with webhook URL management
    - Channel-specific notification preference management with toggles
    - Test notification sending interface
    - Built-in setup guide with step-by-step instructions
    - Navigation integration in Administration → Integrations section
  - **API Endpoints**:
    - `POST /api/teams/configurations` - Create/update tenant configuration
    - `GET /api/teams/configurations` - List all configurations
    - `GET /api/teams/configurations/active` - Get active configuration
    - `DELETE /api/teams/configurations/{id}` - Delete configuration
    - `POST /api/teams/configurations/{id}/channels` - Configure channel notifications
    - `POST /api/teams/configurations/{id}/test` - Send test notification
    - `GET /api/teams/configurations/{id}/history` - Get notification history

- **Competitors Comparison Page**: New marketing page comparing ShipFlow with alternatives
  - Feature-by-feature comparison matrix with Linear, Asana, Monday.com, Jira, and Basecamp
  - 40+ features compared across 7 categories (Shape Up, Progress, AI, QA, Team, Integrations, Deployment)
  - Key differentiator cards highlighting ShipFlow's unique advantages
  - Individual competitor breakdown cards explaining when to choose each tool
  - Accessible via `/compare` route and linked from Landing page with "Compare to Alternatives" button
  - Professional design with responsive layout
  - Comparison summary also added to README.md for quick reference

- **Server-Side Search for Traceability Dropdowns**: Optimized performance for large datasets
  - Minimum 3-character search with 300ms debouncing to prevent API spam
  - GET `/api/tasks/search?q={query}` endpoint for task search by title/description
  - GET `/api/hill-chart/search?q={query}` endpoint for scope search by scope/description
  - Database-level LIKE queries with case-insensitive partial matching
  - Context-aware loading: pitch/cycle context loads scoped data, otherwise requires search
  - Custom `useDebounce` hook for frontend search optimization
  - Helpful UI messages: "Type to search", "Searching...", "Type at least 3 characters"
  - Max 50 results per search to maintain performance
  - Scales to millions of records via indexed searches
- **Traceability Relationships**: Optional links between tasks, bug reports, test cases, and scopes
  - Tasks can link to pitch and scope (hill chart point)
  - Bug reports can link to scope and related task
  - Test cases can link to scope and related task
  - All relationships optional to support technical debt/improvement work
  - Database migration V53 with nullable foreign keys
  - Comprehensive test coverage (22 unit tests)
  - Frontend dropdowns with search in BugReportModal and TestCaseFormPage
- **Task Dependencies**: Lightweight dependency tracking system for identifying blockers
  - Three dependency types: BLOCKS, DEPENDS_ON, RELATED_TO
  - Automatic circular dependency detection using DFS algorithm
  - Visual blocker indicators in task lists showing blocked task count
  - **Enhanced blocker tooltips**: Hover over blocker badge to see up to 3 blocker task names (with "... and X more" for additional)
  - **Blocking indicators**: Green shield badge showing how many tasks this task is blocking
  - **Subtask indicators**: List badge showing subtask count with tooltip listing subtask titles
  - **Dependency filtering**: Filter backlog by "All Tasks", "Blocked Tasks", or "Blocking Tasks"
  - Dedicated dependency management section in task detail pages with improved UX
  - Quick Guide in dependency dialog explaining which type to select based on task status
  - Result preview showing what will happen when dependency is added
  - REST API endpoints for managing dependencies
  - Same-cycle validation to prevent cross-cycle dependencies
  - Comprehensive test coverage (unit and integration tests)
  - Database migration V52 for task_dependencies table

### Changed
- **Performance Optimization**: Replaced client-side filtering with server-side search
  - Before: Loaded 200+ items then filtered locally (400KB+ per dropdown)
  - After: 0-50 items loaded only when needed (0-100KB)
  - Dramatically improved performance for deployments with 1000+ scopes/tasks
- **Backlog View**: Now displays blocker badges (🔴 blocked) and blocking badges (🛡️ blocking) for all tasks
- **Task View Dialog**: Added dependency management section to the quick view dialog (eye icon) so users can add/remove dependencies without navigating away
- **Task List UX**: Blocker badge tooltip now shows actual task titles instead of just count
- **Backend**: Task DTOs now include children (subtasks) array for displaying subtask count and details

- **Configurable Risk Factor Weights**:
  - Risk calculation now uses configurable weights instead of fixed percentages
  - 4 risk factors with adjustable weights: Budget (default 25%), Bugs (default 30%), Scope (default 25%), Time (default 20%)
  - Weights must sum to 100% with real-time validation and visual feedback
  - 5 preset profiles for quick setup:
    - Balanced (25/30/25/20): Equal priority across all factors
    - Conservative (35/35/15/15): Emphasis on budget and quality
    - Aggressive (15/25/35/25): Focus on speed and scope completion
    - Quality-Focused (15/40/30/15): Maximum weight on bug severity
    - Time-Critical (20/25/20/35): Prioritize deadline pressure
  - New Organization Settings tab "Risk Weights" with slider controls and profile buttons
  - Backend refactoring: Split calculateRuleBasedRiskLevel into 4 separate factor methods
  - Each factor calculates 0-100 score, then weighted sum produces final risk
  - API endpoint: GET /api/admin/settings/risk-profiles returns all preset profiles
  - Database migration V51: Added risk_weights_json column to organization_settings table
  - RiskWeights DTO with validation: isValid() checks sum, normalize() adjusts to 100%
  - Updated PitchHealthService documentation to reflect configurable weights
  - Backend compilation verified with zero errors
  - Frontend TypeScript build successful with proper RiskProfile interface

- **Cycle Date Auto-Calculation with Role-Based Override**:
  - End dates automatically calculated from organization settings (default 6 weeks)
  - Configurable cycle length in Organization Settings (4-12 weeks supported)
  - Role-based override capability: ADMIN and PROJECT_MANAGER can set custom dates
  - Regular users (DEVELOPER, QA, PRODUCT) restricted to auto-calculated dates
  - Frontend toggle for privileged users to choose between auto or manual dates
  - Backend validation with AccessDeniedException for unauthorized overrides
  - Auto-calculation fallback to 6 weeks if configuration is invalid
  - Prevents configuration conflicts ensuring standardized planning horizons
  - Integration tests: 5 new tests covering auto-calculation and role-based access
  - Unit tests: 8 new tests for CycleService date calculation logic
  - Updated CreateCycleRequest DTO to make endDate optional
  - Documentation updates in README.md and inline code comments

- **Circuit Breaker - Shape Up Safety Valve**:
  - Automated overflow detection with configurable thresholds (default 80%, range 50-150%)
  - Real-time budget monitoring: tracks work logs against pitch appetite in hours
  - Color-coded severity indicators: blue (<80%), yellow (80-89%), orange (90-99%), red (≥100%)
  - Trigger circuit breaker mechanism: flag pitches for team discussion with reason documentation
  - Kill pitch capability: permanently cancel pitches with CIRCUIT_BREAKER status
  - Resolve circuit breaker workflow: clear flags and update status when scope is cut
  - Team notifications: dashboard alerts for all pitch stakeholders on trigger/kill events
  - CircuitBreakerDTO with 12 fields: appetite, hours spent, utilization %, overflow %, status
  - 5 REST endpoints: detect overflow, get triggered, trigger, kill, resolve
  - Integration with RiskAnalysisService: +50 risk points for circuit breaker status
  - Comprehensive help guide: `/help/circuit-breaker` with Shape Up principles
  - Frontend monitor page: `/cycles/:id/circuit-breaker` with threshold slider and action dialogs
  - Added V50 database migration for 3 new Pitch fields: isCircuitBreakerTriggered, circuitBreakerReason, circuitBreakerDate
  - Full test coverage: CircuitBreakerControllerIntegrationTest with 15 test cases

- **Anonymous Retrospective Submissions**:
  - Added `isAnonymous` boolean field to RetroItem entity for psychological safety
  - Checkbox option in retro board UI: "Post anonymously"
  - Author attribution hidden when `isAnonymous=true` (author field set to null)
  - Backend validation: CreateRetroItemRequest and RetroItemDTO updated
  - Frontend state management: per-column isAnonymous tracking
  - Database migration V49: `is_anonymous` column with index on RetroItem table
  - Updated RetrospectivesGuide.tsx with anonymous submission documentation
  - Test coverage: RetroControllerIntegrationTest with 3 anonymous-specific test cases

- **Navigation & UX Refinements**:
  - Added comprehensive project detail page with cycles list, teams, and statistics
  - Implemented search functionality across Projects, Teams, Retrospectives, and Pitch Board pages
  - Added sorting options to all list pages (by name, date, status, team, etc.)
  - Made project cards clickable to navigate to detailed project view
  - Standardized UI patterns: using full pages instead of modals for comprehensive data display
  - Enhanced user experience with consistent search and filter patterns across all list views

- **Automated Health Risk Detection**:
  - Enhanced automated health risk detection with weighted 4-factor scoring algorithm
  - Added configurable risk thresholds (30+ parameters) via Organization Settings
  - Implemented intelligent risk calculation: Budget (25%), Bugs (30%), Scope (25%), Time (20%)
  - Visual enhancements for critical items: 8px red borders, CRITICAL badges, pulse animations
  - Enhanced warning banners in Cycle Health Summary with gradient effects
  - Color-coded progress bars with badges for budget tracking
  - Added comprehensive test coverage for configurable thresholds (7 new test cases)
  
- **Configurable Risk Thresholds**:
  - **Budget Thresholds**: Warning at 80%, Overrun at 100%, Critical at 120% (customizable)
  - **Bug Count Thresholds**: Critical bugs (1/3/5), Major bugs (3/5), Open bugs (5/10/15) levels
  - **Scope Progress Thresholds**: Uphill max position, progress rate expectations, lag detection
  - **Time-based Thresholds**: Urgency at 3 days, Warning at 7 days, Concern at 14 days
  - **Schedule Variance**: Moderate gap at 15%, Significant gap at 30%
  - **Cycle Progress**: Midpoint (50%), Late phase (60%), Final quarter (75%)
  - **Stagnation Detection**: Scope stagnation (7 days), Peak stuck (5 days), No progress (7 days)
  - **Work Rate Indicators**: High hours threshold (15hrs/3days), High appetite usage (90%)
  - All thresholds customizable per organization with sensible defaults

- **Shape Up Pitch Enhancements**:
  - Added comprehensive Shape Up methodology fields to pitch creation and editing
  - Implemented 6 new fields: Problem Statement, Solution, Rabbit Holes, Risks, No-Gos, Wireframe Links
  - Created AI-powered pitch document extraction using RunPod/Mistral
  - Added automatic knowledge base indexing for pitch documents
  - Implemented 3-tab pitch creation dialog: Basic Info, Shape Up Details, Documents
  - Added document upload with drag-and-drop support during pitch creation
  - Enhanced pitch detail page with Shape Up Details card and inline editing
  - Added visual feedback for document extraction (green indicator with filename)
  - Implemented auto-tab switching after successful document extraction
  - Added support for multiple wireframe links (one per line)

- **Document Management**:
  - Added document download functionality with proper Content-Type headers
  - Implemented download endpoint: `GET /api/documents/{id}/download`
  - Added Download button in DocumentDropZone component
  - Support for downloading PDF, DOCX, DOC, TXT, and MD files
  - Files download as attachments with original filenames
  - Added document preview capability (view extracted text)
  - Display document metadata: filename, file size, file type, extraction status

### Changed
- **Database Schema**:
  - Added V48 migration for Shape Up fields (problem_statement, solution, rabbit_holes, risks, no_gos, wireframe_links)
  - Fixed H2 database compatibility in partial index syntax
  - Updated Pitch entity, PitchDTO, and CreatePitchRequest with Shape Up fields

- **AI Configuration**:

  - Enhanced PitchShapingExtractorService with structured JSON extraction prompt
  - Improved knowledge base integration for pitch documents

- **UX Improvements**:
  - Fixed Content-Type header for multipart/form-data uploads
  - Improved visual feedback with "Document Extracted" indicator
  - Auto-navigation to Shape Up tab after extraction
  - Enhanced wireframe links input with clear placeholder and helper text
  - Better document visibility with badges for extraction and indexing status

### Fixed

- **H2 SQL Syntax**: Removed WHERE clause from partial index for H2 compatibility
- **Document Visibility**: Added extractedDocumentName state to show uploaded document name
- **Tab Navigation**: Implemented activeTab state for controlled tab switching

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.1] - 2026-01-14

### Added
- **Branding & Icon System**:
  - Added high-quality application icon (2048x2048) for consistent branding
  - Generated multi-resolution favicons (16x16, 32x32, 48x48, 180x180, 192x192, 512x512)
  - Created PWA-ready web manifest with proper icon configuration
  - Updated logo across application: sidebar, login page, landing page, and README
  - Replaced text-based logos ("SF", "SU") with actual icon image

### Fixed
- **Build System**:
  - Fixed Lombok annotation processing compatibility with Java 21
  - Added explicit annotation processor path in Maven compiler plugin
  - Updated Lombok to version 1.18.36 for better Java 21 support
  - Resolved compilation errors with entity getter/setter methods
  
- **Database Migration**:
  - Fixed CyclePhase enum mismatch: changed "EXECUTION" to "BUILD" in V47 migration
  - Resolved Flyway checksum validation errors
  - Fixed Java/Maven version compatibility issues (enforced Java 17)

## [0.2.0] - 2026-01-14

### Fixed
  - Fixed H2 compatibility issues in screenshot seed data migration
  - Changed `INTERVAL '42 days'` to H2-compatible `DATEADD('DAY', 42, CURRENT_DATE)`
  - Corrected pitches table INSERT to use `appetite_days` instead of non-existent `appetite` column
  - Fixed PitchStatus enum value from `ACCEPTED` to `IN_PROGRESS`
  - Fixed FlywayRepair class package declaration

- **Authentication Token Storage**:
  - Fixed GitHub and Slack services using incorrect localStorage key
  - Changed from `'token'` to `'shipflow_token'` to match AuthContext
  - Resolved 401 Unauthorized errors on `/api/github/repositories` and `/api/slack/configurations`
  - Services now correctly retrieve JWT tokens for authenticated requests

### Removed
- **Navigation Cleanup**:
  - Removed "Help" menu section from sidebar navigation (keyboard shortcuts moved to future release)
  - Removed "Seed Hill Chart Data" button from User Management page (development-only feature)

### Improved
- **Navigation Organization**:
  - Reorganized Administration section with better grouping
  - Created "User & Access" collapsible group containing User Management and Permissions
  - Separated Organization Settings as standalone item for better visibility
  - Maintained Integrations group (Slack, GitHub) under Administration

### Added

- **Help & Guides System**:
  - **Help Center Hub** (`/help`):
    - Comprehensive help center landing page with 8 guide categories
    - Guide cards with icons, descriptions, and color coding
    - Quick access links with keyboard shortcuts displayed
    - Responsive grid layout for guide navigation
  - **Interactive Guide Pages**:
    - **Getting Started Guide** (`/help/getting-started`): Introduction to ShipFlow, navigation, and core concepts
    - **Hill Charts Guide** (`/help/hill-charts`): Master hill chart visualization for progress tracking
    - **Betting Meeting Guide** (`/help/betting-meeting`): Step-by-step guide for effective betting meetings
    - **AI Risk Advisor Guide** (`/help/ai-risk-advisor`): Leverage AI-powered risk assessments
    - **Cycle Setup Guide** (`/help/cycle-setup`): Complete cycle creation and management walkthrough
    - **QA & Testing Guide** (`/help/qa-testing`): Manage test cases, AI test generation, and bug tracking
    - **Retrospectives Guide** (`/help/retrospectives`): Run effective retrospectives
    - **Reports & Dashboards Guide** (`/help/reports`): Visualize metrics and create custom dashboards
  - **Guide Features**:
    - Rich content with step-by-step instructions
    - Screenshots embedded in guides (15+ screenshots in `/public/guides/`)
    - Cross-linked related guides for easy navigation
    - Back to help center navigation on all guide pages
    - Organized sections with clear headings and best practices
  - **Navigation Integration**:
    - Added "Help & Guides" menu item in sidebar with BookOpen icon
    - Accessible via `/help` route
    - Integrated with application layout and navigation system

- **Keyboard Shortcuts System**:
  - **KeyboardShortcutsHelp Component**:
    - Modal dialog displaying all available keyboard shortcuts
    - Organized shortcut list with descriptions
    - Visual key chips showing keyboard combinations (⌘, Ctrl, Alt, ⇧)
    - Accessible interface with ARIA labels
    - Responsive hover effects for better UX
  - **useKeyboardShortcuts Hook**:
    - Custom React hook for managing keyboard shortcuts
    - Support for meta, ctrl, alt, and shift modifiers
    - Global event listener with cleanup
    - TypeScript interface for shortcut definitions
  - **Available Shortcuts**:
    - Quick navigation to common pages
    - Dashboard and cycle management shortcuts
    - Integration with QuickLinks component

- **Quick Links Component**:
  - Quick access widget for frequent actions
  - 8 pre-configured quick links with icons and colors:
    - New Cycle (⇧N)
    - Log Work (⇧W)
    - View Pitches (P)
    - Tasks
    - Run Reports
    - Current Cycle
    - QA Dashboard
    - Hill Chart
  - Keyboard shortcut hints displayed on each link
  - Smooth animations with Framer Motion
  - Tooltip descriptions for better UX
  - Color-coded cards for visual distinction

- **Slack Integration**:
  - **Backend Features**:
    - Database tables for Slack configuration, channel settings, and notification history
    - `SlackConfiguration` entity for workspace-level settings
    - `SlackChannelConfig` entity for channel-specific notification preferences
    - `SlackNotificationHistory` entity for audit logging
    - `SlackIntegrationService` for sending notifications and managing configuration
    - `SlackIntegrationController` with REST endpoints for configuration management
    - Integration with `DashboardNotificationService` for automatic Slack notifications
    - Support for 8 notification types: task assigned, task completed, task blocked, pitch shaped, cycle started, cycle cooldown, betting completed, sprint started
    - Channel-specific notification filtering
    - Test notification functionality
    - RestTemplate HTTP client configuration
  - **Frontend Features**:
    - Slack Integration settings page at `/slack`
    - Workspace configuration UI with webhook URL management
    - Channel-specific notification preference management
    - Test notification sending interface
    - Notification history viewing
    - Navigation integration with MessageSquare icon
    - TypeScript service with full type definitions
  - **Documentation**:
    - Comprehensive Slack integration guide
    - Setup instructions with screenshots
    - API documentation
    - Troubleshooting section
    - Security considerations
  - **Testing**:
    - Controller integration tests with MockMvc
    - Service unit tests with Mockito
    - 90%+ code coverage for Slack integration components
- **Role-Based Access Control (RBAC) System**:
  - **Backend Features**:
    - New `Permission` entity linking roles to resource actions
    - `ResourceType` enum: CYCLE, PITCH, BUG, REPORT, PROJECT, TEAM, USER, DASHBOARD, AI_FEATURES, SYSTEM
    - `PermissionType` enum: CREATE, READ, UPDATE, DELETE, EXECUTE, MANAGE, APPROVE
    - `PermissionService` for checking and managing permissions
    - `PermissionRepository` with optimized queries for permission lookups
    - `@RequirePermission` annotation for declarative permission checking
    - `PermissionAspect` AOP component for enforcing permissions
    - V44 migration: Creates permissions table and loads default role permissions
    - Default permissions for all roles (ADMIN, PROJECT_MANAGER, PRODUCT, DEVELOPER, QA)
    - Configuration property `app.security.rbac.enabled` to enable/disable RBAC
  - **Frontend Features**:
    - **Permission Management UI** (`/permissions`): Comprehensive interface for viewing and understanding RBAC
    - **Permission Matrix View**: Visual grid showing all roles vs. resources with abbreviated permissions
    - **Role Details View**: Detailed permissions for each role organized by resource type
    - **My Permissions View**: Personal permission dashboard for current user
    - **Search & Filter**: Search resources and filter by resource type
    - **Color-Coded Roles**: Visual distinction between different roles (ADMIN=Red, PM=Blue, etc.)
    - **Permission Service**: TypeScript service for frontend permission operations
    - **Responsive Design**: Mobile-friendly permission management interface
    - **Access Control**: Non-admin users can only view their own permissions
  - **API Endpoints**:
    - `GET /api/permissions/current-user`: Get current user's permissions
    - `GET /api/permissions/role/{role}`: Get permissions for a role (admin only)
    - `GET /api/permissions/resource/{resourceType}`: Get permissions for a resource (admin only)
  - **Documentation**:
    - Comprehensive RBAC_GUIDE.md with architecture, usage, and best practices
    - PERMISSION_MANAGEMENT_UI_GUIDE.md for frontend UI documentation
    - Permission matrix for all roles and resources
    - Migration guide from legacy system
    - Future enhancement roadmap
  - **Testing**:
    - 20+ unit tests for PermissionService (100% coverage)
    - Graceful degradation when permissions table doesn't exist (test mode)
    - Backward compatibility with @PreAuthorize annotations
  - **Security**:
    - Fine-grained permissions per resource type and action
    - Layered security with both Spring Security and RBAC checks
    - Protection for all major controllers (Cycle, Pitch, User, etc.)
    - Extensible design for future custom permissions

### Fixed
- **H2 Database Compatibility for GitHub Integration**:
  - Fixed V43 migration SQL syntax for H2 database compatibility
  - Changed `TEXT` column type to `CLOB` in migration files
  - Changed `UNIQUE KEY` syntax to `CONSTRAINT ... UNIQUE` syntax
  - Removed incompatible `INDEX` creation statements
  - Updated GitHub entity annotations to use `@Lob` instead of `columnDefinition = "TEXT"`
  - Entities updated: `GitHubCommit`, `GitHubPullRequest`, `GitHubWebhookEvent`
  - Migration V43 now successfully executes on H2 in-memory database

### Added
- **Development Environment Improvements**:
  - Added `.env` file configuration support for AI providers
  - Set Ollama as default/recommended AI provider for local development
  - Updated documentation to prioritize Ollama over RunPod for easier setup
  - No API keys required for local development with Ollama

- **Organization Settings - Colors & Bug Configuration**:
  - **Backend Features**:
    - New `colors_json` TEXT column for appetite/actual hour color customization
    - New `bug_statuses_json` TEXT column for bug workflow statuses
    - New `severity_levels_json` TEXT column for bug priority levels
    - V41 migration: Complete organization settings table with new columns
    - V42 migration: Backward compatibility for existing installations
    - JSON serialization/deserialization in `OrganizationSettingsService`
    - Default configurations: 4 colors, 5 bug statuses, 4 severity levels
    - Explicit `@Column(name="snake_case")` annotations for H2 database compatibility
  - **Frontend Features**:
    - New "Colors" tab in Organization Settings with HTML5 color pickers
    - New "Bug Config" tab displaying bug statuses and severity levels
    - `ColorSettings` interface: appetiteHours, actualHours, overBudget, underBudget
    - `BugStatusConfig` interface: name, description, color, isActive, order, isClosed
    - `SeverityLevelConfig` interface: name, description, color, isActive, order, priority
    - Real-time color preview with hex codes
  - **Default Configurations**:
    - **Colors**: `appetiteHours` (Blue #3B82F6), `actualHours` (Green #10B981), `overBudget` (Red #EF4444), `underBudget` (Green #22C55E)
    - **Bug Statuses**: NEW, IN_PROGRESS, FIXED, VERIFIED, WONT_FIX
    - **Severity Levels**: CRITICAL, HIGH, MEDIUM, LOW
  - **Database Compatibility**:
    - Fixed H2 column naming issue with explicit JPA annotations
    - Verified with 34/34 tests passing
    - Successfully applied all 36 migrations (v1 → v42)

- **Custom Dashboards - Smart Context Filter Toggle**:
  - **Backend Features**:
    - New `user_context_filter` boolean column on `custom_dashboards` table (V39 migration)
    - `CustomDashboardService.toggleUserContextFilter()` method for toggling filter state
    - New endpoint: `PUT /api/dashboards/custom/{id}/toggle-context-filter`
    - Intelligent defaults: Developer/QA templates default to personal context, Executive/Manager templates default to organization-wide
    - Stateful toggle persisted per dashboard in database
  - **Frontend Features**:
    - Toggle switch with Filter icon in dashboard header
    - Shows "Personal" vs "Organization-Wide" label based on state
    - Automatic widget data refresh when toggled
    - Success messages indicating current filter mode
  - **Data Filtering**:
    - **Personal Context Mode**: Filters tasks to user's assignments, teams to user's memberships
    - **Organization-Wide Mode**: Shows all data across the organization
    - Client-side filtering using current user from localStorage
    - Supports TASK_LIST, TEAM_STATS, CYCLE_SUMMARY, PITCH_LIST data sources
  - **User Experience**:
    - Developers/QA see their own data by default with option to view organization-wide
    - Executives/Managers see organization-wide data by default with option to view personal context
    - Toggle state persists across sessions per dashboard

- **Dashboard Widget Improvements**:
  - **Table Widget Enhancements**:
    - Fixed sticky header scrolling issue with proper z-index layering
    - Added solid background to prevent data overlap during scroll
    - Improved header visibility with `bg-background` and `z-10` styling
  - **QA Dashboard Template Fixes** (V40 migration):
    - Fixed invalid widget filters (removed non-existent "QA" category and "overdue" field)
    - Updated widget configurations:
      - "Blocked Tasks": Shows tasks with `status = BLOCKED`
      - "High Priority Tasks": Shows tasks with `priority = HIGH`
      - "In Progress Tasks": Shows tasks with `status = IN_PROGRESS`
      - "Recently Completed": Shows tasks with `status = DONE`
    - Increased page size from 5 to 10 for better data visibility
    - All widgets now display data correctly in both personal and organization-wide modes

- **Reports Module - Comprehensive Analytics and Reporting**:
  - **Backend Features**:
    - `EnhancedCycleReportDTO` with comprehensive metrics including risk distribution
    - `RiskDistributionDTO` for risk analysis aggregation (LOW/MEDIUM/HIGH/CRITICAL counts)
    - `ReportService.getEnhancedCycleReport()` method for complete cycle analytics
    - `ReportService.calculateRiskDistribution()` integrating with RiskAnalysisService
    - New endpoint: `GET /api/reports/cycle/{cycleId}/enhanced` for enhanced reports
    - Pitch metrics: total, completed, in-progress, not-started counts
    - Hours analysis: appetite vs actual with variance calculations
    - Efficiency ratios: (actual/appetite) × 100
    - Team member statistics: total, average, max, min hours per member
    - Top performers identification (members with ≥6h/day avg and above-average hours)
    - Over-budget pitches flagging
    - Out-of-scope work (tasks) tracking with estimate vs actual hours
  - **Risk Distribution Analysis**:
    - LOW/MEDIUM/HIGH/CRITICAL risk level counts
    - Average, max, and min risk scores across all pitches
    - Integration with fast rule-based risk analysis for performance
    - Risk score calculations (0-100 scale)
  - **Frontend Features**:
    - Enhanced Reports page with comprehensive UI overhaul
    - Risk Distribution pie chart with color-coded segments
    - Variance Analysis section showing over/under budget metrics
    - Top Performers highlight cards
    - Over-Budget Pitches warning section
    - Appetite vs Actual hours bar chart (side-by-side comparison)
    - Pitch Status Distribution pie chart
    - Hours by Team Member horizontal bar chart
    - Summary statistics cards with 6 key metrics
    - Detailed pitch reports table with variance indicators
    - Member work summary table with role badges
    - Responsive layout optimized for all screen sizes
  - **Export Functionality**:
    - PDF export with all enhanced metrics (backward compatible)
    - CSV export with all enhanced metrics (backward compatible)
    - Automatic filenames: `cycle_report_{cycleId}.pdf/csv`
  - **Performance Optimizations**:
    - Uses fast rule-based risk analysis (not AI) for quick report generation
    - Leverages AICacheService for risk calculation caching
    - Batch processing of pitch and member calculations
    - Optimized database queries with minimal round trips
  - **Test Coverage**:
    - `ReportServiceTest`: 95%+ coverage with comprehensive unit tests
    - `ReportControllerIntegrationTest`: 100% endpoint coverage
    - Tests cover: risk distribution, variance analysis, top performers, empty cycles
    - Integration tests validate JSON responses and export functionality
  - **Documentation**:
    - API examples with sample responses
    - Usage guide for stakeholders and developers
    - Architecture documentation
    - Performance and security considerations

- **Health Overview - Automated Risk Detection**: Comprehensive risk analysis system for pitch health monitoring
  - **Backend Features**:
    - Automated risk level calculation based on multiple factors (bugs, scope completion, budget, timeline)
    - Bug count analysis: Critical/blocker bugs add significant risk scores
    - Scope completion tracking via Hill Chart positions to detect stagnant work
    - Work hours analysis comparing budget usage vs timeline progress
    - Risk trend indicators (IMPROVING, STABLE, WORSENING) based on recent changes
    - Enhanced `PitchHealthService.calculateRuleBasedRiskLevel()` with comprehensive scoring
    - New `PitchHealthService.calculateRiskTrend()` method analyzing last 3-7 days of activity
    - Risk scoring thresholds: CRITICAL (≥70), HIGH (≥50), MEDIUM (≥25), LOW (<25)
  - **Risk Detection Rules**:
    - **Critical Bugs**: 3+ critical/blocker bugs = +35 points, 1+ = +20 points
    - **Open Bugs**: >10 open bugs with <7 days = +15 points
    - **Budget Overruns**: >120% = +40 points, >100% = +25 points, >80% = +10 points
    - **Stagnant Scopes**: Scopes unchanged for 7+ days in uphill phase = +10-30 points
    - **Timeline Pressure**: <3 days remaining without testing/done status = +30 points
    - **Behind Schedule**: Time progress exceeding work progress by 30% = +30 points
  - **Trend Analysis**:
    - Recent critical bugs (last 3 days) trigger WORSENING trend
    - Hill chart updates (last 7 days) trigger IMPROVING trend
    - Accelerating budget burn (>15 hours in 3 days while >90% budget) = WORSENING
    - No progress with <14 days remaining = WORSENING
  - **Frontend Features**:
    - Priority sorting: Pitches automatically sorted by risk level (CRITICAL → HIGH → MEDIUM → LOW)
    - Pulsing animations on CRITICAL and HIGH risk items for immediate attention
    - Dynamic border widths: Critical (6px), High (5px), Medium/Low (4px)
    - Shadow effects with red glow on critical pitches
    - Alert banner showing count of critical pitches requiring attention
    - Risk trend badges with directional icons (↓ green, ↑ red, − gray)
    - URGENT badge on critical pitches with pulsing animation
    - Days-left badges when ≤3 days remaining
    - Enhanced stat cards with hover effects and time-sensitive coloring
    - Improved visual hierarchy emphasizing critical items
  - **UI/UX Improvements**:
    - Critical pitch stat card pulses and shows "Needs attention!" label
    - Days-left counter turns orange when ≤7 days
    - Smooth transitions and animations for better user feedback
    - Tooltips explaining risk trends and status indicators
  - **Test Coverage**:
    - 14 unit tests in `PitchHealthServiceTest` covering all risk scenarios
    - 15 integration tests in `PitchHealthControllerIntegrationTest`
    - Tests verify bug detection, budget analysis, scope tracking, and trend calculation
    - Tests cover healthy, at-risk, and critical pitch scenarios
  - **API Endpoints**: Existing endpoints enhanced with new risk data
    - `GET /api/health/pitch/{pitchId}` - Returns risk level, color, and trend
    - `GET /api/health/cycle/{cycleId}` - Aggregated health with risk breakdown
    - `GET /api/health/active-cycles` - All active cycles with risk metrics

  - **Test Coverage**:
    - `CustomDashboardServiceTest`: Added 5 new tests for user context filter toggle functionality
    - Tests cover: toggle from false to true, toggle from true to false, toggle from null, not found, unauthorized access
    - Total dashboard service test coverage: 13 tests (8 scope tests + 5 context filter tests), 100% pass rate
    - All integration tests passing with comprehensive coverage across modules
  
  - **Documentation**:
    - Documented smart context filter toggle usage and behavior
    - Added filter operators reference and best practices
    - Included API endpoints reference for developers
    - Migration history (V38, V39, V40) documented

## [0.1.0] - 2026-01-10

### Added
- **Meeting Module Enhancements**: Comprehensive improvements to meeting management
  - **Backend Features**:
    - Server-side pagination with configurable page size and sorting
    - Advanced filtering by type, date range, DOR/DOD readiness status, pitch, cycle, and project
    - Dynamic Specification-based queries for flexible filtering
    - New MeetingAction entity for tracking action items with status, assignments, and due dates
    - Retrospective linking for meetings
    - Decisions and attendees text fields
    - Database migration V35 with meeting_actions table and enhanced meeting fields
  - **API Endpoints**:
    - `GET /api/meetings/paginated` - Paginated meetings with DESC sort by default
    - `GET /api/meetings/filter` - Advanced filtering with multiple criteria
  - **Frontend Features**:
    - Collapsible filter panel with type selection, date range pickers, and status toggles
    - Pagination controls with page size selector (10, 20, 50 items per page)
    - Enhanced meeting dialog (max-w-3xl) with retrospective selector
    - Decisions and attendees fields
    - Dynamic action items manager with add/update/remove capabilities
    - Person assignment for action items with status tracking (OPEN, IN_PROGRESS, COMPLETED, CANCELLED)
    - Due date picker for action items
  - **Test Coverage**:
    - 16 unit tests in MeetingServiceTest (up from 8)
    - 15 integration tests in MeetingControllerIntegrationTest (up from 6)
    - Tests cover pagination, filtering, action items, retrospective linking, and error validation

### Fixed
- **Exception Handling**: BadRequestException now correctly returns 400 status instead of 500
  - Added explicit handler in GlobalExceptionHandler for BadRequestException
  - Prevents RuntimeException handler from catching BadRequestException
  - WorkLogTimer validation errors now return proper HTTP 400 responses
  - All 525 backend tests now pass
  - **Test Coverage**:
    - 16 unit tests in MeetingServiceTest (up from 8)
    - 15 integration tests in MeetingControllerIntegrationTest (up from 6)
    - Tests cover pagination, filtering, action items, retrospective linking, and error validation

- **Sub-task Hierarchy**: Tasks can now have parent-child relationships for better organization
  - Database migration V33 adds self-referencing `parent_task_id` column with CASCADE delete
  - Backend support for creating, updating, and querying hierarchical tasks
  - Circular reference prevention (tasks cannot be their own ancestor)
  - New REST endpoints:
    - `GET /api/tasks/{id}/subtasks` - Get direct children of a task
    - `GET /api/tasks/cycle/{cycleId}/roots` - Get root-level tasks (no parent)
    - `GET /api/tasks/cycle/{cycleId}/tree` - Get complete task tree with nested children
  - Comprehensive unit tests (10/10 passing) for hierarchy operations
  - **Frontend UI Features**:
    - Parent task selector in create/edit dialog
    - "Add Sub-task" button on each task row for quick sub-task creation
    - Visual indentation and arrow icon for sub-tasks
    - Display of parent task title below sub-task name
    - Tasks with same parent grouped visually

- **Task-based Time Logging**: Work logs can now be associated with Tasks in addition to Pitches
  - Manual time entry for tasks
  - Toggle between Pitch and Task when logging time
  - Backend API support for task-based work logs
  - Updated work log entities, DTOs, and services
  - Database migration V32 to support optional task references
  - Validation to ensure either pitchId or taskId is provided (but not both)
  - Frontend UI with toggle buttons for selecting Pitch or Task
  - Work log table displays both pitch and task information with badges
  - Edit dialog supports switching between pitch and task
  - Comprehensive unit tests (12/12 passing) for work log operations

- **Timer Integration for Time Tracking**: Added timer-based time tracking alongside manual entry
  - Database migration V34 for work_log_timers table
  - Backend REST API for timer operations:
    - `POST /api/timers/start` - Start timer for pitch or task
    - `POST /api/timers/stop` - Stop timer and create work log entry
    - `GET /api/timers/active` - Get currently running timer
    - `DELETE /api/timers/cancel` - Cancel timer without logging
  - Timer Service with business logic:
    - Automatic time rounding to nearest 0.25 hours (15 minutes)
    - One active timer per user enforcement
    - Elapsed time calculation with real-time updates
    - Quarter-hour rounding on timer stop
  - Frontend Timer Widget (floating card):
    - Real-time elapsed time display (HH:MM:SS format)
    - Shows associated pitch/task and note
    - Stop & Log button with confirmation dialog
    - Cancel button to discard timer
    - Automatically appears when timer is running
  - Timer Integration in My Work Logs page:
    - "Start Timer" button alongside manual entry form
    - Auto-reloads work logs when timer stopped
    - Uses same pitch/task selector as manual entry
  - Comprehensive testing:
    - Backend unit tests: 11/11 passing (WorkLogTimerServiceTest)
    - Backend integration tests: 9/9 passing (WorkLogTimerControllerIntegrationTest)
    - Total timer tests: 20/20 passing

### Testing Summary
- **Backend Tests**: 42 tests passing across all new features
  - Task Hierarchy: 10/10 tests
  - Task-based Work Logs: 12/12 tests
  - Timer Service: 11/11 tests
  - Timer Controller Integration: 9/9 tests
- **Frontend Tests**: 106 tests passing
  - taskService: 19/19 tests (including subtask hierarchy: getSubTasks, getRootTasks, getTaskTree)
  - workLogService: Tests for work log CRUD operations
  - pitchService: Tests for pitch management
  - Other services and components
- **Test Coverage**: Service layer, repository layer, REST endpoints, and error scenarios

### Design Decisions
- **Dual Time Logging**: Both manual entry AND timer integration for maximum flexibility
- **Timer Rounding**: Automatic rounding to 0.25 hours (15-minute increments) for consistency
- **Single Active Timer**: Only one timer per user at a time to prevent accidental double-tracking
- **Seamless Integration**: Timer and manual entry share same UI/UX for pitch/task selection
- **Dashboard Customization**: Users can now customize which widgets appear on their dashboard and in what order
  - Widget visibility toggle
  - Configurable display order with bulk update support
  - Default widgets: Stats Cards, Quick Links, Active Cycles, Recent Pitches, Hill Chart, Recent Activity, Risk Overview
  - Reset to defaults functionality
- **Notification System**: Real-time notifications for important events
  - NotificationCenter component in header with unread badge
  - Notification types: Overdue Tasks, Blocked Tasks, Cycle Deadlines, Stalled Hill Charts
  - Severity levels: INFO, WARNING, ERROR, CRITICAL
  - Click-to-navigate to related entities
  - Mark as read, delete, and mark all as read actions
  - Auto-poll for new notifications every 30 seconds
  - Automated daily generation at 8 AM
  - Automatic cleanup of old notifications (30 days)
- Initial release of ShipFlow - Modern project management application implementing the Shape Up methodology
- **Core Features**:
  - Cycles: 6-week development cycles with betting table
  - Pitches: Shape work with appetite, problem definition, and solution
  - Hill Charts: Visual progress tracking with drag-and-drop dots
  - Tasks: Independent work management during cycles with categorization (Pitch Scope vs. Debt & Improvements)
  - Retrospectives: Team retros with voting and merging capabilities
  - Projects & Teams: Organization-wide project and team management
  - Work Logs & Meetings: Time tracking and meeting documentation
  - Health Dashboard: Project health metrics and risk insights
  
- **AI-Powered Features**:
  - Q&A System: RAG-based knowledge retrieval from project documents with smart relevance filtering, source citation tracking, and conversation memory
  - Test Case Generation: AI-assisted test case generation with type-specific prompts and quality validation
  - Query Decomposition: Handles complex multi-part questions
  - Active Learning: Continuous quality improvement from user feedback
  - LLM Response Caching: 40-60% cost reduction with Redis support
  - Prompt Compression: 10-20% token reduction
  - Content Guardrails: Production safety with toxic content and bias detection
  - Document Processing: Support for PDF, DOCX, DOC, TXT, MD files with automatic text extraction
  
- **User Experience**:
  - WCAG 2.1 AA compliant accessibility (88/100 score)
  - Fully responsive mobile design with touch-friendly 44px minimum touch targets
  - Keyboard shortcuts for navigation and quick actions (press `?` to view all)
  - Page transitions and animated components
  - Breadcrumb navigation
  - Enhanced empty states with illustrations
  - Dark/light theme support
  
- **Backend Architecture**:
  - Spring Boot REST API with comprehensive Swagger documentation
  - PostgreSQL database with Flyway migrations
  - Redis support for distributed caching
  - ChromaDB integration for vector storage
  - Security features: JWT authentication, malicious request filtering, CORS protection
  - Comprehensive test coverage with JUnit and integration tests
  
- **Frontend Architecture**:
  - React with TypeScript and Vite
  - shadcn/ui component library (Radix UI primitives with Tailwind CSS)
  - Framer Motion for animations
  - React Query for data fetching
  - Comprehensive form validation and error handling
  
- **DevOps & Configuration**:
  - Docker and Docker Compose support
  - Ollama integration for local LLM inference
  - Redis configuration for production caching
  - Environment-based configuration (dev/prod profiles)
  - Health checks and monitoring endpoints

### Security
- Malicious request detection and blocking (Log4Shell, XSS, SQL Injection, Path Traversal)
- Header injection attack prevention
- JWT-based authentication
- CORS configuration
- Security document filtering for access control
