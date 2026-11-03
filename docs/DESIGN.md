# Design notes

Why the app works the way it does. For usage see the [README](../README.md); for how it was verified see
[TESTING.md](TESTING.md).

## Goals

A personal, sideloaded Android app with a list of small **predefined** automations. Each can be enabled or disabled;
scheduled ones have a time of day and days of the week; all can be launched manually. The first one turns airplane
mode on at a set time and does nothing if it is already on. It must keep working in battery-saver mode and after a
reboot.

## Decisions

| Topic | Decision | Reason |
|---|---|---|
| Platform | Android only, Kotlin, Jetpack Compose (Material 3), single activity | Simple, current stack |
| Minimum Android | 13 (API 33) | Runtime notification permission and `USE_EXACT_ALARM` exist from there; tested on 15 only |
| Distribution | Sideload / build from source | Needs permissions and requests that Google Play policy restricts |
| Airplane mode | App is the default **digital assistant** | Normal apps cannot toggle it on Android 15 (see below) |
| Scheduling | `AlarmManager.setAlarmClock`, no foreground service | Exact, Doze-exempt, near-zero battery cost; the cost is a status-bar alarm icon |
| Missed runs | Always skipped | Running a night-time automation late is worse than not running it |
| Schedule shape | One time of day + weekday selection | Covers daily and weekday routines without a complex editor |
| Feedback | Run history in the app, notification only on failure | Quiet unless something is wrong |
| Wi-Fi / Bluetooth | Plain airplane mode, Android's own rules | No extra permissions |
| Persistence | Room for configuration and history | |
| Dependency injection | None (small lazy service locator) | The app is small |

## Why the digital-assistant role

Verified on Android 15: writing `Settings.Global.AIRPLANE_MODE_ON` (what `WRITE_SECURE_SETTINGS` allows) flips the flag
but the radios stay on, and apps cannot call the privileged connectivity API. Android does let the default assistant
launch the system's voice-control activity `Settings.ACTION_VOICE_CONTROL_AIRPLANE_MODE`, which switches the radios
properly. The app therefore implements a `VoiceInteractionService` with an invisible session
(`setUiEnabled(false)`), asks it to start that activity, then polls `AIRPLANE_MODE_ON` for up to 5 seconds to confirm.

Trade-off: only one assistant can be active, so choosing this app replaces the user's current assistant. The app
never records audio or reads the screen.

Alternatives considered: an accessibility service tapping the Quick Settings tile (fragile, depends on the OEM
layout); Shizuku or root (the shell privilege does not survive a reboot without root, which breaks the reboot
requirement).

## Architecture

```
automation/   Automation interface, AutomationRegistry, AutomationRunner, AutomationManager, AirplaneOnAutomation
assistant/    AutomationsVoiceInteractionService, AutomationsSessionService, ActionSession, StubRecognitionService
schedule/     AlarmScheduler, AlarmReceiver, BootReceiver, NextRunCalculator, Weekdays, ScheduleStore
data/         Room: AutomationConfig, RunLog, DAOs, repository
setup/        SetupChecker: the first-run checklist
notify/       Notifier: failure notification channel
ui/           Compose screens and AutomationsViewModel
```

### Automation model

`Automation` has a stable `id`, title, description, an optional default `Schedule` (null means manual-only),
`requirements()` (things the user must set up, each with an optional settings screen to open) and a suspending
`run()` returning `Success`, `Skipped` or `Failed` with a message. The configuration row (`automation_config`:
enabled, hour, minute, weekday bitmask) is created lazily; automations start disabled. Runs are written to `run_log`
(pruned to the last 200 rows) and serialized by a mutex.

### Scheduling

- One alarm-clock alarm per enabled scheduled automation, always for the **next** occurrence only
  (`NextRunCalculator`: pure, handles DST gaps and weekday masks). Each automation has a unique PendingIntent.
- `AlarmReceiver` re-arms the next occurrence first, then runs the automation, logs it, and posts a notification on
  failure. It uses `goAsync()`.
- `BootReceiver` re-arms everything on `LOCKED_BOOT_COMPLETED`, `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`
  and `MY_PACKAGE_REPLACED`. When storage is unlocked it also reconciles with Room and imports queued log entries.
- **Direct boot.** After a reboot, credential-protected storage (Room) is unreadable until the first unlock. The
  schedule is therefore mirrored into device-protected preferences (`ScheduleStore`), which both receivers use.
  Run results produced while locked are queued there and imported on unlock.
- `AutomationManager.update` is the single entry point for configuration changes (Room + mirror + alarm).

### Setup checklist

`SetupChecker` derives the list: each automation's requirements (e.g. the assistant role), notifications,
unrestricted battery use, and, on realme / OPPO / OnePlus, three manual items that cannot be detected (auto launch,
background activity, lock in Recents). `Run now` is disabled while a requirement is unmet.

## Known risks

- Vendor ROMs that kill background apps: mitigated by the checklist, not eliminated.
- An assistant panel flashing during a run: prevented by disabling the session UI; confirmed absent on the reference
  phone, though a run lasts only about 200 ms so a split-second flicker cannot be ruled out by screenshots.
- Other Android versions and manufacturers are untested.
