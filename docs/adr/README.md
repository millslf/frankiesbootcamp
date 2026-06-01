# ADRs

Use this folder for short architecture decision records.

## When to add one

Add an ADR when a ticket changes system structure, persistence direction, authorization boundaries, or long-term integration strategy.

Good examples in this repo:

- replacing the in-memory summary with DB-backed data
- introducing explicit competition membership
- defining competition-scoped authorization boundaries
- choosing webhook plus reconciliation sync behavior

## Format

Create files named like:

- `0001-short-title.md`
- `0002-another-decision.md`

Keep them short and practical.

Recommended structure:

```md
# Title

## Status
Proposed | Accepted | Superseded

## Context
What problem are we solving?

## Decision
What are we doing?

## Consequences
What does this enable, constrain, or defer?
```

Link terms back to `CONTEXT.md` where useful so naming stays consistent.
