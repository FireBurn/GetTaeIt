# Get Tae It — delivery backlog

This is a prioritised backlog derived from the project brief and the current
codebase. Validate each item on a physical device before marking it complete.

## Release blockers

- [ ] Add Firebase project configuration locally (`app/google-services.json`),
  configure Auth/Firestore rules, and verify Google sign-in plus backup/restore
  on two devices.
- [ ] Define and test sync conflict rules: offline edits, deletion versus edit,
  concurrent completion, and repeated Firestore listener events.
- [x] Implement the two promised home-screen widgets (Work and Personal) with
  an empty state and a direct link into the planner.
- [x] Refresh installed widgets when the local task stream changes.
- [x] Add a direct widget completion action.
- [ ] Test widget completion and refresh behaviour on a physical device.
- [x] Add unit tests for context-mode selection, including vacation boundaries.
- [ ] Add unit tests for Firestore mapping/conflict handling.
- [x] Add unit tests for recurrence and meal-offset generation.
- [x] Add unit tests for dependency-cycle detection and dependency ranking.
- [ ] Run an end-to-end permission and notification audit on Android 13–16:
  notification, precise/background location, geofence recovery after reboot,
  and exact-alarm behaviour where applicable.
- [ ] Test phone ↔ Wear completion/snooze/voice sync and the Auto experience on
  actual supported hardware or emulators.
- [ ] Add a privacy policy, in-app explanation of location/Wi-Fi/AI use, data
  deletion/export controls, and Play Store data-safety declarations.

## High-value product work

- [x] Add **Goblin Mode**: a deliberate, reversible focus view that shows one
  highest-value available task and hides planning/navigation noise.
- [x] Finish gamification: surface XP meaningfully, use restrained completion
  celebration, and make streak rules transparent and forgiving.
- [ ] Build a proper shopping model (`ShoppingItemEntity`), shopping list UI,
  bought state, and supermarket location support. Add aisle sorting only after
  users can correct the category easily.
- [ ] Add a lightweight adaptive suggestion engine based on explicit task
  completions and snoozes. Make every suggestion dismissible and explainable;
  never silently change a reminder schedule.
- [x] Add an “energy/time available” check-in to filter the next task by
  estimated duration, including a low-energy option.
- [x] Let users set an explicit effort level rather than inferring low effort
  solely from duration.
- [x] Add a weekly review: completed wins, deferred tasks, stale tasks, and a
  simple reschedule/archival path that avoids guilt-inducing language.
- [x] Support built-in task templates for morning, leaving home, and work
  shutdown routines.
- [ ] Add shopping/cleaning templates and let users create reusable templates.

## Ecosystem polish

- [ ] Add a Wear complication for the one next actionable task and confirm the
  declared Tiles have complete implementations and previews.
- [ ] Add distinct, accessible Wear haptic patterns for start, reminder, and
  timer completion, with a user setting to disable them.
- [ ] Complete Auto’s driver-safe morning briefing and voice capture feedback;
  defer maps/errand interception until it can be validated against Car App
  library and Play review constraints.
- [ ] Make the commute mode use a real vehicle/connection signal where
  available, rather than only a time-window heuristic.
- [ ] Add accessibility coverage: TalkBack labels/order, font scaling, contrast,
  reduced motion, and non-colour-only Work/Personal cues.

## Additional ADHD-first ideas

- [ ] Add a **gentle re-entry** flow for tasks left untouched: offer “make it
  smaller”, “snooze without guilt”, “ask for help”, or “archive” instead of an
  overdue warning.
- [x] Add **body-doubling/focus sessions** with an optional countdown and a
  concise start/finish ritual; keep it useful without requiring social sharing.
- [ ] Add **friction notes** to tasks (what stopped me last time?) so the app
  can suggest the smallest next action or prerequisite.
- [x] Add **capture now, classify later**: one always-available inbox action
  that accepts text without forcing dates, priority, or context up front.
- [ ] Extend quick capture to voice input without forcing scheduling choices.
