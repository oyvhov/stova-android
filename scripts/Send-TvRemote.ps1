# Drives a TV over adb, step by step, for checks and screenshots:
#   key:NAME   presses a key (DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT, DPAD_CENTER, BACK, HOME, MENU)
#   hold:MS    holds OK for MS milliseconds, as a real remote does (default 1500)
#   wait:S     waits S seconds
#   shot:FILE  saves a PNG screenshot
#   focus      prints the bounds and text of the focused element
# Example: pwsh scripts/Send-TvRemote.ps1 key:DPAD_UP key:DPAD_RIGHT hold:1500 shot:menu.png
# adb's own long press releases at the long-press timeout and races Compose's timer, so hold: registers a
# virtual Bluetooth remote with Android's hid tool and holds its OK key; Android repeats it like a real one.
[CmdletBinding(PositionalBinding = $false)]
param(
    [string]$Serial = 'emulator-5580',
    [Parameter(Mandatory, ValueFromRemainingArguments)][string[]]$Steps
)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/StovaSdk.ps1"
Assert-NotSpole $Serial
function Invoke-Adb { & $Adb -s $Serial @args; if ($LASTEXITCODE) { throw "adb $args feila." } }

# A consumer-control remote: one 16-bit usage per report. 0x41 is Menu Pick, which Android maps to OK.
$descriptor = @(5, 12, 9, 1, 161, 1, 21, 0, 38, 255, 3, 25, 0, 42, 255, 3, 117, 16, 149, 1, 129, 0, 192)
function Send-Hold([int]$Ms) {
    $events = @(
        @{ id = 1; command = 'register'; name = 'Stova remote'; vid = 6353; pid = 11330; bus = 'bluetooth'; descriptor = $descriptor },
        @{ id = 1; command = 'delay'; duration = 1500 },
        @{ id = 1; command = 'report'; report = @(65, 0) },
        @{ id = 1; command = 'delay'; duration = $Ms },
        @{ id = 1; command = 'report'; report = @(0, 0) },
        @{ id = 1; command = 'delay'; duration = 600 }
    )
    $file = Join-Path ([System.IO.Path]::GetTempPath()) 'stova-hold.json'
    ($events | ForEach-Object { $_ | ConvertTo-Json -Compress -Depth 3 }) -join "`n" | Set-Content -NoNewline -Encoding ascii $file
    Invoke-Adb push $file /data/local/tmp/stova-hold.json 2>&1 | Out-Null
    Invoke-Adb shell hid /data/local/tmp/stova-hold.json | Out-Null
}

foreach ($step in $Steps) {
    $kind, $arg = $step -split ':', 2
    switch ($kind) {
        'key' {
            $code = if ($arg -like 'KEYCODE_*') { $arg } else { "KEYCODE_$arg" }
            Invoke-Adb shell input keyevent $code; Start-Sleep -Milliseconds 900
        }
        'hold' { Send-Hold ($(if ($arg) { [int]$arg } else { 1500 })); Start-Sleep -Seconds 1 }
        'wait' { Start-Sleep -Seconds ([double]::Parse($arg, [cultureinfo]::InvariantCulture)) }
        'shot' {
            Invoke-Adb shell screencap -p /sdcard/stova-shot.png
            Invoke-Adb pull /sdcard/stova-shot.png $arg 2>&1 | Out-Null
            Invoke-Adb shell rm /sdcard/stova-shot.png
            Write-Output "Skjermbilete: $arg"
        }
        'focus' {
            Invoke-Adb shell uiautomator dump --compressed /data/local/tmp/stova-ui.xml | Out-Null
            [xml]$tree = (Invoke-Adb shell cat /data/local/tmp/stova-ui.xml) -join "`n"
            $tree.SelectNodes('//node[@focused="true"]') | ForEach-Object {
                Write-Output "Fokus: $($_.bounds) $($_.text)$($_.'content-desc')"
            }
        }
        default { throw "Ukjent steg: $step" }
    }
}
