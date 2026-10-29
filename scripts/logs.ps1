. "$PSScriptRoot\env.ps1"
# Dumps the app's own log lines (tag prefix "Auto"); add -c first via `adb logcat -c` to clear.
adb logcat -d -v time -s AutoSpike:V AutoRun:V AutoSched:V AndroidRuntime:E
