# Related Test Cases on Pitch Detail

See a pitch's linked test cases without leaving the Pitch Detail page.

## Where to Find It

On a **Pitch Detail** page, below the Tasks card, the **Test Cases** card lists every test case already linked to that pitch.

## What It Shows

Each row shows the test case key, title, type, priority, status, and (once it has been run) its pass rate. Up to 5 test cases are shown inline; if there are more, a note tells you how many are hidden.

Clicking a row opens that test case's detail page. Clicking **View All** — or the button in the empty state when the pitch has no test cases yet — navigates to the pitch's full test-management page (`/pitches/{id}/test`), where you can add test cases manually, generate them with AI, record test runs, and file bugs.

## Where the Data Comes From

Test cases are linked to a pitch either because they were created directly against it (manually or via AI generation, `QATestGenerationService`) or because they were created against one of the pitch's tasks. The card calls the same `GET /api/qa/test-cases/pitch/{pitchId}` endpoint the full test-management page uses, so the two views never disagree.

## Requirements

No extra configuration — the card is visible on every Pitch Detail page under the same **AI Features** / QA permissions as the rest of test management (see `PERMISSION_MATRIX.md`).
