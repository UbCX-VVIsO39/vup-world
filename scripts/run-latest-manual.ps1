param(
    [int]$Port = 18086,
    [int]$MaxDay = 0,
    [switch]$Refresh
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$Java = "D:\Develop\JDK21\bin\java.exe"
if (-not (Test-Path $Java)) {
    $Java = "java.exe"
}

Set-Location $Root

$mainClass = Join-Path $Root "target\classes\com\example\vupworld\VupWorldApplication.class"
$classpathFile = Join-Path $Root "target\classpath.txt"

if ($Refresh -or -not (Test-Path $mainClass) -or -not (Test-Path $classpathFile)) {
    $Maven = "mvn.cmd"
    Write-Host "Compiling classes and syncing static resources..."
    & $Maven compile
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    Write-Host "Building runtime classpath..."
    & $Maven dependency:build-classpath "-Dmdep.outputFile=target\classpath.txt" "-Dmdep.pathSeparator=;"
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} else {
    Write-Host "Using existing compiled classes and classpath. Run with -Refresh to rebuild before starting."
}

$classpath = (Join-Path $Root "target\classes") + ";" + ((Get-Content -Raw $classpathFile).Trim())
$argFile = Join-Path $Root "target\manual-$Port.args"
$appArgs = @(
    "-cp",
    $classpath,
    "com.example.vupworld.VupWorldApplication",
    "--spring.profiles.active=manual",
    "--server.port=$Port"
)
if ($MaxDay -gt 0) {
    $appArgs += "--game.run.max-day=$MaxDay"
}
Set-Content -Path $argFile -Encoding ASCII -Value $appArgs

Write-Host "Starting VUP World manual server: http://localhost:$Port/"
if ($MaxDay -gt 0) {
    Write-Host "Short run max day: $MaxDay"
}
Write-Host "Press Ctrl+C in this terminal to stop it."
& $Java "@$argFile"
