# Issue tracker: Jira

Issues and PRDs for this repo live in Jira. Use the Atlassian Jira CLI workflow for all issue-tracker operations.

## Repo-specific current status

- Jira project key: `FBC`
- Current board focus from the handoff files:
  1. `FBC-47` auth foundation / multiple providers + email/password
  2. `FBC-40` automated tests protecting current scoring/summary behavior
  3. `FBC-86` All Sports Equal product philosophy
  4. `FBC-32` Hibernate + migration/persistence foundation
  5. `FBC-2` reproducible local development environment
  6. `FBC-3` containerized build + automated deployment pipeline
  7. `FBC-4` automated security scanning in CI/CD
  8. `FBC-36` credential/secret configuration cleanup
  9. `FBC-53` direct HTTPS + aligned upstream/proxy configuration
  10. `FBC-6` stricter browser script security / less inline script dependence
- Current implemented-and-ready-to-close ticket state:
  - `FBC-22` should be treated as effectively complete as a first persistence slice and ready to close in Jira.
  - `FBC-91` owns the deferred webhook optimization to rebuild from persisted activity rows instead of full Strava refetch.
  - `FBC-92` owns broader removal of remaining legacy in-memory summary code.
  - `FBC-93` owns analysis-only heart-rate-informed equivalent-distance metric work.
- Current backlog note from the handoff files:
  - `FBC-40` was later considered done by the user, even though older board-order notes still list it near the top.
  - For persistence direction, the handoff recommends keeping `FBC-22` and `FBC-32` tightly coupled, then moving to `FBC-30`, `FBC-16`, `FBC-54`, `FBC-37`, and `FBC-38`.
  - `FBC-56` is already done and should be treated as completed Strava-link gating/onboarding-state visibility work.
  - `FBC-12` was re-checked in Jira and now means competition-aware onboarding flow, not registration/auth foundation.
  - For onboarding direction, treat `FBC-12` as the explicit onboarding-state and routing layer before `FBC-16`, `FBC-30`, and `FBC-54`.

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
