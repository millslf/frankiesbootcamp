# Backlog grooming handoff

New way of working

- Use SESSION_HANDOFF.md for session context and BACKLOG_GROOMING_HANDOFF.md for backlog priorities.
- Keep answers ~50% shorter than default and be direct and practical.
- Work one ticket at a time and use existing tickets before proposing new ones.
- For each ticket provide: refined goal, a pasteable ticket prompt, suggested ordering/dependencies, and key risks/scope boundaries.
- Flag tickets that should be renamed, split, or moved in order.

## Purpose

This file captures where backlog grooming stopped, what ticket prompts/order were agreed, what was merged, and where to resume next time.

## Current outcome

A "dev ready" top-of-board order was established to prioritize:

1. identity/auth foundation
2. behavioral safety via automated tests
3. product philosophy and defaults
4. persistence/migration foundation
5. reproducible local/dev + CI/CD
6. practical security hardening
7. mobile shell/store readiness
8. onboarding and authorization
9. multi-competition model and competition-specific rules
10. replacement of the large in-memory summary model

## Since this handoff was written

- `FBC-40` is now considered done by the user.
- The user wants to bring forward removal of the large in-memory summary object and move toward DB-backed activity/stat handling.
- Recommendation given in-session: bring `FBC-22` forward and keep it tightly coupled to `FBC-32` rather than letting product-philosophy work interrupt the persistence transition.
- The user also clarified the next major business direction after that refactor:
  - move toward explicit competition membership
  - stop implicitly placing newly joined users into the one active competition
  - reduce cross-competition visibility assumptions
  - prepare for multi-competition and later multi-sport behavior
- Recommendation given in-session for the next competition-related sequence after `FBC-22` / `FBC-32`:
  1. `FBC-30`
  2. `FBC-16`
  3. `FBC-54`
  4. `FBC-37`
  5. `FBC-38`
- A concrete architecture interrogation was completed for `FBC-22` / `FBC-32` and documented in Confluence:
  - `https://millses.atlassian.net/wiki/spaces/FB/pages/4751361/FBC-22+Persistence+Architecture`
- A local ADR was also added at `docs\adr\0001-fbc22-competition-derived-persistence.md`.
- Current recommended implementation shape:
  - use `competition_athlete` as the core competition-scoped relation
  - store thin `competition_activity_detail` rows matching current `stravaActivityDetails` fields so webhook add/update/delete remains possible
  - persist weekly stats, weekly sport stats, summary stats, summary sport stats, and honour-roll rows as read models
  - rebuild one `competition_athlete` at a time and delete/recreate derived rows during recalculation
  - treat `FBC-32` as the first persistence/migration slice that enables this model rather than as an isolated framework ticket
- Current delivery status for `FBC-22`:
  - a delivered first implementation slice is now in place
  - persistent leaderboard and honour-roll reads are working from DB-backed read models
  - persistent webhook add/delete is currently stable again, but still uses full athlete refetch from Strava
  - the attempted local persisted-activity webhook rebuild was reverted after causing missing derived rows / stale UI behavior
  - history, summary, ZenBot stats context, and full performance-list reads are now DB-backed as well
  - the retained persistent-mode `performanceList` cache has been removed from `PersistentActivityProcessService`
  - `FBC-91`, `FBC-92`, and `FBC-93` now hold the deferred follow-up work, so `FBC-22` should be treated as ready to close as the first persistence slice
- Current delivery status for `FBC-30`:
  - `FBC-30` is functionally complete and browser-tested on `feature/fbc-30-competition-selection`; PR #16 is open and the user is moving the ticket to code review.
  - Delivered scope includes active competition chooser/defaulting, past competition switching, explicit selected competition context, competition-scoped sick weeks, bounded/background historical rebuilds, and historical competition recap messaging.
  - Join/create competition discovery is hidden from the in-app menu until `FBC-89` implements invitation-aware join/create behavior.
  - Leaving/removing yourself from a competition is intentionally deferred to `FBC-89` for membership lifecycle and `FBC-54` for authorization rules.
