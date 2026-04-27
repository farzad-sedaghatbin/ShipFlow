# QA Testing

## What is QA Testing in ShipFlow?
ShipFlow includes a built-in QA test management system for tracking test cases linked to pitches and cycles.

## How to Create Test Cases
1. Navigate to a **Pitch** or **Cycle** detail page
2. Find the **QA Test Cases** section
3. Click **Add Test Case**
4. Fill in the test title, description, steps, and expected result
5. Set the priority (Critical, High, Medium, Low)

## How to Run Tests
1. Open a test case
2. Mark it as **Pass**, **Fail**, or **Blocked**
3. Add notes or evidence (screenshots, logs)
4. Failed tests can be linked to bug reports

## QA Dashboard
The **QA Dashboard** provides an overview of:
- Total test cases per cycle
- Pass/Fail/Blocked ratios
- Test coverage by pitch
- Trends over time

## Bug Reports
- Failed tests can generate bug reports
- Bug reports track severity, status, and assignment
- Bugs can be tagged to cycles and releases

# Circuit Breaker

## What is Circuit Breaker?
The Circuit Breaker feature automatically monitors pitch progress and triggers alerts when work stagnates beyond configured thresholds.

## How It Works
1. ShipFlow monitors scope/hill chart movement
2. If a scope hasn't moved for a configurable number of days, an alert is triggered
3. Alerts notify the team lead and project manager
4. The circuit "trips" — meaning the pitch needs immediate attention

## How to Configure Circuit Breaker
1. Go to **Organization Settings**
2. Find the **Circuit Breaker** section
3. Set the stagnation threshold (e.g., 3 days without progress)
4. Choose notification channels (in-app, email, Slack webhook)
5. Enable/disable per project or globally

# Cooldown Activities

## What are Cooldown Activities?
Cooldown activities are tasks specifically done during the cooldown period between cycles.

## Types of Cooldown Activities
- **Bug Fixes** — Address bugs found during the build phase
- **Technical Debt** — Refactor code, improve performance
- **Exploration** — Prototype new ideas for future cycles
- **Documentation** — Update docs, write guides

## How to Track Cooldown Activities
1. Navigate to the current cycle's **Cooldown** tab
2. Click **Add Activity**
3. Select the activity type
4. Describe the work and assign team members
5. Track completion status
