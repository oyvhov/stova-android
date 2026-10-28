# Installs the debug build of Stova on a TV or emulator and opens it. Builds it first if the APK is
# missing, or always with -Build. Keeps profiles, accounts and settings that are already there.
# -Tv: the fast build for real TVs (same package as debug, built like a release); see build.gradle.kts.
# -Demo (emulator only): fills a fresh install with fictional data, so Home has rows without any
# Jellyfin, Emby or Home Assistant: a «Demo» profile, five favourite apps, three cached media rows
# with local art and an offline smart-home card. It refuses to replace existing profiles.
param(
    [string]$Serial = 'emulator-5580',
    [switch]$Demo,
    [switch]$Build,
    [switch]$Tv
)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/StovaSdk.ps1"
Assert-NotSpole $Serial
if ($Tv -and $Demo) { throw 'Demodata finst berre i debug-bygget.' }
$apk = Join-Path $StovaRoot $(if ($Tv) { 'app/build/outputs/apk/tv/app-tv.apk' } else { 'app/build/outputs/apk/debug/app-debug.apk' })
if ($Build -or -not (Test-Path $apk)) {
    & "$PSScriptRoot/gradle.ps1" $(if ($Tv) { 'assembleTv' } else { 'assembleDebug' })
    if ($LASTEXITCODE) { throw 'Bygget feila.' }
}
& $Adb -s $Serial install -r $apk
if ($LASTEXITCODE) { throw "Kunne ikkje installere på $Serial." }

if ($Demo) {
    & $Adb -s $Serial logcat -c
    & $Adb -s $Serial shell am start -W -n app.stova.debug/app.stova.performance.PerformanceSeedActivity | Out-Null
    Start-Sleep -Seconds 3
    $crash = & $Adb -s $Serial logcat -d -s AndroidRuntime:E
    if ($crash -match 'Refusing to replace personal profiles') { throw 'Det finst alt eigne profilar her. Demodata blir berre lagt inn på ei fersk installering.' }
    if ($crash -match 'Demo seeding requires an emulator') { throw 'Demodata kan berre leggjast inn på ein emulator.' }
    Write-Output 'Demodata er lagt inn.'
} else {
    & $Adb -s $Serial shell monkey -p app.stova.debug -c android.intent.category.LEANBACK_LAUNCHER 1 2>&1 | Out-Null
}
Write-Output "Stova er opna på $Serial."
