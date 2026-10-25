param(
    [string]$Serial,
    [string]$Package = 'app.stova',
    [switch]$SelfTest
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Get-StartTotalTime([string]$amOutput) {
    if ($amOutput -notmatch '(?m)^\s*TotalTime:\s*(\d+)\s*$') {
        throw "am start manglar gyldig TotalTime: $amOutput"
    }
    return [int]$Matches[1]
}
function Get-JankPercent([string]$gfxinfo) {
    if ($gfxinfo -notmatch 'Janky frames:\s*\d+\s*\((\d+(?:\.\d+)?)%\)') {
        throw 'gfxinfo manglar Janky frames med prosent'
    }
    $percent = [double]::Parse($Matches[1], [cultureinfo]::InvariantCulture)
    if ($percent -gt 100) { throw 'Ugyldig prosent frå gfxinfo' }
    return $percent
}
function Get-TotalPssKb([string]$meminfo) {
    if ($meminfo -match '\bTOTAL PSS:\s*(\d+)\b' -or $meminfo -match '(?m)^\s*TOTAL\s+(\d+)\b') {
        return [int]$Matches[1]
    }
    throw 'meminfo manglar TOTAL PSS eller eldre TOTAL-rad'
}
function Get-Median([double[]]$Values) {
    if ($Values.Count -eq 0) { throw 'Median krev minst ei måling' }
    $sorted = @($Values | Sort-Object)
    $middle = [int][math]::Floor($sorted.Count / 2)
    if ($sorted.Count % 2) { return $sorted[$middle] }
    return ($sorted[$middle - 1] + $sorted[$middle]) / 2
}
function Get-ScrollKey([int]$Step) {
    $position = $Step % 46
    if ($position -lt 20) { return 'KEYCODE_DPAD_RIGHT' }
    if ($position -lt 40) { return 'KEYCODE_DPAD_LEFT' }
    if ($position -lt 43) { return 'KEYCODE_DPAD_DOWN' }
    return 'KEYCODE_DPAD_UP'
}

function Invoke-Adb([string[]]$Arguments) {
    $output = (& $script:adb -s $Serial @Arguments 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $output -match '(?m)^(Error:|Exception occurred|Security exception)') {
        throw "adb $($Arguments -join ' ') feila: $output"
    }
    return $output
}

function Wait-HomeReady {
    $deadline = [datetime]::UtcNow.AddSeconds(15)
    do {
        Invoke-Adb -Arguments @('shell', 'uiautomator', 'dump', '/data/local/tmp/stova-measure.xml') | Out-Null
        $xml = Invoke-Adb -Arguments @('shell', 'cat', '/data/local/tmp/stova-measure.xml')
        if ($xml.Contains('content-desc="home-ready"')) { return }
    } while ([datetime]::UtcNow -lt $deadline)
    throw 'Heim vart ikkje klar (home-ready). Lag/vel profil før målinga.'
}

function Invoke-SelfTest {
    $script:checks = 0
    function Assert-Equal($Expected, $Actual) {
        if ($Expected -ne $Actual) { throw "Expected $Expected, got $Actual" }
        $script:checks++
    }
    function Assert-Rejected([scriptblock]$Action) {
        $rejected = $false
        try { & $Action | Out-Null } catch { $rejected = $true }
        if (-not $rejected) { throw 'Invalid/missing measurement was accepted' }
        $script:checks++
    }
    Assert-Equal 812 (Get-StartTotalTime "Status: ok`nLaunchState: COLD`nTotalTime: 812`nWaitTime: 830")
    Assert-Equal 0 (Get-StartTotalTime "TotalTime: 0`r`nWaitTime: 10")
    Assert-Equal 3.0 (Get-JankPercent "Total frames rendered: 1200`nJanky frames: 36 (3.00%)")
    Assert-Equal 0.0 (Get-JankPercent "Total frames rendered: 1200`nJanky frames: 0 (0.00%)")
    Assert-Equal 187654 (Get-TotalPssKb '           TOTAL PSS:   187654            TOTAL RSS:   250000')
    Assert-Equal 187654 (Get-TotalPssKb "  TOTAL SWAP PSS: 99`n        TOTAL 187654 1234 5678")
    Assert-Equal 5.5 (Get-Median @(10, 2, 9, 1, 8, 3, 7, 4, 6, 5))
    Assert-Equal 2 (Get-Median @(3, 1, 2))
    Assert-Rejected { Get-StartTotalTime 'Error: Activity not started' }
    Assert-Rejected { Get-StartTotalTime 'TotalTime: -1' }
    Assert-Rejected { Get-JankPercent 'Total frames rendered: 0' }
    Assert-Rejected { Get-JankPercent 'Janky frames: 1 (103.00%)' }
    Assert-Rejected { Get-TotalPssKb 'TOTAL RSS: 250000' }
    Assert-Rejected { Get-TotalPssKb 'No process found' }
    Assert-Rejected { Get-Median @() }
    Assert-Equal 'KEYCODE_DPAD_RIGHT' (Get-ScrollKey 0)
    Assert-Equal 'KEYCODE_DPAD_RIGHT' (Get-ScrollKey 19)
    Assert-Equal 'KEYCODE_DPAD_LEFT' (Get-ScrollKey 20)
    Assert-Equal 'KEYCODE_DPAD_LEFT' (Get-ScrollKey 39)
    Assert-Equal 'KEYCODE_DPAD_DOWN' (Get-ScrollKey 40)
    Assert-Equal 'KEYCODE_DPAD_DOWN' (Get-ScrollKey 42)
    Assert-Equal 'KEYCODE_DPAD_UP' (Get-ScrollKey 43)
    Assert-Equal 'KEYCODE_DPAD_UP' (Get-ScrollKey 45)
    Assert-Equal 'KEYCODE_DPAD_RIGHT' (Get-ScrollKey 46)
    Write-Output "SELFTEST OK ($script:checks/$script:checks)"
}

try {
    if ($SelfTest) { Invoke-SelfTest; exit 0 }
    if ([string]::IsNullOrWhiteSpace($Serial)) { throw 'Oppgje -Serial eksplisitt.' }
    if ($Serial -match '^emulator-' -and $Serial -ne 'emulator-5582') {
        throw 'Berre emulator-5582 er tillaten. Bruk ein eksplisitt Shield-serie for Shield.'
    }
    if ($Package -notmatch '^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$') { throw 'Ugyldig pakkenamn' }
    $script:adb = if ($env:ANDROID_SDK_ROOT -and (Test-Path "$env:ANDROID_SDK_ROOT/platform-tools/adb.exe")) {
        "$env:ANDROID_SDK_ROOT/platform-tools/adb.exe"
    } elseif (Test-Path 'C:/Android/sdk/platform-tools/adb.exe') {
        'C:/Android/sdk/platform-tools/adb.exe'
    } else { (Get-Command adb -ErrorAction Stop).Source }
    Invoke-Adb -Arguments @('get-state') | Out-Null
    $component = "$Package/.MainActivity"
    Write-Output "Måler $Package på $Serial. Emulatortal validerer skriptet; Shield-release avgjer budsjetta."
    $cold = @(for ($i = 0; $i -lt 10; $i++) {
        Invoke-Adb -Arguments @('shell', 'am', 'force-stop', $Package) | Out-Null
        Get-StartTotalTime (Invoke-Adb -Arguments @('shell', 'am', 'start', '-S', '-W', '-n', $component))
        Wait-HomeReady
    })
    Write-Output "Kaldstartprøver (ms): $($cold -join ', ')"

    # Leave the process alive, put TV Settings in front, then time returning to Home.
    Invoke-Adb -Arguments @('shell', 'input', 'keyevent', 'KEYCODE_HOME') | Out-Null
    Invoke-Adb -Arguments @('shell', 'am', 'start', '-W', '-n', 'com.android.tv.settings/.MainSettings') | Out-Null
    $warmOutput = Invoke-Adb -Arguments @('shell', 'am', 'start', '-W', '-n', $component)
    $warm = Get-StartTotalTime $warmOutput
    Wait-HomeReady

    Invoke-Adb -Arguments @('shell', 'dumpsys', 'gfxinfo', $Package, 'reset') | Out-Null
    # A monotonic host deadline includes adb overhead, and works with 32-bit Android shells.
    $scrollWatch = [Diagnostics.Stopwatch]::StartNew()
    $step = 0
    while ($scrollWatch.Elapsed.TotalSeconds -lt 30) {
        Invoke-Adb -Arguments @('shell', 'input', 'keyevent', (Get-ScrollKey $step)) | Out-Null
        $step++
        Start-Sleep -Milliseconds 150
    }
    $scrollWatch.Stop()
    Write-Output ('Rulling: {0} tastetrykk over {1:F2} s' -f $step, $scrollWatch.Elapsed.TotalSeconds)
    $gfx = Invoke-Adb -Arguments @('shell', 'dumpsys', 'gfxinfo', $Package)
    if ($gfx -notmatch 'Total frames rendered:\s*([1-9]\d*)') { throw 'Ingen teikna ramer; hakking kan ikkje målast.' }
    $jank = Get-JankPercent $gfx
    Start-Sleep -Seconds 10
    $pss = (Get-TotalPssKb (Invoke-Adb -Arguments @('shell', 'dumpsys', 'meminfo', $Package))) / 1024.0
    $measurements = @(
        @{ Name = 'Kaldstart (median av 10)'; Value = (Get-Median $cold); Budget = 900; Unit = 'ms' }
        @{ Name = 'Varm Home'; Value = $warm; Budget = 300; Unit = 'ms' }
        @{ Name = 'Hakkande ramer'; Value = $jank; Budget = 5; Unit = '%' }
        @{ Name = 'PSS'; Value = $pss; Budget = 250; Unit = 'MiB' }
    )
    $measurements | ForEach-Object {
        [pscustomobject]@{
            Måling = $_.Name
            Verdi = ('{0:F2} {1}' -f $_.Value, $_.Unit)
            Budsjett = ('≤ {0} {1}' -f $_.Budget, $_.Unit)
            Resultat = $(if ($_.Value -le $_.Budget) { 'OK' } else { 'FEIL' })
        }
    } | Format-Table -AutoSize | Out-String | Write-Output
    if (@($measurements | Where-Object { $_.Value -gt $_.Budget }).Count) { exit 1 }
    exit 0
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
