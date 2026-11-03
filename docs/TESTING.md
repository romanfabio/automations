# Testing

## Automated

```bash
./gradlew testDebugUnitTest   # 8 JVM tests for NextRunCalculator: weekdays, DST gaps, fall-back, "exactly now"
./gradlew lintRelease         # no errors; only "newer version available" and targetSdk notes remain
```

Everything that touches Android (alarms, receivers, the assistant session, the UI) was verified by hand on a device;
there are no instrumented tests yet.

## On-device verification

Reference device: realme RMX3890, Android 15 (API 35), realme UI 15 (`RMX3890GDPR_15.C.43`), not rooted. Final build
checked: version 1.0.0.

| Scenario | Result |
|---|---|
| Scheduled run, screen off and PIN-locked | Pass. Fires at the exact second, airplane mode on, run logged, next-day alarm armed |
| Airplane mode already on | Pass. Logged as skipped |
| App process crashed before the alarm | Pass. The alarm cold-starts the app, the system re-binds the assistant, the run succeeds |
| Battery Saver on, battery-optimization exemption removed, Restricted standby bucket | Pass. Two alarms fired on the exact minute |
| Deep Doze | **Not tested.** The device never left `INACTIVE` with `dumpsys deviceidle force-idle` / `step deep` over USB. Alarm-clock alarms are Doze-exempt by design |
| Reboot | Pass. Alarm re-armed and fired after `adb reboot` (repeated) |
| Alarm firing **before the first unlock** after a reboot | **Not tested.** `sys.boot_completed` appears only after the PIN was entered. The code path uses only device-protected storage |
| Disabling an automation | Pass. Alarm cancelled, nothing fires |
| Weekday filter | Pass on device and in unit tests (Monday only, weekend only, a weekday whose time already passed) |
| Time-zone change | **Not tested on device** (the shell lacks the permission to change it). `TIMEZONE_CHANGED` re-arms; unit tests cover DST |
| No assistant role | Pass. Run logged as failed, notification shown, Run now disabled with the reason |
| Setup checklist | Pass. Each Open button, the notification and battery dialogs, the Done buttons, the "all good" state |
| Assistant panel visibility | Not visible in screenshots; no assistant window remains registered after a run |
| Crashes | None in the crash buffer during the whole test session |

### Things worth verifying by hand

- A run before the first unlock: enable a schedule a few minutes ahead, reboot, do not unlock until after that time,
  then unlock; the run should appear in History and airplane mode should have switched on.
- A real night with Doze: leave a 23:00 schedule enabled and check History in the morning.
- Other Android versions (13, 14, 16) and other manufacturers.

## adb recipes (debug builds)

```bash
# configure / run an automation through the real code paths
adb shell am broadcast -n io.github.romanfabio.automations/.debug.DebugReceiver -a CONFIG \
  --es id airplane_on --ez enabled true --ei hour 23 --ei minute 0 --ei days 127
adb shell am broadcast -n io.github.romanfabio.automations/.debug.DebugReceiver -a RUN --es id airplane_on

# assistant role
adb shell cmd role add-role-holder android.app.role.ASSISTANT io.github.romanfabio.automations
adb shell cmd role get-role-holders android.app.role.ASSISTANT

# airplane mode (setup / teardown)
adb shell cmd connectivity airplane-mode disable

# power scenarios
adb shell settings put global low_power 1                       # Battery Saver on (set back to 0 afterwards)
adb shell am set-standby-bucket io.github.romanfabio.automations restricted
adb shell dumpsys deviceidle whitelist -io.github.romanfabio.automations   # remove exemption; use + to restore
adb shell dumpsys battery unplug                                # use "dumpsys battery reset" afterwards

# state
adb shell 'dumpsys alarm | grep -F "Alarm{" | grep io.github.romanfabio.automations'
adb logcat -d -v time -s AutoSched:V AutoRun:V AutoAssist:V AndroidRuntime:E
```

Always restore the phone afterwards: airplane mode off, Battery Saver off, battery whitelist back, schedule disabled.
