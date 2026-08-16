# Dashboard & Widgets

## Dashboard Overview
The ShipFlow Dashboard is your command center. It shows a high-level view of active cycles, team progress, and key metrics.

## How to Access the Dashboard
1. Click **Dashboard** in the sidebar (it's the default landing page after login)
2. The dashboard displays: active cycle status, pitch progress, hill chart overview, recent activity

## Customizable Dashboard
ShipFlow offers customizable dashboard views:
1. Click the **Customize** or **Manage Dashboard** button
2. Add, remove, or rearrange **widgets**
3. Available widgets include: Cycle Health, Team Velocity, Hill Chart Overview, Recent Pitches, Notification Feed

## Dashboard Widgets
- **Cycle Health Widget** — Shows overall cycle progress and at-risk pitches
- **Team Velocity Widget** — Displays tasks completed per team over time
- **Hill Chart Overview Widget** — Mini hill charts for all active pitches
- **Recent Activity Widget** — Feed of recent actions across the organization
- **Custom Metric Widget** — Display custom KPIs you've defined

## Health Overview
The Health Overview page shows:
- Overall system health metrics
- Active cycle progress at a glance
- Team workload distribution
- Alerts and warnings from Circuit Breaker

# Backlog

## What is the Backlog?
The Backlog in ShipFlow holds ideas and tasks that haven't been shaped into pitches yet, or pitches that were deferred from previous cycles.

## How to Use the Backlog
1. Navigate to **Backlog** from the sidebar
2. View all unscheduled items
3. Filter by priority, assignee, or release
4. Drag items to reorder by priority

### Sub-Tasks Group Under Their Parent
A sub-task always renders directly under its parent task — in the list view regardless of the active sort field, and on the Kanban board when they share a status column. This is especially useful in Kanban-mode projects, which have no Pitch concept to group backlog items by otherwise. If a sub-task's column differs from its parent's, a "N subtasks" badge on the parent and a "Sub-task of ..." caption on the sub-task keep the relationship visible.

### Pitch Tasks vs. Debt & Improvements is a Filter, Not a Tab
The Backlog page shows one merged list of every task for the project — there's no longer a "Pitch Tasks" / "Debt & Improvements" tab to switch between. The distinction (shaped bet work linked to a pitch, vs. opportunistic filler that isn't) is still available as a **Category** filter alongside Status, Priority, and the other filters, so you can narrow the list down when you need to without it being the default view. Kanban projects still have no Pitch concept, so their tasks are effectively all Debt & Improvement-equivalent filler — the Category filter works the same way there, it just rarely has anything to filter to "Pitch Tasks".

## Adding Items to the Backlog
1. Click **Add Item** on the Backlog page
2. Enter a title and description
3. Optionally tag the item with a priority level
4. Items can later be promoted to pitches for the Betting Table

No task requires picking a cycle up front anymore — link it to a Pitch and its cycle follows the pitch's own bet automatically (including if the pitch is later re-bet onto a different cycle), or leave it unlinked as opportunistic Debt & Improvement work with no cycle at all. See the "Tasks No Longer Require Picking a Cycle Up Front" section of the Pitches & Betting guide for the full picture.

## Moving Backlog Items to Pitches
1. Select a backlog item
2. Click **Convert to Pitch**
3. Fill in the pitch details (appetite, solution, etc.)
4. The pitch is ready for the next betting round
