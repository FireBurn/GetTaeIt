# Get Tae It

Get Tae It is an offline-first, context-aware Android planner for people with
ADHD. Its voice is direct, warm, and lightly Scottish: helpful without being
patronising. The core product boundary is protecting personal time from work
overwhelm.

## Repository map

- `app/` — phone application and Compose Material 3 UI. Owns Android UI,
  notifications, authentication UI, and permission flows.
- `shared/` — Android library containing Room data, repositories, domain
  logic, Firebase integration, WorkManager jobs, geofencing, and AI strategies.
- `wear/` — Wear OS "Doer" application: glanceable tasks, quick actions, and
  voice capture.
- `auto/` — Android Auto "Transitioner" application. Keep all UI template
  based and driver-safe.

The app uses Kotlin, Jetpack Compose, Hilt, Room, Coroutines/Flow, Firebase
Auth/Firestore, WorkManager, geofencing, the Wearable Data Layer, and optional
Gemini Nano assistance. Minimum SDK is 26; compile SDK is 37 and target SDK is 36.

## Architectural rules

- Keep `shared` independent of UI concerns. Put Android screen code in the
  appropriate device module.
- Keep the app offline-first: write to Room first, then mirror remotely. Do
  not make task creation or completion wait for a network call.
- Use repositories and injected dependencies; do not reach into Room,
  Firebase, or Android system services directly from composables.
- Model asynchronous state with `Flow`/coroutines and collect it lifecycle
  aware in UI.
- Preserve the Work/Personal boundary. Work tasks must not leak into personal
  time except for genuinely critical, explicitly supported cases.
- Avoid heavy AI work on Wear or Auto. Send intent/voice input to the phone or
  use a deterministic fallback when an AI model is unavailable.
- Do not add `google-services.json`, API keys, tokens, local databases, or
  keystores to Git. Firebase features must degrade gracefully when the app is
  not configured.

## Domain conventions

- `TaskContext` is `WORK`, `PERSONAL`, or `ANY`; `AppMode` also has `COMMUTE`.
- Priorities run from `1` (urgent) to `5` (someday).
- Task dependencies are blockers. Never introduce a cycle; use
  `DependencyGraph` for checks and ranking.
- Recurring tasks distinguish `IGNORABLE` missed instances from `PERSISTENT`
  ones. Snoozing is not cancellation and should not break a streak.
- Meal-prep task offsets are relative to the main event through
  `offsetReferenceId` and `offsetDuration`.

## Build and validation

Use the Gradle wrapper, not a system Gradle installation. In restricted
environments, keep the Gradle cache outside the repository:

```bash
GRADLE_USER_HOME=/tmp/gettaeit-gradle ./gradlew test --console=plain
GRADLE_USER_HOME=/tmp/gettaeit-gradle ./gradlew :app:assembleDebug --console=plain
```

`test` currently verifies compilation but has little/no unit-test coverage.
For changes to device behaviour, also test on real hardware or an emulator:
notifications, runtime/background location permission, Wi-Fi context changes,
geofences, authentication, Firestore sync, Wear Data Layer, and Android Auto.

## Working practices

- Read existing code before changing it; the working tree may contain valuable
  uncommitted work.
- Make small, coherent changes. Run formatting/compilation checks appropriate
  to the touched modules.
- Prefer accessible, low-friction UX: large targets, short copy, clear empty
  states, and a single obvious next action.
- Keep Auto screens concise and driver-safe. Keep Wear screens glanceable;
  avoid long lists and configuration flows there.
- When adding a feature, include failure/offline behaviour and a test where
  practical.
