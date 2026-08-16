---
title: "Running Shape Up, Scrum, and Kanban in One Workspace"
slug: shape-up-scrum-kanban-together
date: 2026-08-16
description: "Most teams don't run just one. How Shape Up, Scrum and Kanban divide up real work — and what breaks when you force it all into a single process."
keywords: ["shape up and scrum", "shape up kanban", "mixed methodology teams", "shape up scrum kanban", "multiple methodologies one tool", "methodology agnostic project management"]
author: farzad
---

## The premise most tools get wrong

Methodology debates are usually framed as a choice: you're a Scrum shop, or
you're a Kanban shop, or you've read Shape Up and you're doing that now.

In practice almost nobody is any one of those. A single engineering
organisation typically has, at the same time:

- **Product bets** — meaningful new capability, uncertain scope, worth a fixed
  budget of time. Shape Up work.
- **Committed delivery** — a roadmap with dates attached, often because someone
  external is depending on it. Scrum work.
- **Continuous flow** — support, bugs, infrastructure, small requests that
  arrive unpredictably. Kanban work.

Forcing all three into one process is where most process pain comes from. Put
support tickets in a six-week cycle and the cycle gets destroyed by
interruptions. Put a genuine product bet into two-week sprints and it gets
chopped into fragments that individually ship nothing. Put date-committed
delivery on a Kanban board and nobody can answer when it lands.

The methodologies aren't competing for the same work. They're each good at a
different *kind* of work.

## What each one is actually for

### Shape Up: high-uncertainty, high-value work

The defining feature is the **appetite** — a fixed time budget set before the
work starts, with scope flexing to fit rather than time flexing to fit scope.
That trade only makes sense when scope is genuinely negotiable, which is true
of most new product work and untrue of most commitments.

The second defining feature is **uninterrupted time**. A six-week cycle only
works if it's actually protected. Which is exactly why support work can't live
in it.

Use it for: new features, redesigns, anything where "what should this be" is
still partly open.

Read more: [The Shape Up Process, Step by Step](/blog/the-shape-up-process).

### Scrum: predictable delivery against commitments

Scrum's two-week cadence and estimation machinery exist to answer a question
Shape Up deliberately refuses to answer: *when will this be done?*

If you have contractual dates, a launch tied to a conference, or a dependency
another team is waiting on, that question matters more than the flexibility
Shape Up buys. Velocity and burndown are how you answer it.

The cost is ceremony — planning, refinement, review, retro — and a strong pull
toward decomposing work before it's understood. Worth paying when the date is
real; wasteful when it isn't.

Use it for: committed roadmap work, integrations with external deadlines,
platform migrations with dependent teams.

### Kanban: continuous, unplannable flow

Kanban doesn't plan. It limits work in progress and measures how long things
take to get through. There's no iteration boundary because the work doesn't
arrive on a schedule.

For a support queue, a bug backlog, or an infrastructure rotation, this is
simply correct. Any attempt to batch that work into sprints or cycles produces
either idle time or constant re-planning.

Use it for: support, bugs, ops, small requests, anything demand-driven.

## How teams actually mix them

Three patterns work in practice.

**By team.** The product team runs Shape Up cycles, the platform team runs
Scrum against roadmap commitments, and support runs a Kanban board. Each team
has one process. This is the cleanest split and the most common.

**By work type within a team.** One team, but bets go through cycles while
interrupt work goes on a Kanban board with a WIP limit. Requires discipline
about what qualifies as which, but keeps a small team from needing two teams.

**By phase.** Shape Up for the uncertain first version, then Scrum once the
thing exists and has committed follow-up work. Common for a product moving from
discovery to delivery.

What doesn't work: switching everyone's methodology every quarter, or running
Shape Up cycles that are quietly interrupted by whatever's urgent. A cycle
that isn't protected isn't a cycle — it's a sprint with worse tooling.

## The tooling problem

Here's the practical difficulty. Most project management tools have exactly one
model of time.

Jira and Linear are sprint-shaped. You can approximate Shape Up in them with
custom fields and conventions, but appetite isn't enforced, and — the killer —
unfinished work rolls forward automatically, which is the precise opposite of
Shape Up's circuit breaker. Basecamp is Shape-Up-shaped and doesn't try to do
Scrum. Kanban tools don't model cycles or sprints at all.

So mixed-methodology organisations end up with two or three tools, and lose the
one thing that actually matters at that scale: a single place to see what
everyone is working on.

## How ShipFlow handles it

This is our project, so weigh accordingly — but it's the specific problem
ShipFlow was built for.

A ShipFlow workspace holds projects of three types, and the **project type
determines which model applies**:

- A **Shape Up** project gets cycles with shaping, betting, build and cooldown
  phases, a betting table, appetite as a budget, hill charts, and the circuit
  breaker. Unfinished bets do not roll forward.
- A **Scrum** project gets sprints, velocity, burndown, and a product backlog,
  with sprint planning that works the way Scrum teams expect.
- A **Kanban** project gets a board with WIP limits and no iteration boundary
  at all.

They coexist. The same people, tasks, releases, reporting, wiki, and
permissions span all of them, so a support ticket on a Kanban board and a bet
in a Shape Up cycle are visible in the same backlog and the same reports.
Nobody has to standardise a methodology to get one view of the work.

It's open source under MIT and self-hostable, so you can also just look at how
it's modelled rather than take our word for it. There's a
[feature comparison against Linear, Jira, Asana and Basecamp](/compare) if
you're weighing options.

## If you take one thing away

Don't pick a methodology for your organisation. Pick one per *kind of work*,
be honest about which kind you're looking at, and then make sure your tooling
doesn't force you to lie about it.

The most common failure isn't choosing wrong. It's choosing once, for
everything, and then quietly breaking the rules for the work that never fitted.

## Further reading

- [Shape Up vs Scrum: which is right for your team?](/blog/shape-up-vs-scrum)
- [The Shape Up Process, Step by Step](/blog/the-shape-up-process)
- [What Is a Betting Table?](/blog/what-is-a-betting-table)
- [Shape Up Software: what to look for in a tool](/blog/shape-up-software)
