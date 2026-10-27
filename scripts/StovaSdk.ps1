# Shared paths for Stova's scripts. Dot-source it: . "$PSScriptRoot/StovaSdk.ps1"
# Finds the Android SDK from ANDROID_HOME or ANDROID_SDK_ROOT, else from the usual install folder.
$StovaRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$OnWindows = $IsWindows -or $PSVersionTable.PSEdition -eq 'Desktop'

$candidates = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)
if ($OnWindows) { $candidates += 'C:\Android\sdk', (Join-Path "$env:LOCALAPPDATA" 'Android\Sdk') }
elseif ($IsMacOS) { $candidates += Join-Path $HOME 'Library/Android/sdk' }
else { $candidates += Join-Path $HOME 'Android/Sdk' }
$StovaSdk = $candidates | Where-Object { $_ -and (Test-Path (Join-Path $_ 'platform-tools')) } | Select-Object -First 1
if (-not $StovaSdk) { throw 'Fann ikkje Android SDK. Set ANDROID_HOME til SDK-mappa (ho har platform-tools).' }
$StovaSdk = (Resolve-Path $StovaSdk).Path

$exe = if ($OnWindows) { '.exe' } else { '' }
$bat = if ($OnWindows) { '.bat' } else { '' }
$Adb = Join-Path $StovaSdk "platform-tools/adb$exe"
$EmulatorTool = Join-Path $StovaSdk "emulator/emulator$exe"
$AvdManager = Join-Path $StovaSdk "cmdline-tools/latest/bin/avdmanager$bat"
$SdkManager = Join-Path $StovaSdk "cmdline-tools/latest/bin/sdkmanager$bat"
$StovaLogs = Join-Path $StovaRoot '.avd/logs'

# The owner's Spole emulators hold real user data. Stova's scripts never touch them.
$SpoleSerials = @('emulator-5560', 'emulator-5562', 'emulator-5564')
function Assert-NotSpole([string]$Serial) {
    if ($Serial -in $SpoleSerials) { throw "$Serial tilhøyrer Spole og har ekte brukardata. Bruk ein annan emulator." }
}
