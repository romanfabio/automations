# Dot-source from the other scripts. Sets JAVA_HOME, ANDROID_HOME and PATH (adb) for a Windows dev machine.
# Android Studio's bundled JDK is preferred when present; otherwise the JAVA_HOME you already have
# is used and must point to JDK 17 or newer. ANDROID_HOME defaults to the standard SDK location.
$studioJdk = "$env:ProgramFiles\Android\Android Studio\jbr"
if (Test-Path "$studioJdk\bin\java.exe") { $env:JAVA_HOME = $studioJdk }
if (-not $env:JAVA_HOME) { throw "JAVA_HOME is not set and Android Studio's JDK was not found." }

if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk" }
if (-not (Test-Path "$env:ANDROID_HOME\platform-tools")) { throw "Android SDK not found at $env:ANDROID_HOME. Set ANDROID_HOME." }

$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
$script:Root = Split-Path -Parent $PSScriptRoot
$script:Pkg = "io.github.romanfabio.automations"
