param(
    [int]$UiPort = 18087,
    [int]$SaveProbePort = 18141,
    [int]$RoutesPort = 18080,
    [string]$SaveDir = "target\singleplayer-probe-save",
    [switch]$Refresh,
    [switch]$SkipBrowserEvidence,
    [switch]$SkipRoutes,
    [switch]$SkipSaveProbe
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$Java = "D:\Develop\JDK21\bin\java.exe"
if (-not (Test-Path $Java)) {
    $Java = "java.exe"
}
$Maven = "mvn.cmd"
if (Test-Path "D:\Develop\apache-maven-3.9.9\bin\mvn.cmd") {
    $Maven = "D:\Develop\apache-maven-3.9.9\bin\mvn.cmd"
}

Set-Location $Root

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Block
    )

    Write-Host ""
    Write-Host "== $Name =="
    $started = Get-Date
    try {
        & $Block
        $elapsed = [int]((Get-Date) - $started).TotalSeconds
        Write-Host "PASS $Name ($elapsed s)"
    } catch {
        Write-Host "FAIL $Name"
        throw
    }
}

function Test-HomeReady {
    param([int]$Port)
    try {
        $status = & curl.exe -s -o NUL -w "%{http_code}" "http://localhost:$Port/"
        return $status -eq "200"
    } catch {
        return $false
    }
}

function Wait-HomeReady {
    param(
        [int]$Port,
        [int]$Seconds = 90,
        [string]$OutLog = "",
        [string]$ErrLog = ""
    )

    for ($i = 0; $i -lt $Seconds; $i++) {
        if (Test-HomeReady -Port $Port) {
            Write-Host "Server ready at http://localhost:$Port/"
            return
        }
        Start-Sleep -Seconds 1
    }

    if ($OutLog -and (Test-Path -LiteralPath $OutLog)) {
        Write-Host "---- $OutLog tail ----"
        Get-Content -LiteralPath $OutLog -Tail 80
    }
    if ($ErrLog -and (Test-Path -LiteralPath $ErrLog)) {
        Write-Host "---- $ErrLog tail ----"
        Get-Content -LiteralPath $ErrLog -Tail 80
    }
    throw "Server did not become ready at http://localhost:$Port/"
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

function Start-ManualServer {
    param(
        [string]$Classpath,
        [int]$Port
    )

    if (Test-HomeReady -Port $Port) {
        Write-Host "Reusing existing HTTP server at http://localhost:$Port/ for browser evidence."
        return $null
    }

    $argFile = Join-Path $Root "target\release-candidate-manual-$Port.args"
    $outLog = Join-Path $Root "target\release-candidate-manual-$Port.out.log"
    $errLog = Join-Path $Root "target\release-candidate-manual-$Port.err.log"
    Remove-Item -LiteralPath $outLog, $errLog -Force -ErrorAction SilentlyContinue
    $appArgs = @(
        "-cp",
        $Classpath,
        "com.example.vupworld.VupWorldApplication",
        "--spring.profiles.active=manual",
        "--server.port=$Port"
    )
    Set-Content -Path $argFile -Encoding ASCII -Value $appArgs
    Write-Host "Starting manual evidence server: http://localhost:$Port/"
    $process = Start-Process -FilePath $Java -ArgumentList "@$argFile" -PassThru -WindowStyle Hidden -WorkingDirectory $Root -RedirectStandardOutput $outLog -RedirectStandardError $errLog
    Wait-HomeReady -Port $Port -OutLog $outLog -ErrLog $errLog
    return $process
}

function Stop-ProcessSafe {
    param([object]$Process)
    if ($null -eq $Process) {
        return
    }
    try {
        if (-not $Process.HasExited) {
            Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
            $Process.WaitForExit(5000) | Out-Null
        }
    } catch {
        Write-Host "Warning: failed to stop process $($Process.Id): $($_.Exception.Message)"
    }
}

$manualProcess = $null

try {
    Invoke-Step "verify.cmd" {
        & (Join-Path $Root "scripts\verify.cmd")
        if ($LASTEXITCODE -ne 0) {
            throw "scripts\verify.cmd failed with exit code $LASTEXITCODE"
        }
    }

    if (-not $SkipSaveProbe) {
        Invoke-Step "singleplayer save probe" {
            & (Join-Path $Root "scripts\probe-singleplayer-save-flow.cmd") -Port $SaveProbePort -SaveDir $SaveDir
            if ($LASTEXITCODE -ne 0) {
                throw "probe-singleplayer-save-flow failed with exit code $LASTEXITCODE"
            }
        }
    } else {
        Write-Host ""
        Write-Host "SKIP singleplayer save probe"
    }

    if (-not $SkipRoutes) {
        Invoke-Step "11-route acceptance" {
            & (Join-Path $Root "scripts\acceptance-routes.cmd") -Port $RoutesPort
            if ($LASTEXITCODE -ne 0) {
                throw "acceptance-routes failed with exit code $LASTEXITCODE"
            }
        }
    } else {
        Write-Host ""
        Write-Host "SKIP 11-route acceptance"
    }

    Invoke-Step "quality readiness" {
        & node scripts\quality-readiness-check.mjs
        if ($LASTEXITCODE -ne 0) {
            throw "quality-readiness-check failed with exit code $LASTEXITCODE"
        }
    }

    Invoke-Step "ending share image contract" {
        & node scripts\ending-share-screenshot-check.mjs
        if ($LASTEXITCODE -ne 0) {
            throw "ending-share-screenshot-check failed with exit code $LASTEXITCODE"
        }
    }

    if (-not $SkipBrowserEvidence) {
        Invoke-Step "browser UI evidence" {
            $classpath = Build-Classpath
            $manualProcess = Start-ManualServer -Classpath $classpath -Port $UiPort
            & node scripts\browser-ui-evidence-check.mjs --app "http://localhost:$UiPort/" --out target\browser-ui-evidence
            if ($LASTEXITCODE -ne 0) {
                throw "browser-ui-evidence-check failed with exit code $LASTEXITCODE"
            }
        }
    } else {
        Write-Host ""
        Write-Host "SKIP browser UI evidence"
    }

    Write-Host ""
    Write-Host "[RELEASE CANDIDATE] PASS"
    Write-Host "Evidence refreshed:"
    Write-Host "- target\quality-readiness-report.md"
    Write-Host "- target\ending-share-screenshot-check.json"
    if (-not $SkipRoutes) {
        Write-Host "- target\acceptance-routes-report.md"
        Write-Host "- target\acceptance-routes.json"
    }
    if (-not $SkipBrowserEvidence) {
        Write-Host "- target\browser-ui-evidence\browser-ui-evidence-report.json"
        Write-Host "- target\browser-ui-evidence\*.png"
    }
} finally {
    Stop-ProcessSafe -Process $manualProcess
}
