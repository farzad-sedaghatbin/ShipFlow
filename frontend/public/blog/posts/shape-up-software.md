---
title: "Shape Up Software: What to Look For (and What Most Tools Get Wrong)"
slug: shape-up-software
date: 2026-08-16
description: "Most tools can't run Shape Up without workarounds. What a tool actually needs for appetites, betting and hill charts — and how the real options compare."
keywords: ["shape up software", "shape up tool", "shape up project management", "shape up app", "betting table software", "hill chart tool"]
author: farzad
---

## The problem with running Shape Up in a generic tool

Shape Up asks for a handful of things that most project management tools simply do not model:

- Work is budgeted with an **appetite**, not estimated with points.
- Bets are made at a **betting table**, and unfinished bets are **not** automatically rolled forward.
- Progress is reported on a **hill chart**, not a percentage bar.
- Scope is discovered during the cycle, not decomposed up front.

You can approximate all of this in Jira or Linear. Teams do it every day. But you end up encoding the method in naming conventions and team discipline rather than in the tool — a custom field called "Appetite" that nothing enforces, an epic named "Cycle 7" that has no phases, a label called "shaped" that anyone can apply.

That works right up until the person who set the conventions goes on holiday.

This post is about what actually needs to exist in software for Shape Up to hold up on its own, and an honest look at the options.

## Six things a Shape Up tool needs

### 1. Appetite as a budget, not an estimate

This is the one most tools get backwards. An estimate says "we think this will take six weeks." An appetite says "this is worth six weeks to us, and if it doesn't fit, we reconsider it."

The difference is what happens when the work overruns. An estimate that was wrong gets revised. An appetite that was exceeded triggers a decision. If your tool stores appetite in a free-text field, nothing will ever trigger.

What to look for: appetite as a first-class property with a small fixed vocabulary (Basecamp's original two are the small batch at two weeks and the big batch at six), and something that actually notices when a cycle's committed work exceeds it.

### 2. A real betting table

The betting table is a meeting with a specific output: a set of pitches assigned to the next cycle, and a much larger set explicitly not assigned. Most tools model the first half and lose the second.

The pitches you *didn't* bet on matter. They aren't a backlog — Shape Up deliberately has no backlog that accumulates forever. They're just ideas that didn't win this time, and most of them should quietly die.

What to look for: a distinct betting step that takes a pool of shaped pitches and produces a cycle, where not-betting is a normal outcome rather than a deferral.

### 3. Cycles with phases

A cycle isn't a date range. It moves through shaping, betting, build, and cooldown, and different activities are appropriate in each. A tool that models a cycle as "sprint with a longer duration" has thrown away most of the structure.

Cooldown especially tends to get dropped, and it's the part teams miss most when it's gone. It's where bugs get fixed, debt gets paid, and people get to explore things nobody bet on.

### 4. Hill charts, not progress bars

A percentage bar answers "how much is done." A hill chart answers a more useful question: "have we figured out how to do it yet?"

Work climbs the uphill side while the unknowns are still being resolved, crests when the approach is clear, and rolls down the far side as the known work gets executed. Something stuck at 80% for two weeks looks fine on a bar chart. Something stuck on the uphill for two weeks is a visible, specific problem.

We wrote about this in more depth in [What Is a Hill Chart?](/blog/what-is-a-hill-chart).

What to look for: hill charts you can actually move, attached to scopes rather than tasks, with history — so you can see that a scope has been stuck, not just where it is now.

### 5. Scopes that emerge

In Shape Up, you don't decompose a pitch into tasks before the cycle starts. Scopes are discovered as the team gets into the work. A tool that demands a complete task breakdown before the cycle can begin is fighting the method.

What to look for: the ability to add scopes mid-cycle without ceremony, and without it reading as "scope creep" in every report.

### 6. A circuit breaker

If a bet doesn't ship by the end of its cycle, it doesn't get an extension by default. The work stops and has to be re-pitched and re-bet to continue. This is the single most important rule in Shape Up and the first one teams abandon, because no tool enforces it.

What to look for: unfinished work that does *not* silently roll into the next cycle.

## The honest comparison

**Basecamp** invented the method and its tooling reflects it — hill charts are native, and the overall philosophy fits. It's less strong if you need detailed task-level tracking, QA workflows, or reporting for stakeholders who expect them.

**Jira** models nothing here natively, but models everything eventually if you're willing to build it. Custom fields, custom issue types, custom workflows, and a fair amount of admin time. Large orgs already on Jira often take this path because the migration cost of leaving is higher than the cost of bending it.

**Linear** is excellent software with an opinionated cycle model that is closer to continuous flow than to Shape Up. Appetite and betting have no home. Its cycles auto-roll unfinished work forward, which is precisely the opposite of a circuit breaker.

**Notion / Airtable / spreadsheets** will model anything you can describe, and enforce none of it. Genuinely fine for a small team with strong discipline, and a common honest answer.

**ShipFlow** — our project, so read this with appropriate suspicion — was built specifically around these six requirements: appetite as a budget, a betting table, phased cycles with cooldown, interactive hill charts, scopes added mid-cycle, and a circuit breaker. It's open source under MIT and self-hostable, and it also supports Kanban and Scrum projects in the same workspace, because most teams have some work that isn't shaped. You can read the [full feature comparison](/compare) or look at the [source on GitHub](https://github.com/farzad-sedaghatbin/shipflow).

## How to choose

If your team is small and disciplined, a spreadsheet plus the [free Shape Up book](https://basecamp.com/shapeup) genuinely works. Don't buy a tool to solve a problem you don't have yet.

Adopt dedicated tooling when a specific failure keeps recurring: bets quietly rolling into the next cycle, appetites drifting into estimates, or nobody being able to answer "is this scope stuck?" without a meeting. Those are tool problems. Pick whichever tool makes the failure impossible rather than merely discouraged.

And whichever you choose, adopt the circuit breaker first. It's free, it needs no software, and it's the rule that makes the rest of the method mean anything.

## Further reading

- [What is Shape Up? A developer's introduction](/blog/what-is-shape-up)
- [The Shape Up Process, Step by Step](/blog/the-shape-up-process)
- [What Is a Betting Table?](/blog/what-is-a-betting-table)
- [Shape Up vs Scrum](/blog/shape-up-vs-scrum)
- [Running Shape Up, Scrum, and Kanban in One Workspace](/blog/shape-up-scrum-kanban-together)
