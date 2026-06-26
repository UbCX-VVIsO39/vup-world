param(
    [int]$Port = 18080,
    [string]$OutFile = "target/probe-stage-flow-latest.json"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$BaseUrl = "http://localhost:$Port"

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
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = $Body | ConvertTo-Json -Compress
    }
    try {
        Invoke-RestMethod @params
    } catch {
        Write-Error "$Method $Path failed: $($_.Exception.Message)"
        throw
    }
}

$web = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$suffix = Get-Random
$username = "stage_probe_$suffix"
$password = "pass1234"

$registerBody = @{
    username = $username
    password = $password
    nickname = "stage probe"
}
$register = Invoke-Json -Method Post -Path "/api/auth/register" -Body $registerBody -Session $web

$loginBody = @{
    username = $username
    password = $password
}
$login = Invoke-Json -Method Post -Path "/api/auth/login" -Body $loginBody -Session $web

$createBody = @{
    name = "StageProbe$suffix"
    persona = "quiet talk and singing rookie"
}
$create = Invoke-Json -Method Post -Path "/api/vup/create" -Body $createBody -Session $web

$plans = Invoke-Json -Method Get -Path "/api/stream/plans" -Session $web
$plan = @($plans.data | Where-Object { $_.planType -eq "REGULAR_TALK" })[0]
if (-not $plan) {
    $plan = @($plans.data)[0]
}

$submitBody = @{
    actionType = "STREAM_PLAN"
    planType = $plan.planType
    idempotencyKey = "stage-probe-stream-$suffix"
}
$submit = Invoke-Json -Method Post -Path "/api/day/action" -Body $submitBody -Session $web

$titleSession = Invoke-Json -Method Get -Path "/api/day/session" -Session $web
$titles = Invoke-Json -Method Get -Path "/api/stream/titles" -Session $web
$title = @($titles.data)[0]
if (-not $title) {
    throw "No title candidate returned by /api/stream/titles"
}

$chooseTitleBody = @{
    titleTemplateId = $title.id
    idempotencyKey = "stage-probe-title-$suffix"
}
$chooseTitle = Invoke-Json -Method Post -Path "/api/stream/title/choose" -Body $chooseTitleBody -Session $web

$afterTitleSession = Invoke-Json -Method Get -Path "/api/day/session" -Session $web
$offStreamOptions = $null
$offStreamChoice = $null
$offStreamResult = $null
$eventChoiceResult = $null
$interactionChoiceResult = $null

if ($afterTitleSession.data.phase -eq "OFF_STREAM_READY") {
    $offStreamOptions = Invoke-Json -Method Get -Path "/api/offstream/options" -Session $web
    $recommended = @($offStreamOptions.data | Where-Object { $_.recommended })[0]
    $offStreamChoice = if ($recommended) { $recommended } else { @($offStreamOptions.data)[0] }
    if ($offStreamChoice -and $offStreamChoice.type) {
        $offStreamBody = @{
            offStreamType = $offStreamChoice.type
        }
        $offStreamResult = Invoke-Json -Method Post -Path "/api/offstream/action" -Body $offStreamBody -Session $web
    } else {
        $offStreamResult = Invoke-Json -Method Post -Path "/api/offstream/skip" -Session $web
    }
    $afterTitleSession = Invoke-Json -Method Get -Path "/api/day/session" -Session $web
}

if ($afterTitleSession.data.phase -eq "NEED_INTERACTION_CHOICE") {
    $pendingInteraction = Invoke-Json -Method Get -Path "/api/interaction/pending" -Session $web
    $interactionChoice = @($pendingInteraction.data.choices)[0]
    if (-not $interactionChoice) {
        throw "NEED_INTERACTION_CHOICE returned no choices"
    }
    $interactionBody = @{
        choiceType = $interactionChoice.choiceType
        idempotencyKey = "stage-probe-interaction-$suffix"
    }
    $interactionChoiceResult = Invoke-Json -Method Post -Path "/api/interaction/choose" -Body $interactionBody -Session $web
    $afterTitleSession = Invoke-Json -Method Get -Path "/api/day/session" -Session $web
}

if ($afterTitleSession.data.phase -eq "NEED_EVENT_CHOICE") {
    $pendingEvent = Invoke-Json -Method Get -Path "/api/event/pending" -Session $web
    $eventChoice = @($pendingEvent.data.choices)[0]
    if (-not $eventChoice) {
        throw "NEED_EVENT_CHOICE returned no choices"
    }
    $eventBody = @{
        choiceId = $eventChoice.choiceId
        idempotencyKey = "stage-probe-event-$suffix"
    }
    $eventChoiceResult = Invoke-Json -Method Post -Path "/api/event/choose" -Body $eventBody -Session $web
    $afterTitleSession = Invoke-Json -Method Get -Path "/api/day/session" -Session $web
}

$todayReport = $null
if ($afterTitleSession.data.phase -eq "REPORT_READY" -or $afterTitleSession.data.phase -eq "ENDING_READY") {
    $todayReport = Invoke-Json -Method Get -Path "/api/report/today" -Session $web
}

$next = $null
if ($afterTitleSession.data.phase -eq "REPORT_READY") {
    $nextBody = @{
        idempotencyKey = "stage-probe-next-$suffix"
    }
    $next = Invoke-Json -Method Post -Path "/api/day/next" -Body $nextBody -Session $web
}

$finalSession = Invoke-Json -Method Get -Path "/api/day/session" -Session $web

$result = [pscustomobject]@{
    home = "200"
    register = $register.success
    login = $login.success
    create = $create.success
    createdPhase = $create.data.phase
    selectedPlanType = $plan.planType
    submit = $submit.success
    phaseAfterStreamPlan = $titleSession.data.phase
    titleCount = @($titles.data).Count
    chosenTitleId = $title.id
    chooseTitle = $chooseTitle.success
    offStreamCovered = [bool]$offStreamResult
    offStreamOptionCount = if ($offStreamOptions) { @($offStreamOptions.data).Count } else { 0 }
    offStreamChoice = if ($offStreamChoice) { $offStreamChoice.type } else { "SKIPPED_OR_NOT_REACHED" }
    offStreamResultPhase = $offStreamResult.data.phase
    interactionHandled = [bool]$interactionChoiceResult
    eventHandled = [bool]$eventChoiceResult
    phaseAfterTitle = $afterTitleSession.data.phase
    reportLoaded = [bool]$todayReport
    reportDay = $todayReport.data.day
    nextDay = $next.success
    finalPhase = $finalSession.data.phase
    finalDay = $finalSession.data.day
}

$json = $result | ConvertTo-Json -Depth 8
if ($OutFile) {
    $resolvedOut = if ([System.IO.Path]::IsPathRooted($OutFile)) { $OutFile } else { Join-Path (Get-Location) $OutFile }
    $outDir = Split-Path -Parent $resolvedOut
    if ($outDir) {
        New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    }
    $json | Set-Content -Encoding UTF8 -Path $resolvedOut
}
$json
