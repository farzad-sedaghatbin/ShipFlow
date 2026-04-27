# Shape Up Workflow

ShipFlow is the only faithful open-source implementation of the [Shape Up](https://basecamp.com/shapeup) methodology by Basecamp.

## The Cycle

```
Pitch → Betting → Cycle → Hill Chart → Circuit Breaker → Cooldown
```

## Pitch Board

Pitches move through these statuses:

| Status | Meaning |
|--------|---------|
| `IDEA` | Raw idea, not yet shaped |
| `DRAFT` | Being written up |
| `SHAPED` | Fully shaped, ready for betting |
| `PENDING` | Bet placed, awaiting cycle start |
| `STARTED` | Active in a cycle |
| `IN_PROGRESS` | Actively being built |
| `TESTING` | In QA |
| `DONE` | Shipped |

Navigate to **Pitches** in the sidebar to view and create pitches.

## Betting Table

The Betting Table is where the team decides what to build next cycle. Drag shaped pitches into team slots to allocate work.

Navigate to **Betting Table** in the sidebar.

## Hill Charts

Hill charts show the progress of scopes within a cycle. The left side of the hill = figuring out; the right side = execution.

- Dots represent **scopes** (defined on pitches)
- Drag dots to update their position
- Positions are saved immediately

Navigate to **Cycles → [cycle name] → Hill Chart**.

## Circuit Breaker

If a cycle runs into trouble, the Circuit Breaker lets you declare a problem and decide whether to continue or cut scope.

## Cooldown

A 1-2 week cooldown period after each cycle for bug fixes, exploration, and retrospectives.
