# Session handoff

New way of working

- Treat these handoff files (SESSION_HANDOFF.md and BACKLOG_GROOMING_HANDOFF.md) as the primary context for sessions.
- Keep answers ~50% shorter than default; be direct and practical.
- Prefer clear recommendations over long option lists.
- Work one ticket at a time unless explicitly asked to broaden scope.
- For each ticket provide: 1) a refined goal; 2) a pasteable ticket prompt; 3) suggested ordering/dependencies; 4) key risks and scope boundaries.
- Flag tickets that should be renamed, split, or moved in order.
- Do not assume repo-specific setup unless stated in the handoff.

## Current state

The latest completed work focused on `FBC-16`: competition setup/join, lifecycle-aware onboarding, competition-scoped reads, and historical competition outcome access. Broader competition-selection UX beyond that slice should now move into `FBC-30`.

### FBC-16 completion status

- `FBC-16` is now effectively wrapped on the local branch `feature/fbc-16-competition-setup`.
- Completed in this slice:
  - create competition flow from authenticated onboarding
  - join existing competition flow
  - per-competition starting-goal capture
  - competition start date and end date capture
  - active-competition date-window rules:
    - before start: competition-starts-soon screen
    - during window: normal active competition behavior
    - after end: no longer current active competition for default reads
  - removal of unsafe persistent read fallback to unrelated competition `1`
  - competition-scoped leaderboard and honour-roll reads for persistent mode
  - past-competitions-only onboarding state and screen
  - historical competition outcome opening from the past-competitions screen
  - session-backed selected historical competition context
  - historical reads now prefer persisted derived state and only backfill if no DB snapshot exists for that athlete + competition
- Important boundary agreed with the user:
  - this session intentionally stopped short of a general competition chooser/switcher model across the dashboard shell
  - that broader competition-selection/defaulting behavior should move into `FBC-30`

### FBC-30 handoff boundary

- `FBC-30` should now own the next layer of multi-competition UX and selection behavior, including:
  - selecting between competitions from the normal dashboard experience
  - defaulting to the current active competition when there is only one active option
  - deciding the UX when there is more than one simultaneously active competition
  - making selected competition context explicit and user-visible, not just implicit/session-backed from the history-only page
  - cleaning up any remaining mixed assumptions between "current active competition" and "explicitly selected competition"

### Current historical outcome behavior

- Historical outcome access is intentionally persistence-first:
  - when a past competition is selected, the app first checks persisted DB-derived state for that athlete + competition
  - if found, it trusts that historical result as the outcome
  - only if no persisted snapshot exists does it backfill the selected historical competition from Strava data
- This was chosen specifically to avoid unnecessary rebuilds of already-finished competitions.

### FBC-22 / FBC-32 architecture interrogation outcome

- A working architecture direction was agreed and documented in Confluence:
  - `https://millses.atlassian.net/wiki/spaces/FB/pages/4751361/FBC-22+Persistence+Architecture`
- A short local ADR was added at `docs\adr\0001-fbc22-competition-derived-persistence.md`.
- The current preferred persistence shape is competition-scoped and uses:
  - `competition`
  - `competition_athlete`
  - `competition_activity_detail`
  - `competition_weekly_stats`
  - `competition_weekly_sport_stats`
  - `competition_summary`
  - `competition_summary_sport_stats`
  - `competition_honour_roll`
- `competition_athlete` is the key relation instead of hanging weekly rows directly off `athlete`, because one athlete can participate in multiple competitions.
- `competition_activity_detail` was added back into the design after reviewing the current in-memory object. It keeps the thin activity-level fields already carried in `stravaActivityDetails` so webhook add/update/delete remains possible without storing the full raw Strava payload.
- Agreed read/write rule: reads stay dumb; rebuild/sync paths do the calculations and write fully computed rows.
- Agreed rebuild scope: one `competition_athlete` at a time, using competition-specific time boundaries.
- Agreed recalculation rule: delete and recreate all derived rows for that `competition_athlete`.
- Agreed admin rule: each competition must have at least one competition admin athlete, and only a competition admin can update another athlete's starting goal.
- Screen coverage was reviewed against the current UI screenshots and current in-memory object. Honour Roll is now expected to have its own read-model table.

### FBC-22 implementation status

