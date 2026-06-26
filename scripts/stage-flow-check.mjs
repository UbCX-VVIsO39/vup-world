import { readFileSync } from 'node:fs';

const app = readFileSync('src/main/resources/static/app.js', 'utf8');
const css = readFileSync('src/main/resources/static/app.css', 'utf8');
const index = readFileSync('src/main/resources/static/index.html', 'utf8');
const registry = readFileSync('src/main/resources/static/js/action-registry.js', 'utf8');
const actionService = readFileSync('src/main/java/com/example/vupworld/service/ActionService.java', 'utf8');
const debtService = readFileSync('src/main/java/com/example/vupworld/service/risk/DebtService.java', 'utf8');
const endingService = readFileSync('src/main/java/com/example/vupworld/service/ending/EndingService.java', 'utf8');

const fail = message => {
    console.error(message);
    process.exit(1);
};

const ruleFor = selector => {
    const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    return css.match(new RegExp(`${escaped}\\s*\\{[\\s\\S]*?\\n\\s*\\}`))?.[0] || '';
};

const mustContain = (haystack, needle, message) => {
    if (!haystack.includes(needle)) fail(message);
};

const mustNotContain = (haystack, needle, message) => {
    if (haystack.includes(needle)) fail(message);
};

const mustMatch = (haystack, pattern, message) => {
    if (!pattern.test(haystack)) fail(message);
};

const mustPrecede = (haystack, first, second, message) => {
    const firstIndex = haystack.indexOf(first);
    const secondIndex = haystack.indexOf(second);
    if (firstIndex === -1 || secondIndex === -1 || firstIndex > secondIndex) fail(message);
};

const branchFor = (haystack, marker) => {
    const start = haystack.indexOf(marker);
    if (start === -1) return '';
    const nextElse = haystack.indexOf('} else if', start + marker.length);
    const nextHandlerEnd = haystack.indexOf('\n    }\n});', start + marker.length);
    const endCandidates = [nextElse, nextHandlerEnd].filter(index => index !== -1);
    const end = endCandidates.length ? Math.min(...endCandidates) : haystack.length;
    return haystack.slice(start, end);
};

for (const marker of [
    'title-decision-loop',
    'titleDecisionChips',
    'title-chip heat',
    'title-chip risk',
    'title-chip route',
    'event-decision-loop',
    'event-pressure-strip',
    'eventPressureChips',
    'event-pressure-chip severity',
    'event-pressure-chip choices',
    'event-pressure-chip tempo',
    'eventChoiceChips',
    'event-choice-line payoff',
    'event-choice-line cost',
    'event-choice-line route',
    'event-choice-line debt',
    'report-loop',
    'ending-main'
]) {
    mustContain(app, marker, `Stage flow markup is missing ${marker}.`);
}

