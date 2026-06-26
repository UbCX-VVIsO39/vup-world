param(
    [int]$Port = 18141,
    [string]$SaveDir = "target\singleplayer-probe-save",
    [switch]$Refresh
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$BaseUrl = "http://localhost:$Port"
$FormalEndingTypes = @(
    "UNKNOWN",
    "ELECTRONIC_PICKLE",
    "SINGING_IDOL",
    "SLICE_SAINT",
    "BLACK_RED_MAIN_STAGE",
    "CYBER_GIRLFRIEND",
    "DD_BUS_STOP",
    "MAIN_STAGE_KING",
    "GLORIOUS_GRADUATION"
)
$Java = "D:\Develop\JDK21\bin\java.exe"
if (-not (Test-Path $Java)) {
    $Java = "java.exe"
}
$Maven = "mvn.cmd"
if (Test-Path "D:\Develop\apache-maven-3.9.9\bin\mvn.cmd") {
    $Maven = "D:\Develop\apache-maven-3.9.9\bin\mvn.cmd"
}

Set-Location $Root

$resolvedSaveDir = if ([System.IO.Path]::IsPathRooted($SaveDir)) {
    [System.IO.Path]::GetFullPath($SaveDir)
} else {
    [System.IO.Path]::GetFullPath((Join-Path $Root $SaveDir))
}
$targetRoot = [System.IO.Path]::GetFullPath((Join-Path $Root "target"))
if (-not $resolvedSaveDir.StartsWith($targetRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Probe SaveDir must be inside target for safe cleanup: $resolvedSaveDir"
}
if (Test-Path -LiteralPath $resolvedSaveDir) {
    Remove-Item -LiteralPath $resolvedSaveDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $resolvedSaveDir | Out-Null

function Invoke-Json {
    param(
        [ValidateSet("Get", "Post")]
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [object]$Session
    )

    $params = @{
        Uri = "$BaseUrl$Path"
        Method = $Method
        WebSession = $Session
        TimeoutSec = 20
    }
    if ($null -ne $Body) {
        $jsonBody = $Body | ConvertTo-Json -Compress -Depth 32
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = [System.Text.Encoding]::UTF8.GetBytes($jsonBody)
    }
    try {
        Invoke-RestMethod @params
    } catch [System.Net.WebException] {
        $response = $_.Exception.Response
        if ($null -ne $response) {
            $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
            $bodyText = $reader.ReadToEnd()
            throw "HTTP $([int]$response.StatusCode) from $Method ${Path}: $bodyText"
        }
        throw
    }
}

function Require-Success {
    param(
        [object]$Response,
        [string]$Label
    )
    if (-not $Response -or -not $Response.success) {
        throw "$Label failed"
    }
    return $Response
}

function Ensure-JsonArray {
    param([object]$Value)
    if ($null -eq $Value) {
        return @()
    }
    if ($Value -is [System.Array]) {
        return @($Value)
    }
    return @($Value)
}

function Save-Archive-Payload {
    param([object]$Archive)

    [ordered]@{
        saveVersion = $Archive.saveVersion
        exportedAt = $Archive.exportedAt
        sourceSlotNumber = $Archive.sourceSlotNumber
        slot = $Archive.slot
        player = $Archive.player
        saveSlot = $Archive.saveSlot
        manualSave = $Archive.manualSave
        vup = $Archive.vup
        daySessions = @(Ensure-JsonArray -Value $Archive.daySessions)
        businessLogs = @(Ensure-JsonArray -Value $Archive.businessLogs)
        dailyReports = @(Ensure-JsonArray -Value $Archive.dailyReports)
        endingReviews = @(Ensure-JsonArray -Value $Archive.endingReviews)
        riskDebts = @(Ensure-JsonArray -Value $Archive.riskDebts)
        unlocks = @(Ensure-JsonArray -Value $Archive.unlocks)
        statsHistory = @(Ensure-JsonArray -Value $Archive.statsHistory)
    }
}

function Test-HomeReady {
    try {
        $status = & curl.exe -s -o NUL -w "%{http_code}" "$BaseUrl/"
        return $status -eq "200"
    } catch {
        return $false
    }
}

function Wait-HomeReady {
    param(
        [int]$Seconds = 90,
        [string]$LogPrefix = ""
    )
    for ($i = 0; $i -lt $Seconds; $i++) {
        if (Test-HomeReady) {
            Write-Host "Server ready at $BaseUrl"
            return
        }
        Start-Sleep -Seconds 1
    }
    if ($LogPrefix) {
        $outLog = Join-Path $Root "target\$LogPrefix.out.log"
        $errLog = Join-Path $Root "target\$LogPrefix.err.log"
        if (Test-Path -LiteralPath $outLog) {
            Write-Host "---- $outLog tail ----"
            Get-Content -LiteralPath $outLog -Tail 80
        }
        if (Test-Path -LiteralPath $errLog) {
            Write-Host "---- $errLog tail ----"
            Get-Content -LiteralPath $errLog -Tail 80
        }
    }
    throw "Server did not become ready at $BaseUrl"
}

function Build-Classpath {
    $mainClass = Join-Path $Root "target\classes\com\example\vupworld\VupWorldApplication.class"
    $classpathFile = Join-Path $Root "target\classpath.txt"
    if ($Refresh -or -not (Test-Path $mainClass) -or -not (Test-Path $classpathFile)) {
        & $Maven compile
        if ($LASTEXITCODE -ne 0) {
            throw "mvn compile failed"
        }
        & $Maven dependency:build-classpath "-Dmdep.outputFile=target\classpath.txt" "-Dmdep.pathSeparator=;"
        if ($LASTEXITCODE -ne 0) {
            throw "mvn dependency:build-classpath failed"
        }
    }
    return (Join-Path $Root "target\classes") + ";" + ((Get-Content -Raw $classpathFile).Trim())
}

function Start-SingleplayerProcess {
    param(
        [string]$Classpath,
        [string]$Label
    )

    $databaseBase = (Join-Path $resolvedSaveDir "vupworld") -replace "\\", "/"
    $datasourceUrl = "jdbc:h2:file:$databaseBase;MODE=MySQL;NON_KEYWORDS=DAY,LOCKED;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0"
    $argFile = Join-Path $Root "target\singleplayer-probe-$Port.args"
    $outLog = Join-Path $Root "target\singleplayer-probe-$Port-$Label.out.log"
    $errLog = Join-Path $Root "target\singleplayer-probe-$Port-$Label.err.log"
    Remove-Item -LiteralPath $outLog, $errLog -Force -ErrorAction SilentlyContinue
    $appArgs = @(
        "-cp",
        $Classpath,
        "com.example.vupworld.VupWorldApplication",
        "--spring.profiles.active=singleplayer",
        "--server.port=$Port",
        "--spring.datasource.url=$datasourceUrl",
        "--game.singleplayer.save-directory=$resolvedSaveDir",
        "--game.singleplayer.database-name=vupworld"
    )
    Set-Content -Path $argFile -Encoding ASCII -Value $appArgs
    Write-Host "Starting $Label singleplayer process on $BaseUrl"
    Write-Host "App logs: $outLog"
    Start-Process -FilePath $Java -ArgumentList "@$argFile" -PassThru -WindowStyle Hidden -WorkingDirectory $Root -RedirectStandardOutput $outLog -RedirectStandardError $errLog
}

function Choose-FirstEnabled {
    param(
        [object[]]$Items,
        [string[]]$PreferredValues,
        [string]$ValueProperty,
        [string]$Label
    )

    $enabled = @($Items | Where-Object { $_ -and ($null -eq $_.enabled -or $_.enabled -ne $false) })
    if (-not $enabled -or $enabled.Count -eq 0) {
        throw "No enabled $Label returned"
    }
    foreach ($value in $PreferredValues) {
        $match = @($enabled | Where-Object { $_.$ValueProperty -eq $value })[0]
        if ($match) {
            return $match
        }
    }
    return $enabled[0]
}

function Resolve-Current-Phase {
    param(
        [object]$SessionState,
        [object]$WebSession,
        [string]$Suffix
    )

    $sessionState = $SessionState
    while ($true) {
        switch ($sessionState.phase) {
            "OFF_STREAM_READY" {
                $options = Invoke-Json -Method Get -Path "/api/offstream/options" -Session $WebSession
                $choice = Choose-FirstEnabled -Items @($options.data) -PreferredValues @("RECOVER", "CLIP", "THANK_SC") -ValueProperty "type" -Label "offstream option"
                if ($choice -and $choice.type) {
                    Require-Success -Response (Invoke-Json -Method Post -Path "/api/offstream/action" -Body @{
                        offStreamType = $choice.type
                    } -Session $WebSession) -Label "offstream action" | Out-Null
                } else {
                    Require-Success -Response (Invoke-Json -Method Post -Path "/api/offstream/skip" -Session $WebSession) -Label "offstream skip" | Out-Null
                }
                $sessionState = (Invoke-Json -Method Get -Path "/api/day/session" -Session $WebSession).data
            }
            "NEED_INTERACTION_CHOICE" {
                $pendingInteraction = Invoke-Json -Method Get -Path "/api/interaction/pending" -Session $WebSession
                $choice = Choose-FirstEnabled -Items @($pendingInteraction.data.choices) -PreferredValues @("safe", "traffic", "meme") -ValueProperty "choiceType" -Label "interaction choice"
                Require-Success -Response (Invoke-Json -Method Post -Path "/api/interaction/choose" -Body @{
                    choiceType = $choice.choiceType
                    idempotencyKey = "singleplayer-probe-interaction-$Suffix"
                } -Session $WebSession) -Label "interaction choice" | Out-Null
                $sessionState = (Invoke-Json -Method Get -Path "/api/day/session" -Session $WebSession).data
            }
            "NEED_EVENT_CHOICE" {
                $pendingEvent = Invoke-Json -Method Get -Path "/api/event/pending" -Session $WebSession
                $choice = Choose-FirstEnabled -Items @($pendingEvent.data.choices) -PreferredValues @("safe", "traffic", "meme") -ValueProperty "choiceType" -Label "event choice"
                $choiceKey = if ($choice.choiceId) { $choice.choiceId } else { $choice.choiceType }
                Require-Success -Response (Invoke-Json -Method Post -Path "/api/event/choose" -Body @{
                    choiceId = $choiceKey
                    choiceType = $choice.choiceType
                    idempotencyKey = "singleplayer-probe-event-$Suffix"
                } -Session $WebSession) -Label "event choice" | Out-Null
                $sessionState = (Invoke-Json -Method Get -Path "/api/day/session" -Session $WebSession).data
            }
            default {
                return $sessionState
            }
        }
    }
}

function Stop-SingleplayerProcess {
    param($Process)
    if ($null -ne $Process -and -not $Process.HasExited) {
        Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
        try {
            Wait-Process -Id $Process.Id -Timeout 10 -ErrorAction SilentlyContinue
        } catch {
        }
    }
}

function Advance-One-Day {
    param([object]$Session)

    $suffix = Get-Random
    $sessionState = (Invoke-Json -Method Get -Path "/api/day/session" -Session $Session).data
    if ($sessionState.phase -ne "READY") {
        throw "Expected READY before advancing a day, got $($sessionState.phase)"
    }

    $actions = Invoke-Json -Method Get -Path "/api/actions" -Session $Session
    $action = Choose-FirstEnabled -Items @($actions.data) -PreferredValues @("TRAIN_TALK", "FAN_GROUP_MAINTAIN", "STREAM_PLAN", "REST") -ValueProperty "actionType" -Label "action"
    if ($action.actionType -eq "STREAM_PLAN") {
        $plans = Invoke-Json -Method Get -Path "/api/stream/plans" -Session $Session
        $plan = Choose-FirstEnabled -Items @($plans.data) -PreferredValues @("TALK", "SC_THANKS", "SING_TALK") -ValueProperty "planType" -Label "stream plan"
        Require-Success -Response (Invoke-Json -Method Post -Path "/api/day/action" -Body @{
            actionType = "STREAM_PLAN"
            planType = $plan.planType
            idempotencyKey = "singleplayer-probe-action-$suffix"
        } -Session $Session) -Label "stream action" | Out-Null

        $titles = Invoke-Json -Method Get -Path "/api/stream/titles" -Session $Session
        $title = Choose-FirstEnabled -Items @($titles.data) -PreferredValues @("SAFE", "BUSINESS_SAFE", "BAIT_TRAFFIC") -ValueProperty "style" -Label "title"
        Require-Success -Response (Invoke-Json -Method Post -Path "/api/stream/title/choose" -Body @{
            titleTemplateId = $title.id
            idempotencyKey = "singleplayer-probe-title-$suffix"
        } -Session $Session) -Label "title choose" | Out-Null
    } else {
        Require-Success -Response (Invoke-Json -Method Post -Path "/api/day/action" -Body @{
            actionType = $action.actionType
            idempotencyKey = "singleplayer-probe-action-$suffix"
        } -Session $Session) -Label "day action" | Out-Null
    }

    $sessionState = Resolve-Current-Phase -SessionState ((Invoke-Json -Method Get -Path "/api/day/session" -Session $Session).data) -WebSession $Session -Suffix $suffix

    if ($sessionState.phase -ne "REPORT_READY") {
        if ($sessionState.phase -eq "ENDING_READY") {
            return $sessionState
        }
        throw "Expected REPORT_READY or ENDING_READY after resolving day, got $($sessionState.phase)"
    }
    Invoke-Json -Method Get -Path "/api/report/today" -Session $Session | Out-Null
    Invoke-Json -Method Post -Path "/api/day/next" -Body @{
        idempotencyKey = "singleplayer-probe-next-$suffix"
    } -Session $Session | Out-Null
    return (Invoke-Json -Method Get -Path "/api/day/session" -Session $Session).data
}

function Advance-To-Ending {
    param([object]$Session)

    $maxDay = 30
    try {
        $currentVup = Invoke-Json -Method Get -Path "/api/vup/current" -Session $Session
        if ($currentVup.data.maxDay) {
            $maxDay = [int]$currentVup.data.maxDay
        }
    } catch {
    }

    for ($guard = 0; $guard -lt ($maxDay * 4); $guard++) {
        $sessionState = (Invoke-Json -Method Get -Path "/api/day/session" -Session $Session).data
        if ($sessionState.phase -eq "ENDING_READY") {
            return $sessionState
        }
        if ($sessionState.phase -eq "REPORT_READY") {
            Invoke-Json -Method Get -Path "/api/report/today" -Session $Session | Out-Null
            Invoke-Json -Method Post -Path "/api/day/next" -Body @{
                idempotencyKey = "singleplayer-probe-next-existing-$($sessionState.day)-$guard"
            } -Session $Session | Out-Null
            continue
        }
        if ($sessionState.phase -ne "READY") {
            $sessionState = Resolve-Current-Phase -SessionState $sessionState -WebSession $Session -Suffix "existing-$($sessionState.day)-$guard"
            if ($sessionState.phase -eq "ENDING_READY") {
                return $sessionState
            }
            if ($sessionState.phase -ne "REPORT_READY") {
                throw "Cannot advance from Day $($sessionState.day) phase $($sessionState.phase)"
            }
            Invoke-Json -Method Get -Path "/api/report/today" -Session $Session | Out-Null
            Invoke-Json -Method Post -Path "/api/day/next" -Body @{
                idempotencyKey = "singleplayer-probe-next-resolved-$($sessionState.day)-$guard"
            } -Session $Session | Out-Null
            continue
        }
        Write-Host "Advance Day $($sessionState.day) of $maxDay"
        $advanced = Advance-One-Day -Session $Session
        if ($advanced.phase -eq "ENDING_READY") {
            return $advanced
        }
    }

    $finalState = (Invoke-Json -Method Get -Path "/api/day/session" -Session $Session).data
    throw "Did not reach ENDING_READY within guard. Last state: Day $($finalState.day) $($finalState.phase)"
}

$classpath = Build-Classpath
$process = $null
$restartProcess = $null

try {
    if (Test-HomeReady) {
        throw "Port $Port is already serving HTTP. Pick another port."
    }

    $process = Start-SingleplayerProcess -Classpath $classpath -Label "first"
    Wait-HomeReady -LogPrefix "singleplayer-probe-$Port-first"

    $web = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    Write-Host "POST /api/game/quick-start"
    $quickStart = Invoke-Json -Method Post -Path "/api/game/quick-start" -Session $web
    if (-not $quickStart.success) {
        throw "quick-start failed"
    }
    Write-Host "Advance one day"
    $dayTwo = Advance-One-Day -Session $web
    if ($dayTwo.day -ne 2 -or $dayTwo.phase -ne "READY") {
        throw "Expected Day 2 READY after advancing one day, got Day $($dayTwo.day) $($dayTwo.phase)"
    }
    Write-Host "POST /api/save-slots/1/save"
    $saved = Invoke-Json -Method Post -Path "/api/save-slots/1/save" -Session $web
    if (-not $saved.success -or $saved.data.day -ne 2) {
        throw "Manual save did not report Day 2"
    }

    Write-Host "Stopping first process"
    Stop-SingleplayerProcess -Process $process
    $process = $null
    Start-Sleep -Seconds 2

    $restartProcess = Start-SingleplayerProcess -Classpath $classpath -Label "restart"
    Wait-HomeReady -LogPrefix "singleplayer-probe-$Port-restart"

    $newWeb = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    Write-Host "GET /api/game/bootstrap after restart"
    $bootstrap = Invoke-Json -Method Get -Path "/api/game/bootstrap" -Session $newWeb
    Write-Host "POST /api/game/continue after restart"
    $continued = Invoke-Json -Method Post -Path "/api/game/continue" -Session $newWeb
    if (-not $bootstrap.data.hasSave) {
        throw "bootstrap did not see a local save after restart"
    }
    if ($continued.data.daySession.day -ne 2 -or $continued.data.daySession.phase -ne "READY") {
        throw "continue did not return Day 2 READY after restart"
    }
    $databaseFile = Join-Path $resolvedSaveDir "vupworld.mv.db"
    if (-not (Test-Path -LiteralPath $databaseFile)) {
        throw "Database file not found: $databaseFile"
    }

    Write-Host "GET /api/save-slots/1/export after restart"
    $exported = Invoke-Json -Method Get -Path "/api/save-slots/1/export" -Session $newWeb
    if (-not $exported.success -or $exported.data.saveVersion -ne "singleplayer-save-v1") {
        throw "Exported save archive is missing the expected save version"
    }
    if ($exported.data.sourceSlotNumber -ne 1 -or -not $exported.data.vup -or -not $exported.data.daySessions) {
        throw "Exported save archive is missing source slot, run data, or day sessions"
    }

    Write-Host "POST /api/save-slots/2/import from exported archive"
    $archivePayload = Save-Archive-Payload -Archive $exported.data
    $archivePayloadPath = Join-Path $Root "target\singleplayer-probe-$Port-archive-payload.json"
    $archivePayload | ConvertTo-Json -Depth 32 | Set-Content -Path $archivePayloadPath -Encoding UTF8
    Write-Host "Archive payload: $archivePayloadPath"
    $imported = Invoke-Json -Method Post -Path "/api/save-slots/2/import" -Body $archivePayload -Session $newWeb
    if (-not $imported.success) {
        throw "Import into slot 2 failed"
    }
    if ($imported.data.activeRun.slotNumber -ne 2) {
        throw "Imported save did not activate slot 2"
    }
    if ($imported.data.daySession.day -ne 2 -or $imported.data.daySession.phase -ne "READY") {
        throw "Imported slot did not restore Day 2 READY"
    }
    if (-not $imported.data.saveSlots[1].occupied -or $imported.data.saveSlots[1].slotNumber -ne 2) {
        throw "Imported slot 2 is not visible in the save slot list"
    }

    Write-Host "GET /api/save-slots/2/continue after import"
    $continuedImport = Invoke-Json -Method Get -Path "/api/save-slots/2/continue" -Session $newWeb
    if (-not $continuedImport.success -or $continuedImport.data.daySession.day -ne 2 -or $continuedImport.data.daySession.phase -ne "READY") {
        throw "Imported slot 2 could not be continued at Day 2 READY"
    }

    Write-Host "Advance imported continued run to ending"
    $endingSession = Advance-To-Ending -Session $newWeb
    if ($endingSession.phase -ne "ENDING_READY") {
        throw "Expected ENDING_READY after full run, got Day $($endingSession.day) $($endingSession.phase)"
    }
    if ($endingSession.day -ne 30) {
        throw "Expected full singleplayer run to end on Day 30, got Day $($endingSession.day)"
    }
    $endingReview = Invoke-Json -Method Get -Path "/api/ending/review" -Session $newWeb
    if (-not $endingReview.success -or -not $endingReview.data.endingType) {
        throw "Ending review did not include an ending type"
    }
    if ($FormalEndingTypes -notcontains $endingReview.data.endingType) {
        throw "Ending review returned '$($endingReview.data.endingType)', which is outside the current 9 formal ending keys"
    }

    Write-Host "POST /api/rebirth/restart after ending"
    $restart = Invoke-Json -Method Post -Path "/api/rebirth/restart" -Body @{
        confirmRestart = $true
        restartBiasType = "UNKNOWN"
        idempotencyKey = "singleplayer-probe-restart-$Port"
    } -Session $newWeb
    if (-not $restart.success -or $restart.data.day -ne 1 -or $restart.data.phase -ne "READY") {
        throw "Restart did not return Day 1 READY"
    }
    $afterRestartSession = (Invoke-Json -Method Get -Path "/api/day/session" -Session $newWeb).data
    if ($afterRestartSession.day -ne 1 -or $afterRestartSession.phase -ne "READY") {
        throw "Current session after restart is not Day 1 READY"
    }

    [pscustomobject]@{
        quickStart = $quickStart.success
        savedDay = $saved.data.day
        savedPhase = $saved.data.phase
        restartHasSave = $bootstrap.data.hasSave
        continuedDay = $continued.data.daySession.day
        continuedPhase = $continued.data.daySession.phase
        exportedSlot = $exported.data.sourceSlotNumber
        exportSaveVersion = $exported.data.saveVersion
        importedSlot = $imported.data.activeRun.slotNumber
        importedDay = $imported.data.daySession.day
        importedPhase = $imported.data.daySession.phase
        continuedImportedDay = $continuedImport.data.daySession.day
        continuedImportedPhase = $continuedImport.data.daySession.phase
        endingDay = $endingSession.day
        endingPhase = $endingSession.phase
        endingType = $endingReview.data.endingType
        formalEndingType = [bool]($FormalEndingTypes -contains $endingReview.data.endingType)
        restartDay = $restart.data.day
        restartPhase = $restart.data.phase
        databaseFile = $databaseFile
    } | ConvertTo-Json -Depth 6
} finally {
    Stop-SingleplayerProcess -Process $process
    Stop-SingleplayerProcess -Process $restartProcess
}
