import { readFileSync } from 'node:fs';

const app = readFileSync('src/main/resources/static/app.js', 'utf8');
const css = readFileSync('src/main/resources/static/app.css', 'utf8');

const fail = message => {
    console.error(message);
    process.exit(1);
};

const mustContain = (haystack, needle, message) => {
    if (!haystack.includes(needle)) fail(message);
};

const mustPrecede = (haystack, first, second, message) => {
    const firstIndex = haystack.indexOf(first);
    const secondIndex = haystack.indexOf(second);
    if (firstIndex === -1 || secondIndex === -1 || firstIndex > secondIndex) fail(message);
};

const simpleDecisionIndex = app.indexOf('class="simple-decision-card');
const detailListIndex = app.indexOf('<div class="action-list action-detail-list">');
const defenseIndex = app.indexOf('${defenseCockpit}');

if (simpleDecisionIndex < 0 || detailListIndex < 0) {
    fail('READY cockpit markup is missing the simplified decision card or folded detail list.');
}

if (defenseIndex >= 0 || app.includes('const defenseCockpit = renderDefenseCockpit();')) {
    fail('READY cockpit must keep classroom demo controls out of the playable action screen.');
}

if (app.includes('<div class="action-list desktop-action-list">${items}</div>')) {
    fail('READY first screen must not stack the full action list beside the simplified decision card.');
}

mustContain(app, 'const dailyPlanCards = renderDailyPlanCards(primaryReadyAction, quickActions);', 'READY panel must build the daily plan board from the selected primary action.');
mustContain(app, '${dailyPlanCards}', 'READY panel must render the daily plan board in the active panel markup.');
mustPrecede(app, 'const dailyPlanCards = renderDailyPlanCards(primaryReadyAction, quickActions);', '${dailyPlanCards}', 'READY daily plan board must be built before it is rendered.');
mustContain(app, 'const recommendedActionTypes = new Set(quickActions', 'READY detail cards must derive recommended action types from the current quick actions.');
mustContain(app, 'const recommended = recommendedActionTypes.has(action.actionType);', 'READY detail cards must calculate per-action recommended state.');
mustContain(app, 'data-recommended="${recommended ? \'true\' : \'false\'}"', 'READY detail cards must expose data-recommended for styling and focus checks.');
mustContain(app, 'data-action-state="${html(cardState)}"', 'READY detail cards must expose data-action-state for recommended/playable/blocked states.');
mustContain(app, '<span class="action-state-pill">${html(actionCardStateLabel(cardState))}</span>', 'READY detail cards must show a compact state pill.');
mustContain(app, 'const DAILY_SCHEDULE_PRESETS = [', 'READY daily plan board must expose schedule presets.');
mustContain(app, 'function dailyScheduleRecommendedPlanKey(primaryAction, quickActions)', 'READY daily plan board must choose a recommended schedule preset.');
mustContain(app, 'function normalizeDailyScheduleForResources(slots)', 'READY daily plan board must normalize stamina/material/inspiration resources.');
mustContain(app, 'data-action="daily-plan-next-step"', 'READY board must expose a primary recommended next-step CTA.');
mustContain(app, 'data-action-type="${html(primaryActionType)}"', 'READY next-step CTA must expose the selected primary schedule action type for probes.');
mustContain(app, 'data-action="daily-schedule-preset"', 'READY board must expose strategy preset buttons.');
mustContain(app, 'data-change-action="daily-schedule-action-change"', 'READY board must let players edit each scheduled action.');
mustContain(app, 'data-change-action="daily-schedule-intensity-change"', 'READY board must let players edit each scheduled intensity.');
mustContain(app, "await api('/api/day/schedule'", 'READY schedule submission must call the real day schedule API.');
mustContain(app, "async function submitAction(actionType)", 'READY action submission must keep a single actionType entrypoint.');
mustContain(app, "async function doSubmitAction(actionType, options = {})", 'READY action submission must flow through doSubmitAction with the chosen actionType.');
mustContain(app, 'const requestBody = {\n            actionType,', 'READY action submission must include actionType in the request body object.');
mustContain(app, "idempotencyKey: stableIdempotencyKey('day-action', keyParts)", 'READY action submission must attach a stable day-action idempotency key.');
mustContain(app, "await api('/api/day/action'", 'READY action submission must call the real day action API.');
mustContain(app, 'body: requestBody', 'READY action submission must send the requestBody containing actionType to the API.');
mustPrecede(app, 'const primaryActionType = dailySchedulePrimaryActionType(draft);', 'data-action-type="${html(primaryActionType)}"', 'READY next-step CTA must be wired from the active schedule draft.');

