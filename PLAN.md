# Automations — Android app implementation plan

Personal, sideloaded Android app that runs small **predefined** automations, either on a
schedule or manually. Each automation can be enabled/disabled; scheduled ones have a
time of day + days of week. First automation: **turn airplane mode on at a set time**
(do nothing if it is already on).

## Decisions (from Q&A, 2026-10-29)

| Topic | Decision |
|---|---|
| Platform / stack | Android only, Kotlin, Jetpack Compose (Material 3), single activity |
| Package name | `io.github.romanfabio.automations` |
| Distribution | Sideload, personal use (no Play Store policy constraints) |
| Airplane mode mechanism | App is the **default Digital Assistant** (`VoiceInteractionService`) — same route LlamaLab Automate / MacroDroid use. Gemini isn't used on this phone. |
| Scheduling | `AlarmManager.setAlarmClock()` (exact, fires in Doze + battery saver; alarm icon in status bar is accepted). **No** always-on foreground service. |
| Missed runs (phone off, etc.) | **Always skip** — just schedule the next occurrence |
| Schedule shape | One time of day + selectable weekdays (default: every day) |
| Feedback | In-app run history + notification **only on failure** |
| Wi-Fi / BT | Plain airplane mode; Android's own radio rules apply |
| Phone locked and the assistant route fails | **Fail and notify** (no retry, no screen wake) |

## Facts verified on the test device

- Device: realme **RMX3890**, Android **15 (API 35)**, not rooted, no Shizuku.
- `settings put global airplane_mode_on 1` (what `WRITE_SECURE_SETTINGS` would allow) **does
  not** turn radios off: the flag flipped but LTE stayed `IN_SERVICE`. Reverted. → rules out
  the "just write the setting" approach and confirms the assistant route.
- `cmd connectivity airplane-mode enable` exists (shell-only; useful for test setup/teardown).
- Toolchain: Android SDK at `%LOCALAPPDATA%\Android\Sdk` (platforms 34–36.1), Android Studio
  installed. `adb` is **not on PATH**; `java` on PATH is **1.8** → builds must use Android
  Studio's JBR (`C:\Program Files\Android\Android Studio\jbr`) as `JAVA_HOME`.

## Known risks (from Automate users on Android 15)

- An empty assistant panel may flash each time the airplane action runs — we control the
  session UI, so we make it transparent and dismiss it immediately.
- Behaviour with the screen off / locked is **unknown** → the Phase 1 spike settles it.
- realme UI kills background apps aggressively → setup screen must get the app exempted.

---

## Architecture

```
app/
  MainActivity (Compose)
  ui/            AutomationListScreen, AutomationDetailScreen, SetupScreen
  automation/
    Automation.kt          interface + RunResult (Success / Skipped(reason) / Failed(reason))
    AutomationRegistry.kt  hard-coded list of predefined automations (add new ones here)
    airplane/AirplaneOnAutomation.kt
  data/          Room: AutomationConfig, RunLog; Repository
  schedule/
    NextRunCalculator.kt   pure function: (config, now, zone) -> next Instant  (unit tested)
    AlarmScheduler.kt      setAlarmClock / cancel per automation
    AlarmReceiver.kt       runs the automation, logs, reschedules
    BootReceiver.kt        BOOT_COMPLETED, LOCKED_BOOT_COMPLETED, TIME_SET,
                           TIMEZONE_CHANGED, MY_PACKAGE_REPLACED -> reschedule all
  assistant/
    AutomationsVoiceInteractionService   holds a static `instance` while bound by the system
    AutomationsSessionService / ActionSession   transparent session -> startVoiceActivity(...)
    StubRecognitionService               required for the app to be selectable as assistant
  notify/        Notifier (failure channel)
```

### Automation model

```kotlin
interface Automation {
    val id: String                 // stable key, e.g. "airplane_on"
    val title: String
    val description: String
    val kind: Kind                 // SCHEDULED or MANUAL
    fun requirements(ctx: Context): List<Requirement>   // e.g. AssistantRole
    suspend fun run(ctx: Context): RunResult
}
```

- **Persisted config** (Room `automation_config`): `id`, `enabled`, `hour`, `minute`,
  `daysMask` (bit per weekday). Rows are created lazily with defaults from the registry.
- **Run history** (Room `run_log`): `automationId`, `timestamp`, `trigger` (SCHEDULED/MANUAL),
  `result`, `message`. Pruned to the last 200 rows.
- Adding a future automation = one new class + one line in `AutomationRegistry`.

### Scheduling

