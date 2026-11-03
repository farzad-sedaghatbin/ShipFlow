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
- Related knowledge is filtered with **your** teams and projects, the same way the Q&A widget does it.
- Everyone who can open the thread can read the reply, so don't ask it things you wouldn't say in the comment itself.

The agent is **read-only** here. It posts one comment and changes nothing else — it will not move tasks, edit pitches or create work. (Agent actions with a review step are planned for the next release.)

## Running Privately

The agent uses whatever AI provider your administrator configured. With a local model (Ollama) and Air-Gapped Mode, nothing leaves your own servers. If no AI provider is configured, the agent replies once to say so rather than staying silent.

## Good to Know

- Mention it by `@ShipFlow`. Emails like `name@shipflow.dev` and longer names like `@ShipFlowBot` do not wake it.
- The agent never replies to its own comments, so threads can't loop.
- Comment text is treated as data, not as instructions: a comment saying "ignore your rules" doesn't change how it behaves.
- The agent appears in User Management as an inactive account named `shipflow-agent`. It cannot sign in and does not use up a Community Edition seat. Please leave it as is.

## For Administrators

- The agent is on by default. Set `app.agent.enabled=false` (env `APP_AGENT_ENABLED=false`) to turn it off; mentions are then treated as plain text.
- It uses the same AI configuration as the other AI features (`app.ai.provider` and the provider's settings).
- Replies are normal comments, so they are covered by the usual audit trail and can be deleted by managers and admins.
