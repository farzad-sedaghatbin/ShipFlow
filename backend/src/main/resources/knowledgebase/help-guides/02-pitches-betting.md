# Pitches & Betting

## What is a Pitch?
A Pitch is a shaped proposal for work to be done in a cycle. It includes:
- **Problem Statement** — What problem are we solving?
- **Appetite** — The time budget (e.g., 2 weeks or 6 weeks) — this is the max time allowed
- **Solution** — How we plan to solve it, with rough boundaries
- **Rabbit Holes** — Known risks and areas to avoid
- **No-Gos** — Things explicitly out of scope

## How to Create a Pitch
1. Navigate to **Pitches** from the sidebar
2. Click **New Pitch**
3. Fill in the title, problem statement, appetite, and solution
4. Add any rabbit holes or no-gos to clarify scope
5. Submit the pitch — it starts as a Draft

## Pitch Statuses
- **Idea** — A raw idea, not yet shaped
- **Draft** — Being shaped but not ready for betting
- **In Progress** — Approved and being built in a cycle
- **Completed** — Finished

## The Pitch's Task Board
A Pitch's detail page shows its linked tasks as a Kanban board — one column per status (Backlog, To Do, In Progress, Blocked, In Review, Done, Cancelled), scoped to just that pitch's work. Drag a card between columns to change its status; the change saves immediately. Use **Create Task** to log a task directly against the pitch (works even before the pitch has been bet to a cycle — see below), or open a card's menu to add a subtask, view, edit, or delete it. This replaced an earlier drag-to-reorder list — manual task ordering within a pitch is no longer available, since the board's column position now carries that signal instead.

## Tasks No Longer Require Picking a Cycle Up Front
Creating a task no longer forces you to pick a cycle first. A task's cycle is now a fact that follows from which pitch it's linked to, not something you choose manually when logging work:
- Log a task against a Pitch that hasn't been bet yet (Idea/Draft/Shaped) — it's created with no cycle, and that's fine. It's still fully visible and manageable from the Backlog and the pitch's own task list.
- When that pitch is later bet onto a cycle at the Betting Table, every task already linked to it automatically picks up the cycle — you don't need to go back and re-assign anything.
- If the pitch is un-bet (moved back to the betting pool) or re-bet onto a different cycle, its tasks follow along automatically each time.
- A task's full cycle history (which cycle it was in, and when) is kept for reporting, even as it moves — nothing is lost when a pitch changes cycles.
- Debt & Improvement tasks (not linked to any pitch) never get a cycle at all — they're opportunistic filler work picked up whenever there's room, tracked purely against the project.

The only requirement left is that the pitch itself needs to resolve to a project somehow — either it's already bet on a cycle, has its own project set, or is linked to an epic that has one. A pitch floating with none of those (rare — usually only brand-new ideas not yet organized under an epic) can't have tasks logged against it yet; add it to an epic or bet it on a cycle first.

## What is the Betting Table?
The Betting Table is where stakeholders decide which pitches to bet on for the next cycle. It happens during the BETTING phase.

## How to Run a Betting Meeting
1. Navigate to **Betting Table** from the sidebar
2. Review all pitches submitted for the upcoming cycle
3. Discuss each pitch's appetite, risks, and value
4. **Approve** pitches to include them in the next cycle, or **Reject** to defer
5. Approved pitches automatically move to the BUILD phase when the cycle starts

## Appetite
Appetite is NOT an estimate — it's a budget. It defines how much time the team is willing to spend on a pitch:
- **Small Batch** — 2 weeks
- **Big Batch** — 6 weeks
If the work can't be completed within the appetite, the pitch is reconsidered.
