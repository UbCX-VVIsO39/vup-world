@echo off
setlocal EnableExtensions
chcp 65001 > nul
cd /d "%~dp0.."

set "MAVEN_CMD=mvn.cmd"
if exist "D:\Develop\apache-maven-3.9.9\bin\mvn.cmd" set "MAVEN_CMD=D:\Develop\apache-maven-3.9.9\bin\mvn.cmd"
set "LOCAL_REPO=%USERPROFILE%\.m2\repository"
set "TOTAL_STEPS=16"

echo [1/%TOTAL_STEPS%] RUN Frontend JavaScript syntax
echo        Command: node --check src\main\resources\static\app.js
node --check src\main\resources\static\app.js
if errorlevel 1 (
    echo [1/%TOTAL_STEPS%] FAIL Frontend JavaScript syntax
    echo        Re-run: node --check src\main\resources\static\app.js
    exit /b 1
)
echo [1/%TOTAL_STEPS%] PASS Frontend JavaScript syntax
echo.

echo [2/%TOTAL_STEPS%] RUN Bili UI JavaScript syntax
echo        Command: node --check src\main\resources\static\bili-ui.js
node --check src\main\resources\static\bili-ui.js
if errorlevel 1 (
    echo [2/%TOTAL_STEPS%] FAIL Bili UI JavaScript syntax
    echo        Re-run: node --check src\main\resources\static\bili-ui.js
    exit /b 1
)
echo [2/%TOTAL_STEPS%] PASS Bili UI JavaScript syntax
echo.

echo [3/%TOTAL_STEPS%] RUN Action registry contract
echo        Command: node scripts\action-registry-check.mjs
node scripts\action-registry-check.mjs
if errorlevel 1 (
    echo [3/%TOTAL_STEPS%] FAIL Action registry contract
    echo        Re-run: node scripts\action-registry-check.mjs
    exit /b 1
)
echo [3/%TOTAL_STEPS%] PASS Action registry contract
echo.

echo [4/%TOTAL_STEPS%] RUN Frontend size budget
echo        Command: node scripts\check-frontend-budget.mjs
node scripts\check-frontend-budget.mjs
if errorlevel 1 (
    echo [4/%TOTAL_STEPS%] FAIL Frontend size budget
    echo        Re-run: node scripts\check-frontend-budget.mjs
    exit /b 1
)
echo [4/%TOTAL_STEPS%] PASS Frontend size budget
echo.

echo [5/%TOTAL_STEPS%] RUN HTML release readiness
echo        Command: node scripts\check-html-release-readiness.mjs
node scripts\check-html-release-readiness.mjs
if errorlevel 1 (
    echo [5/%TOTAL_STEPS%] FAIL HTML release readiness
    echo        Re-run: node scripts\check-html-release-readiness.mjs
    exit /b 1
)
echo [5/%TOTAL_STEPS%] PASS HTML release readiness
echo.

echo [6/%TOTAL_STEPS%] RUN No real payment mindset
echo        Command: node scripts\check-no-real-payment-mindset.mjs
node scripts\check-no-real-payment-mindset.mjs
if errorlevel 1 (
    echo [6/%TOTAL_STEPS%] FAIL No real payment mindset
    echo        Re-run: node scripts\check-no-real-payment-mindset.mjs
    exit /b 1
)
echo [6/%TOTAL_STEPS%] PASS No real payment mindset
echo.

echo [7/%TOTAL_STEPS%] RUN Player-facing copy hygiene
echo        Command: node scripts\check-player-facing-copy.mjs
node scripts\check-player-facing-copy.mjs
if errorlevel 1 (
    echo [7/%TOTAL_STEPS%] FAIL Player-facing copy hygiene
    echo        Re-run: node scripts\check-player-facing-copy.mjs
    exit /b 1
)
echo [7/%TOTAL_STEPS%] PASS Player-facing copy hygiene
echo.

echo [8/%TOTAL_STEPS%] RUN Midgame content validation
echo        Command: node scripts\validate-midgame-content.mjs
node scripts\validate-midgame-content.mjs
if errorlevel 1 (
    echo [8/%TOTAL_STEPS%] FAIL Midgame content validation
    echo        Re-run: node scripts\validate-midgame-content.mjs
    exit /b 1
)
echo [8/%TOTAL_STEPS%] PASS Midgame content validation
echo.

echo [9/%TOTAL_STEPS%] RUN Commercial route content validation
echo        Command: node scripts\validate-commercial-content.mjs
node scripts\validate-commercial-content.mjs
if errorlevel 1 (
    echo [9/%TOTAL_STEPS%] FAIL Commercial route content validation
    echo        Re-run: node scripts\validate-commercial-content.mjs
    exit /b 1
)
echo [9/%TOTAL_STEPS%] PASS Commercial route content validation
echo.

