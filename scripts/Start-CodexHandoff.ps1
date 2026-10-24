# Starts Codex in C:\Launcher, detached from the calling session, so it keeps working after
# Claude's usage window is spent. Codex uses the user's own ~/.codex/config.toml (approval policy
# and sandbox are set there). It stops after the task in progress once -StopAfter has passed,
# then writes docs/HANDOFF.md so Claude can pick up again.
#
# Usage: pwsh -NoProfile -File scripts/Start-CodexHandoff.ps1 -StopAfter "2026-10-25 00:40" [-DryRun]
param(
    [Parameter(Mandatory = $true)][string]$StopAfter,
    [string]$Repo = 'C:\Launcher',
    [switch]$DryRun
)
$ErrorActionPreference = 'Stop'

$codex = Get-ChildItem "$env:LOCALAPPDATA\OpenAI\Codex\bin" -Recurse -Filter codex.exe |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $codex) { throw 'Fann ikkje codex.exe under %LOCALAPPDATA%\OpenAI\Codex\bin' }

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$logDir = Join-Path $Repo '.superpowers'
New-Item -ItemType Directory -Force $logDir | Out-Null
$log = Join-Path $logDir "codex-handoff-$stamp.log"
$last = Join-Path $logDir "codex-handoff-$stamp-last.md"

$prompt = "Les AGENTS.md og docs/HANDOFF.md i $Repo, og hald fram med planen frå første steg som ikkje er avkryssa. " +
    "Følg stega nøyaktig (test først, sjå han feile, implementer, sjå han bestå, commit), hak av stega i planfila, " +
    "og legg til ei 'Task N: complete (...)'-linje i ledgeren under .superpowers/sdd/ når ei oppgåve er ferdig. " +
    "Når klokka har passert $StopAfter (lokal tid): fullfør steget du held på med, commit, skriv status øvst i docs/HANDOFF.md, commit og stopp. " +
    "Stopp også når Plan 1 er ferdig. Ikkje push og ikkje publiser noko."

# cmd /c gives the redirection; Win32_Process.Create puts the process outside this session's
# process tree, so it survives when the Claude session ends.
$cmdLine = "cmd.exe /c `"`"$($codex.FullName)`" exec -C `"$Repo`" -o `"$last`" `"$($prompt.Replace('"', "'"))`" > `"$log`" 2>&1`""

if ($DryRun) {
    Write-Output "codex: $($codex.FullName)"
    Write-Output "log:   $log"
    Write-Output "cmd:   $cmdLine"
    exit 0
}

$result = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{
    CommandLine      = $cmdLine
    CurrentDirectory = $Repo
}
if ($result.ReturnValue -ne 0) { throw "Win32_Process.Create feila med kode $($result.ReturnValue)" }
Write-Output "Codex starta (PID $($result.ProcessId)). Logg: $log"
