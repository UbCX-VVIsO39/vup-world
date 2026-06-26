import { readFileSync } from 'node:fs';

const app = readFileSync('src/main/resources/static/app.js', 'utf8');
const panelBundle = readFileSync('src/main/resources/static/js/panel-bundle.js', 'utf8');
const frontendApp = `${app}\n${panelBundle}`;
const index = readFileSync('src/main/resources/static/index.html', 'utf8');
const main = readFileSync('src/main/resources/static/js/main.js', 'utf8');
const registry = readFileSync('src/main/resources/static/js/action-registry.js', 'utf8');

const fail = message => {
    console.error(message);
    process.exit(1);
};

const registeredActions = new Map();
for (const match of registry.matchAll(/registerAction\('([^']+)',\s*([^\n;]+)/g)) {
    const handler = match[2].trim().replace(/\)+$/, '').trim();
    registeredActions.set(match[1], handler);
}

const staticActions = new Map();
const sourceFiles = [
    ['src/main/resources/static/index.html', index],
    ['src/main/resources/static/app.js', app],
    ['src/main/resources/static/js/panel-bundle.js', panelBundle]
];
for (const [file, source] of sourceFiles) {
    const inlineEvent = source.match(/\son(?:click|change|input|load|error|submit)=["']/);
    if (inlineEvent) {
        fail(`${file} must not contain inline HTML event attributes such as ${inlineEvent[0].trim()}.`);
    }
}
for (const [file, source] of sourceFiles) {
    const lines = source.split(/\r?\n/);
    lines.forEach((line, index) => {
        for (const match of line.matchAll(/data-action="([^"]+)"/g)) {
            const action = match[1];
            if (action.includes('${')) continue;
            if (!staticActions.has(action)) staticActions.set(action, []);
            staticActions.get(action).push({ file, line: index + 1, source: line.trim() });
        }
    });
}

const staticActionSources = action => staticActions.get(action) || [];

function collectLiteralActionAttribute(attribute) {
    const actions = new Map();
    const pattern = new RegExp(`${attribute}="([^"]+)"`, 'g');
    for (const [file, source] of sourceFiles) {
        const lines = source.split(/\r?\n/);
        lines.forEach((line, index) => {
            for (const match of line.matchAll(pattern)) {
                const action = match[1];
                if (action.includes('${')) continue;
                if (!actions.has(action)) actions.set(action, []);
                actions.get(action).push({ file, line: index + 1, source: line.trim() });
            }
        });
    }
    return actions;
}

for (const action of staticActions.keys()) {
    if (!registeredActions.has(action)) {
        fail(`Missing action-registry registration for data-action="${action}".`);
    }
}

for (const [attribute, actions] of [
    ['data-change-action', collectLiteralActionAttribute('data-change-action')],
    ['data-input-action', collectLiteralActionAttribute('data-input-action')]
]) {
    for (const action of actions.keys()) {
        if (!registeredActions.has(action)) {
            fail(`Missing action-registry registration for ${attribute}="${action}".`);
        }
    }
}

const dynamicContracts = [
    ['data-action="${html(dataAction)}"', ['choose-event', 'choose-interaction']],
    ['data-action="offstream-${html(String(type).toLowerCase())}"', []]
];

for (const [marker, required] of dynamicContracts) {
    if (!frontendApp.includes(marker)) {
        fail(`Expected dynamic action marker is missing: ${marker}`);
    }
    for (const action of required) {
        if (!registeredActions.has(action)) {
            fail(`Dynamic action ${action} must be registered.`);
        }
    }
}

const offStreamSource = frontendApp.slice(
    frontendApp.indexOf('const ROUTE_OFFSTREAM_OPTIONS'),
    frontendApp.indexOf('function routeVisualFor')
);
if (!offStreamSource) {
    fail('Offstream option source block is missing.');
}
for (const match of offStreamSource.matchAll(/type:\s*'([A-Z_]+)'/g)) {
    const action = `offstream-${match[1].toLowerCase()}`;
    if (!registeredActions.has(action)) {
        fail(`Dynamic offstream action ${action} must be registered.`);
    }
}

if (!main.includes("element.hasAttribute('onclick')")) {
    fail('main.js must let legacy onclick handlers own already-migrated-later buttons to prevent double writes.');
}

if (!registry.includes('function legacyAction(') || !registry.includes('callGlobal(')) {
    fail('action-registry must expose a real legacyAction compatibility route, not only noop registrations.');
}

if (!registry.includes('result.catch')) {
    fail('action-registry runAction must catch async handler failures.');
}

const registryWriteActions = [
    'register',
    'login',
    'logout',
    'create-vup',
    'quick-start-guest',
    'continue-local-run',
    'save-local-slot',
    'export-local-slot',
    'import-local-slot',
    'rename-local-slot',
    'restart-local-slot',
    'simple-primary-action',
    'simple-alt-action',
    'daily-plan-submit',
    'daily-plan-next-step',
    'daily-schedule-submit',
    'quick-submit-action',
    'detail-submit-action',
    'confirm-pending-action',
    'opening-style-choice',
    'skip-offstream',
    'choose-title',
    'reroll-title',
    'cancel-action',
    'choose-event',
    'choose-interaction',
    'choose-fan-topic',
    'use-risk-tool',
    'next-day',
    'restart',
    'restart-from-share-card',
    'copy-ending-share',
    'copy-stage-milestone',
    'download-ending-share-card',
    'demo-run',
    'demo-reset',
    'demo-reset-run',
    'demo-fast-forward'
];

for (const action of registryWriteActions) {
    const handler = registeredActions.get(action);
    if (!handler || handler.includes('noopAction')) {
        fail(`Action ${action} must be routed through a real registry handler, got ${handler || 'missing'}.`);
    }
}

const allowedNoopActions = new Set([
    'select-creation-style',
    'create-loadout-summary'
]);

for (const [action, handler] of registeredActions.entries()) {
    if (handler.includes('noopAction') && !allowedNoopActions.has(action)) {
        fail(`Unexpected noopAction registration for ${action}.`);
    }
}

for (const functionName of [
    'register',
    'login',
    'quickStartGuest',
    'continueLocalRun',
    'saveLocalSlot',
    'exportLocalSlot',
    'importLocalSlot',
    'restartLocalSlot',
    'renameLocalSlot',
    'logout',
    'createVup',
    'submitSchedule',
    'doSubmitAction',
    'chooseTitle',
    'rerollTitle',
    'chooseEvent',
    'chooseInteraction',
    'nextDay',
    'restart',
    'chooseFanTopic',
    'useRiskTool',
    'cancelAction'
]) {
    const pattern = new RegExp(`async function ${functionName}\\s*\\([\\s\\S]*?\\)\\s*\\{[\\s\\S]*?await withBusy\\(`);
    if (!pattern.test(frontendApp)) {
        fail(`${functionName} must enter its API write path through withBusy().`);
    }
}

for (const action of ['ending-review', 'ending-restart']) {
    const handler = registeredActions.get(action);
    if (!handler || handler.includes('noopAction')) {
        fail(`${action} should be handled by action-registry fullscreen focus logic.`);
    }
}

const migratedEndingActions = [
    'copy-ending-share',
    'download-ending-share-card',
    'restart-from-share-card',
    'restart'
];

for (const action of migratedEndingActions) {
    const inlineSources = staticActionSources(action).filter(item => /\bonclick\s*=/.test(item.source));
    if (inlineSources.length) {
        fail(`Ending journey action ${action} must be handled by action-registry without inline onclick. First inline source: ${inlineSources[0].file}:${inlineSources[0].line}`);
    }
}

for (const item of staticActionSources('copy-ending-share')) {
    if (!item.source.includes('data-share-text=')) {
        fail(`copy-ending-share migrated button must provide data-share-text at ${item.file}:${item.line}.`);
    }
}

for (const item of staticActionSources('download-ending-share-card')) {
    if (!item.source.includes('data-share-format=')) {
        fail(`download-ending-share-card migrated button must provide data-share-format at ${item.file}:${item.line}.`);
    }
}

const migratedTitleActions = [
    'choose-title',
    'reroll-title',
    'cancel-action'
];

const htmlStaticActionSources = action => staticActionSources(action).filter(item => item.source.includes('<'));

for (const action of migratedTitleActions) {
    const sources = htmlStaticActionSources(action);
    if (!sources.length) {
        fail(`Title journey action ${action} must remain present in static UI.`);
    }
    const inlineSources = sources.filter(item => /\bonclick\s*=/.test(item.source));
    if (inlineSources.length) {
        fail(`Title journey action ${action} must be handled by action-registry without inline onclick. First inline source: ${inlineSources[0].file}:${inlineSources[0].line}`);
    }
}

for (const item of htmlStaticActionSources('choose-title')) {
    if (!item.source.includes('data-title-id=')) {
        fail(`choose-title migrated button must provide data-title-id at ${item.file}:${item.line}.`);
    }
}

const migratedReportActions = [
    'copy-stage-milestone',
    'open-report-risk-recovery',
    'open-report-next-risk',
    'next-day'
];

for (const action of migratedReportActions) {
    const sources = htmlStaticActionSources(action);
    if (!sources.length) {
        fail(`Report journey action ${action} must remain present in static UI.`);
    }
    const inlineSources = sources.filter(item => /\bonclick\s*=/.test(item.source));
    if (inlineSources.length) {
        fail(`Report journey action ${action} must be handled by action-registry without inline onclick. First inline source: ${inlineSources[0].file}:${inlineSources[0].line}`);
    }
}

for (const item of htmlStaticActionSources('copy-stage-milestone')) {
    if (!item.source.includes('data-share-text=')) {
        fail(`copy-stage-milestone migrated button must provide data-share-text at ${item.file}:${item.line}.`);
    }
}

const eventChoiceSource = frontendApp.slice(
    frontendApp.indexOf('function renderEventChoice('),
    frontendApp.indexOf('function renderEvent()')
);
if (!eventChoiceSource) {
    fail('renderEventChoice source block is missing.');
}
if (/event-choice-button[\s\S]*?\bonclick\s*=/.test(eventChoiceSource)) {
    fail('Formal event choice buttons must be handled by action-registry without inline onclick.');
}
if (!eventChoiceSource.includes('data-action="${html(dataAction)}"') || !eventChoiceSource.includes('data-choice="${html(choiceKey)}"')) {
    fail('Formal event choice buttons must provide data-action and data-choice registry parameters.');
}

const saveEntrySource = frontendApp.slice(
    frontendApp.indexOf('function renderHeader('),
    frontendApp.indexOf('function getPhaseText(')
);
const authSource = frontendApp.slice(
    frontendApp.indexOf('function renderAuth('),
    frontendApp.indexOf('function localSaveSlotSummary(')
);
const localSlotSource = frontendApp.slice(
    frontendApp.indexOf('function renderLocalSaveSlotCards('),
    frontendApp.indexOf('function renderCreationStylePresets(')
);
if (!saveEntrySource || !authSource || !localSlotSource) {
    fail('Local save entry source blocks are missing.');
}
const saveJourneySource = `${saveEntrySource}\n${authSource}\n${localSlotSource}`;
const migratedSaveActions = [
    'register',
    'login',
    'logout',
    'create-vup',
    'save-local-slot',
    'export-local-slot',
    'import-local-slot',
    'quick-start-guest',
    'continue-local-run',
    'rename-local-slot',
    'restart-local-slot'
];
for (const action of migratedSaveActions) {
    if (!saveJourneySource.includes(`data-action="${action}"`)) {
        fail(`Local save journey action ${action} must remain present in save entry UI.`);
    }
}
for (const action of migratedSaveActions) {
    const actionPattern = new RegExp(`data-action="${action}"[^>]*`, 'g');
    const sources = [...saveJourneySource.matchAll(actionPattern)].map(match => match[0]);
    if (sources.some(source => /\bonclick\s*=/.test(source))) {
        fail(`Local save journey action ${action} must be handled by action-registry without inline onclick.`);
    }
}
for (const action of [
    'export-local-slot',
    'import-local-slot',
    'quick-start-guest',
    'continue-local-run',
    'rename-local-slot',
    'restart-local-slot'
]) {
    const actionPattern = new RegExp(`data-action="${action}"[^>]*data-slot-number=`);
    if (!actionPattern.test(saveJourneySource)) {
        fail(`Local save journey action ${action} must provide data-slot-number.`);
    }
}

const readyActionSource = frontendApp.slice(
    frontendApp.indexOf('function renderSimpleDecisionCard('),
    frontendApp.indexOf('const openingStyleChoices = [')
);
const confirmActionSource = frontendApp.slice(
    frontendApp.indexOf('function actionConfirmPanelHtml('),
    frontendApp.indexOf('async function chooseTitle(')
);
if (!readyActionSource || !confirmActionSource) {
    fail('READY action source blocks are missing.');
}
const readyJourneySource = `${readyActionSource}\n${confirmActionSource}`;
const migratedReadyActions = [
    'simple-primary-action',
    'simple-alt-action',
    'daily-plan-submit',
    'daily-plan-next-step',
    'open-daily-plan-risk',
    'open-debt-control',
    'open-operation-risk',
    'open-cockpit-goal',
    'open-cockpit-stage',
    'open-objective-item',
    'open-objective-today',
    'focus-ready-first-run',
    'focus-route-mastery',
    'open-ready-atlas-goal',
    'open-ready-goal',
    'toggle-action-detail',
    'detail-submit-action',
    'confirm-pending-action',
    'cancel-pending-action',
    'retry-actions'
];
for (const action of migratedReadyActions) {
    if (!readyJourneySource.includes(`data-action="${action}"`)) {
        fail(`READY action journey must keep data-action="${action}" in player-facing UI.`);
    }
    const actionPattern = new RegExp(`data-action="${action}"[^>]*`, 'g');
    const sources = [...readyJourneySource.matchAll(actionPattern)].map(match => match[0]);
    if (sources.some(source => /\bonclick\s*=/.test(source))) {
        fail(`READY action journey ${action} must be handled by action-registry without inline onclick.`);
    }
}
for (const action of [
    'simple-primary-action',
    'simple-alt-action',
    'daily-plan-submit',
    'daily-plan-next-step',
    'detail-submit-action'
]) {
    const actionPattern = new RegExp(`data-action="${action}"[^>]*data-action-type=`);
    if (!actionPattern.test(readyJourneySource)) {
        fail(`READY submit action ${action} must provide data-action-type.`);
    }
}
if (!readyJourneySource.includes('data-change-action="sync-action-options-summary"')) {
    fail('READY action option selects must use data-change-action for registry-backed change handling.');
}
if (readyJourneySource.includes('onchange="syncActionOptionsSummary()"')) {
    fail('READY action option selects must not use inline onchange.');
}
if (!main.includes("delegate(document, 'change', '[data-change-action]'")) {
    fail('main.js must delegate change events for registry-backed selects.');
}
if (!main.includes("delegate(document, 'input', '[data-input-action]'")) {
    fail('main.js must delegate input events for registry-backed text fields.');
}
if (!registeredActions.has('sync-action-options-summary')) {
    fail('sync-action-options-summary must be registered for READY select changes.');
}
if (!registry.includes('context?.event?.stopPropagation()')) {
    fail('detail-submit-action must stop propagation in action-registry.');
}

const openingStyleSource = frontendApp.slice(
    frontendApp.indexOf('function renderOpeningStyleChoices('),
    frontendApp.indexOf('function renderOffStreamPanel(')
);
const offStreamPanelSource = frontendApp.slice(
    frontendApp.indexOf('function renderOffStreamPanel('),
    frontendApp.indexOf('function danmakuMoodText(')
);
const fanAndRiskSource = frontendApp.slice(
    frontendApp.indexOf('function renderFanTopics('),
    frontendApp.indexOf('function renderTitleOption(')
);
if (!openingStyleSource || !offStreamPanelSource || !fanAndRiskSource) {
    fail('Opening, offstream, fan-topic, or risk-tool source blocks are missing.');
}
for (const [label, source, actions] of [
    ['opening style journey', openingStyleSource, ['opening-style-choice']],
    ['offstream journey', offStreamPanelSource, ['skip-offstream']],
    ['fan and risk journey', fanAndRiskSource, ['choose-fan-topic', 'use-risk-tool']]
]) {
    for (const action of actions) {
        if (!source.includes(`data-action="${action}"`)) {
            fail(`${label} must keep data-action="${action}" in player-facing UI.`);
        }
        const actionPattern = new RegExp(`data-action="${action}"[^>]*`, 'g');
        const sources = [...source.matchAll(actionPattern)].map(match => match[0]);
        if (sources.some(item => /\bonclick\s*=/.test(item))) {
            fail(`${label} action ${action} must be handled by action-registry without inline onclick.`);
        }
    }
}
if (/opening-style-choice-card[\s\S]*?\bonclick\s*=/.test(openingStyleSource)) {
    fail('Opening style choice cards must be handled by action-registry without inline onclick.');
}
if (/offstream-option[\s\S]*?\bonclick\s*=/.test(offStreamPanelSource)
    || /offstream-skip[\s\S]*?\bonclick\s*=/.test(offStreamPanelSource)) {
    fail('Offstream option and skip buttons must be handled by action-registry without inline onclick.');
}
if (!offStreamPanelSource.includes('data-offstream-type="${html(type)}"')) {
    fail('Offstream options must provide data-offstream-type for registry handlers.');
}
if (/fan-topic-choice[\s\S]*?\bonclick\s*=/.test(fanAndRiskSource)) {
    fail('Fan topic choices must be handled by action-registry without inline onclick.');
}
if (!fanAndRiskSource.includes('data-topic="${html(topic.topicKey)}"')
    || !fanAndRiskSource.includes('data-topic-choice="${html(c.choiceType)}"')
    || !fanAndRiskSource.includes('data-topic-choice="${html(choice.choiceType)}"')) {
    fail('Fan topic choices must provide topic and choice registry parameters.');
}
if (/risk-tool-chip[\s\S]*?\bonclick\s*=/.test(fanAndRiskSource)) {
    fail('Risk tool chips must be handled by action-registry without inline onclick.');
}
if (!fanAndRiskSource.includes('data-risk-tool-type="${html(tool.toolType)}"')
    || !fanAndRiskSource.includes('data-target-debt-id="${html(riskToolTargetIdLiteral(tool))}"')) {
    fail('Risk tool chips must provide tool type and target debt registry parameters.');
}
if (!registry.includes('targetDebtId == null ? [data.riskToolType, null]')) {
    fail('Risk tool registry args must preserve null targetDebtId semantics.');
}

const creationJourneySource = frontendApp.slice(
    frontendApp.indexOf('function renderAuth('),
    frontendApp.indexOf('function selectedCreationStylePreset(')
);
if (!creationJourneySource) {
    fail('Creation journey source block is missing.');
}
for (const action of ['register', 'login', 'create-vup', 'select-creation-style']) {
    const sources = [...creationJourneySource.matchAll(new RegExp(`data-action="${action}"[^>]*`, 'g'))].map(match => match[0]);
    if (!sources.length) {
        fail(`Creation journey action ${action} must remain present in player-facing UI.`);
    }
    if (sources.some(source => /\bonclick\s*=/.test(source))) {
        fail(`Creation journey action ${action} must be handled by action-registry without inline onclick.`);
    }
}
if (creationJourneySource.includes('onclick="register()"')
    || creationJourneySource.includes('onclick="login()"')
    || creationJourneySource.includes('onclick="createVup()"')) {
    fail('Creation journey auth/create buttons must not use inline onclick.');
}
if (!creationJourneySource.includes('data-input-action="sync-creation-style-preview"')) {
    fail('Creation journey text fields must use data-input-action for live loadout preview.');
}
if (!creationJourneySource.includes('data-change-action="apply-creation-style-preset"')) {
    fail('Creation style radios must use data-change-action for preset selection.');
}
if (creationJourneySource.includes('oninput="syncCreationStylePreview()"')
    || creationJourneySource.includes("onchange='applyCreationStylePreset(")) {
    fail('Creation journey must not use inline oninput/onchange for style presets.');
}
for (const action of [
    'toggle-action-confirm-skip',
    'sync-creation-style-preview',
    'apply-creation-style-preset',
    'select-creation-style'
]) {
    const handler = registeredActions.get(action);
    if (!handler || handler.includes('noopAction')) {
        fail(`Creation/header action ${action} must be handled by action-registry.`);
    }
}

for (const action of ['retry-bootstrap', 'logout']) {
    const inlineSources = staticActionSources(action).filter(item => /\bonclick\s*=/.test(item.source));
    if (inlineSources.length) {
        fail(`${action} must be handled by action-registry without inline onclick. First inline source: ${inlineSources[0].file}:${inlineSources[0].line}`);
    }
}

const scoreGuideSource = frontendApp.slice(
    frontendApp.indexOf('<div class="score-guide-card"'),
    frontendApp.indexOf('// Calculate deltas for animation')
);
if (!scoreGuideSource) {
    fail('Score guide source block is missing.');
}
for (const action of ['open-score-guide-route', 'open-score-guide-evidence', 'open-score-guide-risk']) {
    const sources = [...scoreGuideSource.matchAll(new RegExp(`data-action="${action}"[^>]*`, 'g'))].map(match => match[0]);
    if (!sources.length) {
        fail(`Score guide action ${action} must remain present in player-facing UI.`);
    }
    if (sources.some(source => /\bonclick\s*=/.test(source))) {
        fail(`Score guide action ${action} must be handled by action-registry without inline onclick.`);
    }
}
if (scoreGuideSource.includes('onclick="openScoreGuideTarget(')) {
    fail('Score guide buttons must not use inline onclick.');
}

function sourceBlock(label, startMarker, endMarker) {
    const start = frontendApp.indexOf(startMarker);
    const end = frontendApp.indexOf(endMarker, start + 1);
    if (start < 0 || end <= start) {
        fail(`${label} source block is missing.`);
    }
    return frontendApp.slice(start, end);
}

function tagSourcesForAction(source, action) {
    return [...source.matchAll(new RegExp(`<[^>]*data-action="${action}"[^>]*`, 'g'))].map(match => match[0]);
}

for (const [label, source, actions, requiredMarkers] of [
    [
        'Info brief',
        sourceBlock('Info brief', 'function renderInfoBriefItem(', 'function renderInfoBrief('),
        ['open-info-brief'],
        ['data-info-tab="${tab}"']
    ],
    [
        'Insight digest',
        sourceBlock('Insight digest', 'function renderInsightDigestItem(', 'function renderInsightDigest('),
        ['open-insight-tab'],
        ['data-insight-tab="${tab}"']
    ],
    [
        'Combo live action',
        sourceBlock('Combo live action', 'function renderActiveComboActions(', 'function personaTagLabelText('),
        ['focus-live-combo'],
        ['data-action-type="${html(action.actionType)}"', 'data-combo-key="${html(action.comboKey)}"']
    ],
    [
        'Ending gap action',
        sourceBlock('Ending gap action', 'function renderEndingForecastGapCta(', 'function renderEndingForecast('),
        ['focus-ending-gap'],
        ['data-action-type="${html(actionType)}"']
    ],
    [
        'Leaderboard dimension',
        sourceBlock('Leaderboard dimension', 'function renderLeaderboard(', 'async function switchLeaderboardDimension('),
        ['leaderboard-dim'],
        ['data-dimension="${html(key)}"']
    ]
]) {
    for (const action of actions) {
        const sources = tagSourcesForAction(source, action);
        if (!sources.length) {
            fail(`${label} action ${action} must remain present in player-facing UI.`);
        }
        if (sources.some(item => /\bonclick\s*=/.test(item))) {
            fail(`${label} action ${action} must be handled by action-registry without inline onclick.`);
        }
    }
    for (const marker of requiredMarkers) {
        if (!source.includes(marker)) {
            fail(`${label} must provide ${marker} for action-registry routing.`);
        }
    }
}

const restartBiasSource = sourceBlock('Restart bias', 'function renderRestartOptions(', 'function renderLeaderboard(');
if (!restartBiasSource.includes('data-change-action="update-restart-bias-hint"')) {
    fail('Restart bias select must use data-change-action for registry-backed hint updates.');
}
if (restartBiasSource.includes('onchange="updateRestartBiasHint(')) {
    fail('Restart bias select must not use inline onchange.');
}
const restartBiasHandler = registeredActions.get('update-restart-bias-hint');
if (!restartBiasHandler || restartBiasHandler.includes('noopAction')) {
    fail('update-restart-bias-hint must be handled by action-registry.');
}
if (!registry.includes("legacyAction('updateRestartBiasHint', () => [true])")) {
    fail('Restart bias registry action must preserve userChanged=true semantics.');
}

const settingsPanelSource = index.slice(
    index.indexOf('<div class="settings-panel'),
    index.indexOf('<!-- Tab 导航栏 -->')
);
if (!settingsPanelSource) {
    fail('Settings panel source block is missing.');
}
for (const action of ['toggle-bgm', 'toggle-sfx', 'toggle-tts']) {
    if (!settingsPanelSource.includes(`data-change-action="${action}"`)) {
        fail(`Settings panel must route ${action} through data-change-action.`);
    }
}
if (!settingsPanelSource.includes('data-input-action="set-bgm-volume"')) {
    fail('Settings panel BGM volume slider must use data-input-action for registry-backed volume updates.');
}
if (settingsPanelSource.includes('oninput="BGM.setVolume(') || settingsPanelSource.includes('onclick=')) {
    fail('Settings panel must not use inline event handlers.');
}
for (const action of ['toggle-settings-panel', 'close-settings-panel']) {
    if (!registeredActions.has(action)) {
        fail(`Settings panel action ${action} must be registered.`);
    }
}
const bgmVolumeHandler = registeredActions.get('set-bgm-volume');
if (!bgmVolumeHandler || bgmVolumeHandler.includes('noopAction')) {
    fail('set-bgm-volume must be handled by action-registry.');
}
if (!registry.includes('globalThis.BGM.setVolume(value / 100)')) {
    fail('set-bgm-volume must preserve the existing 0-100 slider to 0-1 volume mapping.');
}

const demoPanelSource = sourceBlock('Demo panel', 'function renderDemo(', 'async function demoReset(');
const defenseRunwaySource = sourceBlock('Defense runway', 'function renderDefenseCockpit(', 'function renderActions(');
for (const [label, source, actions] of [
    ['Demo panel', demoPanelSource, ['demo-run', 'demo-reset', 'demo-fast-forward']],
    ['Defense runway', defenseRunwaySource, ['demo-reset-run', 'open-defense-demo']]
]) {
    for (const action of actions) {
        const sources = tagSourcesForAction(source, action);
        if (!sources.length) {
            fail(`${label} action ${action} must remain present.`);
        }
        if (sources.some(item => /\bonclick\s*=/.test(item))) {
            fail(`${label} action ${action} must be handled by action-registry without inline onclick.`);
        }
    }
}
const demoStrategySource = `${demoPanelSource}\n${defenseRunwaySource}`;
if (!demoStrategySource.includes('data-change-action="sync-demo-strategy-selects"')) {
    fail('Demo strategy selects must use data-change-action for registry-backed synchronization.');
}
if (demoStrategySource.includes('onchange="syncDemoStrategySelects(')) {
    fail('Demo strategy selects must not use inline onchange.');
}
const syncDemoStrategyHandler = registeredActions.get('sync-demo-strategy-selects');
if (!syncDemoStrategyHandler || syncDemoStrategyHandler.includes('noopAction')) {
    fail('sync-demo-strategy-selects must be handled by action-registry.');
}

console.log('Action registry check passed.');
