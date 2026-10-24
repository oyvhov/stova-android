# Starts Stova's own TV emulator (Stova_TV, API 30 Android TV, port 5580) and waits until it has
# booted. Never touches Spole's emulators (5560/5562/5564). Creates the AVD on first use.
param([switch]$Headless)
$ErrorActionPreference = 'Stop'
$env:ANDROID_AVD_HOME = 'C:\Launcher\.avd'
$env:ANDROID_SDK_ROOT = 'C:\Android\sdk'
$sdk = 'C:\Android\sdk'
$adb = "$sdk\platform-tools\adb.exe"
$serial = 'emulator-5580'

if (-not (Test-Path "$env:ANDROID_AVD_HOME\Stova_TV.avd")) {
    New-Item -ItemType Directory -Force $env:ANDROID_AVD_HOME | Out-Null
    'no' | & "$sdk\cmdline-tools\latest\bin\avdmanager.bat" create avd -n Stova_TV -k 'system-images;android-30;android-tv;x86' -d tv_1080p
}

$args = @('-avd', 'Stova_TV', '-port', '5580', '-no-snapshot-save', '-no-boot-anim', '-gpu', 'auto')
if ($Headless) { $args += '-no-window' }
Start-Process -FilePath "$sdk\emulator\emulator.exe" -ArgumentList $args -WindowStyle Minimized `
    -RedirectStandardOutput 'C:\Launcher\.superpowers\emulator-out.log' -RedirectStandardError 'C:\Launcher\.superpowers\emulator-err.log'

& $adb -s $serial wait-for-device
$deadline = (Get-Date).AddMinutes(6)
while ((& $adb -s $serial shell getprop sys.boot_completed 2>$null) -ne '1') {
    if ((Get-Date) -gt $deadline) { throw 'Stova_TV starta ikkje innan 6 minutt.' }
    Start-Sleep -Seconds 3
}
Write-Output "Stova_TV er klar på $serial."