mustContain(app, 'data-stage-hotkey', 'Stage choices need visible keyboard shortcuts.');
mustContain(app, 'data-stage-advance-hotkey="Enter"', 'Report and ending advance buttons need Enter shortcuts.');
mustContain(app, '<details class="report-details">', 'Report details should be folded behind a details control.');
mustContain(app, '<details class="ending-details">', 'Ending evidence should be folded behind a details control.');
mustContain(app, 'class="daily-plan-card-facts"', 'READY plan cards should expose a compact decision fact strip.');
mustContain(app, '<em>体力</em>', 'READY schedule slots should label stamina cost.');
mustContain(app, '<em>资源</em>', 'READY schedule slots should label material and inspiration tradeoffs.');
mustContain(app, '<em>路线倾向</em>', 'READY plan cards should label the route tendency.');
mustContain(app, '<em>顺序</em>', 'READY schedule slots should label the time-slot order.');
mustContain(app, 'DAILY_SCHEDULE_PRESETS', 'READY panel should define the strategy preset list.');
mustContain(app, '推荐：${plan.shortLabel}', 'The recommended READY schedule should show a one-line recommendation badge.');
mustContain(app, 'daily-schedule-adjustments', 'READY schedule should show visible automatic stamina/resource adjustments.');
mustContain(app, 'data-action="daily-schedule-preset"', 'READY plan board should expose strategy preset buttons.');
mustContain(app, 'data-change-action="daily-schedule-action-change"', 'READY schedule slots should expose editable action selects.');
mustContain(app, 'data-change-action="daily-schedule-intensity-change"', 'READY schedule slots should expose editable intensity selects.');
mustContain(app, '排班预设', 'READY panel badge should describe schedule presets.');
mustContain(app, '<summary>展开详细数值</summary>', 'Detailed READY numbers should be folded behind "展开详细数值".');
mustContain(app, 'endingProofCards', 'Ending route proof cards should expose a short evidence recap.');
mustContain(app, 'routeEvidenceTrail || []).slice(-3)', 'Ending proof cards should use only the latest route evidence items.');
mustContain(app, 'class="ending-proof-strip" aria-label=', 'Ending proof strip needs an accessible label.');
mustContain(app, 'class="ending-conclusion-summary"', 'Ending first screen should render a one-sentence conclusion before evidence.');
mustContain(app, 'class="ending-proof-strip ending-proof-strip-first"', 'Ending first screen should show the three key evidence cards before restart.');
mustContain(app, 'class="ending-next-run-card"', 'Ending first screen should show next-run advice before restart.');
mustContain(app, 'function renderEndingGradeContract(scorecard, nextRunGoal)', 'Ending review should turn the scorecard into a replay grade contract.');
mustContain(app, 'class="ending-grade-contract"', 'Ending restart console should render the replay grade contract.');
mustContain(app, 'function renderRestartBiasPreview(routeKey)', 'Ending restart choice should preview the selected replay bias.');
mustContain(app, 'class="ending-restart-preview"', 'Ending restart console should show target, first-week plan, and boundary preview.');
mustContain(app, 'id="restartBias"', 'Ending restart flow must render a route-bias select before submitting restart.');
mustMatch(index, /data-action="ending-restart"[^>]*>[^<]*(选路线|复盘|路线)[^<]*<\/button>/, 'Ending fullscreen restart button should send players to route selection instead of direct restart.');
mustContain(registry, "registerAction('ending-restart', endingRestartAction)", 'Ending fullscreen restart should be owned by action-registry.');
const endingRestartBranch = registry.slice(
    registry.indexOf('function endingRestartAction()'),
    registry.indexOf('function switchMainTabAction')
);
mustContain(endingRestartBranch, "getElementById('restartBias')", 'Ending fullscreen restart should find the route-bias selector.');
mustContain(endingRestartBranch, '.focus(', 'Ending fullscreen restart should focus the route-bias selector.');
if (/\brestart\(\)/.test(endingRestartBranch)) {
    fail('Ending fullscreen restart must not call restart() before the route-bias selector is visible.');
}
mustContain(app, 'function renderEndingAdvancedEvidence(scorecard)', 'Ending review should expose advanced play evidence from the scorecard.');
mustContain(app, 'class="ending-advanced-evidence"', 'Ending scorecard should render advanced play evidence chips.');
const renderEndingSource = app.slice(app.indexOf('function renderEnding()'), app.indexOf('function animateEndingReveal()'));
mustPrecede(renderEndingSource, 'class="ending-title"', '${renderEndingGradeCallout(scorecard)}', 'Ending title should appear before rating.');
mustPrecede(renderEndingSource, '${renderEndingGradeCallout(scorecard)}', '${endingConclusionSummary}', 'Ending rating should appear before one-sentence conclusion.');
mustPrecede(renderEndingSource, '${endingConclusionSummary}', '${endingFirstScreenProof}', 'Ending conclusion should appear before three key evidence cards.');
mustPrecede(renderEndingSource, '${endingFirstScreenProof}', '${endingNextRunCard}', 'Ending evidence should appear before next-run advice.');
mustPrecede(renderEndingSource, '${endingNextRunCard}', 'class="ending-restart-simple"', 'Ending next-run advice should appear before restart controls.');
mustPrecede(renderEndingSource, 'class="ending-restart-simple"', 'class="ending-reason-card"', 'Ending restart controls should stay above the long reason copy.');

