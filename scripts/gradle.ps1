# Runs the Gradle wrapper with the given tasks. Gradle breaks on Windows when the user folder has
# letters outside ASCII (such as «Ø»), so there its home and temp files move to ASCII paths.
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$onWindows = $IsWindows -or $PSVersionTable.PSEdition -eq 'Desktop'
if ($onWindows -and $env:USERPROFILE -match '[^\x00-\x7F]') {
    if (-not $env:GRADLE_USER_HOME) {
        # The owner shares one Gradle home with Spole; anyone else gets one in the repo (git-ignored).
        $env:GRADLE_USER_HOME = if (Test-Path 'C:\JellyBin\.gradle-home') { 'C:\JellyBin\.gradle-home' } else { Join-Path $root '.gradle-home' }
    }
    $env:TEMP = Join-Path $root '.gradle-tmp'; $env:TMP = $env:TEMP
    New-Item -ItemType Directory -Force $env:TEMP | Out-Null
}
$wrapper = if ($onWindows) { Join-Path $root 'gradlew.bat' } else { Join-Path $root 'gradlew' }
& $wrapper @args
exit $LASTEXITCODE