- One `setAlarmClock` per enabled scheduled automation (PendingIntent request code derived
  from `id`; the "show" intent opens the app). Permission: `USE_EXACT_ALARM` (auto-granted, fine
  for sideload; no user prompt).
- `AlarmReceiver` uses `goAsync()`, runs the automation, writes a `RunLog`, posts a notification
  on failure, then schedules the next occurrence.
- Skip-missed policy: on boot/time change we only compute the **next future** occurrence.
- **Direct boot:** if the phone reboots at night and isn't unlocked, `BOOT_COMPLETED` doesn't
  arrive, so the alarm wouldn't be re-armed. Fix: mirror the schedules into
  **device-protected** SharedPreferences and handle `LOCKED_BOOT_COMPLETED` with a
  `directBootAware` receiver. Alarms that fire before first unlock run the automation without
  the Room DB and record the log later.
- Toggling enable/disable or editing the time reschedules immediately.

### Airplane mode automation

1. If `Settings.Global.AIRPLANE_MODE_ON == 1` → `Skipped("already on")`.
2. If the app doesn't hold `RoleManager.ROLE_ASSISTANT` or the service isn't bound → `Failed`.
3. `AutomationsVoiceInteractionService.instance.showSession(bundle(action=AIRPLANE_ON), 0)`.
4. The session (`onShow`) calls
   `startVoiceActivity(Intent(Settings.ACTION_VOICE_CONTROL_AIRPLANE_MODE)
   .putExtra(Settings.EXTRA_AIRPLANE_MODE_ENABLED, true))`, then `hide()`.
   Theme is transparent with no content → no visible panel.
5. Poll `AIRPLANE_MODE_ON` for up to ~5 s → `Success`, otherwise `Failed("not applied")`
   → failure notification.

### Setup / health screen

A checklist banner on the main screen (red items until fixed), each with a button:

