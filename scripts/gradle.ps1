$env:GRADLE_USER_HOME = 'C:\JellyBin\.gradle-home'
$env:TEMP = 'C:\Launcher\.gradle-tmp'; $env:TMP = $env:TEMP
New-Item -ItemType Directory -Force $env:TEMP | Out-Null
& "$PSScriptRoot\..\gradlew.bat" @args
exit $LASTEXITCODE
