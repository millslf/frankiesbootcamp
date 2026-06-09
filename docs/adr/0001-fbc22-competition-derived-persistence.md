# FBC-22 competition-derived persistence model

## Status
Accepted

## Context
The application currently relies on a large in-memory summary object for weekly history, leaderboard, honour roll, summary output, and some athlete-specific context. That makes reads dependent on rebuild timing and keeps current behavior tightly coupled to runtime-only state. The next major direction is to move to DB-backed reads while preserving existing screens and keeping room for multi-competition behavior.

## Decision
- Persist competition-scoped derived state around `competition_athlete` rather than hanging weekly stats directly off `athlete`.
- Use these core tables for the new slice:
  - `competition`
  - `competition_athlete`
  - `competition_activity_detail`
  - `competition_weekly_stats`
  - `competition_weekly_sport_stats`
  - `competition_summary`
  - `competition_summary_sport_stats`
  - `competition_honour_roll`
- `competition_activity_detail` is a thin persisted activity row matching the current `stravaActivityDetails` shape enough to support webhook add/update/delete. It is not a full Strava payload mirror.
- Reads should stay dumb. Rebuild and sync paths do the calculations and write fully computed rows.
- Rebuild scope is one `competition_athlete` at a time, using competition-specific period boundaries.
- Recalculation should delete and recreate derived rows for that `competition_athlete`.
- `competition_athlete` carries competition-scoped role information and the competition-specific `starting_goal`.
- Each competition must have at least one competition admin athlete, and only a competition admin can change another athlete's starting goal.

## Consequences
- Weekly history, leaderboard, honour roll, and summary screens can move to DB-backed reads without redoing calculations in the read path.
- The model supports an athlete appearing in multiple competitions with separate goals and derived state.
- Thin activity persistence preserves current webhook capabilities without committing to full raw Strava storage.
- Hibernate/JPA and migration tooling can be introduced around this new slice first while older JDBC/startup-DDL areas remain in place temporarily.
