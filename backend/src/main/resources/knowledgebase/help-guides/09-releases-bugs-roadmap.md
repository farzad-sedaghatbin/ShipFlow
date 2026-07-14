# Releases

## What is a Release?
A Release groups completed work from one or more cycles into a shippable version. Releases help track what's included in each version of your product.

## How to Create a Release
1. Navigate to **Releases** from the sidebar
2. Click **New Release**
3. Enter the release name (e.g., "v1.2.0") and description
4. Select which cycles to include
5. The release automatically aggregates pitches, tasks, and bug fixes from those cycles

## Release Details
Each release shows:
- **Task Breakdown** — Number of tasks by status (open, in progress, completed, blocked)
- **Bug Breakdown** — Bugs by severity and resolution status
- **Slipped Items** — Tasks or bugs that didn't make it into this release
- **Release Notes** — Auto-generated or manually written

## Release Notes
1. Open a release detail page
2. Click **Generate Release Notes** for AI-assisted notes, or write them manually
3. Release notes can be exported as markdown

# Bug Reports

## How to Report a Bug
1. Navigate to **Bug Reports** from the sidebar
2. Click **New Bug Report**
3. Fill in title, description, severity, and steps to reproduce
4. Optionally link to a pitch, cycle, or release
5. Assign a priority and team member

## Bug Severity Levels
- **Critical** — Application is unusable, data loss risk
- **High** — Major feature broken, no workaround
- **Medium** — Feature partially broken, workaround available
- **Low** — Minor issue, cosmetic

## Assignee vs QA Tester
A bug has two distinct people:
- **Assignee** — the person responsible for *fixing* the bug.
- **QA Tester** — the person responsible for *testing/verifying* the fix.

Set either one inline from the bug detail dialog, or when creating/editing a bug. Leave the QA Tester unassigned until the fix is ready, then pick who should verify it. Changing the QA Tester is recorded in the bug's Activity history.

## Sharing a Bug
Use the **Copy link** action (the three-dot menu on a kanban card, or the link button on a list row) to copy a direct URL to the bug. Anyone with access can open the link to land on that bug's full page.

## Filtering Bugs
The Bug Reports page has multi-select filters for **Status**, **Severity**, **Assignee**, and **Reporter** — pick any combination and bugs matching all active filters are shown (toggle **Exclude** to invert the selection). The Reporter filter lets you narrow the backlog to bugs raised by specific people, which is handy when triaging incoming reports. Active filters are reflected in the page URL, so a filtered view is shareable and survives the browser back button, and they also drive the overview stat cards at the top of the page. You can save a filter combination as a named filter to reuse it later.

## List vs Kanban View
Toggle between the **list** (paginated table) and **kanban** (status-column board) views from the top-right of the Bug Reports page. The kanban board groups every matching bug into its status column, while the list view pages through results — use the filters to narrow large backlogs in either view.

## Tracking Bugs Across Releases
- Bugs can be tagged to specific releases
- Filter bugs by release to see what was fixed
- Slipped bugs (not fixed in target release) are highlighted

# Roadmap

## What is the Roadmap?
The Roadmap provides a high-level strategic view of your product using Initiatives and Epics layered on top of the Shape Up cycle structure. It displays items on a Gantt-style timeline so you can see how work is distributed across quarters and the year.

## How to View the Roadmap
1. Navigate to **Roadmap** from the sidebar
2. Choose between **Quarterly** or **Yearly** view using the dropdown
3. Use the arrow buttons to navigate between periods
4. Click the expand icon to enter **Presentation Mode** for a fullscreen view

## What Appears on the Roadmap
Only initiatives and epics that have **both a start date and an end date** set will appear on the roadmap. Items without dates are excluded so the timeline stays clean and meaningful.

Each initiative and epic displays a **quarter label** (e.g., "Q2 2026" or "Q2 – Q3 2026") indicating which quarter(s) it spans.

Items are filtered by the currently visible time window — an epic scheduled for October will not appear in the Q2 quarterly view.

## Releases on the Roadmap
Releases appear as milestone flags on the timeline at their target date. Hovering over a release shows:
- Release name, version, and target date
- Overall progress percentage
- Linked item counts: pitches, tasks, and bugs targeted for that release

To associate items with a release, set the **Target Release** field on individual pitches, tasks, or bug reports. The roadmap aggregates these automatically.

## How to Set Dates
- **Drag and drop**: Grab a timeline bar to move it, or drag its edges to resize
- **Click "Set dates"**: For items without dates, click the link to assign default dates
- **Edit directly**: Open the initiative or epic detail page to set exact dates

## Initiatives
Initiatives are high-level strategic goals (e.g., "Improve user onboarding"):
1. Navigate to **Initiatives** from the sidebar
2. Click **New Initiative**
3. Fill in name, description, status, owner, and **target start/end dates**
4. Link epics to the initiative

## Epics
Epics are large bodies of work within an initiative that break down into pitches:
1. Navigate to **Epics** from the sidebar
2. Click **New Epic**
3. Fill in name, description, status, **target start/end dates**, and link to an initiative
4. Link pitches to the epic for end-to-end traceability

## Linking Work to Releases
To see pitches and bugs on a release milestone, open the item and set its **Target Release** field to the desired release — the roadmap automatically counts and displays these on the release tooltip.

For tasks, the **Target Release** field only appears on tasks that aren't linked to a pitch (e.g. `Debt/Improvement` category work). A pitch-scoped task doesn't get its own picker — it's counted against a release through its parent pitch instead. Assigning cycles to a release also pulls in that cycle's pitches automatically, without needing to set each pitch's target release by hand.

## Initiative → Epic → Pitch Hierarchy
- **Initiative**: Strategic goal (e.g., "Mobile App Launch")
- **Epic**: Major work area within the initiative (e.g., "Authentication System")
- **Pitch**: Specific shaped proposal within the epic (e.g., "Social Login with OAuth")

## Tips
- Set dates on all initiatives and epics you want visible on the roadmap
- Use the quarterly view for near-term planning and yearly view for the big picture
- Quarter labels help quickly identify when work is scheduled without reading exact dates
