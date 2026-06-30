# Project Types: Shape Up, Kanban & Scrum

ShipFlow is methodology-agnostic. Each project runs in one of three modes, and the app adapts its navigation and tools to match. You pick the methodology when you **create** a project.

## Choosing a Methodology

1. Go to **Projects** and click **New Project**.
2. Enter a **Project Name** and **Project Key**.
3. Pick a **Project Type**:
   - **Shape Up** — 6-week cycles with betting, pitches & cooldown
   - **Kanban** — continuous flow with a visual board
   - **Scrum** — time-boxed sprints with story points, burndown, and velocity
4. Click **Create Project**.

> A project's type **cannot be changed after creation** — it determines the data model and UI. If you need a different methodology, create a new project (you can use Import to move work over).

## How the Three Modes Differ

| Concept | Shape Up | Kanban | Scrum |
|---------|----------|--------|-------|
| Time period | Cycle (6 weeks) | None (continuous) | Sprint (1–4 weeks) |
| Planning unit | Pitch | Task | Backlog item |
| Estimation | Appetite | — | Story points |
| Progress view | Hill chart | Board columns | Burndown chart |
| Commitment | Betting table | — | Sprint goal |
| Forecasting | — | — | Velocity |

The sidebar changes per mode:
- **Shape Up** shows **Cycles**, the Pitch Board, Betting Table, Health, and Retrospectives.
- **Kanban** hides cycles and betting; the Backlog opens as a **board** by default.
- **Scrum** renames "Cycles" to **Sprints** and adds a dedicated **Sprint Planning** page.

## Kanban Mode

Kanban projects are for continuous flow — support, maintenance, operations. There are no cycles to manage; work simply moves across board columns: **Backlog → To Do → In Progress → In Review → Done** (plus Blocked / Cancelled). Drag a card between columns to update its status. Pitch, scope, and cycle controls are hidden to keep the interface simple.

## Scrum Mode

Scrum projects run on fixed-length **sprints** and use **story points** to estimate effort.

Key concepts:
- **Product Backlog** — all tasks not yet assigned to a sprint.
- **Sprint Backlog** — tasks committed to the active sprint.
- **Sprint Goal** — an optional one-line objective shown during planning.
- **Burndown chart** — remaining story points vs. the ideal line, updated as tasks complete.
- **Velocity** — average story points completed per sprint, used to forecast capacity.

### Sprint Planning

Open **Sprint Planning** from the sidebar (Scrum projects only). It's a two-column board:

1. Pick a sprint from the **Select Sprint** dropdown.
2. The **Product Backlog** (left) lists unassigned tasks; the **Sprint Backlog** (right) lists tasks in the chosen sprint. Each column shows its total story points.
3. Click the **→** arrow on a backlog task to move it into the sprint, or **←** to move it back.
4. Watch the point totals update live so you don't overcommit relative to your velocity.
5. Below the board, switch between the **Burndown Chart** and **Velocity Chart** tabs to check progress and historical capacity.

To set a task's story points, open the task and fill in the **Estimate (story points)** field. Tasks without points count as zero in the totals.

## Tips

- Use **Shape Up** for shaped, fixed-time feature bets; **Scrum** when you want sprint velocity and burndown; **Kanban** for steady, flow-based work.
- Retrospectives are available in Shape Up and Scrum.
- When "All Projects" is selected in the project switcher, the sidebar defaults to the Shape Up layout.