- `FBC-22` now has a delivered first implementation slice on the working branch.
- Completed in this slice:
  - parallel non-breaking processing toggle via `BOOTCAMP_ACTIVITY_MODE`
  - new persistent competition-scoped tables and startup DDL
  - persistent leaderboard reads from `competition_summary`
  - persistent honour-roll reads from `competition_honour_roll`
  - thin persisted activity rows in `competition_activity_detail`
  - stable webhook add/delete behavior restored in persistent mode
- Important current constraint:
  - webhook add/delete in persistent mode currently rebuilds the athlete from Strava again
  - this was intentionally kept because the first attempt to rebuild purely from persisted activity rows caused missing derived rows and stale UI behavior
- Assumed follow-up ticket:
  - `FBC-91` captures the deferred optimization to rebuild webhook changes from persisted activity rows instead of full Strava refetch, mainly to reduce future Strava API limit risk
- Practical status:
  - treat `FBC-22` as materially implemented and potentially closeable as a first slice if `FBC-91` owns the deferred webhook/local-rebuild optimization
  - otherwise it remains technically in progress

### Session workflow / AI-working setup

- A lightweight transferable AI workflow was added to the repo:
  - `CONTEXT.md` now holds shared project language and current strategic direction.
  - `docs\adr\README.md` defines a minimal ADR convention for structural decisions.
- The intended working pattern going forward is:
  1. refine/grill the ticket first
  2. check or update `CONTEXT.md` when domain language changes
  3. add a short ADR for structural decisions
  4. implement in small slices
  5. leave/update session handoff notes
- This was chosen to be useful both for this repo and as a workplace-style AI workflow the user can practice.

### Local BYOK / Atlassian bootstrap

- Added `copilot-byok.ps1` to bootstrap a terminal session for Copilot CLI BYOK and Atlassian access.
- The script now:
  - sets Copilot OpenAI provider defaults
  - prompts for the OpenAI API key
  - optionally prompts for Atlassian API token
  - prepopulates non-secret Atlassian/Jira/Confluence defaults at the top of the file for easy editing
- Current non-secret defaults in the script:
  - Atlassian base URL: `https://millses.atlassian.net`
  - Atlassian email: `millslf@gmail.com`
  - Jira project key: `FBC`
  - Jira board ID: `35`
  - Jira board URL: `https://millses.atlassian.net/jira/software/projects/FBC/boards/35`
  - Confluence space key: `FB`
  - Confluence space URL: `https://millses.atlassian.net/wiki/spaces/FB/overview`
  - Confluence homepage ID: `4587688`
- Secrets are still intended to be entered interactively per session rather than stored in the repo.
- No Jira/Confluence helper commands were added yet; only session environment variable setup exists so far.

### FrankiZen

- ZenBot is now only rendered for logged-in users.
- The widget is server-gated using the authenticated athlete on the request.
- ZenBot tracks conversation turns.
- Replies are shorter and keep a more playful "crazy zen" tone unless the user asks a real factual question.
- Public competition facts are allowed in responses.
- Private athlete-specific facts are limited to the signed-in athlete.
- Nicknames are now generated by AI rather than a fixed Java nickname ladder, but should still be based on the athlete's real name or surname.
- ZenBot prompt/reply history is persisted in the database table `zenbot_messages`.

### Audit logging

- Athlete auth events are audited in `athlete_audit_log`.
- Current auth events:
  - `login`
  - `refresh`
- Dashboard audit events are centralized through `TabAuditServlet`.
- The dashboard now records:
  - one `page-landing` event on initial `/app/` load
  - `tab-click` events only for real user clicks on:
    - `history`
    - `leaderboard`
    - `honour-roll`
    - `summary`
- Synthetic tab activation for the default History tab is guarded so it does not create a fake audit event.
- Direct audit writes were removed from the tab render servlets so tab auditing stays centralized.

### Dashboard tabs

- The attempted lazy-loading approach was rolled back.
- Tabs are back to server-rendered eager loading because the lazy-load version caused layout, rendering, and script execution regressions.
- Weekly History stays as originally rendered.
- Leaderboard, Honour Roll, and Summary should be treated as stable server-rendered tabs unless a different loading strategy is reworked properly later.

### Database-related additions

- `DBService` now creates and uses:
  - `zenbot_messages`
  - `athlete_audit_log`
