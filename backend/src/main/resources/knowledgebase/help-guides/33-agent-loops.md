# Agent Loops (Scheduled Agent Reports)

Agent loops let the ShipFlow Agent report to your team **on a schedule, without anyone asking**: a morning standup digest, a cycle health check, and a brief before the betting meeting. They are set up on the **Automations** page like any other rule.

Nothing here changes your data. A loop only reads the project and posts a message.

---

## The three loops

| Loop | What it posts | Works for |
|------|---------------|-----------|
| **Standup digest** | What finished in the last N hours, what is in progress, blocked and overdue, and bugs reported since. | Every project type |
| **Cycle health check** | How the active cycle or sprint is doing against the calendar: behind schedule, blocked and overdue tasks, and (Shape Up) pitches rated high risk. | Every project type |
| **Betting table prep** | The shaped pitches waiting for a bet with their appetite, the next cycle and how much capacity it has, and capacity warnings. | Shape Up only |

A loop that has nothing to say **stays quiet**: no "nothing happened" message on a calm weekend, and by default no message from the health check when the cycle is on track.

---

## Setting one up

1. Open **Automations** and click **Browse Templates**, then pick one from the **Agent** category — or click **New Rule** and choose an *Agent* action.
2. **Schedule:** choose *Every weekday*, *Every day*, *Every week* (and the day), or *Custom (cron)*. Set the time and the **timezone** — the time is read in that timezone. Runs can be at most once an hour.
3. **Send to:** tick **Slack**, **Teams** and/or **In-app notification**. Slack and Teams must be configured under Integrations first. You can name a channel to override the integration's default channel.
4. Optional: the look-back window (standup digest), "only post when something is at risk" (cycle health), and whether the agent may add a short summary.
5. Click **Preview** to see exactly what it would send right now, then save and switch it on.

Betting table prep may also be set to run **when a cycle ends** instead of on a schedule.

---

## Preview and Run now

Every agent loop has two buttons on its rule card:

- **Preview** builds the message from your project's current data and shows it. **Nothing is sent.** It also tells you if a real run would be skipped because there is nothing to report.
- **Run now** runs it immediately, as if its time had come, and records the result in the execution history. Use it to check your Slack or Teams setup.

You need to be a Manager or Admin, and a member of the project.

---

## The agent's summary — and why you can trust the numbers

Every message has two parts:

1. **The facts** — counts and names taken straight from your project's data by code. Never written by an AI.
2. **An optional short summary** — one or two sentences from the AI model at the top, saying what stands out.

The summary is **dropped automatically** (the facts are still sent) if the model is unavailable, writes too much, adds a link or @mention, or quotes **any number that isn't in the facts**. You can turn it off per rule with *Add a short summary written by the agent*. With a local model (Ollama) and Air-Gapped Mode, nothing leaves your servers.

Names and titles that people wrote are cleaned before posting: `@channel`/`@here` pings and Slack link syntax are neutralised, so a task title can't make the agent ping everyone.

---

## Reading the execution history

Each run is logged with what it did, in words:

- `Sent to Slack, Teams` — handed to those integrations. (Slack and Teams don't confirm delivery back to ShipFlow; if a message is missing, check the integration's notification history.)
- `Sent to Slack; skipped: Teams (not configured)` — a channel you asked for isn't set up.
- `Skipped: nothing to report` / `Skipped: no risks found` — the quiet-by-design case. Not an error.
- **FAILURE** `Nothing was sent; skipped: …` — none of the chosen channels received it. A run that reaches no one is never logged as a success.

---

## Good to know

- **Time and timezone.** "9:00" means 9:00 in the timezone on the rule, not the server's.
- **Missed runs aren't replayed.** If ShipFlow was down at 9:00, the digest is skipped rather than posted hours late (a run up to two hours late still goes out).
- **Several servers, one message.** In a multi-server deployment each run is claimed by exactly one server, so a digest is never posted twice.
- **Licence.** Agent loops count as automations: the Community Edition limit of 5 enabled automations applies.
- **Who sees it.** Slack/Teams messages are visible to everyone in that channel — choose a channel whose members may see this project's work. In-app notifications go to the project's members.

---

## Common questions

**Why did my digest not appear this morning?** Check the rule is enabled, look at the next run time shown on its card, and read the execution history: it will say if it was skipped (nothing to report), failed (no channel reached) or never ran.

**Can the agent change things in a loop?** No. Loops only read and post. To have the agent make changes, mention `@ShipFlow` in a comment — it proposes, and you confirm (see the ShipFlow Agent guide).

**Can I get the digest in a language other than English?** The facts are labelled in English today. The agent's summary follows the model's language.
