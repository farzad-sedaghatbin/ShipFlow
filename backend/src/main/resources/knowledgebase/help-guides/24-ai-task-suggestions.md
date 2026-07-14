# AI-Recommended Deliverable Tasks

Generate a batch of deliverable task suggestions for a pitch instead of writing each one by hand.

## Where to Find It

On a **Pitch Detail** page, above the Tasks card, click **Suggest Tasks with AI**.

## What Goes Into the Suggestions

The AI reads the pitch's problem statement, solution, appetite, rabbit holes, risks, and no-gos, and suggests 4-8 concrete deliverable tasks grounded in that scope — it won't suggest anything called out in Rabbit Holes or No-Gos, and it respects the pitch's appetite.

If your organization has a **Figma access token** configured (Organization Settings) and the pitch's **wireframe links** field contains a Figma URL, the AI also pulls design context (components, colors, layout) from that file and grounds relevant suggestions in it. If Figma isn't configured, or the pitch has no Figma link, suggestions are still generated — just from the pitch text alone, with a note in the dialog letting you know design context wasn't used.

## Disciplines

Every suggestion lists which delivery disciplines are needed to call it done: **Design**, **Backend**, **Mobile**, and **QA**. Most deliverables need more than one — shipping something usually means an API plus the screen that calls it plus a test pass. A suggestion is single-discipline only when it genuinely has no dependency on the others (e.g. a pure backend migration script).

## Creating Tasks from Suggestions

1. Review the suggestions — each shows a title, description, estimated hours (if any), a **Pitch** or **Pitch + Design** badge, and its discipline tags.
2. Check the ones you want (all are selected by default).
3. Click **Create Selected (N)**.

Selected suggestions are created as real tasks under the pitch in one request — if one fails, the others still get created and you're told which one didn't.

## Requirements

- An LLM provider must be configured (`app.ai.provider`) — the button doesn't appear otherwise.
- The pitch needs an active cycle to create tasks into, same as the manual "Create Task" dialog.
- Generating suggestions requires **AI Features execute** permission (ADMIN, MANAGER, MEMBER); creating tasks requires backlog **create** permission.
