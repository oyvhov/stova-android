# Opens a scrcpy window onto Stova's TV, so it can be driven with the mouse and keyboard:
# arrows = D-pad, Enter = OK, right-click = Back, middle-click = Home, Alt+M = Menu.
# Default is Stova's emulator; pass -Serial <ip>:5555 for the Shield.
param([string]$Serial = 'emulator-5580')
$ErrorActionPreference = 'Stop'
$env:ADB = 'C:\Android\sdk\platform-tools\adb.exe'

$scrcpy = (Get-Command scrcpy -ErrorAction SilentlyContinue).Source
if (-not $scrcpy) {
    $scrcpy = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Filter scrcpy.exe -Recurse -Depth 3 -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $scrcpy) { throw 'scrcpy er ikkje installert. Køyr: winget install Genymobile.scrcpy' }

# AAC, not scrcpy's default Opus: the Android TV emulator image has no Opus encoder.
Start-Process -FilePath $scrcpy -WorkingDirectory (Split-Path $scrcpy) -ArgumentList @(
    '-s', $Serial, '--audio-codec=aac', '--window-title', "`"Stova TV ($Serial)`"") `
    -RedirectStandardOutput 'C:\Launcher\.superpowers\scrcpy.log' -RedirectStandardError 'C:\Launcher\.superpowers\scrcpy.err.log'
Write-Output "scrcpy viser $Serial."
