# Context

This file is the shared language for Frankie's Bootcamp. Use it to keep ticket shaping, architecture discussions, and implementation work aligned.

## How we work

- `SESSION_HANDOFF.md` is the primary session continuity document.
- `BACKLOG_GROOMING_HANDOFF.md` is the primary backlog and sequencing document.
- Use this file for project language and domain terms so tickets and code use the same wording.
- Use `docs\adr\` for short architecture decisions when a ticket changes system structure or long-term direction.
- Work one ticket at a time unless explicitly asked for a broader plan.

## Core domain terms

### Athlete

The current end user participating in Frankie’s Bootcamp. In older parts of the app, "athlete" may also effectively mean the logged-in user.

### User

The authenticated identity. The long-term direction should allow a user identity to exist separately from athlete profile details and competition memberships.

### Competition

A scoped contest or season with its own members, timing, settings, and future rules. The long-term direction is multi-competition, not one global current competition.

### Competition membership

The explicit relationship between an athlete/user and a competition. Membership should be intentional and should replace assumptions that a newly joined user automatically belongs to the current competition.

In the current preferred persistence shape for `FBC-22`, this relationship is likely to be named `competition_athlete` in storage because it is clearer in this codebase than the more abstract term membership.

Membership lifecycle rules such as leaving a competition, removing yourself from a competition, rejoining, preserving historical stats, and protecting the last competition admin are not decided by `FBC-30`; they belong with the invitation/join lifecycle work in `FBC-89` and the authorization rules in `FBC-54`.

### Competition roles

Roles that apply inside a competition, such as member, admin, or owner. The model should leave room for these roles even when a ticket is not implementing the full authorization rules.

Current clarified rule: each competition must have at least one competition admin athlete, and only a competition admin can update another athlete's starting goal inside that competition.

### Global roles

Roles that apply across the whole application rather than within a single competition. These should stay separate from competition-scoped roles.

## Product language

### All Sports Equal

The default product philosophy: all sports are welcome, all sports are valid effort, and they should be measured fairly by default. This does not prevent competition-specific sport restrictions or competition-specific ratio rules.

### Eligible sports

The list of sports a competition allows. The default should stay broad, but specific competitions may narrow it later.

### Distance ratios / normalization

The rules that make different sports comparable in scoring or progress calculations. These may become competition-specific later.

## Data and architecture terms

### In-memory summary

The current large runtime object that holds activity-derived summary data, leaderboard-related values, and weekly/performance outputs. This is a temporary architecture that should be removed in favor of database-backed reads and writes.

### Normalized activities

Persisted activity records stored in a relational shape designed for the app’s behavior, not as a mirror of the full Strava payload.

### Derived stats

Stored or recomputed summary values based on normalized activities. These support leaderboard, weekly progress, and dashboard views without depending on the old in-memory summary object.

### DB-backed summary

The target direction where current summary outputs come from persisted activities plus derived stats rather than from the large in-memory object.

For the current `FBC-22` direction, the preferred read model is competition-scoped and built around `competition_athlete`, weekly derived stats, summary rows, summary sport totals, honour-roll rows, and a thin `competition_activity_detail` table that preserves current webhook add/update/delete behavior without mirroring the full Strava payload.

### Startup DDL

The current schema-management approach where database tables are created or changed at app startup. This is a temporary approach and not a full migration strategy.

## Integration terms

### Strava link

The authenticated action that connects a user to Strava. It should not act as the main enrolment shortcut.

### Webhook-driven updates

The preferred long-term way to apply incremental changes when upstream activity data changes.

### Reconciliation / backfill

Scheduled or manual sync work that repairs drift or imports missed changes after the main sync path has already run.

## Current strategic direction

1. Protect behavior with tests before major refactors.
2. Move away from the in-memory summary toward DB-backed activity and stats models.
3. Remove the single-competition assumption and introduce explicit competition membership.
4. Add competition-scoped authorization after the membership model is in place.
5. Layer in competition-specific sport/rule behavior after the competition boundary is sound.

## Writing guidance

- Prefer these terms in ticket prompts, ADRs, and code discussions.
- If a ticket changes the meaning of a term, update this file as part of that work.
- If a ticket introduces a major structural decision, add a short ADR under `docs\adr\`.
