# ShipFlow Agent (@ShipFlow)

The ShipFlow Agent is an AI teammate you can talk to inside any comment thread. Mention it, ask a question, and it replies as a comment.

## How to Use It

1. Open a **task**, **bug report** or **wiki page** and go to its **Comments** section.
2. Type `@` — **ShipFlow** appears at the top of the suggestions (marked "AI teammate"). Pick it, or just type `@ShipFlow`.
3. Write your question after the mention, for example:
   - `@ShipFlow what is blocking this?`
   - `@ShipFlow summarise this thread`
   - `@ShipFlow have we dealt with something like this before?`
4. Post the comment. A "ShipFlow is thinking…" line appears under the thread, and the agent's answer shows up as a new comment with an **AI** badge, usually within a few seconds (a local model can take longer).

If nothing arrives after about 90 seconds the page stops waiting; refresh the thread to check later.

## What the Agent Knows

When you mention it, the agent reads:

- the item itself (status, priority, assignee, due date, description — or the wiki page text),
- the latest comments in that thread,
- a short summary of the project (cycles, initiatives, recent shipped work),
- related knowledge from your Knowledge Center, the same material the Q&A widget searches.

It answers only from that material. If the answer isn't there it says so instead of guessing.

## Permissions — It Sees What You See

The agent has no access of its own. It answers **as the person who mentioned it**:

- If you can't see the item's project (or the wiki space), the agent stays silent.
- Related knowledge is filtered with **your** teams and projects, the same way the Q&A widget does it. Content indexed from now on is labelled with its project, so another project's cycles, pitches, epics, wiki pages and documents are kept out of the answer if you can't see that project (administrators see everything).
- Everyone who can open the thread can read the reply, so don't ask it things you wouldn't say in the comment itself.

The agent never changes anything by itself. By default it only answers; if you **ask it to make a change**, it can *propose* one and you decide (see below).

## Asking the Agent to Make a Change

If you explicitly ask for a change — for example `@ShipFlow mark this task as done`, `@ShipFlow assign this bug to Sara` or `@ShipFlow create a task "Write the migration guide" in this project` — the agent replies with a **Proposed changes** list instead of doing it. Nothing has happened yet.

1. Under the agent's comment you'll see each proposed change as the exact call that would run (for example `update_task_status(status=DONE, taskId=3)`), plus the agent's own one-line description.
2. Click **Apply** to make the change, or **Dismiss** to drop it. Only **you** (the person who asked) see these buttons; everyone else sees the same list read-only, with its status.
3. When you apply it, the change runs **as you**, with your permissions, and is recorded like any change you made by hand. Refresh the page to see the result.

What it can propose: create or update tasks (including status and assignee), create bugs and update their status and assignee, and update pitches and their status. It cannot touch anything else (roadmap, work logs, analyses, permissions…), and it can only touch items in the **same project** as the thread.

Safeguards:

- You need to be able to make the change yourself: a **READONLY** account, or a project **Viewer**, is never offered changes. This is checked again when you click Apply.
- A proposal is valid for **24 hours**, after which it shows **Expired**.
- Each proposal can be applied **once**. If it fails (for example an invalid value) it shows **Failed** with the reason and nothing is half-applied.
- The agent proposes at most three changes per reply, and only when you ask for one — not because of something written in a comment or document.
- Always read the call that is shown: it is generated from what will actually run, not from the agent's wording.

## Scheduled Reports

The agent can also report on its own, on a schedule — a daily standup digest, a cycle health check, and betting-table prep. These are set up as automation rules; see the **Agent Loops** guide.

## Running Privately

The agent uses whatever AI provider your administrator configured. With a local model (Ollama) and Air-Gapped Mode, nothing leaves your own servers. If no AI provider is configured, the agent replies once to say so rather than staying silent.

## Good to Know

- Mention it by `@ShipFlow`. Emails like `name@shipflow.dev` and longer names like `@ShipFlowBot` do not wake it.
- The agent never replies to its own comments, so threads can't loop.
- Comment text is treated as data, not as instructions: a comment saying "ignore your rules" doesn't change how it behaves.
- The agent appears in User Management as an inactive account named `shipflow-agent`. It cannot sign in and does not use up a Community Edition seat. Please leave it as is.

## For Administrators

- The agent is on by default. Set `app.agent.enabled=false` (env `APP_AGENT_ENABLED=false`) to turn it off; mentions are then treated as plain text.
- Agent changes can be switched off on their own with `app.agent.actions.enabled=false` (env `APP_AGENT_ACTIONS_ENABLED=false`); the agent then only answers. `app.agent.actions.ttl-hours` (default 24) sets how long a proposal stays confirmable. This does not depend on the MCP server's write toggle.
- It uses the same AI configuration as the other AI features (`app.ai.provider` and the provider's settings).
- **Existing knowledge:** project labels are added when content is (re)embedded. Content indexed before this release has no label and stays visible to everyone who can use Q&A until it is embedded again — that happens when its source changes, or if you clear the vector store and rebuild it. An admin "reindex" does not by itself relabel unchanged content.
- Replies are normal comments, so they are covered by the usual audit trail and can be deleted by managers and admins.
