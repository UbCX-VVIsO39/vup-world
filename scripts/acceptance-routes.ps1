param(
    [int]$Port = 18080,
    [string[]]$Strategies = @(
        "steady",
        "clip",
        "black_red",
        "social",
        "singing",
        "cyber_girlfriend",
        "main_stage_king",
        "glorious_graduation",
        "idle",
        "defense",
        "random"
    ),
    [int]$TargetDay = 30,
    [string]$JsonOutput = "target\acceptance-routes.json",
    [string]$MarkdownOutput = "target\acceptance-routes-report.md"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$BaseUrl = "http://localhost:$Port"
$StartedJob = $null
$StartedByScript = $false
$FullRouteSpecs = @{
    steady = @{
        label = "稳健电子榨菜"
        expectedEnding = "ELECTRONIC_PICKLE"
    }
    clip = @{
        label = "切片圣体"
        expectedEnding = "SLICE_SAINT"
    }
    black_red = @{
        label = "黑红主会场"
        expectedEnding = "BLACK_RED_MAIN_STAGE"
    }
    social = @{
        label = "DD公交站"
        expectedEnding = "DD_BUS_STOP"
    }
    singing = @{
        label = "唱歌偶像"
        expectedEnding = "SINGING_IDOL"
    }
    cyber_girlfriend = @{
        label = "赛博女友"
        expectedEnding = "CYBER_GIRLFRIEND"
    }
    main_stage_king = @{
        label = "主会场之王"
        expectedEnding = "MAIN_STAGE_KING"
    }
    glorious_graduation = @{
        label = "光荣毕业"
        expectedEnding = "GLORIOUS_GRADUATION"
    }
    idle = @{
        label = "查无此V"
        expectedEnding = "UNKNOWN"
    }
    defense = @{
        label = "米线防守毕业"
        expectedEnding = "GLORIOUS_GRADUATION"
    }
    random = @{
        label = "随机整活"
        expectedEnding = $null
    }
}
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

function Test-HomeReady {
    try {
        $status = & curl.exe -s -o NUL -w "%{http_code}" "$BaseUrl/"
        return $status -eq "200"
    } catch {
        return $false
    }
}

function Start-ManualServer {
    $maven = "mvn.cmd"
    if (Test-Path "D:\Develop\apache-maven-3.9.9\bin\mvn.cmd") {
        $maven = "D:\Develop\apache-maven-3.9.9\bin\mvn.cmd"
    }
    $localRepo = Join-Path $env:USERPROFILE ".m2\repository"

    Start-Job -ScriptBlock {
        param($root, $port, $maven, $localRepo)
        Set-Location $root
        & $maven "-Dmaven.repo.local=$localRepo" spring-boot:run "-Dspring-boot.run.profiles=manual" "-Dspring-boot.run.arguments=--server.port=$port"
    } -ArgumentList $Root, $Port, $maven, $localRepo
}

function ConvertFrom-JsonResponse {
    param([object]$Response)
    $stream = $Response.RawContentStream
    if ($null -eq $stream) {
        return $Response.Content | ConvertFrom-Json
    }
    if ($stream.CanSeek) {
        $stream.Position = 0
    }
    $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
    try {
        return $reader.ReadToEnd() | ConvertFrom-Json
    } finally {
        $reader.Dispose()
    }
}

function Invoke-Json {
    param(
        [ValidateSet("Get", "Post")]
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [object]$Session = $null
    )

    $uri = if ($Path -match "^https?://") { $Path } else { "$BaseUrl$Path" }
    $params = @{
        Uri = $uri
        Method = $Method
        UseBasicParsing = $true
    }
    if ($Session) {
        $params.WebSession = $Session
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = $Body | ConvertTo-Json -Compress
    }
    ConvertFrom-JsonResponse (Invoke-WebRequest @params)
}

function Invoke-DemoPost {
    param(
        [string]$Path,
        [object]$Body,
        [object]$Session
    )
    Invoke-Json -Method Post -Path $Path -Body $Body -Session $Session
}

function Escape-Markdown {
    param([string]$Value)
    if ($null -eq $Value) {
        return ""
    }
    return $Value.Replace("|", "\|").Replace("`r", " ").Replace("`n", " ")
}

try {
    if (-not (Test-HomeReady)) {
        Write-Host "Starting temporary manual server on $BaseUrl ..."
        $StartedJob = Start-ManualServer
        $StartedByScript = $true
        for ($i = 0; $i -lt 45; $i++) {
            if (Test-HomeReady) {
                break
            }
            Start-Sleep -Seconds 1
        }
    }

    if (-not (Test-HomeReady)) {
        if ($StartedJob) {
            Receive-Job -Job $StartedJob -Keep | Select-Object -Last 100
        }
        throw "Server did not become ready at $BaseUrl"
    }

    $config = Invoke-Json -Method Get -Path "/api/system/config-check"
    if (-not $config.success -or -not $config.data.canPlayP0) {
        throw "/api/system/config-check did not report canPlayP0=true"
    }

    $unknownStrategies = @($Strategies | Where-Object { -not $FullRouteSpecs.ContainsKey($_) })
    if ($unknownStrategies.Count -gt 0) {
        throw "Unknown acceptance strategies: $($unknownStrategies -join ', '). Expected the current 11 acceptance routes: $($FullRouteSpecs.Keys -join ', ')"
    }

    $invalidExpectedEndingSpecs = @($FullRouteSpecs.GetEnumerator() | Where-Object {
            $_.Value.expectedEnding -and $FormalEndingTypes -notcontains $_.Value.expectedEnding
        } | ForEach-Object {
            "$($_.Key)=$($_.Value.expectedEnding)"
        })
    if ($invalidExpectedEndingSpecs.Count -gt 0) {
        throw "Acceptance route specs contain ending keys outside the current 9 formal endings: $($invalidExpectedEndingSpecs -join ', ')"
    }

    $startedAt = Get-Date
    $fullRouteStrategyNames = @($FullRouteSpecs.Keys | Sort-Object)
    $requestedStrategyNames = @($Strategies | Sort-Object -Unique)
    $isFullRouteRun = $requestedStrategyNames.Count -eq $fullRouteStrategyNames.Count -and @(
        $fullRouteStrategyNames | Where-Object { $requestedStrategyNames -notcontains $_ }
    ).Count -eq 0
    $usingDefaultJsonOutput = -not $PSBoundParameters.ContainsKey("JsonOutput")
    $usingDefaultMarkdownOutput = -not $PSBoundParameters.ContainsKey("MarkdownOutput")
    if (-not $isFullRouteRun -and ($usingDefaultJsonOutput -or $usingDefaultMarkdownOutput)) {
        $focusSlug = ((@($Strategies) -join "-") -replace "[^A-Za-z0-9_-]", "_").Trim("_")
        if (-not $focusSlug) {
            $focusSlug = "focused"
        }
        $focusStamp = $startedAt.ToString("yyyyMMddHHmmss")
        if ($usingDefaultJsonOutput) {
            $JsonOutput = "target\acceptance-routes-focused-$focusSlug-$focusStamp.json"
        }
        if ($usingDefaultMarkdownOutput) {
            $MarkdownOutput = "target\acceptance-routes-focused-$focusSlug-$focusStamp.md"
        }
        Write-Host "Focused acceptance run detected; writing diagnostic output to $JsonOutput and $MarkdownOutput so the canonical full 11-route evidence is not overwritten."
    }

    $results = @()

    foreach ($strategy in $Strategies) {
        Write-Host "Running acceptance strategy: $strategy"
        $web = New-Object Microsoft.PowerShell.Commands.WebRequestSession
        $seed = "acceptance-$strategy-" + $startedAt.ToString("yyyyMMddHHmmss")
        $routeSpec = $FullRouteSpecs[$strategy]
        $routeLabel = if ($routeSpec) { $routeSpec.label } else { $strategy }
        $expectedEnding = if ($routeSpec) { $routeSpec.expectedEnding } else { $null }

        $reset = Invoke-DemoPost -Path "/api/dev/demo/reset" -Body @{
            scenario = $strategy
            runSeed = $seed
        } -Session $web

        $run = Invoke-DemoPost -Path "/api/dev/demo/run-script" -Body @{
            strategy = $strategy
            targetDay = $TargetDay
            runSeed = $seed
            stopOnError = $true
        } -Session $web

        $status = Invoke-Json -Method Get -Path "/api/dev/demo/status" -Session $web
        $ending = $null
        $todayReport = $null
        $historyReports = $null
        if ($run.data.phase -eq "ENDING_READY") {
            $ending = Invoke-Json -Method Get -Path "/api/ending/review" -Session $web
        }
        if ($run.data.reportCount -gt 0 -or $run.data.phase -eq "REPORT_READY" -or $run.data.phase -eq "ENDING_READY") {
            $todayReport = Invoke-Json -Method Get -Path "/api/report/today" -Session $web
            $historyReports = Invoke-Json -Method Get -Path "/api/reports" -Session $web
        }

        $visibleItems = @()
        if ($todayReport -and $todayReport.success -and $todayReport.data.visibleItems) {
            $visibleItems = @($todayReport.data.visibleItems)
        }
        $stageMilestoneReports = @()
        if ($historyReports -and $historyReports.success -and $historyReports.data) {
            foreach ($report in @($historyReports.data)) {
                $refs = @()
                if ($report.evidenceRefs) {
                    $refs = @($report.evidenceRefs)
                }
                $hasStructuredMilestone = [bool]($refs | Where-Object { $_.type -eq "stage_milestone" } | Select-Object -First 1)
                $hasVisibleMilestone = $false
                if ($report.visibleItems) {
                    $hasVisibleMilestone = [bool](@($report.visibleItems) | Where-Object { $_ -match "^阶段爆点：" } | Select-Object -First 1)
                }
                if ($hasStructuredMilestone -or $hasVisibleMilestone) {
                    $stageMilestoneReports += $report
                }
            }
        }
        $stageMilestoneDays = @($stageMilestoneReports | ForEach-Object { [int]$_.day } | Sort-Object -Unique)
        $requiredMilestoneDays = @(7, 15, 23) | Where-Object { $_ -le $TargetDay }
        $missingMilestoneDays = @($requiredMilestoneDays | Where-Object { $stageMilestoneDays -notcontains $_ })
        $stageMilestonesPresent = $missingMilestoneDays.Count -eq 0
        $midgameEvents = @()
        if ($historyReports -and $historyReports.success -and $historyReports.data) {
            foreach ($report in @($historyReports.data)) {
                $refs = @()
                if ($report.evidenceRefs) {
                    $refs = @($report.evidenceRefs)
                }
                foreach ($ref in $refs) {
                    $eventId = $null
                    $window = $null
                    $routeGroup = $null
                    $primaryRoute = $null
                    if ($ref.midgameEventId) {
                        $eventId = [string]$ref.midgameEventId
                        $window = [string]$ref.midgameWindow
                        $routeGroup = [string]$ref.midgameRouteGroup
                        $primaryRoute = [string]$ref.midgamePrimaryRouteType
                    }
                    if (-not $eventId -and $ref.rollDetail) {
                        $roll = $ref.rollDetail
                        if ($roll.midgameEventId) {
                            $eventId = [string]$roll.midgameEventId
                            $window = [string]$roll.midgameWindow
                            $routeGroup = [string]$roll.routeGroup
                            $primaryRoute = [string]$roll.primaryRouteType
                        }
                    }
                    if ($eventId) {
                        $midgameEvents += [pscustomobject]@{
                            day = [int]$report.day
                            eventId = $eventId
                            window = $window
                            routeGroup = $routeGroup
                            primaryRouteType = $primaryRoute
                        }
                    }
                }
            }
        }
        $midgameEventWindows = @($midgameEvents | ForEach-Object { $_.window } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Sort-Object -Unique)
        $requiredMidgameWindows = @()
        if ($TargetDay -ge 12) { $requiredMidgameWindows += "FIRST_NAMING" }
        if ($TargetDay -ge 18) { $requiredMidgameWindows += "MID_BACKLASH" }
        if ($TargetDay -ge 25) { $requiredMidgameWindows += "LOCK_WARNING" }
        $missingMidgameWindows = @($requiredMidgameWindows | Where-Object { $midgameEventWindows -notcontains $_ })
        $midgameEventsPresent = $missingMidgameWindows.Count -eq 0
        $lateGameEvents = @()
        if ($historyReports -and $historyReports.success -and $historyReports.data) {
            foreach ($report in @($historyReports.data)) {
                $refs = @()
                if ($report.evidenceRefs) {
                    $refs = @($report.evidenceRefs)
                }
                foreach ($ref in $refs) {
                    $eventId = $null
                    $window = $null
                    $routeGroup = $null
                    $primaryRoute = $null
                    if ($ref.lateGameEventId) {
                        $eventId = [string]$ref.lateGameEventId
                        $window = [string]$ref.lateGameWindow
                        $routeGroup = [string]$ref.lateGameRouteGroup
                        $primaryRoute = [string]$ref.lateGamePrimaryRouteType
                    }
                    if (-not $eventId -and $ref.rollDetail) {
                        $roll = $ref.rollDetail
                        if ($roll.lateGameEventId) {
                            $eventId = [string]$roll.lateGameEventId
                            $window = [string]$roll.lateGameWindow
                            $routeGroup = [string]$roll.routeGroup
                            $primaryRoute = [string]$roll.primaryRouteType
                        }
                    }
                    if ($eventId) {
                        $lateGameEvents += [pscustomobject]@{
                            day = [int]$report.day
                            eventId = $eventId
                            window = $window
                            routeGroup = $routeGroup
                            primaryRouteType = $primaryRoute
                        }
                    }
                }
            }
        }
        $lateGameEventWindows = @($lateGameEvents | ForEach-Object { $_.window } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Sort-Object -Unique)
        $requiredLateGameWindows = @()
        if ($TargetDay -ge 25) { $requiredLateGameWindows += "FINAL_PRESSURE" }
        if ($TargetDay -ge 27) { $requiredLateGameWindows += "FINAL_LOCK" }
        if ($TargetDay -ge 29) { $requiredLateGameWindows += "FINAL_SCREENSHOT" }
        $missingLateGameWindows = @($requiredLateGameWindows | Where-Object { $lateGameEventWindows -notcontains $_ })
        $lateGameEventsPresent = $missingLateGameWindows.Count -eq 0
        $keyEvents = @()
        if ($ending -and $ending.success -and $ending.data.keyEvents) {
            $keyEvents = @($ending.data.keyEvents)
        }
        $routeEvidenceTrail = @()
        if ($ending -and $ending.success -and $ending.data.routeReview -and $ending.data.routeReview.routeEvidenceTrail) {
            $routeEvidenceTrail = @($ending.data.routeReview.routeEvidenceTrail)
        }
        $debtRefs = @()
        if ($ending -and $ending.success -and $ending.data.debtRefs) {
            $debtRefs = @($ending.data.debtRefs)
        }
        $endingTags = @()
        if ($ending -and $ending.success -and $ending.data.endingTags) {
            $endingTags = @($ending.data.endingTags)
        }

        $passed = $reset.success `
            -and $run.success `
            -and $status.success `
            -and $run.data.currentDay -eq $TargetDay `
            -and $run.data.phase -eq "ENDING_READY" `
            -and $run.data.reportCount -ge $TargetDay `
            -and $run.data.logCount -ge $TargetDay `
            -and $null -ne $run.data.endingReviewId `
            -and -not [string]::IsNullOrWhiteSpace($run.data.endingType) `
            -and -not [string]::IsNullOrWhiteSpace($run.data.endingReason) `
            -and ($FormalEndingTypes -contains $run.data.endingType) `
            -and $stageMilestonesPresent `
            -and $midgameEventsPresent `
            -and $lateGameEventsPresent `
            -and ($null -eq $expectedEnding -or $run.data.endingType -eq $expectedEnding)

        $results += [pscustomobject]@{
            strategy = $strategy
            label = $routeLabel
            expectedEnding = $expectedEnding
            passed = [bool]$passed
            runSeed = $seed
            currentDay = $run.data.currentDay
            phase = $run.data.phase
            reportCount = $run.data.reportCount
            logCount = $run.data.logCount
            endingReviewId = $run.data.endingReviewId
            endingType = $run.data.endingType
            formalEndingType = [bool]($FormalEndingTypes -contains $run.data.endingType)
            endingReason = $run.data.endingReason
            finalTitle = $ending.data.finalTitle
            subtitle = $ending.data.subtitle
            keyEventCount = $keyEvents.Count
            routeEvidenceTrailCount = $routeEvidenceTrail.Count
            finalReportDay = $ending.data.routeReview.finalReportRef.day
            debtRefCount = $debtRefs.Count
            endingTagCount = $endingTags.Count
            routeReviewCurrentRoute = $ending.data.routeReview.currentRoute
            routeScore = $ending.data.routeReview.scorecard.routeScore
            routeEvidenceCount = $ending.data.routeReview.scorecard.routeEvidenceCount
            memeLevel = $ending.data.fanProfile.memeLevel
            funFans = $ending.data.fanProfile.funFans
            reportVisibleItemCount = $visibleItems.Count
            stageReviewPresent = [bool]($visibleItems | Where-Object { $_ -match "阶段目标复盘|最终收束|路线成型|冲刺预警" } | Select-Object -First 1)
            stageMilestoneCount = $stageMilestoneReports.Count
            stageMilestoneDays = $stageMilestoneDays
            stageMilestonesPresent = $stageMilestonesPresent
            missingMilestoneDays = $missingMilestoneDays
            midgameEventCount = $midgameEvents.Count
            midgameEventWindows = $midgameEventWindows
            midgameEventsPresent = $midgameEventsPresent
            missingMidgameWindows = $missingMidgameWindows
            midgameEvents = $midgameEvents
            lateGameEventCount = $lateGameEvents.Count
            lateGameEventWindows = $lateGameEventWindows
            lateGameEventsPresent = $lateGameEventsPresent
            missingLateGameWindows = $missingLateGameWindows
            lateGameEvents = $lateGameEvents
            summary = $run.data.summary
            lastError = $status.data.lastError
        }
    }

    $allPassed = ($results | Where-Object { -not $_.passed }).Count -eq 0
    $summary = [pscustomobject]@{
        generatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
        baseUrl = $BaseUrl
        canPlayP0 = $config.data.canPlayP0
        targetDay = $TargetDay
        routeScope = if ($isFullRouteRun) { "full-30-day-11-route-acceptance" } else { "focused-30-day-acceptance" }
        isFullRouteRun = $isFullRouteRun
        requestedStrategies = @($Strategies)
        formalEndingTypes = $FormalEndingTypes
        expectedStrategies = @($FullRouteSpecs.Keys | Sort-Object)
        allPassed = $allPassed
        strategies = $results
    }

    $jsonPath = Join-Path $Root $JsonOutput
    $jsonDir = Split-Path $jsonPath -Parent
    if (-not (Test-Path $jsonDir)) {
        New-Item -ItemType Directory -Path $jsonDir | Out-Null
    }
    $summary | ConvertTo-Json -Depth 8 | Set-Content -Path $jsonPath -Encoding UTF8

    $mdPath = Join-Path $Root $MarkdownOutput
    $mdDir = Split-Path $mdPath -Parent
    if (-not (Test-Path $mdDir)) {
        New-Item -ItemType Directory -Path $mdDir | Out-Null
    }

    $lines = New-Object System.Collections.Generic.List[string]
    $reportTitle = if ($isFullRouteRun) { "# 完整目标路线验收报告" } else { "# Focused 路线验收诊断报告" }
    $lines.Add($reportTitle)
    $lines.Add("")
    $lines.Add("生成时间：$($summary.generatedAt)")
    $lines.Add("")
    $lines.Add("服务地址：$BaseUrl")
    $lines.Add("")
    $lines.Add("配置检查：canPlayP0=$($summary.canPlayP0)")
    $lines.Add("")
    $lines.Add("目标天数：$TargetDay 天")
    $lines.Add("")
    if ($isFullRouteRun) {
        $lines.Add("证据范围：FULL 11-route canonical evidence。")
    } else {
        $lines.Add("证据范围：FOCUSED diagnostic evidence（$($Strategies -join ', ')）。本报告用于定位单路线问题，不覆盖 full 11-route 发布证据。")
    }
    $lines.Add("")
    $lines.Add("完整目标：30 天完整局、11 条验收路线、9 个正式结局 key、下播链路与结局证据链可回放。公开发布、支付和云存档仍属于发行工程，不作为本脚本验收项。")
    $lines.Add("")
    $overallText = if ($allPassed) { "PASS" } else { "FAIL" }
    $lines.Add("总体验收：$overallText")
    $lines.Add("")
    $lines.Add("| 验收路线 | 策略 | 结果 | 天数 | 阶段 | 日报数 | 日志数 | 预期结局 | 实际结局 | 结局ID | 关键事件 | 证据链 | 阶段复盘 | 阶段爆点 | 中期事件 | 收官事件 |")
    $lines.Add("|----------|------|--------|-----|-------|---------|------|----------|----------|-----------|----------|--------|----------|----------|----------|----------|")
    foreach ($result in $results) {
        $mark = if ($result.passed) { "PASS" } else { "FAIL" }
        $stageReview = if ($result.stageReviewPresent) { "PASS" } else { "FAIL" }
        $stageMilestone = if ($result.stageMilestonesPresent) { "PASS " + (@($result.stageMilestoneDays) -join "/") } else { "FAIL missing " + (@($result.missingMilestoneDays) -join "/") }
        $midgameEvent = if ($result.midgameEventsPresent) { "PASS " + (@($result.midgameEventWindows) -join "/") } else { "FAIL missing " + (@($result.missingMidgameWindows) -join "/") }
        $lateGameEvent = if ($result.lateGameEventsPresent) { "PASS " + (@($result.lateGameEventWindows) -join "/") } else { "FAIL missing " + (@($result.missingLateGameWindows) -join "/") }
        $lines.Add("| $(Escape-Markdown $result.label) | $($result.strategy) | $mark | $($result.currentDay) | $($result.phase) | $($result.reportCount) | $($result.logCount) | $(Escape-Markdown $result.expectedEnding) | $(Escape-Markdown $result.endingType) | $($result.endingReviewId) | $($result.keyEventCount) | $($result.routeEvidenceTrailCount) | $stageReview | $(Escape-Markdown $stageMilestone) | $(Escape-Markdown $midgameEvent) | $(Escape-Markdown $lateGameEvent) |")
    }
    $lines.Add("")
    $lines.Add("## 结局证据")
    foreach ($result in $results) {
        $lines.Add("")
        $lines.Add("### $($result.label) / $($result.strategy)")
        $lines.Add("")
        $lines.Add("- 运行种子：$($result.runSeed)")
        $lines.Add("- 预期结局：$(Escape-Markdown $result.expectedEnding)")
        $lines.Add("- 最终标题：$(Escape-Markdown $result.finalTitle)")
        $lines.Add("- 副标题：$(Escape-Markdown $result.subtitle)")
        $lines.Add("- 结局理由：$(Escape-Markdown $result.endingReason)")
        $lines.Add("- 证据链：关键事件 $($result.keyEventCount)，路线证据 $($result.routeEvidenceTrailCount)，最终日报 Day $($result.finalReportDay)，阶段复盘=$($result.stageReviewPresent)，阶段爆点=$((@($result.stageMilestoneDays) -join '/'))，中期事件=$((@($result.midgameEventWindows) -join '/'))，收官事件=$((@($result.lateGameEventWindows) -join '/'))")
        if ($result.lastError) {
            $lines.Add("- 最后错误：$(Escape-Markdown ($result.lastError | ConvertTo-Json -Compress))")
        }
    }
    $lines.Add("")
    $lines.Add("## 验收标准")
    $lines.Add("")
    $lines.Add("完整目标验收覆盖 11 条验收路线：稳健电子榨菜、切片圣体、黑红主会场、DD公交站、唱歌偶像、赛博女友、主会场之王、光荣毕业、查无此V、米线防守毕业、随机整活。每条路线都必须推进到目标天数、阶段为 ENDING_READY，并拥有不少于目标天数的日报和业务日志、结局ID、结局理由、关键事件、路线证据链、最终阶段复盘证据、第7/15/23天阶段爆点、Day 10-25 的 FIRST_NAMING / MID_BACKLASH / LOCK_WARNING 中期事件窗口，以及 Day 24-29 的 FINAL_PRESSURE / FINAL_LOCK / FINAL_SCREENSHOT 收官事件窗口；实际结局必须属于当前 9 个正式 endingType，且除随机整活外必须命中预期结局。")
    $lines | Set-Content -Path $mdPath -Encoding UTF8

    $summary | ConvertTo-Json -Depth 8

    if (-not $allPassed) {
        exit 1
    }
} finally {
    if ($StartedByScript -and $StartedJob) {
        Stop-Job -Job $StartedJob -ErrorAction SilentlyContinue
        Remove-Job -Job $StartedJob -Force -ErrorAction SilentlyContinue
    }
}