- Both tables are intended to link to `athletes.id`.
- There is currently no proper migration framework, so schema changes are still handled via startup DDL.
- `FBC-22` / `FBC-32` should introduce a proper persistence slice with migration tooling instead of extending startup DDL for the new competition-derived model.

### Test fixture added for future refactor safety

- A sanitized check-in-safe fixture was added at `src\test\resources\fixtures\memory-summary-sanitized.json`.
- It preserves the current in-memory summary structure shape while using fake names and a smaller realistic data set.
- Intended primary use: `FBC-40` automated tests protecting current summary/scoring behavior before persistence refactors.
- A second focused fixture was added at `src\test\resources\fixtures\memory-summary-goal-cases.json` for under-goal / exact-goal / over-goal scenarios.

### Unit test progress

- Unit test work for `FBC-40` was started and is meaningfully underway.
- Added test classes:
  - `src\test\java\com\frankies\bootcamp\model\WeeklyPerformanceTest.java`
  - `src\test\java\com\frankies\bootcamp\model\PerformanceResponseTest.java`
  - `src\test\java\com\frankies\bootcamp\service\ActivityProcessServiceTest.java`
  - `src\test\java\com\frankies\bootcamp\service\PersistentActivityProcessServiceTest.java`
  - `src\test\java\com\frankies\bootcamp\fixture\PerformanceFixtureLoadingTest.java`
  - `src\test\java\com\frankies\bootcamp\sport\SportFactoryTest.java`
- Current estimated progress against the `FBC-40` prompt: about **68%**.
- Covered so far:
  - weekly goal progression rules
  - exact-goal and over-goal scoring
  - sick week scoring behavior
  - sport add/remove behavior
  - add/remove/update-style activity mutations in `ActivityProcessService`
  - persistent activity rebuild totals, persisted-row shaping, and webhook-triggered rebuild behavior in `PersistentActivityProcessService`
  - persistent-vs-legacy parity checks for overlapping calculation flows
  - leaderboard ordering basics
  - sport factory routing / unsupported sport handling
  - fixture-driven totals, score ranking, remaining-distance, and goal-band checks
- Important discovered issue while testing:
  - `ActivityProcessService.ensureWeeksUpTo(...)` appears to have an off-by-one/null bug due to `get(w - 2)` when creating later weeks. This was exposed by a test path, but not fixed yet.
- Important testing note:
  - fixture-loading tests must use raw JSON assertions rather than Gson deserialization into `PerformanceResponse`, because `stravaActivityDetails.sport` is typed as abstract `BaseSport`.

### FBC-22 closeout status

- `FBC-91` exists in Jira and explicitly owns the deferred webhook optimization to rebuild from persisted activity rows instead of full Strava refetch.
- `FBC-22` is now effectively complete as a first usable slice and is ready to move to closed once Jira is updated.
- The persistent path now serves the main app reads from DB-backed state instead of the retained persistent-mode in-memory cache.
- `PersistentActivityProcessService` no longer retains `performanceList`; performance-list, history, summary, honour-roll, and ZenBot read paths now reconstruct/read from persisted tables.
- The remaining closeout work was narrowed to persistent-service unit/parity tests, and `PersistentActivityProcessServiceTest` was added and updated for that purpose.
- While landing those tests, `PersistentActivityProcessService` gained small protected test seams for DB-backed writes:
  - `rebuildAthleteState(...)` is now `protected`
  - `ensureCompetitionAthlete(...)`
  - `replacePersistentCompetitionState(...)`
  - `replaceCompetitionHonourRoll(...)`
- A legacy auth shortcut was also removed: `AuthenticationFilter` no longer trusts `Ngrok-Auth-User-Email` to bootstrap a logged-in session. API/browser requests now need a real authenticated session cookie.
- `copilot-byok.ps1` now prepends the local Maven bin path `C:\TFS\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin` when present so future Copilot CLI sessions can run `mvn` directly.
- Deferred follow-up tickets now stand as:
  - `FBC-91` webhook optimization from persisted activity rows
  - `FBC-92` broader removal of remaining legacy in-memory summary code
  - `FBC-93` analysis-only heart-rate-informed equivalent-distance metric

## Important files

