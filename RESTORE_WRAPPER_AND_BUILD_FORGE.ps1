$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$wrapperPath = Join-Path $root "gradle\wrapper\gradle-wrapper.jar"
$wrapperDirectory = Split-Path -Parent $wrapperPath
$expectedSha256 = "2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046"
$wrapperUrl = "https://raw.githubusercontent.com/gradle/gradle/v8.10.2/gradle/wrapper/gradle-wrapper.jar"

New-Item -ItemType Directory -Force -Path $wrapperDirectory | Out-Null

$restoreWrapper = -not (Test-Path $wrapperPath)
if (-not $restoreWrapper) {
    $currentHash = (Get-FileHash $wrapperPath -Algorithm SHA256).Hash.ToLowerInvariant()
    $restoreWrapper = $currentHash -ne $expectedSha256
}

if ($restoreWrapper) {
    $temporaryPath = "$wrapperPath.download"
    Remove-Item $temporaryPath -Force -ErrorAction SilentlyContinue
    Write-Host "Restoring the verified Gradle 8.10.2 wrapper..."
    Invoke-WebRequest -UseBasicParsing -Uri $wrapperUrl -OutFile $temporaryPath
    $downloadHash = (Get-FileHash $temporaryPath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($downloadHash -ne $expectedSha256) {
        Remove-Item $temporaryPath -Force -ErrorAction SilentlyContinue
        throw "Gradle wrapper checksum mismatch. Expected $expectedSha256 but received $downloadHash."
    }
    Move-Item $temporaryPath $wrapperPath -Force
}

$javaVersion = (& java -version 2>&1 | Out-String)
if ($javaVersion -notmatch 'version "17[\._]') {
    throw "Forge 1.20.1 must be built with JDK 17. Current Java output:`n$javaVersion"
}

Write-Host "Building Forge 1.20.1..."
& .\gradlew.bat :forge-1.20.1:clean :forge-1.20.1:build --no-daemon
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed with exit code $LASTEXITCODE."
}

$jars = Get-ChildItem ".\forge-1.20.1\build\libs\*.jar" |
    Where-Object { $_.Name -notmatch '(-sources|-dev|-slim)\.jar$' } |
    Sort-Object LastWriteTime -Descending

if (-not $jars) {
    throw "Build completed, but no release Forge JAR was found."
}

Write-Host "Forge JAR created:"
$jars | ForEach-Object { Write-Host $_.FullName }
