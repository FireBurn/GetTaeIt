# Get Tae It — delivery backlog

This is a prioritised backlog derived from the project brief and the current
codebase. Code items are checked off when implemented and automated checks pass;
device and release-console validation remain open until actually completed.

## Release blockers

- [x] Add local Firebase project configuration (`app/google-services.json`)
  and author Firestore rules (`firestore.rules`).
- [x] Configure phone, Wear, and Auto release builds with opt-in local
  signing and R8/resource shrinking; share the signing key across paired apps.
- [ ] Create and protect the production signing key, then verify the release
  bundle and matching phone/Wear signatures before publishing.
- [ ] Deploy the Firestore rules, enable Google sign-in for the Firebase project,
  and verify sign-in plus backup/restore on two devices.
- [x] Define and test sync conflict rules: offline edits, deletion versus edit,
  concurrent completion, and repeated Firestore listener events.
- [x] Implement the two promised home-screen widgets (Work and Personal) with
  an empty state and a direct link into the planner.
- [x] Refresh installed widgets when the local task stream changes.
- [x] Add a direct widget completion action.
- [ ] Test widget completion and refresh behaviour on a physical device.
- [x] Add unit tests for context-mode selection, including vacation boundaries.
- [x] Add unit tests for Firestore mapping/conflict handling.
- [x] Add unit tests for recurrence and meal-offset generation.
- [x] Keep recurring reminder alarms current, suppress stale/snoozed reminders,
  and reschedule after notification actions.
- [x] Add unit tests for dependency-cycle detection and dependency ranking.
- [ ] Run an end-to-end permission and notification audit on Android 13–16:
  notification, precise/background location, geofence recovery after reboot,
  and exact-alarm behaviour where applicable.
- [ ] Test phone ↔ Wear completion/snooze/voice sync and the Auto experience on
  actual supported hardware or emulators.
- [ ] Publish [PRIVACY.md](PRIVACY.md), complete Play Store Data Safety
  declarations, and verify cloud-backup deletion. In-app explanation and local
  export/deletion controls are implemented.

## High-value product work

- [x] Add **Goblin Mode**: a deliberate, reversible focus view that shows one
  highest-value available task and hides planning/navigation noise.
- [x] Finish gamification: surface XP meaningfully, use restrained completion
  celebration, and make streak rules transparent and forgiving.
- [x] Build a proper shopping model (`ShoppingItemEntity`), shopping list UI,
  bought state, and supermarket location support. Add aisle sorting only after
  users can correct the category easily.
- [x] Add a lightweight adaptive suggestion engine based on explicit task
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
- [x] Add user-created reusable templates. Shopping and cleaning resets are included.

## Ecosystem polish

- [x] Add a Wear complication for the one next actionable task, with preview
  data.
- [x] Add a Wear Tile showing the next task or an empty state, with Done,
  Snooze, and open-app actions.
- [ ] Validate distinct, accessible Wear haptic patterns for reminder and timer
  completion on a paired watch. Start, completion, snooze, reminder and timer
  patterns plus a user setting are implemented.
- [x] Complete Auto’s driver-safe morning briefing and voice capture feedback;
  defer maps/errand interception until it can be validated against Car App
  library and Play review constraints.
- [x] Make the commute mode use a real vehicle/connection signal where
  available, rather than only a time-window heuristic.
- [x] Add accessibility coverage: TalkBack labels/order, font scaling, contrast,
  reduced motion, and non-colour-only Work/Personal cues.

## Additional ADHD-first ideas

- [x] Add a **gentle re-entry** flow for tasks left untouched: offer “make it
  smaller”, “snooze without guilt”, “ask for help”, or “archive” instead of an
  overdue warning.
- [x] Add **body-doubling/focus sessions** with an optional countdown and a
  concise start/finish ritual; keep it useful without requiring social sharing.
- [x] Add **friction notes** to tasks (what stopped me last time?) so the app
  can suggest the smallest next action or prerequisite.
- [x] Add **capture now, classify later**: one always-available inbox action
  that accepts text without forcing dates, priority, or context up front.
- [x] Extend quick capture to voice input without forcing scheduling choices.
