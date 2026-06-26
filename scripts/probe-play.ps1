param(
    [int]$Port = 18080
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$BaseUrl = "http://localhost:$Port"
$StartedJob = $null

function Test-HomeReady {
    try {
        $status = & curl.exe -s -o NUL -w "%{http_code}" "$BaseUrl/"
        return $status -eq "200"
    } catch {
        return $false
    }
}

function Start-ProbeServer {
    $java = "D:\Develop\JDK21\bin\java.exe"
    if (-not (Test-Path $java)) {
        $java = "java.exe"
    }

    $maven = "mvn.cmd"
    if (Test-Path "D:\Develop\apache-maven-3.9.9\bin\mvn.cmd") {
        $maven = "D:\Develop\apache-maven-3.9.9\bin\mvn.cmd"
    }

    $localRepo = Join-Path $env:USERPROFILE ".m2\repository"
    Start-Job -ScriptBlock {
        param($root, $port, $java, $maven, $localRepo)
        Set-Location $root
        $argFile = "target\manual-$port.args"
        if (Test-Path $argFile) {
            & $java "@$argFile"
        } else {
            & $maven "-Dmaven.repo.local=$localRepo" spring-boot:run "-Dspring-boot.run.profiles=manual" "-Dspring-boot.run.arguments=--server.port=$port"
        }
    } -ArgumentList $Root, $Port, $java, $maven, $localRepo
}

try {
    if (-not (Test-HomeReady)) {
        Write-Host "Starting temporary manual server on $BaseUrl ..."
        $StartedJob = Start-ProbeServer
        for ($i = 0; $i -lt 30; $i++) {
            if (Test-HomeReady) {
                break
            }
            Start-Sleep -Seconds 1
        }
    }

    if (-not (Test-HomeReady)) {
        if ($StartedJob) {
            Receive-Job -Job $StartedJob -Keep | Select-Object -Last 80
        }
        throw "Server did not become ready at $BaseUrl"
    }

    $web = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $suffix = Get-Random
    $username = "probe_$suffix"
    $password = "pass1234"

    $register = Invoke-RestMethod -Uri "$BaseUrl/api/auth/register" -Method Post -ContentType "application/json" `
        -Body (@{ username = $username; password = $password; nickname = "probe" } | ConvertTo-Json -Compress) `
        -WebSession $web
    $login = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -ContentType "application/json" `
        -Body (@{ username = $username; password = $password } | ConvertTo-Json -Compress) `
        -WebSession $web
    $create = Invoke-RestMethod -Uri "$BaseUrl/api/vup/create" -Method Post -ContentType "application/json" `
        -Body (@{ name = "ProbeV"; persona = "quiet talk and singing rookie" } | ConvertTo-Json -Compress) `
        -WebSession $web

    $actions = Invoke-RestMethod -Uri "$BaseUrl/api/actions" -WebSession $web
    $action = @($actions.data | Where-Object { $_.enabled })[0]
    if (-not $action) {
        throw "No enabled action returned by /api/actions"
    }

    $planType = $null
    if ($action.actionType -eq "STREAM_PLAN") {
        $plans = Invoke-RestMethod -Uri "$BaseUrl/api/stream/plans" -WebSession $web
        $planType = @($plans.data)[0].planType
    } elseif ($action.actionType -eq "NPC_INTERACT") {
        $planType = "RAID"
    }

    $submit = Invoke-RestMethod -Uri "$BaseUrl/api/day/action" -Method Post -ContentType "application/json" `
        -Body (@{ actionType = $action.actionType; planType = $planType; idempotencyKey = "probe-$suffix" } | ConvertTo-Json -Compress) `
        -WebSession $web
    $session = Invoke-RestMethod -Uri "$BaseUrl/api/day/session" -WebSession $web

    [pscustomobject]@{
        home = "200"
        register = $register.success
        login = $login.success
        create = $create.success
        createdPhase = $create.data.phase
        submittedAction = $action.actionType
        submittedPlanType = $planType
        submit = $submit.success
        phaseAfterAction = $session.data.phase
    } | ConvertTo-Json
} finally {
    if ($StartedJob) {
        Stop-Job -Job $StartedJob -ErrorAction SilentlyContinue
        Remove-Job -Job $StartedJob -Force -ErrorAction SilentlyContinue
    }
}