for (const marker of [
    'action.routeBiasLabel',
    'action.riskLevel',
    'action.tempoHint',
    'action.comboKey',
    'actionComboShortText(action)',
    'action-combo-strip',
    'action-route-strip',
    'actionTradeoffMetrics',
    'action-tradeoff-meter',
    'actionScoreSignals',
    'action-score-signals',
    'score-guide-card',
    'openScoreGuideTarget',
    'pulseFocusTarget',
    'renderRestartContractStrip',
    'restart-contract-strip',
    'primaryWeaknessLabel',
    'stageOneGoal',
    'atlasTargetActionType',
    'openAtlasGoalTarget',
    'data-atlas-action-type',
    '.ending-forecast-gap-primary',
    '.combo-next-target',
    '.risk-tool-chip:not(.blocked):not(:disabled), .risk-tool-chip',
    '.cockpit-debt-row.hot, .cockpit-debt-row.watch, .cockpit-debt-row',
    'renderSimpleDecisionCard',
    'simple-decision-card',
    'simple-primary-action',
    'simple-alt-action',
    'simple-action-options-wrap',
    'advanced-ready-details',
    'fanTopicChoiceChips',
    'fan-topic-choice-chip cost',
    'fan-topic-choice-chip effect',
    'fan-topic-choice-chip risk',
    'riskToolChips',
    'risk-tool-token cost',
    'risk-tool-token effect',
    'risk-tool-token state',
    'lastRiskToolResult',
    'renderRiskToolResult',
    'risk-tool-result-chips',
    '路线'
]) {
    if (!app.includes(marker)) {
        fail(`READY action cards must expose route-building gameplay context: missing ${marker}.`);
    }
}