- `copilot-byok.ps1`
- `CONTEXT.md`
- `docs\adr\README.md`
- `src\main\java\com\frankies\bootcamp\service\AiMessageService.java`
- `src\main\java\com\frankies\bootcamp\servlet\ZenBotServlet.java`
- `src\main\java\com\frankies\bootcamp\service\ActivityProcessService.java`
- `src\main\java\com\frankies\bootcamp\service\DBService.java`
- `src\main\java\com\frankies\bootcamp\service\StravaService.java`
- `src\main\java\com\frankies\bootcamp\servlet\TabAuditServlet.java`
- `src\main\webapp\app\index.jsp`
- `src\main\webapp\WEB-INF\jspf\zenbot.jspf`
- `src\main\webapp\WEB-INF\jspf\head-common.jspf`
- `src\main\webapp\styles\main.css`
- `src\test\resources\fixtures\memory-summary-sanitized.json`
- `src\test\resources\fixtures\memory-summary-goal-cases.json`
- `src\test\java\com\frankies\bootcamp\model\WeeklyPerformanceTest.java`
- `src\test\java\com\frankies\bootcamp\model\PerformanceResponseTest.java`
- `src\test\java\com\frankies\bootcamp\service\ActivityProcessServiceTest.java`
- `src\test\java\com\frankies\bootcamp\service\PersistentActivityProcessServiceTest.java`
- `src\test\java\com\frankies\bootcamp\fixture\PerformanceFixtureLoadingTest.java`
- `src\test\java\com\frankies\bootcamp\sport\SportFactoryTest.java`

## Known constraints and follow-up notes

- Maven was initially not available in the shell during the session, but `copilot-byok.ps1` now adds the local Maven bin path for future sessions.
- `mvnw.cmd` is present but the Maven wrapper is incomplete in this repo (`.mvn\wrapper\maven-wrapper.properties` missing), so wrapper-based commands currently fail.
- Existing deployed databases may need manual migration work if the tables already existed before the latest FK/index changes.
- There is an unsafe file at `src\main\resources\credentials.json` containing a live secret and it should not be committed.

## Backlog grooming status

Backlog grooming was continued and a prioritized "dev ready" board order was established for the next major phase. The current top of the board is:

1. `FBC-47` auth foundation / multiple providers + email/password
2. `FBC-40` automated tests protecting current scoring/summary behavior
3. `FBC-86` "All Sports Equal" product philosophy
4. `FBC-32` Hibernate + migration/persistence foundation
5. `FBC-2` reproducible local development environment
6. `FBC-3` containerized build + automated deployment pipeline
7. `FBC-4` automated security scanning in CI/CD
8. `FBC-36` narrowed credential/secret configuration cleanup
9. `FBC-53` direct HTTPS + aligned upstream/proxy configuration
10. `FBC-6` stricter browser script security / less inline script dependence
11. `FBC-19` MAUI mobile app shell using WebView
12. `FBC-20` App Store / Play Store readiness
13. `FBC-12` competition-aware onboarding flow
14. `FBC-56` Strava link only in authenticated onboarding state (done)
15. `FBC-54` global + competition-scoped authorization model
16. `FBC-16` competition creation/setup flow
17. `FBC-30` athlete can belong to multiple competitions
18. `FBC-37` competition-specific eligible sports
19. `FBC-38` competition-specific distance ratios / normalization rules
20. `FBC-22` replace large in-memory activity summary with persisted normalized activities + derived stats

Additional board/backlog decisions made:

- `FBC-35` was absorbed into `FBC-56`.
- `FBC-8` was absorbed into `FBC-53` and `FBC-6`, so it no longer needs to stand alone.
- `FBC-86` was moved up because it is a small early product-principle ticket that should shape later copy and defaults.
- The app's future product direction is now explicitly multi-competition, with global admin, competition admin, owner, and athlete/member concepts.
- Three new placeholder tickets were added to the backlog for:
  - competition invitations and join flow
  - global admin console / admin operations
  - competition admin console
- `FBC-12` was re-checked against Jira and is no longer a registration/auth ticket.
- Current `FBC-12` meaning is competition-aware onboarding flow after authentication.
- `FBC-12` now owns explicit onboarding states and routing such as:
  - authenticated but not fully onboarded
  - app user exists but Strava not linked
  - Strava linked but no competition membership yet
  - eligible for join/create next step
  - fully onboarded and ready for normal dashboard use