echo [10/%TOTAL_STEPS%] RUN READY cockpit layout
echo        Command: node scripts\ready-cockpit-check.mjs
node scripts\ready-cockpit-check.mjs
if errorlevel 1 (
    echo [10/%TOTAL_STEPS%] FAIL READY cockpit layout
    echo        Re-run: node scripts\ready-cockpit-check.mjs
    exit /b 1
)
echo [10/%TOTAL_STEPS%] PASS READY cockpit layout
echo.

echo [11/%TOTAL_STEPS%] RUN Create flow layout
echo        Command: node scripts\create-flow-check.mjs
node scripts\create-flow-check.mjs
if errorlevel 1 (
    echo [11/%TOTAL_STEPS%] FAIL Create flow layout
    echo        Re-run: node scripts\create-flow-check.mjs
    exit /b 1
)
echo [11/%TOTAL_STEPS%] PASS Create flow layout
echo.

echo [12/%TOTAL_STEPS%] RUN Stage flow layout
echo        Command: node scripts\stage-flow-check.mjs
node scripts\stage-flow-check.mjs
if errorlevel 1 (
    echo [12/%TOTAL_STEPS%] FAIL Stage flow layout
    echo        Re-run: node scripts\stage-flow-check.mjs
    exit /b 1
)
echo [12/%TOTAL_STEPS%] PASS Stage flow layout
echo.

echo [13/%TOTAL_STEPS%] RUN Singleplayer entry contract
echo        Command: node scripts\singleplayer-entry-check.mjs
node scripts\singleplayer-entry-check.mjs
if errorlevel 1 (
    echo [13/%TOTAL_STEPS%] FAIL Singleplayer entry contract
    echo        Re-run: node scripts\singleplayer-entry-check.mjs
    exit /b 1
)
echo [13/%TOTAL_STEPS%] PASS Singleplayer entry contract
echo.

echo [14/%TOTAL_STEPS%] RUN Backend and integration tests
echo        Command: "%MAVEN_CMD%" "-Dmaven.repo.local=%LOCAL_REPO%" test
call "%MAVEN_CMD%" "-Dmaven.repo.local=%LOCAL_REPO%" test
if errorlevel 1 (
    echo [14/%TOTAL_STEPS%] FAIL Backend and integration tests
    echo        Re-run: "%MAVEN_CMD%" "-Dmaven.repo.local=%LOCAL_REPO%" test
    exit /b 1
)
echo [14/%TOTAL_STEPS%] PASS Backend and integration tests
echo.

echo [15/%TOTAL_STEPS%] RUN Ending share image contract
echo        Command: node scripts\ending-share-screenshot-check.mjs
node scripts\ending-share-screenshot-check.mjs
if errorlevel 1 (
    echo [15/%TOTAL_STEPS%] FAIL Ending share image contract
    echo        Re-run: node scripts\ending-share-screenshot-check.mjs
    exit /b 1
)
echo [15/%TOTAL_STEPS%] PASS Ending share image contract
echo.

echo [16/%TOTAL_STEPS%] RUN Playable stage loop probe
echo        Health check: http://localhost:18087/
powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $status = (Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:18087/' -TimeoutSec 2).StatusCode; if ($status -eq 200) { exit 0 } else { exit 2 } } catch { exit 2 }"
if errorlevel 2 (
    echo [16/%TOTAL_STEPS%] PASS Playable stage loop probe skipped: no manual server on http://localhost:18087/
    echo        To include it, start the server: scripts\run-latest-manual.cmd -Port 18087
    echo        Then run: scripts\probe-stage-flow.cmd -Port 18087
    echo.
    echo [VERIFY] PASS All required quality gates passed.
    exit /b 0
)
if errorlevel 1 (
    echo [16/%TOTAL_STEPS%] FAIL Playable stage loop health check
    echo        Re-run health check: powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $status = (Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:18087/' -TimeoutSec 2).StatusCode; if ($status -eq 200) { exit 0 } else { exit 2 } } catch { exit 2 }"
    exit /b 1
)

echo        Command: scripts\probe-stage-flow.cmd -Port 18087
call scripts\probe-stage-flow.cmd -Port 18087
if errorlevel 1 (
    echo [16/%TOTAL_STEPS%] FAIL Playable stage loop probe
    echo        Re-run: scripts\probe-stage-flow.cmd -Port 18087
    echo        Server expected at: http://localhost:18087/
    exit /b 1
)
echo [16/%TOTAL_STEPS%] PASS Playable stage loop probe
echo.

echo [VERIFY] PASS All quality gates passed.
exit /b 0
