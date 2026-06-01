# Copilot instructions for Frankie's Bootcamp

## Session context files

- Read `SESSION_HANDOFF.md` first for current implementation context and recent decisions.
- Use `BACKLOG_GROOMING_HANDOFF.md` for ticket order, shaping context, and near-term sequencing.
- Use `CONTEXT.md` for shared product and architecture vocabulary.
- When a change affects long-term structure or project direction, add or update a short ADR under `docs\adr\` using the format in `docs\adr\README.md`.

## Build, test, and lint commands

- This is a Maven WAR project targeting Java 23 and deploying to a Jakarta EE / WildFly-style runtime.
- Use Maven directly from the repo root:
  - `mvn test`
  - `mvn -Dtest=ClassName test`
  - `mvn -Dtest=ClassName#methodName test`
  - `mvn package`
- Current tests live under `src\test\java\com\frankies\bootcamp\...` and are plain JUnit 5 tests without a special integration-test profile.
- `mvnw.cmd` is present, but do not assume the Maven wrapper works: `.mvn\wrapper\maven-wrapper.properties` is missing.
- No dedicated repo lint command is currently defined in `pom.xml`.

## High-level architecture

- The app is primarily a server-rendered Jakarta web application packaged as a WAR. The main user-facing flow is JSP-based under `src\main\webapp`, with servlet endpoints under `src\main\java\com\frankies\bootcamp\servlet`.
- There is also a small JAX-RS layer under `src\main\java\com\frankies\bootcamp\rest` mounted at `/api`. It is used for Strava auth/callback handling, athlete summary APIs, and the Strava webhook endpoint.
- Authentication is session-based. `AuthenticationFilter` protects most routes, allows a small public path list, and can bootstrap a session from ngrok-provided auth headers. `AuthSessionService` is the canonical place for reading/writing the authenticated user and cached athlete session attributes.
- `BootcampServlet` is the base servlet for authenticated app pages. It resolves the signed-in `AuthenticatedUser`, loads the linked `BootcampAthlete`, and redirects users without a completed Strava link into the Strava onboarding JSP. App servlets that need the current athlete usually inherit from this class rather than reimplementing auth checks.
- The dashboard at `src\main\webapp\app\index.jsp` is intentionally server-rendered and eagerly includes the History, Honour Roll, Leaderboard, and Summary tab content via JSP includes and backing servlets. That page also emits the landing audit event and tab-click audit calls.
- Activity and leaderboard behavior still depend on a large in-memory summary model built by `ActivityProcessService`. It fetches athlete data from `DBService`, pulls Strava activities through `StravaService`, converts activities into `BaseSport` implementations through `SportFactory`, and assembles `PerformanceResponse` / `WeeklyPerformance` objects plus honour-roll and summary maps. `ActivityProcessJob` refreshes this summary on startup and daily.
- The currently agreed `FBC-22` direction is documented in Confluence at `https://millses.atlassian.net/wiki/spaces/FB/pages/4751361/FBC-22+Persistence+Architecture` and locally in `docs\adr\0001-fbc22-competition-derived-persistence.md`.
- `DBService` is both the JDBC access layer and the current schema-management mechanism. It looks up the JBoss datasource `java:jboss/strava`, performs startup DDL for newer tables and columns, and handles athlete/auth/audit/ZenBot persistence. There is no separate migration framework yet, so schema changes are often coupled to startup behavior.
- Strava integration is centered in `StravaService`: credential lookup, OAuth token exchange, token refresh, activity fetches, and callback URL construction. The current design expects Strava link initiation and callback handling to stay server-controlled.
- ZenBot is a server-side feature for logged-in athletes. `ZenBotServlet` combines `AiMessageService`, athlete-specific stats context from `ActivityProcessService`, and persisted conversation history in `zenbot_messages`.

## Key conventions

- Use the repo vocabulary from `CONTEXT.md`: athlete, user, competition, competition membership, global roles, competition roles, All Sports Equal, normalized activities, derived stats, and DB-backed summary.
- Preserve the current strategic direction captured in the handoff/context files: replace the in-memory summary with DB-backed activity/stat handling, then move toward explicit competition membership and competition-scoped authorization.
- For the current persistence design, prefer the clearer storage term `competition_athlete` for the athlete-in-competition relation.
- The current preferred `FBC-22` model is competition-scoped and uses `competition`, `competition_athlete`, `competition_activity_detail`, `competition_weekly_stats`, `competition_weekly_sport_stats`, `competition_summary`, `competition_summary_sport_stats`, and `competition_honour_roll`.
- `competition_activity_detail` is intentionally thin and should keep only the fields already needed by the current `stravaActivityDetails` shape so webhook add/update/delete remains possible without mirroring full Strava payloads.
- Rebuilds should be scoped to one `competition_athlete` at a time and should delete/recreate derived rows for that scope.
- Each competition must have at least one competition admin athlete, and only a competition admin can change another athlete's starting goal.
- Keep dashboard tab auditing centralized in `TabAuditServlet`. The initial `/app/` render should record one `page-landing` event, and tab audit writes for `history`, `leaderboard`, `honour-roll`, and `summary` should only happen on real user clicks.
- Treat the current dashboard loading approach as intentional. `app\index.jsp` eagerly includes tab content, and previous lazy-loading attempts caused regressions.
- Preserve server-side gating for Strava onboarding and ZenBot. Users without a linked Strava athlete should be routed through the onboarding flow by `BootcampServlet`, not by ad hoc page logic.
- Follow the existing package split: `servlet` for JSP-backed HTTP endpoints, `rest` for JAX-RS APIs, `service` for orchestration/integration code, `model` for app and API payload models, `sport` for sport-specific scoring behavior.
- Reuse `AuthSessionService` for session attribute names and semantics instead of setting custom auth/session keys ad hoc.
- Be careful with tests that load the sanitized performance fixtures. `PerformanceFixtureLoadingTest` asserts against raw Gson `JsonObject` / `JsonArray` instead of deserializing into `PerformanceResponse` because `stravaActivityDetails.sport` is typed as abstract `BaseSport`.
- Be aware of the known bug candidate in `ActivityProcessService.ensureWeeksUpTo(...)` around `get(w - 2)` when later weeks are created.
- Do not commit or copy secrets from repository files. There is a known unsafe credential file at `src\main\resources\credentials.json`.

## Repo workflow files

- `copilot-byok.ps1` bootstraps local Copilot BYOK and Atlassian-related environment variables.
- `SESSION_HANDOFF.md`, `BACKLOG_GROOMING_HANDOFF.md`, and `CONTEXT.md` are part of the intended working pattern for this repo and should be kept in sync with major changes.
