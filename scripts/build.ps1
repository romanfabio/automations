. "$PSScriptRoot\env.ps1"
Push-Location $Root
try { & .\gradlew.bat --console=plain assembleDebug @args } finally { Pop-Location }
