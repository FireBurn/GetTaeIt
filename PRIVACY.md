# Get Tae It privacy policy

Last updated: 15 September 2026.

Get Tae It is designed to work locally first. Your tasks, routines, shopping
items, settings, and task-history signals are stored on your device.

## What the app uses

- **Tasks and routines:** stored in the on-device database.
- **Local AI:** where supported by the device, task breakdown and parsing use
  on-device Gemini Nano. A built-in local template fallback is used when it is
  unavailable.
- **Microphone:** only used after you choose voice capture; Android's speech
  recogniser returns the dictated text to the app.
- **Location and Wi-Fi:** optional, and only used to switch between Work and
  Personal modes or to trigger work-location reminders. You can remove work
  location and Wi-Fi settings in Gubbins at any time.
- **Paired watch:** if you install Get Tae It on a Wear OS watch, your active
  and recently finished tasks are copied to it over Google Play services' Wear
  connection so you can use them on your wrist. This goes directly between
  your own devices, not through our servers.
- **Firebase backup:** optional and only active after you sign in. Signed-in
  task data is mirrored to your Firebase account under `users/{uid}/tasks` so
  it can be restored on another device. When you delete a task, its backup is
  replaced by a small deletion marker holding only the task's ID and the time
  it was deleted, so other devices don't restore it. Markers are kept for 90
  days on your devices.
- **Focus sessions:** if you join a body-doubling session while signed in, the
  app records only when your session ends, under your account ID, so others
  can see how many people are focusing. No task content is shared.

## What is not sold or used for advertising

Get Tae It does not sell task data, use it for advertising, or run third-party
analytics/advertising SDKs.

## Your choices

You can deny microphone or location permissions and still use the core task
planner. You can sign out to stop Firebase backup.

Gubbins → Your data has **Export**, which shares all your tasks and settings as
JSON, and **Delete All**, which erases local tasks, settings and shopping items,
removes your Firebase task backup if you are signed in and online, and signs
you out. If you are offline at the time, the cloud backup stays until you
delete it from the signed-in Firebase account. Clearing the app's storage in
Android Settings also removes local data, but not the cloud backup.

## Before release

The published Play Store version must link to this policy and include complete
Data Safety answers.
