---
title: "What Is a Betting Table? Shape Up's Alternative to Sprint Planning"
slug: what-is-a-betting-table
date: 2026-08-16
description: "Where Shape Up teams decide what gets built next. How it differs from sprint planning, who belongs in the room, and how to run one."
keywords: ["betting table", "shape up betting table", "shape up betting", "sprint planning alternative", "appetite", "shape up methodology"]
author: farzad
---

## The one-sentence version

A betting table is a short meeting, held once per cycle, where a small group decides which shaped pitches get funded for the next cycle — and, just as importantly, which don't.

The word "bet" is doing real work in that sentence. It isn't a synonym for "plan."

## Why "bet" and not "plan"

A bet has three properties that a plan doesn't:

**A known maximum loss.** When you bet on a pitch, you're risking exactly its appetite — six weeks, or two. That's the most you can lose. You cannot lose eight weeks on a six-week bet, because the cycle ends and the work stops.

**An uncertain payoff.** You might ship something great. You might discover the idea was wrong. Both are acceptable outcomes for a bet; only one is an acceptable outcome for a plan.

**No obligation to keep betting.** Losing a bet doesn't commit you to doubling down. If a six-week bet doesn't ship, the default is *not* to bet another six weeks on it. It goes back to the pool and has to win again on its merits.

That last property is where most teams' adoption falls apart, so it's worth stating plainly: **unfinished work does not automatically continue.** This is Shape Up's circuit breaker, and the betting table is where it's enforced.

## How it differs from sprint planning

| | Sprint planning | Betting table |
|---|---|---|
| Input | A prioritised backlog | A pool of shaped pitches |
| Unit | Tickets, estimated in points | Whole pitches, with an appetite |
| Carry-over | Unfinished work rolls into the next sprint | Unfinished work stops and must be re-bet |
| Who decides | Team, often with a product owner | A small group who can commit the team |
| Frequency | Every 1–2 weeks | Every cycle, typically 6 weeks |
| Rejected work | Stays in the backlog | Isn't tracked anywhere |

The last row is the one people find hardest. Shape Up deliberately has **no backlog**. Ideas that don't get bet on aren't filed for later — they're dropped. The reasoning is that a genuinely good idea will resurface, brought back by whoever cares about it, and an idea nobody re-raises for six months was never worth building. A backlog is mostly a graveyard that costs grooming time.

You don't have to accept that argument to run Shape Up. But you should notice that keeping a backlog quietly reintroduces the pressure the method was designed to remove.

## Who should be in the room

Small. Typically three to five people: whoever can genuinely commit the team's time, plus enough technical judgement to know whether a pitch's appetite is plausible.

At a startup that's often the founders and a senior engineer. At a larger company it's a product lead, an engineering lead, and a designer.

The people who *shaped* the pitches should be there to answer questions. The people who will *build* them generally don't need to be — they'll get a whole shaped pitch to own, not a set of tickets to accept.

## Running the meeting

The meeting is short — often under an hour — because the hard work happened during shaping. If your betting table runs three hours, the pitches weren't finished.

**Before.** Every pitch under consideration should be written up and readable in advance: problem, appetite, solution sketch, rabbit holes, no-gos. Nobody should be encountering a pitch for the first time in the room.

**During.** For each pitch, roughly:

1. Is the problem worth solving *now*?
2. Is the appetite right? Would we still want it at twice the cost? (If yes, the appetite may be too small. If no, that's the answer.)
3. Have the rabbit holes been addressed convincingly?
4. Who would build it?

Then bet, or don't. There's no "maybe" column — a maybe is a no that costs you a decision next cycle too.

**Capacity.** Bet up to the team's real capacity and no further. If you have two teams and six weeks, you have two big bets, or one big bet and two small ones. Resist stretch goals; they reintroduce the overcommitment the fixed cycle exists to prevent.

**After.** Each bet goes to a team, the cycle starts, and the group doesn't reconvene until next cycle. No mid-cycle reprioritisation — that's the promise that makes the bet worth taking.

## Appetite is the thing to get right

Appetite is the number that makes the whole meeting work, and it's the one most often misunderstood.

An **estimate** is a prediction: "this will take about six weeks." It's a claim about the work.

An **appetite** is a constraint: "this is worth six weeks to us." It's a claim about the value.

The difference shows up when reality disagrees. An estimate that turns out wrong gets revised, and the work continues. An appetite that turns out too small triggers a decision: cut scope to fit, or stop.

In practice, appetites come in two sizes: a small batch (about two weeks) and a big batch (about six). Keeping the vocabulary that small is deliberate — it stops the conversation drifting back into estimation.

The useful test at the table: *if this took twice the appetite, would we still want it?* If yes, you've under-set the appetite. If no, you've found the boundary, and now the team knows exactly what to cut against.

## Common failure modes

**Betting on unshaped work.** If a pitch has no rabbit-holes section, it hasn't been shaped, and betting on it means betting on an unknown. Send it back.

**The permanent bet.** A project that gets re-bet cycle after cycle without ever shipping isn't a bet, it's a plan wearing a costume. Three cycles is a strong signal that the problem needs re-shaping, not more funding.

**The stretch goal.** Committing 100% of capacity and then adding "if there's time." There won't be time, and the team knows it, so all it does is make the plan dishonest.

**Backlog by another name.** A "pitch parking lot" that only grows is a backlog. If it needs grooming, it's a backlog.

**The wrong room.** If the people in the meeting can't actually commit the team, the decisions get relitigated afterwards and the cycle's protection from interruption evaporates.

## If you only change one thing

Run one betting table and enforce one rule: nothing carries over automatically. Every piece of work in the next cycle has to be argued for from scratch, including the thing that didn't finish.

It will be uncomfortable once. After that, most teams find it's the meeting that finally makes the priorities honest.

## Further reading

- [The Shape Up Process, Step by Step](/blog/the-shape-up-process)
- [What is Shape Up? A developer's introduction](/blog/what-is-shape-up)
- [What Is a Hill Chart?](/blog/what-is-a-hill-chart)
- [Shape Up Software: what to look for](/blog/shape-up-software)
