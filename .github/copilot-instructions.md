# Copilot instructions for Frankie's Bootcamp

## Read first
0
- Apply `.copilotignore` before reading or indexing repo files.
- Run `copilot-byok.ps1` from the repo root when local Copilot BYOK, Jira, Confluence, or Maven setup is needed.
- Read `SESSION_HANDOFF.md`, `BACKLOG_GROOMING_HANDOFF.md`, `CONTEXT.md`, `AGENTS.md`, and the relevant `docs\agents\*.md` files before changing behavior or workflow-sensitive code.
- Add or update a short ADR under `docs\adr\` when a change affects persistence shape, authorization boundaries, or long-term architecture.

## Build, test, and lint

- Maven WAR project targeting Java 23 and a Jakarta EE / WildFly-style runtime.
- Run from the repo root with Maven:
  - `mvn test`
  - `mvn -Dtest=ClassName test`
  - `mvn -Dtest=ClassName#methodName test`
  - `mvn package`
- Useful single-test examples:
  - `mvn -Dtest=AuthSessionServiceTest test`
  - `mvn -Dtest=ActivityProcessServiceTest test`
  - `mvn -Dtest=PersistentActivityProcessServiceTest test`
  - `mvn -Dtest=PerformanceFixtureLoadingTest test`
- Tests live under `src\test\java\com\frankies\bootcamp\...`.
- `mvnw.cmd` exists, but `.mvn\wrapper\maven-wrapper.properties` is missing, so use Maven directly.
- There is no dedicated lint command in `pom.xml`.

## High-level architecture

- Server-rendered Jakarta web app packaged as a WAR. Main UI is JSP-based under `src\main\webapp`, with servlet endpoints under `src\main\java\com\frankies\bootcamp\servlet`.
- `/app` maps to `AppServlet`, which forwards to `src\main\webapp\app\index.jsp`. That dashboard is intentionally server-rendered and eagerly includes tab content.
- `/api` is a small JAX-RS layer under `src\main\java\com\frankies\bootcamp\rest` for Strava auth callback flow, athlete summary JSON, and the Strava webhook.
- `AuthenticationFilter` protects most routes; `AuthSessionService` is the canonical place for reading and writing authenticated-user and athlete session state.
- `BootcampServlet` centralizes authenticated-page gating, Strava onboarding, competition selection state, and onboarding forwarding. App servlets that need the current athlete should usually inherit from it.
- `ActivityProcessFacade` switches between the legacy in-memory `ActivityProcessService` and the DB-backed `PersistentActivityProcessService` using `BOOTCAMP_ACTIVITY_MODE`. Summary, leaderboard, honour-roll, ZenBot, scheduled rebuild, and webhook changes need to account for both modes unless intentionally scoped to one.
- `DBService` is both the JDBC layer and the current startup-DDL mechanism. It uses the JBoss datasource `java:jboss/strava` and creates the auth, audit, ZenBot, and competition tables at startup.
- `StravaService` owns OAuth, token refresh, activity fetches, and single-activity lookup. `StravaWebhook` accepts webhook events quickly and processes activity changes asynchronously.
- `TabAuditServlet` is the central place for dashboard audit writes; initial `/app/` landing and real tab clicks are treated differently.

## Key conventions

- Use the repo vocabulary from `CONTEXT.md`: athlete, user, competition, competition membership, global roles, competition roles, All Sports Equal, normalized activities, derived stats, and DB-backed summary.
- Prefer `competition_athlete` for the athlete-in-competition row/table name.
- Keep `competition_activity_detail` thin; it should only hold the fields needed for current `stravaActivityDetails` behavior.
- Persistent rebuilds are scoped to one `competition_athlete` at a time and should delete/recreate derived rows for that scope.
- Use `BootcampServlet` for authenticated JSP pages instead of duplicating auth/onboarding logic.
- Reuse `AuthSessionService` for session attribute names and semantics instead of inventing new keys.
- Keep dashboard tab auditing centralized in `TabAuditServlet`; the `/app/` landing event and `history`, `leaderboard`, `honour-roll`, and `summary` clicks should not be conflated.
- `PerformanceFixtureLoadingTest` uses raw Gson `JsonObject` / `JsonArray` because `stravaActivityDetails.sport` is typed as abstract `BaseSport`.
- `PersistentActivityProcessServiceTest` uses protected test seams and fake DB/Strava collaborators instead of a real JNDI datasource.
- Be aware of the known bug candidate in `ActivityProcessService.ensureWeeksUpTo(...)` around `get(w - 2)` when later weeks are created.
- Do not commit or copy secrets from repository files; `src\main\resources\credentials.json` is known unsafe.
