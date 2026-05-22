# Backlog grooming handoff

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
13. `FBC-12` Complete registration flow.
14. `FBC-56` Strava link button should only appear when the user is authenticated and ready to link Strava, and should no longer act as an enrolment shortcut.
15. `FBC-54` Implement global and competition-scoped authorization model.
16. `FBC-16` Set up screens to create competition if athlete is not part of any competitions, set up start week, and set up start goal per competition.
17. `FBC-30` Allow an athlete to belong to multiple competitions.
18. `FBC-37` Allow competition-specific eligible sports while preserving "All Sports Equal" as the default model.
19. `FBC-38` Specify relative distances per competition.
20. `FBC-22` Replace in-memory activity summary with persisted normalized activities and derived stats.

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

### Product philosophy

- "All Sports Equal" stays the main phrase.
- Meaning clarified as: all sports are welcome, all sports are valid effort, and they should be measured fairly/equally by default.
- Competition-specific restrictions and ratio rules are still allowed.

### Persistence direction

- The large in-memory summary structure should eventually be replaced.
- Best-practice direction is:
  - persist normalized activities
  - persist derived stats separately
  - use webhook events for incremental updates
  - use scheduled full sync as reconciliation/backfill
- Do not mirror the full Strava JSON shape as the main table design.
- Raw Strava payload storage was considered unnecessary for now.
- A safe fixture based on the current summary shape was created at `src\test\resources\fixtures\memory-summary-sanitized.json` for future `FBC-40` tests.
- A second focused fixture was added at `src\test\resources\fixtures\memory-summary-goal-cases.json` for goal-specific test cases.

## FBC-40 unit test progress

Work was started directly on `FBC-40`.

### Test files added

- `src\test\java\com\frankies\bootcamp\model\WeeklyPerformanceTest.java`
- `src\test\java\com\frankies\bootcamp\model\PerformanceResponseTest.java`
- `src\test\java\com\frankies\bootcamp\service\ActivityProcessServiceTest.java`
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

Resume backlog grooming from the remaining backlog, starting with either:

1. `FBC-60` if the next conversation should focus on hosting/platform/Azure direction, or
2. the newly added admin/invitation tickets (`FBC-89`, `FBC-90`, `FBC-14`), or
3. the smaller remaining backlog items (`FBC-46`, `FBC-26`, `FBC-27`, `FBC-28`, `FBC-29`, `FBC-33`, `FBC-34`, `FBC-52`, `FBC-24`) to finish making the board fully prompt-ready.

If backlog grooming is paused in favor of testing work, resume `FBC-40` from the current ~68% state before major persistence refactors.

## Session style preference captured

The user asked for answers about 50 percent shorter than prior defaults.

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
