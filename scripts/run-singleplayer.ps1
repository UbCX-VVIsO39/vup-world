param(
    [int]$Port = 18087,
    [string]$SaveDir = "tmp\singleplayer-save",
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

$resolvedSaveDir = if ([System.IO.Path]::IsPathRooted($SaveDir)) {
    [System.IO.Path]::GetFullPath($SaveDir)
} else {
    [System.IO.Path]::GetFullPath((Join-Path $Root $SaveDir))
}
New-Item -ItemType Directory -Force -Path $resolvedSaveDir | Out-Null
$databaseBase = (Join-Path $resolvedSaveDir "vupworld") -replace "\\", "/"
$databaseFile = Join-Path $resolvedSaveDir "vupworld.mv.db"
$datasourceUrl = "jdbc:h2:file:$databaseBase;MODE=MySQL;NON_KEYWORDS=DAY,LOCKED;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0"

$mainClass = Join-Path $Root "target\classes\com\example\vupworld\VupWorldApplication.class"
$classpathFile = Join-Path $Root "target\classpath.txt"

if ($Refresh -or -not (Test-Path $mainClass) -or -not (Test-Path $classpathFile)) {
    $Maven = "mvn.cmd"
    if (Test-Path "D:\Develop\apache-maven-3.9.9\bin\mvn.cmd") {
        $Maven = "D:\Develop\apache-maven-3.9.9\bin\mvn.cmd"
    }
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
$argFile = Join-Path $Root "target\singleplayer-$Port.args"
$appArgs = @(
    "-cp",
    $classpath,
    "com.example.vupworld.VupWorldApplication",
    "--spring.profiles.active=singleplayer",
    "--server.port=$Port",
    "--spring.datasource.url=$datasourceUrl",
    "--game.singleplayer.save-directory=$resolvedSaveDir",
    "--game.singleplayer.database-name=vupworld"
)
Set-Content -Path $argFile -Encoding ASCII -Value $appArgs

Write-Host "Starting VUP World singleplayer server: http://localhost:$Port/"
Write-Host "Local save directory: $resolvedSaveDir"
Write-Host "Local save database: $databaseFile"
Write-Host "Press Ctrl+C in this terminal to stop it."
& $Java "@$argFile"
