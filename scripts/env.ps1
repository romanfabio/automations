# Dot-source from the other scripts: sets JAVA_HOME (Android Studio JBR), ANDROID_HOME and PATH.
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
$script:Root = Split-Path -Parent $PSScriptRoot
$script:Pkg = "io.github.romanfabio.automations"
