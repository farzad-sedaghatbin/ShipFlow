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

## Tracking Bugs Across Releases
- Bugs can be tagged to specific releases
- Filter bugs by release to see what was fixed
- Slipped bugs (not fixed in target release) are highlighted

# Roadmap

## What is the Roadmap?
The Roadmap provides a high-level strategic view of your product using Initiatives and Epics layered on top of the Shape Up cycle structure.

## How to View the Roadmap
1. Navigate to **Roadmap** from the sidebar
2. See initiatives and epics plotted over time
3. Filter by status, owner, or time range

## Initiatives
Initiatives are high-level strategic goals (e.g., "Improve user onboarding"):
1. Navigate to **Initiatives** from the sidebar
2. Click **New Initiative**
3. Fill in name, description, status, owner, and target dates
4. Link epics to the initiative

## Epics
Epics are large bodies of work within an initiative that break down into pitches:
1. Navigate to **Epics** from the sidebar
2. Click **New Epic**
3. Fill in name, description, status, and link to an initiative
4. Link pitches to the epic for end-to-end traceability

## Initiative → Epic → Pitch Hierarchy
- **Initiative**: Strategic goal (e.g., "Mobile App Launch")
- **Epic**: Major work area within the initiative (e.g., "Authentication System")
- **Pitch**: Specific shaped proposal within the epic (e.g., "Social Login with OAuth")
