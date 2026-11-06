# CLAUDE.md

Guidance for working in this repository. User-facing docs are in [README.md](README.md); the reasoning behind the
design is in [docs/DESIGN.md](docs/DESIGN.md) and the device test log in [docs/TESTING.md](docs/TESTING.md).

## What this is

Android app (Kotlin, Compose, Room) that runs predefined automations on a schedule or on demand. Only one
automation exists today: turn airplane mode on. Android-only; **minSdk 33 (Android 13)**, targetSdk 35,
compileSdk 36, versionName 1.0.0. Package and applicationId: `io.github.romanfabio.automations`.

## Commands

```bash
./gradlew testDebugUnitTest          # unit tests (JVM; only schedule math is covered)
./gradlew lintRelease                # must stay free of errors; warnings about newer versions are expected
./gradlew assembleDebug              # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease            # unsigned
```

- Needs **JDK 17+**. The `java` on a dev machine's PATH may be too old: on Windows use `scripts/build.ps1`,
  `scripts/install.ps1`, `scripts/logs.ps1` (they call `scripts/env.ps1`, which uses Android Studio's bundled JDK and
  puts `adb` on PATH).
- Log tags: `AutoSched` (alarms, boot), `AutoRun` (runs), `AutoAssist` (assistant session).

## Architecture in one screen

- `automation/`: `Automation` interface + `AutomationRegistry` (hard-coded list). `AutomationRunner` runs one
  and records a `RunLog`. `AutomationManager` is the **only** place that changes configuration.
- `schedule/`: `AlarmScheduler` (one `setAlarmClock` per enabled automation, only the next occurrence),
  `AlarmReceiver` (fire), `BootReceiver` (boot, clock, time zone, app update), `NextRunCalculator` (pure, unit tested),
  `ScheduleStore` (device-protected mirror of the config).
- `assistant/`: `AutomationsVoiceInteractionService` (bound by the system while the app is the default assistant),
  `ActionSession` (invisible session that launches the system voice-control activity), `StubRecognitionService`
  (required for the app to be selectable as assistant).
- `data/`: Room (`automation_config`, `run_log`). `setup/`: first-run checklist. `ui/`: Compose screens, one
  `AutomationsViewModel`. No DI framework: `AutomationsApplication` is a small lazy service locator.

## Invariants (do not break these)

1. **Config changes go through `AutomationManager.update`**, which writes Room, the device-protected mirror
   (`ScheduleStore`) and re-arms the alarm together. Never write one of the three alone.
2. **Receivers must not touch Room before the first unlock.** After a reboot, `AlarmReceiver` and `BootReceiver`
   can run while storage is locked: they read `ScheduleStore` only. `AutomationRunner` queues log entries in the store
   while locked; `AutomationManager.resyncAll()` imports them. Anything new in a receiver must respect this.
3. **`AlarmReceiver` re-arms the next occurrence before running** the automation, so a crash cannot lose the schedule.
4. **Missed runs are skipped by design**: never add catch-up logic.
5. **`ActionSession` must keep `setUiEnabled(false)`**; otherwise an assistant panel can appear during a run.
6. **Airplane mode is toggled through the assistant session, not by writing `Settings.Global`.** On Android 15 the
   write flips the flag but leaves the radios on (verified). Do not "simplify" this to `WRITE_SECURE_SETTINGS`.
7. `setAlarmClock` throws a `SecurityException` on Android 14+ without `USE_EXACT_ALARM`; keep it in the manifest.
8. An automation's `id` is its storage key. Never rename a shipped id.
9. No `INTERNET` permission and no analytics. Adding either needs an explicit decision and a README privacy update.

## Adding an automation

Implement `Automation` (id, title, description, `defaultSchedule` or null for manual-only, `requirements()`, `run()`
returning `Success` / `Skipped` / `Failed`), add it to `AutomationRegistry`, and keep `run()` idempotent and quick
(it runs inside a `goAsync()` receiver, which has about 10 seconds). New automations start **disabled**. Add unit
tests for any pure logic. If it needs a new permission or system setting, add a `Requirement` with a `fixIntent`
so the Setup card guides the user.

## Testing on a device

Debug builds include `app/src/debug/.../DebugReceiver` (not in release builds). With a device on `adb`:

```bash
# change config (omitted extras keep their value; days is a bitmask, Mon = 1 ... Sun = 64, all = 127)
adb shell am broadcast -n io.github.romanfabio.automations/.debug.DebugReceiver -a CONFIG \
  --es id airplane_on --ez enabled true --ei hour 23 --ei minute 0 --ei days 127
# run now, as if the Run now button was tapped
adb shell am broadcast -n io.github.romanfabio.automations/.debug.DebugReceiver -a RUN --es id airplane_on
# grant / inspect the assistant role; toggle airplane mode from the shell for setup and teardown
adb shell cmd role add-role-holder android.app.role.ASSISTANT io.github.romanfabio.automations
adb shell cmd role get-role-holders android.app.role.ASSISTANT
adb shell cmd connectivity airplane-mode disable
# pending alarms of this app
adb shell 'dumpsys alarm | grep -F "Alarm{" | grep io.github.romanfabio.automations'
```

Always switch airplane mode back off and disable the schedule after a test, and note that granting the assistant
role to this app removes the phone's previous assistant. `adb` over USB cannot force deep Doze on the reference
phone. More scenarios: [docs/TESTING.md](docs/TESTING.md).

## Conventions

- Official Kotlin style. Comments explain *why* (platform quirks, invariants), not what the code does.
- Compose UI uses Material 3 with system dynamic colors; no custom fonts or image assets. App icon is original vector art.
- Dependencies are pinned in `gradle/libs.versions.toml`. The Compose BOM is deliberately **2026.06.01**: newer BOMs need
  AGP 9.1 and compileSdk 37. When changing any dependency, check its license (Apache-2.0 / MIT / BSD only) and update
  `THIRD_PARTY_LICENSES.md`.
- Keep the minimum Android version in sync in `app/build.gradle.kts`, `README.md` and this file.
- Version bumps: `versionName` / `versionCode` in `app/build.gradle.kts`, plus the version line in `README.md`.

## Releasing

Process and checklist: [docs/RELEASING.md](docs/RELEASING.md). Release signing is optional in Gradle: it reads the file named
by the `AUTOMATIONS_KEYSTORE_PROPERTIES` environment variable (kept outside the repo), otherwise the release APK is built
unsigned. Never put a keystore, its password or that properties file in the repository, and never publish an APK that was
not installed and checked on a phone first.

## Never commit

Keystores and signing files (`*.jks`, `*.keystore`, `keystore.properties`), `local.properties`, `.env*`,
`google-services.json`, device serial numbers or personal paths, build output, APKs. `.gitignore` covers these;
check `git status` before committing.
