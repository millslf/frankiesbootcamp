# Copilot instructions for Frankie's Bootcamp

## Session context files

- Start new repo sessions by running `copilot-byok.ps1` from the repo root when local Copilot BYOK, Jira, Confluence, or Maven setup is needed.
- Read `SESSION_HANDOFF.md` first for current implementation status, recent decisions, and active constraints.
- Use `BACKLOG_GROOMING_HANDOFF.md` for ticket order, prompt shaping, and near-term sequencing.
- Use `CONTEXT.md` for shared product and architecture vocabulary.
- Read `AGENTS.md` and `docs\agents\*.md` when work touches issue tracking, triage flow, or domain-doc consumption rules.
- When a change affects long-term structure or project direction, add or update a short ADR under `docs\adr\` using the format in `docs\adr\README.md`.

## Default working style

- Keep answers about 50% shorter than default. Be direct and practical.
- Prefer clear recommendations over long option lists.
- Work one ticket at a time unless explicitly asked for a broader plan.
- For each ticket, give the user:
  1. a refined goal
  2. a pasteable ticket prompt
  3. suggested ordering/dependencies
  4. key risks and scope boundaries
- Flag tickets that should be renamed, split, or moved in order.
- If a ticket changed in Jira or the handoff files since the last local prompt, re-check the current ticket meaning before implementing.

## Agent and tracker docs

- `AGENTS.md` is the short entrypoint for repo-specific skill configuration.
- `docs\agents\issue-tracker.md` defines the Jira-based tracker workflow and current local tracker notes.
- `docs\agents\triage-labels.md` defines the label vocabulary used by triage-style skills.
- `docs\agents\domain.md` tells skills how to consume `CONTEXT.md` and ADRs.
- When implementation or backlog discussions change tracker reality, update the relevant tracker docs along with `SESSION_HANDOFF.md` and `BACKLOG_GROOMING_HANDOFF.md`.

## Build, test, and lint commands

- This is a Maven WAR project targeting Java 23 and a Jakarta EE / WildFly-style runtime.
- Run commands from the repo root with Maven directly:
  - `mvn test`
  - `mvn -Dtest=ClassName test`
  - `mvn -Dtest=ClassName#methodName test`
  - `mvn package`
- Current tests are plain JUnit 5 tests under `src\test\java\com\frankies\bootcamp\...`.
- Useful current single-test examples:
  - `mvn -Dtest=AuthSessionServiceTest test`
  - `mvn -Dtest=ActivityProcessServiceTest test`
  - `mvn -Dtest=PersistentActivityProcessServiceTest test`
  - `mvn -Dtest=PerformanceFixtureLoadingTest test`
- `mvnw.cmd` exists, but do not rely on the Maven wrapper: `.mvn\wrapper\maven-wrapper.properties` is missing.
- There is no dedicated lint command in `pom.xml`.

## High-level architecture

- The app is primarily a server-rendered Jakarta web application packaged as a WAR. The main user-facing flow is JSP-based under `src\main\webapp`, with servlet endpoints under `src\main\java\com\frankies\bootcamp\servlet`.
- The authenticated app entrypoint is `/app` via `AppServlet`, which forwards to `src\main\webapp\app\index.jsp`. That dashboard is intentionally server-rendered and eagerly includes the History, Honour Roll, Leaderboard, and Summary tab content through backing servlets.
- There is also a small JAX-RS layer under `src\main\java\com\frankies\bootcamp\rest` mounted at `/api`. It handles Strava auth callback flow, athlete summary JSON endpoints, and the Strava webhook.
- Authentication is session-based. `AuthenticationFilter` protects most routes, allows a small public path list, and can bootstrap a session from ngrok-provided auth headers. `AuthSessionService` is the canonical place for reading and writing the authenticated user plus cached athlete session attributes.
- `BootcampServlet` is the base servlet for authenticated app pages. It resolves the signed-in `AuthenticatedUser`, loads the linked `BootcampAthlete`, and forwards users without a completed Strava link into the server-controlled Strava onboarding JSP. App servlets that need the current athlete usually inherit from this class rather than reimplementing auth checks.
- Activity and leaderboard behavior currently flow through `ActivityProcessFacade`, which switches between the legacy in-memory `ActivityProcessService` and the newer `PersistentActivityProcessService` based on `BOOTCAMP_ACTIVITY_MODE`. Changes to summary, leaderboard, honour-roll, ZenBot stats context, scheduled rebuilds, or webhook behavior need to account for both modes unless the task explicitly removes one.
- The legacy path still builds a large in-memory summary model in `ActivityProcessService`. It fetches athletes from `DBService`, pulls Strava activities through `StravaService`, converts them into `BaseSport` implementations through `SportFactory`, and assembles `PerformanceResponse` / `WeeklyPerformance` outputs.
- The persistent path uses `PersistentActivityProcessService` plus `DBService` to rebuild one athlete at a time, store thin `competition_activity_detail` rows plus derived weekly and summary tables, and then serve leaderboard and honour-roll reads from DB-backed read models.
- `DBService` is both the JDBC access layer and the current schema-management mechanism. It looks up the JBoss datasource `java:jboss/strava`, performs startup DDL for auth, audit, ZenBot, and competition-derived persistence tables, and owns most database read/write operations. There is no separate migration framework yet.
- `ActivityProcessJob` triggers summary rebuilding on startup and daily, delegating through `ActivityProcessFacade` so scheduled processing follows the active mode.
- Strava integration is centered in `StravaService`: callback URL construction, OAuth token exchange, token refresh, activity fetches, and single-activity retrieval for webhook handling. `AuthResource` keeps Strava link initiation and callback handling server-controlled.
- `StravaWebhook` acknowledges webhook requests quickly and processes create/update/delete events asynchronously, routing activity mutations through `ActivityProcessFacade`.
- ZenBot is a server-side feature for logged-in athletes. `ZenBotServlet` combines `AiMessageService`, athlete-specific stats context from the activity processing layer, and persisted conversation history in `zenbot_messages`.

