. "$PSScriptRoot\env.ps1"
# Dumps the app's own log lines (tag prefix "Auto"); add -c first via `adb logcat -c` to clear.
adb logcat -d -v time -s AutoAssist:V AutoRun:V AutoSched:V AndroidRuntime:E
