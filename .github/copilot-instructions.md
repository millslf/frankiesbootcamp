# Copilot instructions for Frankie's Bootcamp

## Read first
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
- Server-rendered Jakarta web app packaged as a WAR. JSPs live under `src\main\webapp`; annotated servlets live under `src\main\java\com\frankies\bootcamp\servlet`; REST endpoints live under `rest`; shared business logic lives in `service`.
- `web.xml` only wires a few legacy JSP/page routes and the UTF-8 filter; most request handling is annotation-based.
- `/app` is `AppServlet`, which forwards to `src\main\webapp\app\index.jsp`. The dashboard is intentionally server-rendered and eagerly includes tab content.
- `BootcampServlet` is the main authenticated-page gate. It loads the current athlete, resolves onboarding state, syncs the selected competition, and forwards to the correct onboarding/chooser page before child servlets run.
- `AuthenticationFilter` is the broad gate for public vs protected paths. It lets static assets and a small public route list through, redirects browser requests to `/login`, and returns 401 for unauthenticated `/api/*` calls.
- `AuthSessionService` is the canonical source for session keys and semantics (`authUser`, `selectedCompetitionId`, pending invitation token).
- `ActivityProcessFacade` switches between the legacy in-memory `ActivityProcessService` and the DB-backed `PersistentActivityProcessService` via `BOOTCAMP_ACTIVITY_MODE`.
- `DBService` owns JDBC access and startup DDL against the `java:jboss/strava` datasource. The current persistence slice is competition-scoped and centered on `competition`, `competition_athlete`, `competition_activity_detail`, weekly stats, summary, and honour-roll tables.
- `StravaService` owns OAuth, token refresh, activity fetches, and single-activity lookup. `StravaWebhook` accepts webhook events quickly and hands off processing.
- `TabAuditServlet` is the central place for dashboard audit writes; the initial `/app/` landing and real tab clicks are tracked differently.

## Key conventions
- Use the repo vocabulary from `CONTEXT.md`: athlete, user, competition, competition membership, global roles, competition roles, All Sports Equal, normalized activities, derived stats, and DB-backed summary.
- Prefer `competition_athlete` for the athlete-in-competition row/table name.
- Keep `competition_activity_detail` thin; it should only hold the fields needed for current `stravaActivityDetails` behavior.
- Persistent rebuilds are scoped to one `competition_athlete` at a time and should delete/recreate derived rows for that scope.
- Reuse `BootcampServlet` for authenticated JSP pages instead of duplicating auth/onboarding logic.
- Reuse `AuthSessionService` for session attribute names and semantics instead of inventing new keys.
- Keep dashboard tab auditing centralized in `TabAuditServlet`; the `/app/` landing event and `history`, `leaderboard`, `honour-roll`, and `summary` clicks should not be conflated.
- `PerformanceFixtureLoadingTest` uses raw Gson `JsonObject` / `JsonArray` because `stravaActivityDetails.sport` is typed as abstract `BaseSport`.
- `PersistentActivityProcessServiceTest` uses protected test seams and fake DB/Strava collaborators instead of a real JNDI datasource.
- Be aware of the known bug candidate in `ActivityProcessService.ensureWeeksUpTo(...)` around `get(w - 2)` when later weeks are created.
- Do not commit or copy secrets from repository files; `src\main\resources\credentials.json` is known unsafe.