## Key conventions

- Use the repo vocabulary from `CONTEXT.md`: athlete, user, competition, competition membership, global roles, competition roles, All Sports Equal, normalized activities, derived stats, and DB-backed summary.
- Preserve the current strategic direction captured in the handoff files: replace the large in-memory summary with DB-backed activity/stat handling, then move toward explicit competition membership and competition-scoped authorization.
- For the current persistence design, prefer the clearer storage term `competition_athlete` for the athlete-in-competition relation.
- The current preferred `FBC-22` model is competition-scoped and uses `competitions`, `competition_athlete`, `competition_activity_detail`, `competition_weekly_stats`, `competition_weekly_sport_stats`, `competition_summary`, `competition_summary_sport_stats`, and `competition_honour_roll`.
- `competition_activity_detail` is intentionally thin and should keep only the fields needed for the current `stravaActivityDetails` behavior so webhook add, update, and delete remain possible without mirroring the full Strava payload.
- Rebuilds in the persistent path should be scoped to one `competition_athlete` at a time and should delete and recreate derived rows for that scope.
- Keep dashboard tab auditing centralized in `TabAuditServlet`. Initial `/app/` render should record one `page-landing` event, and tab audit writes for `history`, `leaderboard`, `honour-roll`, and `summary` should only happen on real user clicks.
- Treat the current dashboard loading approach as intentional. `src\main\webapp\app\index.jsp` eagerly includes tab content, and previous lazy-loading attempts caused regressions.
- Preserve server-side gating for Strava onboarding and ZenBot. Users without a linked Strava athlete should be routed through the onboarding flow by `BootcampServlet`, not by ad hoc page logic.
- Follow the existing package split: `servlet` for JSP-backed HTTP endpoints, `rest` for JAX-RS APIs, `service` for orchestration and integration code, `model` for app and API payload models, `sport` for sport-specific scoring behavior, `filter` for request filters, and `job` for scheduled refresh work.
- Reuse `AuthSessionService` for session attribute names and semantics instead of setting custom auth or session keys ad hoc.
- Be careful with tests that load the sanitized performance fixtures. `PerformanceFixtureLoadingTest` asserts against raw Gson `JsonObject` / `JsonArray` rather than deserializing into `PerformanceResponse` because `stravaActivityDetails.sport` is typed as abstract `BaseSport`.
- `PersistentActivityProcessServiceTest` uses small protected test seams in the persistent service and fake DB/Strava collaborators instead of requiring a JNDI datasource; follow that pattern for persistent-service unit coverage.
- Be aware of the known bug candidate in `ActivityProcessService.ensureWeeksUpTo(...)` around `get(w - 2)` when later weeks are created.
- Do not commit or copy secrets from repository files. There is a known unsafe credential file at `src\main\resources\credentials.json`.

## Repo workflow files

- `copilot-byok.ps1` bootstraps local Copilot BYOK and Atlassian-related environment variables.
- `SESSION_HANDOFF.md`, `BACKLOG_GROOMING_HANDOFF.md`, and `CONTEXT.md` are part of the intended working pattern for this repo and should be kept in sync with major changes.
- `AGENTS.md` and `docs\agents\*.md` hold the Matt Pocock skill configuration for this repo and should stay aligned with the actual Jira workflow, triage labels, and domain-doc layout.

## Startup shortcut

For a fresh session in this repo, the expected order is:

1. Run `copilot-byok.ps1` if local env/bootstrap is needed.
2. Read `SESSION_HANDOFF.md`.
3. Read `BACKLOG_GROOMING_HANDOFF.md`.
4. Read `CONTEXT.md`.
5. Read `.github\copilot-instructions.md`.
6. Read `AGENTS.md` plus relevant `docs\agents\*.md` files when tracker/skill behavior matters.
