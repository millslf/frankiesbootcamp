# Issue tracker: Jira

Issues and PRDs for this repo live in Jira. Use the Atlassian Jira CLI workflow for all issue-tracker operations.

## Repo-specific current status

- Jira project key: `FBC`
- Current practical board focus from the latest local session and live board:
  1. `FBC-12` competition-aware onboarding flow
  2. `FBC-16` competition creation/setup screens and per-competition start configuration
  3. `FBC-89` competition invitations and join flow
  4. `FBC-30` allow an athlete to belong to multiple competitions
  5. `FBC-54` implement global and competition-scoped authorization model
  6. `FBC-37` allow competition-specific eligible sports while preserving "All Sports Equal" as the default model
  7. `FBC-38` specify relative distances per competition
- Current implemented-and-ready-to-close ticket state:
  - `FBC-22` should be treated as effectively complete as a first persistence slice and ready to close in Jira.
  - `FBC-91` owns the deferred webhook optimization to rebuild from persisted activity rows instead of full Strava refetch.
    - Current local prompt direction:
      - replace webhook-triggered whole-athlete Strava refetch with competition-aware incremental mutation logic
      - persist per-athlete watermarks for latest fully imported activity start time and last full reconciliation time
      - use incremental startup/restart fetches for new activities, but keep webhook update/delete as the source of truth for older edited activities
      - add periodic athlete reconciliation roughly every 3 to 4 days across all linked competitions
  - `FBC-92` owns broader removal of remaining legacy in-memory summary code.
  - `FBC-93` owns analysis-only heart-rate-informed equivalent-distance metric work.
- Current backlog note from the handoff files:
  - `FBC-40` was later considered done by the user, even though older board-order notes still list it near the top.
  - For persistence direction, the handoff recommends keeping `FBC-22` and `FBC-32` tightly coupled, but the live board has since moved active implementation focus into multicomp.
  - `FBC-56` is already done and should be treated as completed Strava-link gating/onboarding-state visibility work.
  - `FBC-12` was re-checked in Jira and now means competition-aware onboarding flow, not registration/auth foundation.
  - For onboarding direction, treat `FBC-12` as the explicit onboarding-state and routing layer before `FBC-16`, `FBC-30`, and `FBC-54`.
  - Invitation-only competition visibility should be treated as `FBC-89` scope: the logged-in athlete should only see competitions they are invited to join unless a later ticket intentionally broadens discovery behavior.
  - Latest local status: `FBC-12` was reviewed as effectively complete and split to branch `feature/fbc-12-onboarding-flow` at commit `217fbbb`; a separate bugfix branch `bugfix/session-persistence` at commit `546c063` was also pushed.
  - Latest local status: `FBC-16` is now effectively complete on local branch `feature/fbc-16-competition-setup`, including lifecycle-aware onboarding and historical competition outcome access.
  - Latest local status: `FBC-30` is functionally complete and browser-tested on branch `feature/fbc-30-competition-selection`; PR #16 is open and the user is moving the ticket to code review.
    - local implementation includes:
      - multi-active competition detection
      - a required chooser state when multiple active competitions exist
      - dashboard-shell competition switching links
      - default to the current active competition when there is exactly one active choice
      - explicitly handle the UX when there are multiple simultaneously active competitions
      - expose competition switching from the normal dashboard shell
      - keep explicit selected competition context separate from implicit current-active defaults
      - list past competitions in the dashboard shell and chooser so athletes can switch into historical outcome context
      - competition-scoped sick weeks are now stored under `competition_athlete_sick_week`; completed competitions cannot be edited through the history UI
      - incomplete historical competitions trigger bounded background rebuilds, then freeze once persisted state is complete and the comp is older than 14 days
      - self-removal/leaving a competition is intentionally not part of `FBC-30`; track that membership lifecycle under `FBC-89`, with authorization constraints in `FBC-54`

## Conventions

- **Create an issue**: use the Jira CLI to create a ticket in the configured project with the agreed summary, description, issue type, and any required fields.
- **Read an issue**: use the Jira CLI to view the ticket details, comments, status, labels, assignee, and linked work.
- **List issues**: use the Jira CLI to search by project, status, labels, assignee, or JQL as needed.
- **Comment on an issue**: use the Jira CLI to add a comment to the ticket.
- **Apply / remove labels**: use the Jira CLI to update the ticket's labels field.
- **Transition / close**: use the Jira CLI to move the ticket through the appropriate Jira workflow states rather than assuming a GitHub-style close action.

Use the Jira project and authentication already configured in the local environment. If a skill needs a ticket key, prefer the existing Jira key format used by this repo.

Before creating, reprioritizing, or closing Jira work, read `SESSION_HANDOFF.md` and `BACKLOG_GROOMING_HANDOFF.md` first. Treat them as the source of truth for current sequencing, implemented slices, and closeout notes unless the user gives newer instructions.

## When a skill says "publish to the issue tracker"

Create or update a Jira issue in project `FBC`, using the current board order and closeout notes above.

## When a skill says "fetch the relevant ticket"

Read the Jira ticket, including comments and current workflow status, then cross-check it against `SESSION_HANDOFF.md` and `BACKLOG_GROOMING_HANDOFF.md` for the latest local status.
