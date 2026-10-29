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

### Phase 4 — Airplane mode automation
Production version of the spike logic, with skip/verify/fail handling as above.

### Phase 5 — Setup / health screen
Checklist + deep links; disable "Run now" and show why when requirements are missing.

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