if (!/\.action-route-strip\s*\{[\s\S]*?grid-template-columns:\s*minmax\(88px,\s*auto\)\s*auto\s*minmax\(0,\s*1fr\)/.test(css)) {
    fail('READY action cards need a compact route/risk/tempo strip.');
}

if (!/\.action-route-strip \.action-combo-strip\s*\{[\s\S]*?color:\s*#ffb36b/.test(css)) {
    fail('READY action cards must display combo previews as a compact strip state.');
}

if (!/\.action-tradeoff-meter\s*\{[\s\S]*?grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/.test(css)) {
    fail('READY action cards need four compact tradeoff meters for payoff/risk/resource/route commitment.');
}

if (!/\.action-tradeoff-item i\s*\{[\s\S]*?overflow:\s*hidden/.test(css)) {
    fail('READY tradeoff meters need stable bars that do not resize the action card.');
}

if (!/\.action-score-signals\s*\{[\s\S]*?flex-wrap:\s*wrap/.test(css)) {
    fail('READY action cards should preview final-score contribution signals compactly.');
}

if (!app.includes('label: "路线证据"') || !app.includes('label: "连招证据"')
    || !app.includes('label: "委托证据"') || !app.includes('label: "风险恢复"')) {
    fail('READY action score signals must explain route, combo, objective, and recovery scoring hooks.');
}

if (!/\.simple-decision-strip\s*\{[\s\S]*?grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/.test(css)) {
    fail('READY decision card should show four compact tradeoff signals without adding a scroller.');
}

if (!app.includes('<em>收益</em>') || !app.includes('<em>风险</em>')
    || !app.includes('<em>结局</em>') || !app.includes('<em>旧账</em>')) {
    fail('READY decision card must expose payoff, risk, ending direction, and debt pressure.');
}

if (!/\.score-guide-grid\s*\{[\s\S]*?grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/.test(css)) {
    fail('READY side rail should teach route/evidence/risk scoring as a compact guide.');
}

if (!app.includes('data-action="open-score-guide-route"')
    || !app.includes('data-action="open-score-guide-evidence"')
    || !app.includes('data-action="open-score-guide-risk"')) {
    fail('READY score guide should let players jump to route, evidence, and risk operations.');
}

if (app.indexOf("if (target === 'evidence')") > app.indexOf("document.querySelector('.ending-forecast-gap-primary')")
    || app.indexOf("document.querySelector('.ending-forecast-gap-primary')") > app.indexOf("document.querySelector('.combo-next-target')")) {
    fail('READY evidence guide should focus the current ending gap before falling back to combo discovery.');
}

if (app.indexOf("if (target === 'risk')") > app.indexOf('openDebtControl();')
    || !app.includes("document.querySelector('.risk-tool-chip:not(.blocked):not(:disabled), .risk-tool-chip')")) {
    fail('READY risk guide should use the debt-control focus path and land on an actionable risk tool.');
}

if (!/\.score-guide-grid button:hover,[\s\S]*?\.score-guide-grid button:focus-visible\s*\{/.test(css)) {
    fail('READY score guide operation buttons need visible hover/focus states.');
}

if (!/\.fan-topic-choice-chip-row\s*\{[\s\S]*?grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/.test(css)) {
    fail('Fan topic choices need compact cost/effect/risk chips.');
}

if (!/\.fan-topic-choice-meta\s*\{[\s\S]*?display:\s*none/.test(css)) {
    fail('Fan topic choices should not spend first-screen height on duplicate long metadata.');
}

if (!/\.risk-tool-token-row\s*\{[\s\S]*?grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/.test(css)) {
    fail('Risk tools need compact cost/effect/state chips.');
}

if (!/\.risk-tool-result-chips\s*\{[\s\S]*?grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/.test(css)) {
    fail('Risk tool results should explain severity, popularity, reputation, and resource deltas compactly.');
}

if (!/\.debt-tool-meta\s*\{[\s\S]*?display:\s*none/.test(css)) {
    fail('Risk tools should not spend first-screen height on duplicate long metadata.');
}

if (!/\.simple-decision-card\s*\{[\s\S]*?display:\s*grid/.test(css)) {
    fail('READY simplified decision card must be visible on desktop.');
}

if (!/\.simple-alt-actions\s*\{[\s\S]*?grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/.test(css)) {
    fail('READY alternative actions should use a compact desktop grid.');
}

if (!/\.ready-mode \.action-detail-list\s*\{[\s\S]*?grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/.test(css)) {
    fail('Folded full action list should still expose all actions as a compact grid.');
}

if (!/\.ready-mode \.action-item\[data-recommended="true"\]\s*\{[\s\S]*?border-color:/.test(css)) {
    fail('READY recommended detail action needs a visible highlighted card state.');
}

if (!/\.ready-mode \.action-item\[data-recommended="true"\] \.action-state-pill\s*\{[\s\S]*?background:/.test(css)) {
    fail('READY recommended detail action needs a distinct state pill.');
}

if (!app.includes('function renderDefenseCockpit()') || !app.includes('data-action="demo-reset-run"')) {
    fail('Classroom demo controls should remain available from the demo panel.');
}

if (!app.includes('function activeInfoPanelIds(target)')
    || !app.includes("npc: stageFocus ? ['npcPanel', 'stageBriefingPanel'] : ['npcPanel']")) {
    fail('READY right rail should keep stage briefing out of the default NPC tab.');
}

if (!app.includes('class="npc-radar-grid"') || !app.includes('class="npc-nextplay"')) {
    fail('READY NPC panel should use a compact radar summary plus one next-play line.');
}

if (!/\.ready-mode \.npc-radar-grid,[\s\S]*?\.ready-mode \.npc-signal-grid\s*\{[\s\S]*?grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/.test(css)) {
    fail('READY NPC signals must fit as a compact two-column radar.');
}

if (!/\.ready-mode #stageBriefingPanel\.active\s*\{[\s\S]*?display:\s*none/.test(css)) {
    fail('READY right rail must not stack stage briefing below the NPC panel.');
}

if (!/\.ready-mode \.action-panel\s*\{[\s\S]*?overflow:\s*hidden/.test(css)) {
    fail('READY action panel must avoid becoming a long internal scroller.');
}

console.log('READY cockpit check passed.');