- Current local follow-up branch:
  - `feature/fbc-insights-tab` was created from the FBC-30 feature branch after the multi-active competition stale rebuild fix was committed and pushed to PR #16 as `4b3462c`.
  - This branch is pushed local work for a read-only dashboard Insights tab, not yet assigned to a Jira ticket in this handoff. PR should target `feature/fbc-30-competition-selection`, because this is stacked on PR #16.
  - Implemented local scope:
    - final dashboard tab `/app/Insights`
    - athlete profile modal for any athlete in the selected competition
    - compact dropdown for choosing another athlete profile
    - global Profile card on athlete profiles, with accepted blurbs persisted as verified and unaccepted generated text lazily regenerated after render
    - clearing the Profile box and saving deletes the verified profile, returning that athlete to generated/unverified mode
    - accepted/verified profiles disable `Save changes` until the text is edited
    - generated current Performance summary at the bottom of the profile modal
    - Strava-link-time storage of `athletes.sex`, used for AI pronouns when present; otherwise prompts stay generic and do not guess gender from names
    - active dashboard tab is stored in the URL as `?tab=...`, so pull-to-refresh restores the selected tab instead of defaulting to Weekly History
    - position-over-time graph showing overall leaderboard rank history
    - current/latest completed week is shown on the left; history runs rightward back to Week 1
    - tied leaderboard scores share the same rank rather than inventing order
    - sport-specific standings and week-by-week winner summaries from existing derived data
  - Privacy/product rules agreed during this branch:
    - public athlete-level km values should remain private
    - new Insights surfaces must not expose athlete km totals
    - Profile blurbs should be timeless/personal and not tied to current competition standings
    - Performance summaries may use public rank/points/current-week context but must not include private athlete km values
    - Honour Roll distance values should also be hidden; show distance leader name only
    - active competitions should exclude the current partial week from Insights to avoid early-week noise
  - Persistent summary regression fixed locally:
    - `getLoggedInAthleteSummary(...)` and `getLoggedInAthleteSummaryForCompetition(...)` now use cumulative sport totals from `competition_summary_sport_stats`, restoring old `PerformanceResponse.toString()` behavior.
  - Current validation:
    - targeted service tests are green, including `CompetitionInsightsServiceTest` and `PersistentActivityProcessServiceTest`
    - full `mvn package` is green after the latest graph/UI/profile-summary/Strava-sex/tab-refresh tweaks
  - This probably deserves its own Jira ticket if it is kept separate from FBC-30 rather than folded into the PR.

## Agreed current board order

