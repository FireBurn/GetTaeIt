# Get Tae It privacy policy

Last updated: 11 August 2026.

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
- **Firebase backup:** optional and only active after you sign in. Signed-in
  task data is mirrored to your Firebase account under `users/{uid}/tasks` so
  it can be restored on another device.

## What is not sold or used for advertising

Get Tae It does not sell task data, use it for advertising, or run third-party
analytics/advertising SDKs.

## Your choices

You can deny microphone or location permissions and still use the core task
planner. You can sign out to stop Firebase backup. To remove local data, clear
the app's storage in Android Settings; this does not remove an optional cloud
backup, which must be deleted from the signed-in Firebase account.

## Before release

The published Play Store version must link to this policy, include complete
Data Safety answers, and expose in-app export/deletion controls rather than
relying solely on Android's app-storage control.
