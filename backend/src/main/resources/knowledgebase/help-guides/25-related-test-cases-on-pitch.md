# Related Test Cases on Pitch Detail and Task Detail

See a pitch's or a task's linked test cases without leaving that page.

## Where to Find It

On a **Pitch Detail** page, below the Tasks card, the **Test Cases** card lists every test case already linked to that pitch. On a **Task Detail** page, the same kind of **Test Cases** card lists every test case linked specifically to that task.

## What It Shows

Each row shows the test case key, title, type, priority, status, and (once it has been run) its pass rate. Up to 5 test cases are shown inline; if there are more, a note tells you how many are hidden.

Clicking a row opens that test case's detail page. On a pitch, **View All** — or the empty-state button when the pitch has no test cases yet — navigates to the pitch's full test-management page (`/pitches/{id}/test`), where you can add test cases manually, generate them with AI, record test runs, and file bugs. On a task, **Add Test Case** opens the new-test-case form pre-linked to that task (`/qa/test-cases/new?taskId={id}`) — there's no AI-generation shortcut or full test-management page at the task level, only the pitch level.

## Where the Data Comes From

A test case can be linked to a pitch, to a task, to both, or to neither — they're independent, optional associations on the test case (set when it's created, manually or via AI generation). The Pitch Detail card calls `GET /api/qa/test-cases/pitch/{pitchId}`; the Task Detail card calls the equivalent `GET /api/qa/test-cases/task/{taskId}`. Creating a test case from a pitch's own "Add Test Case" button links it to that pitch; creating one from a task's own "Add Test Case" button links it to that task — pick whichever button matches what the test case is actually verifying, since neither link implies the other (a test case created from a task does **not** automatically also show up under that task's parent pitch unless it's linked to the pitch too).

## Requirements

No extra configuration — both cards are visible under the same **AI Features** / QA permissions as the rest of test management (see `PERMISSION_MATRIX.md`).