- Default digital assistant = this app (opens `ACTION_VOICE_INPUT_SETTINGS`; the assistant role
  can't be requested with a dialog).
- Notifications permission (`POST_NOTIFICATIONS`).
- Ignore battery optimizations (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).
- realme specifics (manual, with deep links where they resolve): *Allow auto launch*,
  *Allow background activity*, lock the app in Recents.

### UI

- **List**: a card per automation: title, Scheduled/Manual chip, next run ("Tomorrow 23:30"),
  enable switch, **Run now** button (scheduled ones too), last result icon.
- **Detail**: description, time picker, weekday chips (scheduled only), requirements status,
  run history list.

---

## Phases

### Phase 0 — Project setup
- `git init`, `.gitignore`. Gradle wrapper, AGP + Kotlin + Compose BOM (current stable),
  `compileSdk 36`, `targetSdk 35`, `minSdk 33` (personal device is API 35).
- Package name / applicationId: `io.github.romanfabio.automations`.
- Small `scripts/` helpers (PowerShell) that set `JAVA_HOME` to the JBR and use the SDK's
  `adb.exe`: `build.ps1`, `install.ps1`, `logs.ps1`.

### Phase 1 — Feasibility spike (gate)
Bare app with only the assistant components and a "Turn airplane on" button.
Install, set it as assistant (`adb shell cmd role add-role-holder android.app.role.ASSISTANT io.github.romanfabio.automations`,
or via Settings), then check:
1. Screen on, unlocked → airplane turns on? Any visible panel?
2. Triggered from a delayed alarm with the **screen off, locked** → works?
3. After a reboot → service is bound again without opening the app?

Outcome decides how much of the "fail and notify when locked" path we need. If (1) fails
outright on realme UI, stop and reconsider (Accessibility tile tap is the fallback).

**Phase 1 result (done 2026-10-29): GO.** Project scaffold + spike (`assistant/`, `spike/`) built and tested on the RMX3890:
- Screen on, unlocked → airplane on in ~150 ms, radio `POWER_OFF`. ✔
- Alarm-clock alarm fired with screen **off and PIN-locked** → worked, screen stayed asleep. ✔ (so the "fail and notify when locked" path is not needed for now; keep it only as a generic failure path)
- "Already on" → skipped. ✔
- After `adb reboot`: assistant service re-bound by itself, no app launch needed; action worked. ✔ (not tested *before* first unlock: the phone was already unlocked when checked)
- Setting the role via `adb shell cmd role add-role-holder` works (handy for tests). The phone **already had Automate (LlamaLab) as assistant**; ours replaced it.
- Build notes: Compose BOM `2026.09.00` needs AGP 9.1 / compileSdk 37, so the project pins BOM `2026.06.01` with AGP 8.13.1, Kotlin 2.2.21, Gradle 8.14.3.
- Open: no visible panel was checked by screenshot yet; do it in Phase 4.

### Phase 2 — App skeleton
Room entities/DAO/repository, `Automation` interface + registry, Compose list + detail screens,
enable switch, Run now, history. Unit tests for `NextRunCalculator` (weekday masks, DST, day
rollover, "time already passed today").

**Phase 2 result (done 2026-10-29):** Room (config + run log), `Automation` interface/registry, `AutomationRunner`,
`NextRunCalculator` + `Weekdays`, Compose list and detail screens. Spike receiver removed. 8/8 unit tests pass.
Verified on the phone: list shows schedule/next run/last result, enable switch persists, **Run now** turns airplane
mode on and logs "Done · manual", detail screen shows time, weekday chips, history. Not yet exercised: the time picker
dialog and weekday chip taps. Automations start **disabled** by default; the default time is 23:00 (change in `AirplaneOnAutomation`).
Build notes: platform theme `Theme.DeviceDefault.DayNight` (no `Material.DayNight.NoActionBar` in the SDK), KSP 2.3.12 works with Kotlin 2.2.21.

### Phase 3 — Scheduling
`AlarmScheduler`, `AlarmReceiver`, `BootReceiver` (+ direct-boot mirror), rescheduling on
edit/toggle/time change. Failure notification channel.

**Phase 3 result (done 2026-10-29):** `AlarmScheduler` (one `setAlarmClock` per enabled automation, only the next
occurrence armed), `AlarmReceiver` (re-arms first, then runs, logs, notifies on failure), `BootReceiver`
(`LOCKED_BOOT_COMPLETED`, `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`),
`ScheduleStore` (device-protected mirror + queue of run-log entries made while Room is locked), `AutomationManager`
(single entry point: Room + mirror + alarm), `Notifier` (failure channel), debug-only `DebugReceiver` for adb:
`adb shell am broadcast -n io.github.romanfabio.automations/.debug.DebugReceiver -a CONFIG --es id airplane_on --ez enabled true --ei hour 23 --ei minute 0 --ei days 127`.
Verified on the phone: alarm fires at the exact minute with the screen off + PIN locked (✔); with the process
crashed beforehand, the system re-binds the assistant and the run still succeeds (✔); next-day alarm is armed after each
fire (✔); disabling cancels the alarm (✔); with another app as assistant the run is logged **Failed** and a
notification is posted (✔); after `adb reboot` the alarm is re-armed (✔).
Correction to the earlier design note: `USE_EXACT_ALARM` **is** required for `setAlarmClock` on Android 14+ (without it the app crashed with a SecurityException), so it stays in the manifest.
Not verified: a fire *before the first unlock* after reboot (the phone was already unlocked when checked; the code path uses only device-protected storage). Battery-saver / Doze are covered in Phase 6.

### Phase 4 — Airplane mode automation
Production version of the spike logic, with skip/verify/fail handling as above.

**Phase 4 result (done 2026-10-29):** the production airplane automation was already in place from Phase 2/3
(skip if on → require assistant service (waits up to 5 s for the system to re-bind it after a process kill) → verify by polling
`AIRPLANE_MODE_ON` for 5 s → `Failed` + notification otherwise). This phase added: `ActionSession.setUiEnabled(false)` so the
session can never draw a panel (before it, a `VoiceInteractionSession` window stayed registered in `dumpsys window`; after it, none),
a debug `RUN` hook (`... DebugReceiver -a RUN --es id airplane_on`) that runs an automation with the app in the background, and the
log tag rename `AutoSpike` → `AutoAssist`. Verified on the phone with the Settings app in front: Run → Success, radios off,
airplane icon in the status bar; run while already on → Skipped. Visual caveat: a run takes ~150–250 ms, so screenshots only caught the
state afterwards; no panel was visible in any frame and none is registered any more. Not tested: the airplane toggle with Wi-Fi/Bluetooth
previously re-enabled by the user (plain airplane mode was the decision).

### Phase 5 — Setup / health screen
Checklist + deep links; disable "Run now" and show why when requirements are missing.

**Phase 5 result (done 2026-10-29):** `SetupChecker` builds the checklist: each automation's requirements (assistant role, with a
button that opens the system's "Digital assistant app" screen), Notifications (runtime permission dialog; falls back to the app's
notification settings if Android no longer shows it), Unrestricted battery use (system "Let app always run in background?" dialog),
and, on realme/OPPO/OnePlus only, three manual items (Allow auto launch, Allow background activity, Lock in Recents) with an "Open"
button to the app info page and a "Done" button, remembered in preferences (they can't be detected). `SetupCard` sits on top of the list,
open while something is missing and collapsible; state refreshes on every resume. "Run now" is disabled when an automation's
requirement is unmet (list and detail), with the reason shown. The launch-time notification prompt was removed.
Verified on the phone: card renders, assistant "Open" reaches the system screen and the item turns ✔ on return, notification dialog and
battery dialog appear and update the card, Run now greys out when the assistant role is removed.
Findings: no ColorOS auto-launch activity resolves on this ROM (RMX3890, 15.C.43), so the OEM items use the app info page plus written
instructions instead of guessed component names. Not verified: the "Done" buttons and the "all good" collapsed state (the OEM items are still pending).
Left on the test phone: notifications granted, battery exemption granted, Automations is the default assistant.

### Phase 6 — On-device verification (realme RMX3890)
| Scenario | How |
|---|---|
| Normal scheduled run | schedule +2 min, screen off, wait; check `run_log` + airplane state |
| Already in airplane mode | `cmd connectivity airplane-mode enable` first → expect `Skipped` |
| Doze | `adb shell dumpsys deviceidle force-idle`, then fire |
| Battery saver | `adb shell settings put global low_power 1` (and the realme toggle) |
| Reboot | `adb reboot`, don't unlock, check `dumpsys alarm` for our alarm; then unlock |
| Locked phone | expect success **or** failure notification, as decided in Phase 1 |
| Disabled automation | toggle off → `dumpsys alarm` shows no alarm |
| Weekday filter / time-zone change | unit tests + one manual time-zone change |

Teardown after each test: `adb shell cmd connectivity airplane-mode disable` (over USB,
so adb stays connected).

**Phase 6 result (done 2026-10-29, realme RMX3890, Android 15):**

| Scenario | Result |
|---|---|
| Normal scheduled run, screen off + PIN locked | ✔ fires at the exact second, airplane on, run logged, next-day alarm armed (Phase 3) |
| Already in airplane mode | ✔ `Skipped: Already in airplane mode` (Phase 4) |
| App process killed/crashed before the alarm | ✔ alarm cold-starts the app, system re-binds the assistant, run succeeds (Phase 3) |
| Battery Saver on + battery-optimization exemption removed + Restricted standby bucket | ✔ two alarms (20:56, 21:00) fired at the exact minute and succeeded |
| Deep Doze | ⚠ **not reachable**: `deviceidle force-idle` / `step deep` stay at `INACTIVE` on this ROM over USB, so Doze itself was not exercised. Alarm-clock alarms are Doze-exempt by design. |
| Reboot | ✔ alarm re-armed and fired at 21:06 after `adb reboot` (observed twice) |
| Reboot, alarm before first unlock | ⚠ **not verified**: `sys.boot_completed` only appears after the PIN is entered and the user unlocked the phone each time. Device-protected mirror + queued log path is implemented but only exercised for the unlocked case. |
| Disabled automation | ✔ alarm cancelled (0 pending alarms), no fire |
| Weekday filter | ✔ on device: Mon-only → 2026-11-02 23:00, Sat+Sun 08:00 → 2026-10-31, Thu-only after the time → 2026-11-05 (plus unit tests) |
| Time-zone change | ⚠ **not verified on device**: shell lacks `SUGGEST_MANUAL_TIME_AND_ZONE`. `TIMEZONE_CHANGED` → `rearmAllFromStore()` is implemented; unit tests cover DST gaps and fall-back. |
| Failure path (no assistant role) | ✔ `Failed` logged + notification (Phase 3) |
| Setup checklist | ✔ every button and the "all good" state (Phase 5) |
| Crashes | none in the crash buffer during the whole session |

Left on the phone: Automations is the default assistant, notifications and unrestricted battery use are granted, the airplane
automation is **disabled** (23:00 every day), no alarms pending, airplane mode off, battery/Battery Saver/time zone restored.
The three realme manual items are intentionally left unchecked until you do them in system settings.

### Still open / ideas for later
- Verify the before-first-unlock path by hand: enable a schedule 5 min ahead, reboot, don't unlock, unlock after the time → the run should appear in History.
- Verify a real Doze night (leave it enabled at 23:00 and check History next morning).
- Release build + signing key (only needed if you want a non-debug APK; the `DebugReceiver` is not in release builds).
- Next automations go in `AutomationRegistry`.
