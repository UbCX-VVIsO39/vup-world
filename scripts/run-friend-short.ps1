param(
    [int]$Port = 18087,
    [int]$MaxDay = 7,
    [switch]$Refresh
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$manualScript = Join-Path $PSScriptRoot "run-latest-manual.ps1"
Write-Host "Starting friend short playtest: manual profile, H2 memory database, max day $MaxDay."
if ($Refresh) {
    & $manualScript -Port $Port -MaxDay $MaxDay -Refresh
} else {
    & $manualScript -Port $Port -MaxDay $MaxDay
}
