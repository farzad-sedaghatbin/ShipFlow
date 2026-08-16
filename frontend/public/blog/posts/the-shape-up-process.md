---
title: "The Shape Up Process, Step by Step"
slug: the-shape-up-process
date: 2026-08-16
description: "A walkthrough of the full Shape Up cycle — shaping, betting, building and cooldown — what happens in each phase, who does it, and the failure modes."
keywords: ["shape up process", "shape up cycle", "six week cycle", "shape up methode", "shaping", "cooldown", "shape up methodology"]
author: farzad
---

## The shape of the process

Shape Up runs on a repeating loop with four phases. Two of them happen before a single line of code is written:

1. **Shaping** — turning a raw idea into a bounded, de-risked proposal
2. **Betting** — deciding which proposals get built this cycle
3. **Building** — a fixed period, typically six weeks, of uninterrupted work
4. **Cooldown** — typically two weeks of no committed work

The critical structural point: shaping for the *next* cycle happens during the *current* cycle's build phase, by different people. Shapers shape while builders build. The two tracks run in parallel and meet at the betting table.

If you take one thing from this post, take that. Most teams that "tried Shape Up and it didn't work" were shaping and building with the same people at the same time, which produces neither.

## Phase 1: Shaping

**Who:** one or two senior people — usually someone with design sensibility and someone with technical judgement. Not the whole team.

**Duration:** ongoing, in parallel with the current build cycle.

**Output:** a pitch.

Shaping is the work of taking "customers keep asking for better search" and turning it into something a team can actually bet on. It happens at a deliberately rough level of detail — concrete enough that the team knows what to build, abstract enough that they still get to make real decisions.

A finished pitch contains:

- **Problem** — the specific situation, ideally one real story rather than an abstraction
- **Appetite** — how much time this is worth. Not how long it will take.
- **Solution** — the shape of the approach, at a level a team can argue with
- **Rabbit holes** — the specific things that could eat the cycle, and how to avoid them
- **No-gos** — what's explicitly excluded, so nobody has to ask

The rabbit holes section is what separates a pitch from a feature request. Shaping is largely the work of finding the parts that could blow up and either solving them in advance or ruling them out of scope.

**Failure mode:** shaping to too much detail. If the pitch specifies every screen, you've written a spec, and the team's judgement during the build has nowhere to go. Shape the boundaries, not the interior.

## Phase 2: Betting

**Who:** whoever can actually commit the team — typically founders, a product lead, and a senior engineer. A small group.

**Duration:** one meeting.

**Output:** a set of bets for the next cycle.

The betting table reviews the shaped pitches and decides which ones to fund. The vocabulary is deliberate: you're placing a bet, with a known maximum loss (the appetite) and an uncertain payoff.

Three things make this different from sprint planning:

- **There's no backlog to grind through.** Pitches that don't get bet on aren't deferred to a queue. They're just not bet on. If an idea is good, it'll come back; if it never comes back, it wasn't good.
- **Nothing carries over automatically.** Last cycle's unfinished work doesn't get first claim on this cycle. It has to be re-pitched and win on its merits.
- **Teams are assigned whole bets**, not tickets. A small team owns a pitch end to end for the cycle.

We go deeper on this in [What Is a Betting Table?](/blog/what-is-a-betting-table).

**Failure mode:** betting on more than the cycle can hold, then treating the overflow as "stretch goals." That's a sprint with extra steps.

## Phase 3: Building

**Who:** a small team — commonly one designer and one or two engineers.

**Duration:** the appetite. Six weeks for a big batch, two for a small one.

**Output:** shipped work.

The build phase has two properties that most process frameworks lack.

**Uninterrupted time.** No sprint ceremonies mid-cycle, no re-planning, no reprioritising. The bet was made; the team executes it. This is the payoff for doing the shaping work up front.

**Scope discovered, not decomposed.** The team doesn't receive a task breakdown. They find the natural seams of the problem as they work and organise into *scopes* — meaningful slices that can be finished and integrated independently. Scopes emerge in the first week or so and change as understanding improves.

Progress is reported with hill charts rather than percentages. Each scope sits somewhere on a curve: climbing while the unknowns are being worked out, cresting when the approach is settled, descending as the remaining work is just execution. See [What Is a Hill Chart?](/blog/what-is-a-hill-chart) for how to read them.

**The circuit breaker.** If the work isn't done when the cycle ends, it doesn't get an extension. The project stops. To continue, it must be re-shaped and re-bet next cycle.

This sounds harsh and is the most commonly abandoned rule in the method. Its purpose isn't punishment — it's to make the deadline real. A deadline that always slides doesn't constrain scope, so teams gold-plate. A deadline that actually stops the work forces the trade-offs to happen during the cycle, which is when they're useful.

**Failure mode:** a team that hasn't crested any scope by week three. That's the signal the pitch wasn't shaped enough, and it's the moment to intervene — not week six.

## Phase 4: Cooldown

**Who:** everyone.

**Duration:** typically two weeks.

**Output:** deliberately unspecified.

Cooldown is unscheduled time between cycles. Bugs from the last cycle get fixed. Technical debt gets paid down. People explore ideas nobody bet on. Shapers use it to finish pitches for the next betting table.

It is not a buffer for finishing late work. If cooldown routinely absorbs cycle overrun, the circuit breaker isn't functioning and appetites are being set wrong.

Cooldown is also where a lot of the next cycle's good ideas come from. Six weeks of heads-down building produces a long list of "we should really fix..." observations, and cooldown is when someone has room to act on them.

**Failure mode:** cancelling cooldown because there's too much to do. Cooldown is what makes six weeks of uninterrupted focus sustainable. Remove it and the build phase degrades into ordinary crunch within two or three cycles.

## Putting it on a calendar

An eight-week loop, running continuously:

| Week | Builders | Shapers |
|---|---|---|
| 1–6 | Build phase — the bet | Shaping pitches for next cycle |
| 6 | — | Betting table |
| 7–8 | Cooldown | Cooldown |

Then it repeats. Six weeks isn't sacred — some teams run four-week cycles, and two-week small batches exist inside the method already. What matters is that the length is *fixed* and known before the bet is placed. A variable cycle length turns appetite back into estimation.

## Adopting it without a rewrite

You don't need to adopt all four phases at once. In rough order of value-per-effort:

1. **Fix the cycle length and stop mid-cycle reprioritisation.** Costs nothing, changes everything.
2. **Add the circuit breaker.** Also free. Will be unpopular in cycle one and defended by the team by cycle three.
3. **Replace estimates with appetites** on new work.
4. **Add real shaping**, with named people and dedicated time.
5. **Add cooldown**, and protect it.

Hill charts and a formal betting table come last. They're the visible parts of Shape Up, which is why teams start there, but they're the least useful without the four steps above.

## Further reading

- [What is Shape Up? A developer's introduction](/blog/what-is-shape-up)
- [What Is a Betting Table?](/blog/what-is-a-betting-table)
- [Shape Up vs Scrum](/blog/shape-up-vs-scrum)
- [Shape Up Software: what to look for](/blog/shape-up-software)
- Basecamp's original book is free to read at [basecamp.com/shapeup](https://basecamp.com/shapeup)