1. `FBC-47` Implement a Join functionality that can join via multiple OAuth providers, and also just using email and password.
2. `FBC-40` Create unit tests.
3. `FBC-86` All sports Equal, use this as frankies bootcamp philosophy.
4. `FBC-32` Use hibernate ORM for persistence.
5. `FBC-2` Set up reproducible local development environment.
6. `FBC-3` Set up containerized build and automated deployment pipeline.
7. `FBC-4` Add automated security scanning to CI/CD pipeline.
8. `FBC-36` Refactor application credential and secret management.
9. `FBC-53` Run the application on HTTPS directly and align upstream proxy configuration.
10. `FBC-6` Tighten browser script security and remove inline script dependence.
11. `FBC-19` Create a MAUI mobile app shell using WebView for the existing app.
12. `FBC-20` Prepare the mobile app for App Store and Play Store submission.
13. `FBC-12` Competition-aware onboarding flow.
14. `FBC-56` Strava link button should only appear when the user is authenticated and ready to link Strava, and should no longer act as an enrolment shortcut. (Done)
15. `FBC-54` Implement global and competition-scoped authorization model.
16. `FBC-16` Set up screens to create competition if athlete is not part of any competitions, set up start week, and set up start goal per competition.
17. `FBC-30` Allow an athlete to belong to multiple competitions. (Implemented; PR #16 in code review.)
18. `FBC-37` Allow competition-specific eligible sports while preserving "All Sports Equal" as the default model.
19. `FBC-38` Specify relative distances per competition.
20. `FBC-22` Replace in-memory activity summary with persisted normalized activities and derived stats. (Implemented first slice; close after Jira/admin cleanup.)

## Ticket shaping completed in this session

Prompts/order discussions were completed for:

- `FBC-47`
- `FBC-40`
- `FBC-86`
- `FBC-32`
- `FBC-2`
- `FBC-3`
- `FBC-4`
- `FBC-36`
- `FBC-53`
- `FBC-6`
- `FBC-19`
- `FBC-20`
- `FBC-12`
- `FBC-56`
- `FBC-54`
- `FBC-16`
- `FBC-30`
- `FBC-37`
- `FBC-38`
- `FBC-22`

## Important prompt adjustments agreed

### Identity / onboarding

- Email should not be assumed to be the long-term username.
- The future model should allow a separate chosen username/display handle.
- Strava linking should happen after authenticated onboarding, not act as the main enrolment path.
- Strava link initiation should be server-controlled and not built directly in the frontend.
- `FBC-12` was later updated in Jira and should now be treated as a competition-aware onboarding-state and routing ticket, not a registration-flow ticket.
- `FBC-12` should represent explicit post-auth states including:
  - authenticated but not fully onboarded
  - app user exists but Strava not linked
  - Strava linked but no competition membership yet
  - eligible for competition join/create next step
  - fully onboarded and ready for normal dashboard use
- `FBC-56` is already done, so `FBC-12` should reuse the completed authenticated Strava-link gating rather than re-solving that behavior.
- Updated practical ordering after this clarification:
  1. `FBC-12`
  2. `FBC-16`
  3. `FBC-30`
  4. `FBC-54`
  5. `FBC-37`
  6. `FBC-38`

### Product philosophy

- "All Sports Equal" stays the main phrase.
- Meaning clarified as: all sports are welcome, all sports are valid effort, and they should be measured fairly/equally by default.
- Competition-specific restrictions and ratio rules are still allowed.

### Persistence direction

- The large in-memory summary structure should eventually be replaced.
- Best-practice direction is:
  - persist thin normalized activity detail rows only where needed to preserve current webhook behavior
  - persist derived competition stats separately for dumb reads
  - use webhook events for incremental updates where practical
  - use scheduled full sync as reconciliation/backfill
- Updated practical note after implementation work:
   - incremental webhook rebuild from persisted local activity rows is now explicitly tracked in `FBC-91`, not `FBC-22`
   - current stable behavior in persistent mode is full athlete refetch from Strava on webhook add/delete
- Do not mirror the full Strava JSON shape as the main table design.
- Raw Strava payload storage is still considered unnecessary.
- The current preferred table set is:
  - `competition`
  - `competition_athlete`
  - `competition_activity_detail`
  - `competition_weekly_stats`
  - `competition_weekly_sport_stats`
  - `competition_summary`
  - `competition_summary_sport_stats`
  - `competition_honour_roll`
- Current rule addition: each competition must have at least one competition admin athlete, and only a competition admin can change another athlete's starting goal.
- A safe fixture based on the current summary shape was created at `src\test\resources\fixtures\memory-summary-sanitized.json` for future `FBC-40` tests.
- A second focused fixture was added at `src\test\resources\fixtures\memory-summary-goal-cases.json` for goal-specific test cases.

## FBC-40 unit test progress

Work was started directly on `FBC-40`.

### Test files added

- `src\test\java\com\frankies\bootcamp\model\WeeklyPerformanceTest.java`
- `src\test\java\com\frankies\bootcamp\model\PerformanceResponseTest.java`
- `src\test\java\com\frankies\bootcamp\service\ActivityProcessServiceTest.java`
- `src\test\java\com\frankies\bootcamp\service\PersistentActivityProcessServiceTest.java`
- `src\test\java\com\frankies\bootcamp\fixture\PerformanceFixtureLoadingTest.java`
- `src\test\java\com\frankies\bootcamp\sport\SportFactoryTest.java`

### Approximate completion against the FBC-40 prompt

About **68%** complete.

### Covered so far

- weekly goal progression rules
- exact-goal and over-goal scoring
- sick week behavior
- sport add/remove behavior
- add/remove/update-style activity mutation flows
- leaderboard ordering basics
- sport mapping and unsupported sport handling
- fixture-driven totals, score ranking, remaining-distance, and goal-band checks

### FBC-22 closeout note

- Additional unit/parity coverage was started for the DB-backed path in `PersistentActivityProcessServiceTest`.
- That new test class focuses on:
  - athlete rebuild totals and persisted-row shaping
  - webhook-triggered rebuild behavior in persistent mode
  - parity checks against the overlapping legacy calculation flow
- `PersistentActivityProcessService` gained small protected test seams for DB-backed writes so these tests can run without a JNDI datasource.
- Practical closeout rule now: if the persistent-service tests are green, `FBC-22` should be ready to close while leaving `FBC-91` open as the separate optimization ticket.

### Important findings

- A likely bug was exposed in `ActivityProcessService.ensureWeeksUpTo(...)`:
  - it uses `perf.getWeeklyPerformances().get(w - 2)` when creating later weeks
  - this appears to produce a null/invalid previous-week lookup in at least one path
  - the issue was observed through tests but intentionally not fixed yet
- Fixture tests cannot deserialize directly into `PerformanceResponse` with Gson because `stravaActivityDetails.sport` is abstract `BaseSport`.
  - The fixture tests were rewritten to assert against raw `JsonObject` / `JsonArray` instead.

### Suggested next FBC-40 slice

Resume with:

1. recalculation consistency tests from realistic flows
2. summary-facing output assertions (for example summary/stat-context style outputs)
3. any remaining edge cases around current in-memory assumptions before persistence refactor work proceeds

### Security/delivery

- `FBC-35` was merged into `FBC-56`.
- `FBC-8` was merged into `FBC-53` / `FBC-6`.
- `FBC-36` was narrowed because most config already uses WildFly system properties/environment-driven values.

## New placeholder tickets added

- `FBC-89` Competition invitations and join flow
  - Next intended ticket after `FBC-30`.
  - Implement invitation-first competition joining:
    - admins invite people to a competition by email
    - existing FB users see dashboard invite notifications/CTAs
    - non-users receive invite links that survive signup/login, Strava linking, and competition acceptance
    - accepting creates or reactivates the `competition_athlete` row and captures/confirms competition starting goal
    - declining hides the invite and marks it declined
    - expired/invalid/already-used tokens show safe errors
  - Include simple bulk invite:
    - paste comma, newline, or semicolon-separated emails
    - normalize case, trim whitespace, validate, dedupe, and show per-email errors
    - skip/report already-invited or already-joined people
    - create one invitation row per valid invitee
  - Include existing athlete/user search on the invite form:
    - search by name or email
    - show enough context to identify the person
    - selecting an existing athlete adds them to the pending invite list
    - tie invitations to `invited_user_id` where possible
    - exclude athletes already active in the selected competition
  - Also owns the member lifecycle that `FBC-30` intentionally did not decide:
    - whether athletes can leave/remove themselves from active, future, or completed competitions
    - rejoin/reinvite behavior after decline, expiry, prior removal, or prior membership
    - last-admin protection
    - whether historical leaderboard/stat rows are preserved, hidden, or excluded after membership changes
  - Use current `competition_athlete.role = 'admin'` as the temporary permission gate; formal authorization remains `FBC-54`.
  - Keep out of scope for this ticket: CSV/contact import, fancy email templates, batch scheduling, public competition discovery, and full admin console redesign.
- `FBC-90` Global admin console / admin operations
- `FBC-14` Competition admin console

These were identified as the highest-value missing additions.

## Remaining backlog still to prioritize / prompt

Not yet shaped in this grooming pass:

- `FBC-44` Implement push notifications for athletes that gets sent after uploading a activity.
- `FBC-48` OffCanvas needs to keep state even when in hbackground for a long time.
- `FBC-51` Offcanvas loses state after long sleep and shows "login" instead of "Go to Dashboard".
- `FBC-46` Strava link button, remove if already linked.
- `FBC-26` Better error page.
- `FBC-27` Longest streak.
- `FBC-28` Contact details section.
- `FBC-29` Use my domain for contact details eg. admin@frankiesbootcamp.com.
- `FBC-33` Auto goal increase certain weeks.
- `FBC-34` Make scoring servlet dynamic to indicate new sports as they are added.
- `FBC-52` Create doco on wildfly setup.
- `FBC-24` Add diagrams for Architecture.
- `FBC-60` Change the service layer to something that makes more sense in Azure land, since I have azure subscription.
- The MCP implementation sequence `FBC-62` to `FBC-72` may still need final reprioritization against `FBC-22` and the broader multi-competition plan.
- The React migration sequence `FBC-74` to `FBC-85` was already more fully shaped previously, but may still need reprioritization after the new board order settles.

## Suggested resume point next session

### Latest board reality update

- The live board has moved ahead of the older ordering in this file.
- Treat the current practical multicomp sequence as:
  1. `FBC-12`
  2. `FBC-16`
  3. `FBC-89`
  4. `FBC-30`
  5. `FBC-54`
  6. `FBC-37`
  7. `FBC-38`
- `FBC-12` now appears effectively complete as the onboarding-state/routing slice.
- Local branch/commit prepared for that work:
  - `feature/fbc-12-onboarding-flow`
  - `217fbbb`
- `FBC-16` is now actively implemented locally on branch `feature/fbc-16-competition-setup`.
- Current `FBC-16` slice covers:
  - create competition screen and post-auth setup route
  - explicit join-existing-competition flow
  - per-competition starting goal capture
  - competition start date/timezone capture for new competitions
- `FBC-16` should now be treated as effectively wrapped locally with additional delivered scope:
  - end-date support and lifecycle-aware active-competition rules
  - competition-starts-soon onboarding state
  - past-competitions-only onboarding state
  - historical competition outcome opening from the past-competitions screen
  - persistence-first historical viewing with one-time backfill only when DB-derived state is missing
- Invitation-only competition visibility should be treated as follow-up work in `FBC-89`, not assumed as part of the base `FBC-16` join/setup slice.
- Broader competition selection/defaulting behavior should now move to `FBC-30`, not remain in `FBC-16`.
- Current preferred `FBC-30` direction after later discussion:
  - add an explicit competition-selection model for athletes with more than one competition membership
  - default the dashboard to the athlete's current active competition when there is exactly one current active competition
  - define the UX when there is more than one simultaneously active competition instead of silently picking one
  - allow switching competition context from the normal dashboard shell, not only from the past-competitions screen
  - keep historical selected competition context and current active competition context distinct in code and UX
  - make sure any selection/defaulting behavior applies cleanly across leaderboard, honour roll, history, summary, and future ZenBot stats context
 - Current local `FBC-30` implementation status:
   - branch created: `feature/fbc-30-competition-selection`
   - implementation is now complete enough to wrap locally:
     - multi-active competition detection
     - `COMPETITION_SELECTION_REQUIRED` onboarding state
     - chooser screen for multiple active competitions
     - dashboard-shell competition switching links in the off-canvas/header
     - past competitions listed in the dashboard shell and chooser for historical outcome switching
     - one current active competition defaults into selected competition context automatically
     - explicit historical competition selection is preserved instead of being overwritten by the active default
- Current preferred `FBC-91` direction after later discussion:
  - stop rebuilding a whole athlete from Strava on webhook create/update/delete in persistent mode
  - make webhook mutation logic update activity/derived state per linked competition instead of refetching all athlete activity data
  - replace any remaining hardcoded `competition_id = 1` webhook-era persistence helpers with competition-aware helpers
  - persist per-athlete watermarks for:
    - latest fully imported activity start time
    - last full reconciliation time
  - startup/restart behavior should fetch incrementally from Strava using the last imported activity-start watermark with a small overlap window
  - webhook events remain the source of truth for edits/deletes of older activities
  - add a slower periodic reconciliation so each athlete gets a full linked-competition refresh about every 3 to 4 days
  - reconciliation must update all linked competitions for that athlete, not only the current active one
  - if practical, store the per-athlete watermark state close to athlete persistence so restart logic does not depend on in-memory caches
- Current preferred `FBC-89` direction:
  - the competition-selection/join experience should only show competitions where the logged-in athlete has an invite
  - invitation rules should be defined before broadening competition discovery behavior
- Separate local bugfix branch/commit prepared for the session lifetime fix:
  - `bugfix/session-persistence`
  - `546c063`
- Both branches were pushed.
- PRs still need to be created manually or from an environment with PR-creation capability.
 - Latest local branch after `FBC-16` closeout is now `feature/fbc-30-competition-selection` for in-progress multi-competition selection work.

Resume backlog grooming from the remaining backlog, starting with either:

1. `FBC-60` if the next conversation should focus on hosting/platform/Azure direction, or
2. `FBC-89` first, then the remaining admin tickets (`FBC-90`, `FBC-14`), or
3. the smaller remaining backlog items (`FBC-46`, `FBC-26`, `FBC-27`, `FBC-28`, `FBC-29`, `FBC-33`, `FBC-34`, `FBC-52`, `FBC-24`) to finish making the board fully prompt-ready.

If backlog grooming is paused in favor of testing work, resume `FBC-40` from the current ~68% state before major persistence refactors.

Updated recommendation after later discussion:

1. Treat `FBC-40` as done.
2. Bring `FBC-22` forward as the next major technical ticket.
3. Keep `FBC-32` directly adjacent to `FBC-22` as the persistence foundation required for that change.
4. Then bring forward competition-boundary work, starting with `FBC-30`.

## Session style preference captured

The user asked for answers about 50 percent shorter than prior defaults.

Additional workflow preference captured later:

- The user wants repo workflow patterns that are also transferable to workplace AI usage, not just project-specific convenience.
- A lightweight repo workflow was introduced around:
  - `SESSION_HANDOFF.md`
  - `BACKLOG_GROOMING_HANDOFF.md`
  - `CONTEXT.md`
  - `docs\adr\`

Suggested reusable session starter prompt:

```text
Use the attached session handoff / checkpoint as the primary context for where we left off.

Working style:
- Keep answers about 50% shorter than your default.
- Be direct and practical.
- Prefer recommendations over long option lists.
- When discussing backlog, architecture, or sequencing, use my existing tickets first before suggesting new ones.
- Work one ticket at a time unless I explicitly ask for a broader plan.
- For each ticket, give me:
  1. a refined goal
  2. a prompt I can paste into the ticket
  3. suggested ordering/dependencies
  4. key risks or scope boundaries
- Flag when a ticket should be renamed, split, or moved in order.
- Don’t assume a repo-specific setup unless the handoff says so.
- If there is a better strategic direction, say so clearly and briefly.
```
