# Starts one Codex agent on a task, detached from the calling session (it keeps running if Claude
# stops). Codex uses the user's own ~/.codex/config.toml. Output and Codex's final message land in
# C:\Launcher\.superpowers\agents\<Name>-<stamp>.log and -last.md.
#
# Usage: pwsh -NoProfile -File scripts/Start-CodexAgent.ps1 -Name plan-3 -Repo C:\path -Prompt "..." [-Worktree] [-DryRun]
param(
    [Parameter(Mandatory = $true)][string]$Name,
    [Parameter(Mandatory = $true)][string]$Repo,
    [Parameter(Mandatory = $true)][string]$Prompt,
    [switch]$Worktree,
    [switch]$DryRun
)
$ErrorActionPreference = 'Stop'

$codex = Get-ChildItem "$env:LOCALAPPDATA\OpenAI\Codex\bin" -Recurse -Filter codex.exe |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $codex) { throw 'Fann ikkje codex.exe under %LOCALAPPDATA%\OpenAI\Codex\bin' }
if ($Prompt.Contains('%')) { throw 'Prompten kan ikkje innehalde % (cmd.exe ville tolka det som ein variabel).' }

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$logDir = 'C:\Launcher\.superpowers\agents'
New-Item -ItemType Directory -Force $logDir | Out-Null
$log = Join-Path $logDir "$Name-$stamp.log"
$last = Join-Path $logDir "$Name-$stamp-last.md"
$worktreeFlag = if ($Worktree) { ' --worktree' } else { '' }
$text = $Prompt.Replace('"', "'").Replace("`r", ' ').Replace("`n", ' ')

$cmdLine = "cmd.exe /c `"`"$($codex.FullName)`" exec -C `"$Repo`"$worktreeFlag -o `"$last`" `"$text`" > `"$log`" 2>&1`""
if ($cmdLine.Length -gt 8000) { throw "Kommandolinja er $($cmdLine.Length) teikn; cmd.exe tek maks 8191." }

if ($DryRun) {
    Write-Output "codex: $($codex.FullName)"
    Write-Output "log:   $log"
    Write-Output "len:   $($cmdLine.Length)"
    exit 0
}

# Win32_Process.Create puts the process outside this session's process tree.
$result = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{
    CommandLine      = $cmdLine
    CurrentDirectory = $Repo
}
if ($result.ReturnValue -ne 0) { throw "Win32_Process.Create feila med kode $($result.ReturnValue)" }
Write-Output "Codex '$Name' starta (PID $($result.ProcessId)). Logg: $log"
