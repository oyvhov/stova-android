# Starts Stova's TV emulator and waits until it has booted. On first use it installs the Android TV
# system image if it is missing and creates the AVD in the repo's .avd folder (git-ignored).
# Default: Stova_TV on port 5580, API 30 (Android 11, as on the Shield), 1080p; API 34 on ARM hosts,
# which have no API 30 TV image. The screen is shown in scrcpy (mouse and keyboard) if installed.
# -EmulatorWindow: the emulator's own window instead. -Headless: no window at all (automation).
param(
    [switch]$Headless,
    [switch]$EmulatorWindow,
    [string]$Name = 'Stova_TV',
    [int]$Port = 5580,
    [int]$Api = 0
)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/StovaSdk.ps1"
$serial = "emulator-$Port"
Assert-NotSpole $serial
$env:ANDROID_AVD_HOME = Join-Path $StovaRoot '.avd'
$env:ANDROID_SDK_ROOT = $StovaSdk

if ((& $Adb devices) -match "^$serial\s+device") {
    Write-Output "$serial køyrer alt."
} else {
    if (-not (Test-Path (Join-Path $env:ANDROID_AVD_HOME "$Name.avd"))) {
        $arm = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture -eq 'Arm64'
        if ($Api -eq 0) { $Api = if ($arm) { 34 } else { 30 } }
        $abi = if ($arm) { 'arm64-v8a' } else { 'x86' }
        $image = "system-images;android-$Api;android-tv;$abi"
        if (-not (Test-Path $AvdManager)) { throw "Manglar Android SDK Command-line Tools ($AvdManager). Installer dei i SDK Manager." }
        if (-not (Test-Path (Join-Path $StovaSdk "system-images/android-$Api/android-tv/$abi"))) {
            Write-Output "Installerer $image (om lag 1 GB) ..."
            & $SdkManager --install $image
            if ($LASTEXITCODE) { throw "sdkmanager kunne ikkje installere $image." }
        }
        New-Item -ItemType Directory -Force $env:ANDROID_AVD_HOME | Out-Null
        'no' | & $AvdManager create avd -n $Name -k $image -d tv_1080p
        if ($LASTEXITCODE) { throw "avdmanager kunne ikkje lage $Name." }
    }
    New-Item -ItemType Directory -Force $StovaLogs | Out-Null
    $arguments = @('-avd', $Name, '-port', "$Port", '-no-snapshot-save', '-no-boot-anim', '-gpu', 'auto')
    if (-not $EmulatorWindow) { $arguments += '-no-window' }
    $start = @{
        FilePath = $EmulatorTool; ArgumentList = $arguments
        RedirectStandardOutput = Join-Path $StovaLogs "$Name-out.log"
        RedirectStandardError = Join-Path $StovaLogs "$Name-err.log"
    }
    if ($OnWindows) { $start.WindowStyle = if ($Headless) { 'Hidden' } else { 'Minimized' } }
    Start-Process @start

    & $Adb -s $serial wait-for-device
    $deadline = (Get-Date).AddMinutes(6)
    while ((& $Adb -s $serial shell getprop sys.boot_completed 2>$null) -ne '1') {
        if ((Get-Date) -gt $deadline) { throw "$Name starta ikkje innan 6 minutt. Sjå $StovaLogs." }
        Start-Sleep -Seconds 3
    }
    Write-Output "$Name er klar på $serial."
}
if (-not $Headless -and -not $EmulatorWindow) {
    try { & "$PSScriptRoot/Show-StovaScreen.ps1" -Serial $serial } catch { Write-Output $_.Exception.Message }
}
