# Opens a scrcpy window onto Stova's TV, so it can be driven with the mouse and keyboard:
# arrows = D-pad, Enter = OK, right-click = Back, middle-click = Home, Alt+M = Menu.
# Default is Stova's emulator; pass -Serial <ip>:5555 for a TV on the network.
param([string]$Serial = 'emulator-5580')
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/StovaSdk.ps1"
Assert-NotSpole $Serial
$env:ADB = $Adb

$scrcpy = (Get-Command scrcpy -ErrorAction SilentlyContinue).Source
if (-not $scrcpy -and $OnWindows) {
    $scrcpy = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Filter scrcpy.exe -Recurse -Depth 3 -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $scrcpy) { throw 'scrcpy er ikkje installert (Windows: winget install Genymobile.scrcpy, macOS: brew install scrcpy).' }

New-Item -ItemType Directory -Force $StovaLogs | Out-Null
function Start-Scrcpy([string[]]$Audio) {
    Start-Process -PassThru -FilePath $scrcpy -WorkingDirectory (Split-Path $scrcpy) -ArgumentList (@(
        '-s', $Serial, '--window-title', "`"Stova TV ($Serial)`"") + $Audio) `
        -RedirectStandardOutput (Join-Path $StovaLogs 'scrcpy.log') -RedirectStandardError (Join-Path $StovaLogs 'scrcpy.err.log')
}
# AAC, not scrcpy's default Opus: the Android TV emulator image has no Opus encoder.
$window = Start-Scrcpy @('--audio-codec=aac')
Start-Sleep -Seconds 4
# scrcpy quits when the computer has no audio device (a server, or a remote session); show the picture anyway.
if ($window.HasExited -and (Get-Content (Join-Path $StovaLogs 'scrcpy.err.log') -Raw) -match 'audio device') {
    $window = Start-Scrcpy @('--no-audio')
    Start-Sleep -Seconds 4
    if (-not $window.HasExited) { Write-Output 'Ingen lydeining på denne maskina: scrcpy viser biletet utan lyd.' }
}
if ($window.HasExited) { throw "scrcpy avslutta. Sjå $(Join-Path $StovaLogs 'scrcpy.err.log')." }
Write-Output "scrcpy viser $Serial."