- `FBC-12` should build on the already-complete `FBC-56` Strava-link gating rather than reimplementing it.
- Practical sequencing update:
  - `FBC-12` should now sit before `FBC-16`, `FBC-30`, and `FBC-54` as the onboarding-state/routing layer those tickets build on.

## Best next checks next session

### Latest local status update

- The live board is now practically focused on multicomp rather than the older auth-first ordering above.
- Current in-progress working order from the board is:
  1. `FBC-12` competition-aware onboarding flow
  2. `FBC-16` competition creation/setup screens and per-competition start configuration
  3. `FBC-30` allow an athlete to belong to multiple competitions
  4. `FBC-54` implement global and competition-scoped authorization model
  5. `FBC-37` competition-specific eligible sports
  6. `FBC-38` specify relative distances per competition
- `FBC-12` was reviewed locally and judged effectively done as the onboarding-state/routing slice.
- That `FBC-12` work was split onto branch `feature/fbc-12-onboarding-flow` with commit `217fbbb`.
- `FBC-16` is now implemented locally on branch `feature/fbc-16-competition-setup`.
- Delivered `FBC-16` slice:
  - real competition setup page at `/app/competition-setup`
  - create competition flow with name, timezone, start date, and per-competition starting goal
  - join existing competition flow with per-competition starting goal
  - competition onboarding page now routes users into the setup flow instead of a placeholder dead end
  - new DB methods for listing active competitions, creating a competition with the first admin member, and joining a competition
  - focused unit coverage for the competition setup service
- The unrelated session-lifetime fix was split onto branch `bugfix/session-persistence` with commit `546c063`.
- Both branches were pushed, but PRs were not created from the Copilot session because no PR-creation tool was available in the environment.
- Compare URLs prepared for manual PR creation:
  - `https://github.com/millslf/frankiesbootcamp/compare/main...bugfix/session-persistence?expand=1`
  - `https://github.com/millslf/frankiesbootcamp/compare/main...feature/fbc-12-onboarding-flow?expand=1`
- `.github\copilot-instructions.md` still has uncommitted local changes and was intentionally left out of both branch commits.

### How to restart next session

- Start by asking the agent to read `.github\copilot-instructions.md` and get up to date.
- That should lead it into `SESSION_HANDOFF.md`, `BACKLOG_GROOMING_HANDOFF.md`, and `CONTEXT.md`.
- Then tell it the current focus is multicomp and that `FBC-12` has a pushed branch/commit ready for PR review.
- Tell it `FBC-16` work now exists locally on `feature/fbc-16-competition-setup` and should be reviewed/committed next.

1. If continuing the workflow/tooling setup, add lightweight Jira/Confluence helper scripts that use the env vars from `copilot-byok.ps1` for:
   - reading a Jira issue
   - adding a Jira comment
   - fetching a Confluence page
   - updating a Confluence page safely
2. If continuing product/architecture work, use the Confluence page and ADR as the starting point for `FBC-22` / `FBC-32`, and keep `FBC-22` elevated with `FBC-32` as the first implementation slice.
3. Continue moving toward explicit competition membership / multi-competition boundaries using `FBC-30`, then follow with `FBC-54`, `FBC-37`, and `FBC-38`.
1. Verify the `page-landing` audit logs exactly once on initial `/app/` load.
2. Verify `history`, `leaderboard`, `honour-roll`, and `summary` only audit on real user clicks.
3. Confirm ZenBot visibility is correct in normal browsing and not just incognito.
4. Plan the first migration/JPA slice around the agreed `competition_athlete` model and the new derived competition tables instead of extending startup DDL.
5. Resume backlog grooming from the remaining backlog items not yet reprioritized/prompted, starting with architecture/backlog items such as `FBC-60`, `FBC-24`, `FBC-52`, `FBC-26`, `FBC-27`, `FBC-28`, `FBC-29`, `FBC-33`, `FBC-34`, `FBC-44`, and the existing MCP/React migration sequences if more shaping is needed.
6. Resume `FBC-40` from the current ~68% state, focusing next on:
   - recalculation consistency from realistic flows
   - summary-facing outputs / stats-context style assertions
   - any additional edge-case coverage needed before major persistence refactors