const titleListRule = ruleFor('.title-mode .title-list');
mustMatch(titleListRule, /grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/, 'Title choices should fit as three columns on desktop.');

const titleItemRule = ruleFor('.title-mode .title-item');
mustMatch(titleItemRule, /overflow:\s*hidden/, 'Title cards must not grow into a long scroller.');

mustMatch(css, /\.daily-plan-grid\s*\{[\s\S]*?grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/, 'READY plan groups should fit recommendation, route, repair, and rest on desktop.');
mustMatch(css, /\.daily-plan-card \.daily-plan-disabled-reason\s*\{[\s\S]*?-webkit-line-clamp:\s*2/, 'READY disabled reasons should stay visible without growing into a long card.');
mustMatch(css, /\.daily-plan-card-facts span\s*\{[\s\S]*?grid-template-columns:\s*54px\s*minmax\(0,\s*1fr\)/, 'READY card fact labels should fit the four explicit labels.');
mustMatch(css, /\.daily-schedule-strategies\s*\{[\s\S]*?grid-template-columns:\s*repeat\(6,\s*minmax\(0,\s*1fr\)\)/, 'READY schedule strategies should fit six strategy buttons on desktop.');
mustMatch(css, /\.daily-schedule-budget\s*\{[\s\S]*?grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/, 'READY schedule budget should expose four resource chips.');

mustContain(app, '<em>热度</em>', 'Title decision cards should label heat.');
mustContain(app, '<em>口碑风险</em>', 'Title decision cards should label reputation risk.');
mustContain(app, '<em>路线倾向</em>', 'Title decision cards should label route tendency.');
mustContain(app, 'function titleRerollButtonState()', 'Title reroll button should expose remaining count and cost.');
mustContain(app, '`剩余${remaining}次`', 'Title reroll button should show remaining rerolls.');
mustContain(app, '"代价：灵感-1"', 'Title reroll button should show the inspiration cost.');
mustContain(app, 'function titleCancelButtonState()', 'Title cancel button should expose the consequence.');
mustContain(app, '后果：回到行动，今天不能再开直播企划', 'Title cancel button should explain the same-day consequence.');
mustMatch(css, /\.title-decision-strip\s*\{[\s\S]*?grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/, 'Title decision strip should show heat/risk/route chips in three equal columns.');

const eventBoxRule = ruleFor('.event-mode .event-box');
mustMatch(eventBoxRule, /grid-template-areas:[\s\S]*"pressure image"[\s\S]*"choices image"/, 'Event flow should show a pressure strip before choices while keeping the illustration aside.');

const eventDescriptionRule = ruleFor('.event-mode .event-description');
mustMatch(eventDescriptionRule, /-webkit-line-clamp:\s*2/, 'Event description should be clamped for one-screen play.');

mustMatch(css, /\.event-pressure-strip\s*\{[\s\S]*?grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/, 'Event pressure strip should summarize severity, options, and tempo in three compact chips.');
mustMatch(css, /\.event-mode \.event-pressure-strip\s*\{[\s\S]*?grid-area:\s*pressure/, 'Event pressure strip must occupy the pressure grid area.');
mustContain(app, 'function eventVisibleDescriptionText(event)', 'Event middle copy should be collapsed to one visible sentence.');
mustContain(app, '<details class="event-background-details">', 'Long event background should be folded behind a details control.');
mustContain(app, '<summary>展开长背景和氛围</summary>', 'Event background details should have a clear summary label.');
mustContain(app, '<em>获得什么</em>', 'Event choices should label payoff.');
mustContain(app, '<em>牺牲什么</em>', 'Event choices should label cost.');
mustContain(app, '<em>路线影响</em>', 'Event choices should label route impact.');
mustContain(app, '<em>可能旧账</em>', 'Event choices should label possible debt.');
mustMatch(css, /\.event-choice-lines\s*\{[\s\S]*?grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/, 'Event choice lines should summarize payoff/cost/route/debt in four compact columns.');
mustMatch(css, /\.event-choice-line\.cost\s*\{[\s\S]*?border-color:/, 'Event cost line needs its own visual state.');
mustMatch(css, /\.event-choice-line\.payoff\s*\{[\s\S]*?border-color:/, 'Event payoff line needs its own visual state.');
mustMatch(css, /\.event-choice-line\.route\s*\{[\s\S]*?border-color:/, 'Event route line needs its own visual state.');
mustMatch(css, /\.event-choice-line\.debt\s*\{[\s\S]*?border-color:/, 'Event debt line needs its own visual state.');

const reportPanelRule = ruleFor('.report-mode .report-panel');
mustMatch(reportPanelRule, /padding:\s*12px/, 'Report panel should use compact stage padding.');
mustContain(app, 'reportKpiShortText', 'Report KPIs should use short game-feedback text instead of long report paragraphs.');
mustContain(app, 'const platformTrendKpi = reportKpiShortText(platformTrendDescription, "看风向");', 'Platform trend KPI should be shortened for first-screen play.');
mustContain(app, 'const materialReceiptKpi = reportKpiShortText(materialReceiptSummary, "看素材");', 'Material receipt KPI should be shortened for first-screen play.');
mustContain(app, 'const riskHintKpi = reportKpiShortText(riskHintText, "看风险");', 'Risk KPI should be shortened for first-screen play.');
mustContain(app, '<small>${html(platformTrendKpi)}</small>', 'Report platform KPI should render the short KPI text.');
mustContain(app, 'function reportRiskRecoveryCard(result, riskHintText)', 'Report should turn risk/debt into a playable recovery card.');
mustContain(app, 'data-action="open-report-risk-recovery"', 'Report risk recovery should link to the debt control loop.');
mustContain(app, 'function reportNextDayPlanCard(riskHintText)', 'Report should turn today results into a next-day plan.');
mustContain(app, 'class="report-next-plan-grid"', 'Report next-day plan should expose forecast, objective, and risk priorities.');
mustContain(app, 'data-action="open-report-next-risk"', 'Report next-day risk plan should link back to the debt-control loop.');
mustContain(app, 'label: "今天赚了什么"', 'Report first screen must label the payoff sentence.');
mustContain(app, 'label: "今天亏了什么或欠了什么"', 'Report first screen must label the cost/debt sentence.');
mustContain(app, 'label: "明天最该担心什么"', 'Report first screen must label the tomorrow-risk sentence.');
const renderReportSource = app.slice(app.indexOf('function renderReport()'), app.indexOf('function renderEnding()'));
mustPrecede(renderReportSource, '${reportThreeBeat}', '<p class="report-summary">${html(summaryText)}</p>', 'Report three sentences must be the first visible recap block.');
mustContain(app, 'function renderActionResultObjectiveReward(result)', 'Action result HUD should explain stage objective reward payouts.');
mustContain(app, 'class="action-result-objective-bonuses"', 'Action result HUD should show stage objective bonus chips.');
mustContain(actionService, 'actionResult = actionResult.withDebtCreated(actionDebtCreation.created());', 'Action flow should surface newly created action debt back into the result HUD.');
mustContain(actionService, 'log.setDebtIds(jsonService.write(actionDebtCreation.debtIds()));', 'Action flow should write newly created debt ids back into the business log.');
mustContain(actionService, 'detail.put("fatigueInfo", Map.of(', 'Action fatigue should be written into multiplier detail for later review.');
mustContain(debtService, 'public record ActionDebtCreation(', 'Debt service should expose action-debt ids for logging and review.');
mustContain(endingService, 'advancedEvidenceScore', 'Ending scorecard should include combo/objective/recovery evidence in scoring.');
mustContain(endingService, 'fatiguePenaltyCount', 'Ending scorecard should count fatigue as a scoring penalty.');
if (app.indexOf('if (/素材|切片|投稿|入库|弹药/.test(text))') > app.indexOf('if (/暂无|无明显|稳定|稳稳|稳住/.test(text))')) {
    fail('Report material KPI must classify material cues before the generic stable fallback.');
}
if (app.indexOf('if (/歌回|基本功|低压|稳健|平台口味|排班|推荐|流量/.test(text))') > app.indexOf('if (/风险|舆情|旧账|米线|反噬|回旋镖|标题太冲|压流/.test(text))')) {
    fail('Report platform KPI must classify platform cues before generic risk words in trend descriptions.');
}

const reportDetailsRule = ruleFor('.report-mode .report-details[open] .report-items');
mustMatch(reportDetailsRule, /max-height:\s*220px/, 'Expanded report details need an internal height cap.');
mustMatch(css, /\.report-risk-recovery\s*\{[\s\S]*?grid-template-columns:\s*minmax\(0,\s*1fr\)\s*auto/, 'Report risk recovery card should stay compact beside its CTA.');
mustMatch(css, /\.report-mode \.report-risk-recovery-copy small\s*\{[\s\S]*?-webkit-line-clamp:\s*1/, 'Compact report mode should clamp risk recovery details.');
mustMatch(css, /\.report-mode \.report-beat-card p\s*\{[\s\S]*?-webkit-line-clamp:\s*2/, 'Compact report mode should clamp the three report sentences.');
mustMatch(css, /\.report-next-plan-grid\s*\{[\s\S]*?grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/, 'Report next-day plan should keep three priorities in one compact row.');
mustMatch(css, /\.report-next-plan-item small\s*\{[\s\S]*?-webkit-line-clamp:\s*2/, 'Report next-day plan details should be clamped.');
mustMatch(css, /\.action-result-objective-bonuses\s*\{[\s\S]*?grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/, 'Stage objective reward chips should show the four payout channels compactly.');

const endingMainRule = ruleFor('.ending-main');
mustMatch(endingMainRule, /display:\s*grid/, 'Ending review should use a compact grid layout.');
mustMatch(endingMainRule, /gap:\s*10px/, 'Ending review should keep compact spacing between replay blocks.');

const endingImageRule = ruleFor('.ending-mode .ending-image');
mustMatch(endingImageRule, /max-height:\s*190px/, 'Ending image should be capped so restart controls stay visible.');

mustMatch(css, /(^|\n)\.ending-proof-strip\s*\{[\s\S]*?grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/, 'Ending proof strip should show the latest three evidence items as compact columns.');
mustMatch(css, /\.ending-conclusion-summary p\s*\{[\s\S]*?-webkit-line-clamp:\s*2/, 'Ending conclusion should stay compact on the first screen.');
mustMatch(css, /\.ending-grade-contract-grid\s*\{[\s\S]*?grid-template-columns:\s*minmax\(68px,\s*0\.7fr\)\s*repeat\(2,\s*minmax\(0,\s*1\.2fr\)\)/, 'Ending grade contract should show weakness and two-week replay goals in one compact row.');
mustMatch(css, /\.ending-restart-preview\s*\{[\s\S]*?grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/, 'Ending restart bias preview should compare target, first-week plan, and boundary in one row.');
mustMatch(css, /\.ending-advanced-evidence\s*\{[\s\S]*?grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/, 'Ending advanced evidence should fit combo/objective/recovery/fatigue in one compact row.');

const endingProofCardRule = ruleFor('.ending-mode .ending-proof-card');
mustMatch(endingProofCardRule, /min-height:\s*54px/, 'Desktop ending proof cards need a stable compact height.');
mustMatch(endingProofCardRule, /overflow:\s*hidden/, 'Desktop ending proof cards must not grow into the restart controls.');

mustContain(index, 'data-optional-chartjs="true"', 'Chart.js script must be marked optional.');
mustNotContain(index, "onload=\"window.dispatchEvent(new Event('chartjs-ready'))\"", 'Chart.js load notification must not use inline onload.');
mustNotContain(index, 'onerror="window.__chartJsUnavailable = true;"', 'Chart.js failure handling must not use inline onerror.');
mustContain(app, 'function bindOptionalChartJsLoader()', 'Chart.js local script load/error binding must live in app.js.');
mustContain(app, 'localScript.addEventListener(\'load\', handleOptionalChartJsLoad', 'Chart.js local load must notify app charts through a listener.');
mustContain(app, 'localScript.addEventListener(\'error\', function()', 'Chart.js local failure must be handled through a listener.');
mustContain(app, 'window.__chartJsUnavailable = true;', 'Chart.js failure must be explicitly non-blocking.');
mustContain(app, 'requestChartJsFallback();', 'Chart.js local placeholder or failure must request the CDN fallback.');
mustMatch(app, /isAvailable\(\)\s*\{[\s\S]*?typeof window\.Chart !== "undefined"[\s\S]*?\}/, 'Chart availability must be gated on window.Chart.');
mustMatch(app, /renderTextFallback\(canvas, title, items, note\)\s*\{[\s\S]*?canvas\.hidden = true[\s\S]*?fallback\.dataset\.chartFallback = "true"[\s\S]*?fallback\.setAttribute\("role", "note"\)/, 'Missing Chart.js must render an accessible text fallback.');
mustMatch(app, /renderRouteRadar\(canvasId\)[\s\S]*?if \(!this\.isAvailable\(\)\) \{[\s\S]*?this\.renderTextFallback\([\s\S]*?return;[\s\S]*?\}[\s\S]*?new window\.Chart/, 'Route radar must fallback before constructing Chart.');
mustMatch(app, /renderFanLine\(canvasId, reports\)[\s\S]*?if \(!this\.isAvailable\(\)\) \{[\s\S]*?this\.renderTextFallback\([\s\S]*?return;[\s\S]*?\}[\s\S]*?new window\.Chart/, 'Fan trend must fallback before constructing Chart.');
mustMatch(app, /window\.addEventListener\("chartjs-ready"[\s\S]*?CHARTS\.renderRouteRadar\("routeRadarChart"\);[\s\S]*?CHARTS\.renderFanLine\("fanTrendChart", state\.reports\);/, 'Charts must rerender when async Chart.js finishes loading.');
mustContain(app, 'function destroyChartForCanvas(canvas)', 'Chart lifecycle must have a shared canvas destroy helper.');
mustContain(app, 'id="fanSegmentTrendChart"', 'Segment trend chart must not reuse the aggregate fan trend canvas id.');
if ((app.match(/id="fanTrendChart"/g) || []).length !== 1) {
    fail('fanTrendChart id must be unique so Chart.js never binds two charts to the same canvas.');
}
mustMatch(app, /renderFanLine\(canvasId, reports\)[\s\S]*?destroyChartForCanvas\(canvas\);[\s\S]*?this\.fanLine = new window\.Chart/, 'Aggregate fan trend must destroy an existing canvas chart before constructing Chart.');
mustMatch(app, /function initFanChart\(fanStructure\)[\s\S]*?destroyChartForCanvas\(canvas\);[\s\S]*?new window\.Chart\(canvas/, 'Fan pie chart must destroy an existing canvas chart before constructing Chart.');
mustMatch(app, /function initTrendCharts\(reports\)[\s\S]*?document\.getElementById\('fanSegmentTrendChart'\)[\s\S]*?destroyChartForCanvas\(fanCanvas\);[\s\S]*?new window\.Chart\(fanCanvas/, 'Segment fan trend must destroy an existing canvas chart before constructing Chart.');
mustMatch(app, /const opinionCanvas = document\.getElementById\('opinionTrendChart'\);[\s\S]*?destroyChartForCanvas\(opinionCanvas\);[\s\S]*?new window\.Chart\(opinionCanvas/, 'Opinion trend must destroy an existing canvas chart before constructing Chart.');

for (const phase of ['title-mode', 'event-mode', 'report-mode', 'ending-mode']) {
    mustMatch(css, new RegExp(`\\.${phase} \\.game-panels[\\s\\S]*?overflow-y:\\s*hidden`), `${phase} must avoid a tall game panel scroller.`);
}


console.log('Stage flow check passed.');
