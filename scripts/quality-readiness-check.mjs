import { existsSync, mkdirSync, readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import path from 'node:path';

const root = path.resolve(new URL('..', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'));
const docsDir = path.join(root, 'docs');
const targetDir = path.join(root, 'target');
const reportPath = path.join(targetDir, 'quality-readiness-report.md');
const dataSqlPath = path.join(root, 'src/main/resources/data.sql');
const actionServicePath = path.join(root, 'src/main/java/com/example/vupworld/service/ActionService.java');
const randomEventServicePath = path.join(root, 'src/main/java/com/example/vupworld/service/event/RandomEventService.java');
const eventExpansionServicePath = path.join(root, 'src/main/java/com/example/vupworld/service/event/EventExpansionService.java');
const formalEventPresenterPath = path.join(root, 'src/main/java/com/example/vupworld/service/event/FormalEventPresenter.java');
const midgameContentServicePath = path.join(root, 'src/main/java/com/example/vupworld/service/event/MidgameEventContentService.java');
const commercialContentServicePath = path.join(root, 'src/main/java/com/example/vupworld/service/event/CommercialRouteContentService.java');
const midgameContentPath = path.join(root, 'src/main/resources/content/midgame-events.json');
const midgameValidationPath = path.join(root, 'scripts/validate-midgame-content.mjs');
const commercialContentPath = path.join(root, 'src/main/resources/content/commercial-route-content.json');
const commercialValidationPath = path.join(root, 'scripts/validate-commercial-content.mjs');
const endingServicePath = path.join(root, 'src/main/java/com/example/vupworld/service/ending/EndingService.java');
const fanTopicServicePath = path.join(root, 'src/main/java/com/example/vupworld/service/fan/FanTopicService.java');
const reportServicePath = path.join(root, 'src/main/java/com/example/vupworld/service/report/ReportService.java');
const appJsPath = path.join(root, 'src/main/resources/static/app.js');
const appCssPath = path.join(root, 'src/main/resources/static/app.css');
const indexHtmlPath = path.join(root, 'src/main/resources/static/index.html');
const mainJsPath = path.join(root, 'src/main/resources/static/js/main.js');
const actionRegistryPath = path.join(root, 'src/main/resources/static/js/action-registry.js');
const panelBundlePath = path.join(root, 'src/main/resources/static/js/panel-bundle.js');
const staticResourceConfigPath = path.join(root, 'src/main/java/com/example/vupworld/web/StaticResourceConfig.java');
const staticDir = path.join(root, 'src/main/resources/static');
const soundDir = path.join(staticDir, 'sounds');
const galleryDir = path.join(root, '图库');
const galleryManifestPath = path.join(galleryDir, 'v4', 'manifest.json');
const stageProbePath = path.join(targetDir, 'probe-stage-flow-latest.json');
const javaSourceDir = path.join(root, 'src/main/java');

const expectedEndings = [
    'UNKNOWN',
    'ELECTRONIC_PICKLE',
    'SINGING_IDOL',
    'SLICE_SAINT',
    'BLACK_RED_MAIN_STAGE',
    'CYBER_GIRLFRIEND',
    'DD_BUS_STOP',
    'MAIN_STAGE_KING',
    'GLORIOUS_GRADUATION'
];

const requiredStrategies = [
    'steady',
    'clip',
    'black_red',
    'social',
    'singing',
    'cyber_girlfriend',
    'main_stage_king',
    'glorious_graduation',
    'idle',
    'defense',
    'random'
];

const requiredCommercialRouteTypes = [
    'ELECTRONIC_PICKLE',
    'SLICE_SAINT',
    'BLACK_RED_MAIN_STAGE',
    'DD_BUS_STOP',
    'SINGING_IDOL',
    'CYBER_GIRLFRIEND',
    'MAIN_STAGE_KING',
    'GLORIOUS_GRADUATION',
    'UNKNOWN',
    'DANCE_MEME',
    'SOCIAL_COLLAB'
];

const requiredLateGameWindows = ['FINAL_PRESSURE', 'FINAL_LOCK', 'FINAL_SCREENSHOT'];
const requiredCommercialGroups = ['steady', 'burst', 'heat', 'singing', 'relationship', 'social', 'unknown'];

const fullRouteSpecs = new Map([
    ['steady', { label: '稳健电子榨菜', expectedEnding: 'ELECTRONIC_PICKLE' }],
    ['clip', { label: '切片圣体', expectedEnding: 'SLICE_SAINT' }],
    ['black_red', { label: '黑红主会场', expectedEnding: 'BLACK_RED_MAIN_STAGE' }],
    ['social', { label: 'DD公交站', expectedEnding: 'DD_BUS_STOP' }],
    ['singing', { label: '唱歌偶像', expectedEnding: 'SINGING_IDOL' }],
    ['cyber_girlfriend', { label: '赛博女友', expectedEnding: 'CYBER_GIRLFRIEND' }],
    ['main_stage_king', { label: '主会场之王', expectedEnding: 'MAIN_STAGE_KING' }],
    ['glorious_graduation', { label: '光荣毕业', expectedEnding: 'GLORIOUS_GRADUATION' }],
    ['idle', { label: '查无此V', expectedEnding: 'UNKNOWN' }],
    ['defense', { label: '米线防守毕业', expectedEnding: 'GLORIOUS_GRADUATION' }],
    ['random', { label: '随机整活', expectedEnding: null }]
]);

const fullRouteExpectedEndings = [...new Set([...fullRouteSpecs.values()]
    .map(route => route.expectedEnding)
    .filter(Boolean))];

const frontendMarkers = [
    { label: 'READY simplified decision card', file: 'app.js', text: 'simple-decision-card' },
    { label: 'READY primary action button', file: 'app.js', text: 'data-action="simple-primary-action"' },
    { label: 'READY alternate action buttons', file: 'app.js', text: 'data-action="simple-alt-action"' },
    { label: 'READY 11-route playbook card', file: 'app.js', text: 'route-playbook-card' },
    { label: 'READY full 11-route map', file: 'app.js', text: 'full-route-map' },
    { label: 'READY full route map covers defense and random', file: 'app.js', text: '米线防守毕业' },
    { label: 'READY full route map covers random route', file: 'app.js', text: '随机整活' },
    { label: 'READY route fantasy and cost copy', file: 'app.js', text: 'fantasy:' },
    { label: 'READY new player guardrail', file: 'app.js', text: 'new-player-guardrail' },
    { label: 'READY new player three-step copy', file: 'app.js', text: '前三天照这个顺序看就行' },
    { label: 'READY five-core-metric beginner copy', file: 'app.js', text: '粉丝 / 口碑 / 围观' },
    { label: 'READY route playbook covers main stage king', file: 'app.js', text: 'MAIN_STAGE_KING' },
    { label: 'READY route playbook covers cyber girlfriend', file: 'app.js', text: 'CYBER_GIRLFRIEND' },
    { label: 'READY first-run 3-day tutorial line', file: 'app.js', text: 'renderOpeningDecisionHint(action, day)' },
    { label: 'First 3 days one-line decision formula', file: 'app.js', text: 'openingPhaseCoachLine' },
    { label: 'First 3 days click/change/tomorrow copy', file: 'app.js', text: '明天担心' },
    { label: 'READY first-run tutorial gating', file: 'app.js', text: 'isFirstRunOpeningTutorialActive' },
    { label: 'Folded full action list', file: 'app.js', text: '<div class="action-list action-detail-list">' },
    { label: 'Route goal board', file: 'app.js', text: 'ready-goal-board' },
    { label: 'Route cover visible on READY', file: 'app.js', text: 'ready-goal-cover' },
    { label: 'Action route bias explanation', file: 'app.js', text: 'action.routeBiasLabel' },
    { label: 'Action risk level explanation', file: 'app.js', text: 'action.riskLevel' },
    { label: 'Action tempo hint explanation', file: 'app.js', text: 'action.tempoHint' },
    { label: 'Combo key explanation', file: 'app.js', text: 'action.comboKey' },
    { label: 'Action combo short copy', file: 'app.js', text: 'actionComboShortText(action)' },
    { label: 'Action route/risk/tempo strip', file: 'app.js', text: 'action-route-strip' },
    { label: 'Fan topic cost/effect/risk chips', file: 'app.js', text: 'fanTopicChoiceChips' },
    { label: 'Risk tool cost/effect/status chips', file: 'app.js', text: 'riskToolChips' },
    { label: 'Ending atlas replay progress', file: 'app.js', text: 'ending-atlas-summary' },
    { label: 'Restart route bias', file: 'app.js', text: 'restartBias' },
    { label: 'Ending proof strip', file: 'app.js', text: 'ending-proof-strip' },
    { label: 'Report why line', file: 'app.js', text: 'report-why-line' },
    { label: 'Report stage milestone screenshot card', file: 'app.js', text: 'stage-milestone-card' },
    { label: 'Report stage milestone copy button', file: 'app.js', text: 'copy-stage-milestone' },
    { label: 'Report drawer toggle', file: 'index.html', text: 'id="reportDrawer"' },
    { label: 'READY action panel no inner scroll', file: 'app.css', pattern: /\.ready-mode \.action-panel\s*\{[\s\S]*?overflow:\s*hidden/ },
    { label: 'READY simplified decision card compact', file: 'app.css', pattern: /\.ready-mode \.simple-decision-card\s*\{[\s\S]*?min-height:\s*0/ },
    { label: 'READY route playbook responsive grid', file: 'app.css', pattern: /\.route-playbook-card\s*\{[\s\S]*?grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/ },
    { label: 'READY full route map card grid', file: 'app.css', pattern: /\.full-route-map-grid\s*\{[\s\S]*?grid-template-columns:\s*repeat\(auto-fit,\s*minmax\(190px,\s*1fr\)\)/ },
    { label: 'READY beginner guardrail grid', file: 'app.css', pattern: /\.new-player-guardrail-grid\s*\{[\s\S]*?grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/ },
    { label: 'Folded action detail grid', file: 'app.css', pattern: /\.ready-mode \.action-detail-list\s*\{[\s\S]*?grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/ },
    { label: 'Action explanation strip fixed grid', file: 'app.css', pattern: /\.action-route-strip\s*\{[\s\S]*?grid-template-columns:\s*minmax\(88px,\s*auto\)\s*auto\s*minmax\(0,\s*1fr\)/ },
    { label: 'Combo highlight state', file: 'app.css', pattern: /\.action-route-strip \.action-combo-strip\s*\{[\s\S]*?color:\s*#ffb36b/ },
    { label: 'Report drawer collapsed state', file: 'app.css', pattern: /\.report-modal-overlay\.open\s*\{[\s\S]*?display:\s*flex/ },
    { label: 'Stage milestone screenshot card styling', file: 'app.css', pattern: /\.stage-milestone-card\s*\{[\s\S]*?border:\s*1px/ }
];

const weights = {
    readmeRunEntry: 5,
    endingSubtitleDensity: 15,
    galleryReferences: 15,
    frontendMarkers: 15,
    actionRegistryMigration: 10,
    releaseAssetPolicy: 10,
    routeAcceptanceCoverage: 20,
    commercialRouteContent: 20,
    replayabilityAndShare: 15,
    premiumPlayabilityEvidence: 20
};

function readText(filePath) {
    return readFileSync(filePath, 'utf8').replace(/^\uFEFF/, '');
}

function readExistingText(filePath) {
    return existsSync(filePath) ? readText(filePath) : '';
}

function readFrontendJsSource() {
    return [
        readText(appJsPath),
        readExistingText(panelBundlePath)
    ].join('\n');
}

function escapeMarkdown(value) {
    return String(value ?? '').replaceAll('|', '\\|').replaceAll('\r', ' ').replaceAll('\n', ' ');
}

function status(pass) {
    return pass ? 'PASS' : 'FAIL';
}

function visibleLength(value) {
    return String(value || '')
        .replace(/\s/g, '')
        .replace(/[，。！？、,.!?:：；;“”"‘’'（）()[\]【】]/g, '')
        .length;
}

function normalizePathForGallery(value) {
    return value.replaceAll('\\', '/').replace(/^\/?gallery\//, '').replace(/^\/+/, '');
}

function normalizeStaticPath(value) {
    return value.replaceAll('\\', '/').replace(/^\/+/, '');
}

function staticFileExists(value) {
    return existsSync(path.join(staticDir, normalizeStaticPath(value)));
}

function localAssetExists(value, galleryFiles) {
    return galleryFiles.has(value) || staticFileExists(value);
}

function listFilesRecursive(dir, base = dir) {
    if (!existsSync(dir)) return [];
    return readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
        const absolute = path.join(dir, entry.name);
        if (entry.isDirectory()) return listFilesRecursive(absolute, base);
        return path.relative(base, absolute).replaceAll('\\', '/');
    });
}

function parseSqlRows(sql, category) {
    const rows = [];
    const rowPattern = /\(\s*'([^']+)'\s*,\s*'([^']+)'\s*,\s*(null|'(?:''|[^'])*')\s*,\s*'((?:''|[^'])*)'\s*,\s*(\d+)\s*,\s*(TRUE|FALSE)\s*\)/g;
    for (const match of sql.matchAll(rowPattern)) {
        if (match[1] !== category) continue;
        rows.push({
            category: match[1],
            key: match[2],
            text: match[4].replaceAll("''", "'"),
            order: Number(match[5]),
            enabled: match[6] === 'TRUE'
        });
    }
    return rows;
}

function checkReadmeRunEntry() {
    const readmePresent = existsSync(path.join(root, 'README.md'));
    const readmeText = readmePresent ? readText(path.join(root, 'README.md')) : '';
    const hasSingleplayerCommand = readmeText.includes('scripts\\run-singleplayer.cmd');
    const hasRcCommand = readmeText.includes('scripts\\release-candidate-check.cmd');
    const hasAcceptanceCommand = readmeText.includes('scripts\\acceptance-routes.cmd');
    const hasNoGeneratedDocLink = !readmeText.includes('docs/完整单机版游戏设计文档.md');
    const pass = readmePresent && hasSingleplayerCommand && hasRcCommand && hasAcceptanceCommand && hasNoGeneratedDocLink;
    return {
        key: 'readmeRunEntry',
        title: 'README 运行入口',
        pass,
        score: pass ? weights.readmeRunEntry : 0,
        rows: [
            ['README 存在', status(readmePresent), readmePresent ? 'present' : 'missing'],
            ['README 提供正式单机启动命令', status(hasSingleplayerCommand), hasSingleplayerCommand ? 'scripts\\run-singleplayer.cmd' : 'missing'],
            ['README 提供 11 路线验收命令', status(hasAcceptanceCommand), hasAcceptanceCommand ? 'scripts\\acceptance-routes.cmd' : 'missing'],
            ['README 提供候选验收命令', status(hasRcCommand), hasRcCommand ? 'scripts\\release-candidate-check.cmd' : 'missing'],
            ['README 不再依赖生成设计文档', status(hasNoGeneratedDocLink), hasNoGeneratedDocLink ? 'no generated doc link' : 'remove generated doc link']
        ]
    };
}

function checkEndingSubtitleDensity() {
    const sql = readText(dataSqlPath);
    const rows = parseSqlRows(sql, 'ENDING_SUBTITLE').filter(row => row.enabled);
    const rowsByType = new Map();
    for (const row of rows) {
        if (!rowsByType.has(row.key)) rowsByType.set(row.key, []);
        rowsByType.get(row.key).push(row);
    }
    const missing = expectedEndings.filter(type => !rowsByType.has(type));
    const duplicateTexts = [...new Set(rows
        .map(row => row.text)
        .filter((text, index, list) => list.indexOf(text) !== index))];
    const details = expectedEndings
        .filter(type => rowsByType.has(type))
        .map(type => {
            const typeRows = rowsByType.get(type);
            const rowDetails = typeRows.map(row => {
                const length = visibleLength(row.text);
                const hasPunctuation = /[，。！？、,.!?:：；;]/.test(row.text);
                const hasPathLeak = /[\\/]|\.png|\.jpg|\.webp|gallery/i.test(row.text);
                const hasMojibake = /�|\?{2,}|锟/.test(row.text);
                const dense = length >= 10 && length <= 32 && hasPunctuation && !hasPathLeak && !hasMojibake;
                return { text: row.text, length, dense };
            });
            const lengths = rowDetails.map(item => item.length);
            const averageLength = lengths.reduce((sum, value) => sum + value, 0) / lengths.length;
            return {
                type,
                count: typeRows.length,
                denseCount: rowDetails.filter(item => item.dense).length,
                minLength: Math.min(...lengths),
                maxLength: Math.max(...lengths),
                averageLength,
                sample: rowDetails[0]?.text || '',
                dense: typeRows.length >= 12 && rowDetails.every(item => item.dense)
            };
        });
    const averageLength = details.length
        ? details.reduce((sum, item) => sum + item.averageLength, 0) / details.length
        : 0;
    const denseCount = details.filter(item => item.dense).length;
    const averageDense = averageLength >= 12 && averageLength <= 24;
    const pass = missing.length === 0
        && duplicateTexts.length === 0
        && details.length === expectedEndings.length
        && denseCount === expectedEndings.length
        && averageDense;
    const checks = [
        { label: '9个结局副标题齐全', pass: missing.length === 0, detail: missing.length ? `缺失：${missing.join(', ')}` : `${rowsByType.size}/9` },
        { label: '每个结局至少12条副标题', pass: details.every(item => item.count >= 12), detail: details.map(item => `${item.type}:${item.count}`).join('，') },
        { label: '副标题全部唯一', pass: duplicateTexts.length === 0, detail: duplicateTexts.length ? duplicateTexts.join('；') : '无重复' },
        { label: '每条10-32个有效字符且带标点', pass: denseCount === expectedEndings.length, detail: `${denseCount}/${expectedEndings.length}` },
        { label: '平均密度12-24个有效字符', pass: averageDense, detail: averageLength.toFixed(1) }
    ];
    const score = Math.round((checks.filter(check => check.pass).length / checks.length) * weights.endingSubtitleDensity * 10) / 10;
    return { key: 'endingSubtitleDensity', title: '结局副标题密度', pass, score, checks, details, averageLength };
}

function extractGalleryReferences() {
    const sources = [
        { name: 'src/main/resources/static/index.html', text: readText(indexHtmlPath) },
        { name: 'src/main/resources/static/app.js', text: readText(appJsPath) },
        { name: 'src/main/resources/static/js/panel-bundle.js', text: readExistingText(panelBundlePath) },
        { name: 'src/main/resources/static/app.css', text: readText(appCssPath) }
    ];
    const refs = new Map();

    const addRef = (file, source) => {
        const normalized = normalizePathForGallery(file);
        if (normalized.includes('${')) return;
        if (normalized.startsWith('.')) return;
        if (!/\.(png|jpe?g|webp|gif|svg)$/i.test(normalized)) return;
        if (!refs.has(normalized)) refs.set(normalized, new Set());
        refs.get(normalized).add(source);
    };

    for (const source of sources) {
        for (const match of source.text.matchAll(/\/gallery\/([^"'`)>\s]+?\.(?:png|jpe?g|webp|gif|svg))/gi)) {
            addRef(match[1], source.name);
        }
        for (const match of source.text.matchAll(/["'`]([^"'`]*?\.(?:png|jpe?g|webp|gif|svg))["'`]/gi)) {
            const value = match[1];
            if (/^(https?:)?\/\//i.test(value)) continue;
            addRef(value, source.name);
        }
        for (const match of source.text.matchAll(/url\(["']?([^"')]+?\.(?:png|jpe?g|webp|gif|svg))["']?\)/gi)) {
            addRef(match[1], source.name);
        }
    }

    return [...refs.entries()].map(([file, sourceSet]) => ({ file, sources: [...sourceSet].sort() })).sort((a, b) => a.file.localeCompare(b.file));
}

function extractObjectStringMap(source, objectName) {
    const startToken = `const ${objectName} = {`;
    const start = source.indexOf(startToken);
    if (start === -1) return new Map();
    const bodyStart = start + startToken.length;
    let depth = 1;
    let end = bodyStart;
    for (; end < source.length; end++) {
        const char = source[end];
        if (char === '{') depth++;
        if (char === '}') depth--;
        if (depth === 0) break;
    }
    const body = source.slice(bodyStart, end);
    const entries = new Map();
    for (const match of body.matchAll(/([A-Z0-9_]+)\s*:\s*["']([^"']+)["']/g)) {
        entries.set(match[1], normalizePathForGallery(match[2]));
    }
    return entries;
}

function extractRouteCoverMap(source) {
    const startToken = 'const routeVisualProfiles = {';
    const start = source.indexOf(startToken);
    if (start === -1) return new Map();
    const bodyStart = start + startToken.length;
    let depth = 1;
    let end = bodyStart;
    for (; end < source.length; end++) {
        const char = source[end];
        if (char === '{') depth++;
        if (char === '}') depth--;
        if (depth === 0) break;
    }
    const body = source.slice(bodyStart, end);
    const entries = new Map();
    for (const match of body.matchAll(/([A-Z0-9_]+)\s*:\s*\{[\s\S]*?cover:\s*["']([^"']+)["']/g)) {
        entries.set(match[1], normalizePathForGallery(match[2]));
    }
    return entries;
}

function checkGalleryReferences() {
    const refs = extractGalleryReferences();
    const galleryFiles = new Set(listFilesRecursive(galleryDir));
    const appJs = readFrontendJsSource();
    const manifest = JSON.parse(readText(galleryManifestPath));
    const manifestFiles = new Set(['v4/manifest.json']);
    const manifestEntries = Array.isArray(manifest)
        ? manifest
        : Array.isArray(manifest.assets)
            ? manifest.assets
            : [];
    for (const item of manifestEntries) {
        if (!item?.file) continue;
        const file = normalizePathForGallery(item.file);
        manifestFiles.add(file.startsWith('v4/') ? file : `v4/${file}`);
    }
    const missingFiles = refs.filter(ref => !localAssetExists(ref.file, galleryFiles));
    const manifestMissingFiles = [...manifestFiles].filter(file => !galleryFiles.has(file)).sort();
    const unregisteredRefs = refs.filter(ref => galleryFiles.has(ref.file) && !manifestFiles.has(ref.file));
    const routeCoverMap = extractRouteCoverMap(appJs);
    const routeCoverEntries = expectedEndings
        .filter(type => type !== 'UNKNOWN')
        .map(type => ({ type, file: routeCoverMap.get(type) }))
        .filter(entry => entry.file);
    const routeCoverRefs = refs.filter(ref => ref.file.startsWith('v4/routes/') && ref.file.endsWith('/cover-landscape.png'));
    const endingImageMap = extractObjectStringMap(appJs, 'endingImages');
    const endingImageEntries = expectedEndings.map(type => ({ type, file: endingImageMap.get(type) })).filter(entry => entry.file);
    const endingImageExpected = expectedEndings.length;
    const routeCoverPass = routeCoverEntries.length >= expectedEndings.length - 1 && routeCoverEntries.every(entry => localAssetExists(entry.file, galleryFiles));
    const endingImagePass = endingImageEntries.length >= endingImageExpected && endingImageEntries.every(entry => localAssetExists(entry.file, galleryFiles));
    const checks = [
        { label: '前端引用的图库文件都存在', pass: missingFiles.length === 0, detail: missingFiles.length ? `${missingFiles.length}个缺失` : `${refs.length}个引用可解析` },
        { label: '图库manifest登记文件都存在', pass: manifestMissingFiles.length === 0, detail: manifestMissingFiles.length ? `${manifestMissingFiles.length}个缺失` : `${manifestFiles.size}个登记文件可解析` },
        { label: '路线封面映射覆盖主结局路线', pass: routeCoverPass, detail: routeCoverPass ? `${routeCoverEntries.length}/${expectedEndings.length - 1}条路线封面映射且文件存在` : `当前${routeCoverEntries.length}/${expectedEndings.length - 1}条路线封面映射，覆盖不足或文件缺失` },
        { label: '结局图映射覆盖全部结局', pass: endingImagePass, detail: endingImagePass ? `${endingImageEntries.length}/${endingImageExpected}个结局映射且文件存在` : `当前${endingImageEntries.length}/${endingImageExpected}个结局映射，覆盖不足或文件缺失` }
    ];
    const pass = checks.every(check => check.pass);
    const score = Math.round((checks.filter(check => check.pass).length / checks.length) * weights.galleryReferences * 10) / 10;
    return { key: 'galleryReferences', title: '图库引用与覆盖', pass, score, checks, refs, missingFiles, manifestMissingFiles, unregisteredRefs, routeCoverRefs, routeCoverEntries, endingImageEntries };
}

function checkReleaseAssetPolicy() {
    const staticResourceConfig = readText(staticResourceConfigPath);
    const appJs = readText(appJsPath);
    const expectedBgmTracks = [
        'bgm-daily.mp3',
        'bgm-stream.mp3',
        'bgm-event.mp3',
        'bgm-ending.mp3',
        'bgm-achievement.mp3',
        'bgm-title.mp3'
    ];
    const bgmRefs = [...new Set([...appJs.matchAll(/\/sounds\/(bgm-[^"'`]+?\.mp3)/g)].map(match => match[1]))];
    const missingBgmTracks = expectedBgmTracks.filter(file => !existsSync(path.join(soundDir, file)));
    const unexpectedBgmTracks = bgmRefs.filter(file => !expectedBgmTracks.includes(file));
    const sfxWavFiles = readdirSync(soundDir, { withFileTypes: true })
        .filter(entry => entry.isFile() && entry.name.endsWith('.wav'))
        .map(entry => entry.name)
        .sort();
    const checks = [
        {
            label: '公开图库只挂载 /gallery/v4/**',
            pass: staticResourceConfig.includes('addResourceHandler("/gallery/v4/**")') && !staticResourceConfig.includes('addResourceHandler("/gallery/**")'),
            detail: 'StaticResourceConfig'
        },
        {
            label: '6 条 BGM mp3 已实际落地',
            pass: missingBgmTracks.length === 0 && unexpectedBgmTracks.length === 0,
            detail: missingBgmTracks.length ? `缺失：${missingBgmTracks.join('，')}` : `已存在：${expectedBgmTracks.length} 个`
        },
        {
            label: '现有 8 个短音效 wav 仍在',
            pass: sfxWavFiles.length === 8,
            detail: `${sfxWavFiles.length} 个 wav`
        }
    ];
    const pass = checks.every(check => check.pass);
    const score = Math.round((checks.filter(check => check.pass).length / checks.length) * weights.releaseAssetPolicy * 10) / 10;
    return { key: 'releaseAssetPolicy', title: '发布资产与音频闭环', pass, score, checks, expectedBgmTracks, bgmRefs, missingBgmTracks, unexpectedBgmTracks, sfxWavFiles };
}

function checkFrontendMarkers() {
    const files = {
        'app.js': readFrontendJsSource(),
        'app.css': readText(appCssPath),
        'index.html': readText(indexHtmlPath)
    };
    const checks = frontendMarkers.map(marker => {
        const text = files[marker.file];
        const pass = marker.pattern ? marker.pattern.test(text) : text.includes(marker.text);
        return { label: marker.label, pass, detail: marker.file };
    });
    const passCount = checks.filter(check => check.pass).length;
    const pass = passCount === checks.length;
    const score = Math.round((passCount / checks.length) * weights.frontendMarkers * 10) / 10;
    return { key: 'frontendMarkers', title: '前端首屏/行动解释标记', pass, score, checks, missing: checks.filter(check => !check.pass) };
}

function parseRegisteredActions(registrySource) {
    const registeredActions = new Map();
    for (const match of registrySource.matchAll(/registerAction\('([^']+)',\s*([^\n;]+)/g)) {
        const handler = match[2].trim().replace(/\)+$/, '').trim();
        registeredActions.set(match[1], handler);
    }
    return registeredActions;
}

function collectLiteralDataActions(sourceFiles) {
    const actions = new Set();
    for (const source of sourceFiles) {
        for (const match of source.matchAll(/data-action="([^"]+)"/g)) {
            const action = match[1];
            if (action.includes('${')) continue;
            actions.add(action);
        }
    }
    return actions;
}

function collectLiteralDataActionSources(sourceFiles) {
    const sources = new Map();
    for (const [file, source] of sourceFiles) {
        source.split(/\r?\n/).forEach((line, index) => {
            for (const match of line.matchAll(/data-action="([^"]+)"/g)) {
                const action = match[1];
                if (action.includes('${')) continue;
                if (!sources.has(action)) sources.set(action, []);
                sources.get(action).push({ file, line: index + 1, source: line.trim() });
            }
        });
    }
    return sources;
}

function checkActionRegistryMigration() {
    const appJs = readFrontendJsSource();
    const indexHtml = readText(indexHtmlPath);
    const mainJs = readText(mainJsPath);
    const registry = readText(actionRegistryPath);
    const registeredActions = parseRegisteredActions(registry);
    const staticActions = collectLiteralDataActions([appJs, indexHtml]);
    const staticActionSources = collectLiteralDataActionSources([
        ['app.js', appJs],
        ['index.html', indexHtml]
    ]);
    const missingActions = [...staticActions].filter(action => !registeredActions.has(action));
    const writeActions = [
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
    const realWriteActions = writeActions.filter(action => {
        const handler = registeredActions.get(action);
        return handler && !handler.includes('noopAction');
    });
    const allowedNoopActions = new Set([
        'select-creation-style',
        'create-loadout-summary'
    ]);
    const unexpectedNoopActions = [...registeredActions.entries()]
        .filter(([, handler]) => handler.includes('noopAction'))
        .map(([action]) => action)
        .filter(action => !allowedNoopActions.has(action));
    const migratedEndingActions = [
        'copy-ending-share',
        'download-ending-share-card',
        'restart-from-share-card',
        'restart'
    ];
    const endingInlineSources = migratedEndingActions.flatMap(action =>
        (staticActionSources.get(action) || [])
            .filter(item => /\bonclick\s*=/.test(item.source))
            .map(item => `${action}@${item.file}:${item.line}`)
    );
    const copyShareSources = staticActionSources.get('copy-ending-share') || [];
    const downloadShareSources = staticActionSources.get('download-ending-share-card') || [];
    const migratedTitleActions = [
        'choose-title',
        'reroll-title',
        'cancel-action'
    ];
    const htmlStaticActionSources = action => (staticActionSources.get(action) || []).filter(item => item.source.includes('<'));
    const titleInlineSources = migratedTitleActions.flatMap(action =>
        htmlStaticActionSources(action)
            .filter(item => /\bonclick\s*=/.test(item.source))
            .map(item => `${action}@${item.file}:${item.line}`)
    );
    const chooseTitleSources = htmlStaticActionSources('choose-title');
    const migratedReportActions = [
        'copy-stage-milestone',
        'open-report-risk-recovery',
        'open-report-next-risk',
        'next-day'
    ];
    const reportInlineSources = migratedReportActions.flatMap(action =>
        htmlStaticActionSources(action)
            .filter(item => /\bonclick\s*=/.test(item.source))
            .map(item => `${action}@${item.file}:${item.line}`)
    );
    const copyMilestoneSources = htmlStaticActionSources('copy-stage-milestone');
    const eventChoiceSource = appJs.slice(
        appJs.indexOf('function renderEventChoice('),
        appJs.indexOf('function renderEvent()')
    );
    const eventChoiceUsesRegistry = Boolean(eventChoiceSource)
        && !/event-choice-button[\s\S]*?\bonclick\s*=/.test(eventChoiceSource)
        && eventChoiceSource.includes('data-action="${html(dataAction)}"')
        && eventChoiceSource.includes('data-choice="${html(choiceKey)}"');
    const saveEntrySource = appJs.slice(
        appJs.indexOf('function renderHeader('),
        appJs.indexOf('function getPhaseText(')
    );
    const authSource = appJs.slice(
        appJs.indexOf('function renderAuth('),
        appJs.indexOf('function localSaveSlotSummary(')
    );
    const localSlotSource = appJs.slice(
        appJs.indexOf('function renderLocalSaveSlotCards('),
        appJs.indexOf('function renderCreationStylePresets(')
    );
    const saveJourneySource = `${saveEntrySource}\n${authSource}\n${localSlotSource}`;
    const migratedSaveActions = [
        'save-local-slot',
        'export-local-slot',
        'import-local-slot',
        'quick-start-guest',
        'continue-local-run',
        'rename-local-slot',
        'restart-local-slot'
    ];
    const saveActionSources = action => [...saveJourneySource.matchAll(new RegExp(`data-action="${action}"[^>]*`, 'g'))].map(match => match[0]);
    const saveActionsUseRegistry = migratedSaveActions.every(action => {
        const sources = saveActionSources(action);
        return sources.length > 0 && sources.every(source => !/\bonclick\s*=/.test(source));
    });
    const saveSlotActionsHaveParameters = [
        'export-local-slot',
        'import-local-slot',
        'quick-start-guest',
        'continue-local-run',
        'rename-local-slot',
        'restart-local-slot'
    ].every(action => new RegExp(`data-action="${action}"[^>]*data-slot-number=`).test(saveJourneySource));
    const checks = [
        {
            label: '所有静态 data-action 均有注册',
            pass: missingActions.length === 0,
            detail: missingActions.length ? missingActions.join('、') : `${staticActions.size}个静态动作已注册`
        },
        {
            label: 'registry 提供 legacyAction 兼容路由',
            pass: registry.includes('function legacyAction(') && registry.includes('callGlobal('),
            detail: 'legacyAction + callGlobal'
        },
        {
            label: '核心写操作不再注册为 noop',
            pass: realWriteActions.length === writeActions.length,
            detail: `${realWriteActions.length}/${writeActions.length}`
        },
        {
            label: '只允许纯展示占位动作保留 noop',
            pass: unexpectedNoopActions.length === 0,
            detail: unexpectedNoopActions.length ? unexpectedNoopActions.join('、') : [...allowedNoopActions].join('、')
        },
        {
            label: '迁移期间跳过 inline onclick 防双写',
            pass: mainJs.includes("element.hasAttribute('onclick')") && mainJs.includes('runAction(action, { event, element })'),
            detail: 'main.js delegated click guard'
        },
        {
            label: 'registry 捕获异步动作异常',
            pass: registry.includes('result.catch'),
            detail: 'runAction async catch'
        },
        {
            label: '结局全屏焦点由 registry 接管',
            pass: registry.includes("registerAction('ending-review', endingReviewAction)")
                && registry.includes("registerAction('ending-restart', endingRestartAction)")
                && !appJs.includes("dataset.action === 'ending-review'")
                && !appJs.includes("dataset.action === 'ending-restart'"),
            detail: 'ending-review / ending-restart'
        },
        {
            label: '结局战报和复活赛按钮已移除 inline onclick',
            pass: endingInlineSources.length === 0,
            detail: endingInlineSources.length ? endingInlineSources.join('、') : migratedEndingActions.join('、')
        },
        {
            label: '结局战报复制按钮提供 registry 参数',
            pass: copyShareSources.length > 0 && copyShareSources.every(item => item.source.includes('data-share-text=')),
            detail: `${copyShareSources.length}个 copy-ending-share 按钮`
        },
        {
            label: '结局战报下载按钮提供格式参数',
            pass: downloadShareSources.length >= 3 && downloadShareSources.every(item => item.source.includes('data-share-format=')),
            detail: `${downloadShareSources.length}个 download-ending-share-card 按钮`
        },
        {
            label: '标题选择旅程已移除 inline onclick',
            pass: titleInlineSources.length === 0,
            detail: titleInlineSources.length ? titleInlineSources.join('、') : migratedTitleActions.join('、')
        },
        {
            label: '标题选择按钮提供 registry 参数',
            pass: chooseTitleSources.length > 0 && chooseTitleSources.every(item => item.source.includes('data-title-id=')),
            detail: `${chooseTitleSources.length}个 choose-title 按钮`
        },
        {
            label: '日报旅程按钮已移除 inline onclick',
            pass: reportInlineSources.length === 0,
            detail: reportInlineSources.length ? reportInlineSources.join('、') : migratedReportActions.join('、')
        },
        {
            label: '阶段爆点复制按钮提供 registry 参数',
            pass: copyMilestoneSources.length > 0 && copyMilestoneSources.every(item => item.source.includes('data-share-text=')),
            detail: `${copyMilestoneSources.length}个 copy-stage-milestone 按钮`
        },
        {
            label: '事件三选一按钮由 registry 接管',
            pass: eventChoiceUsesRegistry,
            detail: eventChoiceUsesRegistry ? 'renderEventChoice: data-action + data-choice' : 'renderEventChoice still has inline onclick or missing parameters'
        },
        {
            label: '存档入口旅程按钮已移除 inline onclick',
            pass: saveActionsUseRegistry,
            detail: saveActionsUseRegistry ? migratedSaveActions.join('、') : 'save entry still has inline onclick or missing actions'
        },
        {
            label: '存档槽按钮提供 registry 槽号参数',
            pass: saveSlotActionsHaveParameters,
            detail: saveSlotActionsHaveParameters ? 'data-slot-number for slot actions' : 'missing data-slot-number on save slot actions'
        },
        {
            label: '音频控制暴露给模块化动作',
            pass: appJs.includes('window.SFX = SFX')
                && appJs.includes('window.BGM = BGM')
                && appJs.includes('window.TTS = TTS'),
            detail: 'window.SFX/BGM/TTS'
        }
    ];
    const pass = checks.every(check => check.pass);
    const score = Math.round((checks.filter(check => check.pass).length / checks.length) * weights.actionRegistryMigration * 10) / 10;
    return { key: 'actionRegistryMigration', title: '前端 action registry 迁移', pass, score, checks, missingActions, unexpectedNoopActions };
}

function parseAcceptanceMarkdownFallback(filePath) {
    if (!existsSync(filePath)) return null;
    const text = readText(filePath);
    const strategiesByName = new Map();
    for (const line of text.split(/\r?\n/)) {
        const match = line.match(/^\|\s*([^|]+)\|\s*([a-z_]+)\s*\|\s*(PASS|FAIL)\s*\|\s*(\d+)\s*\|\s*([^|]*)\|\s*(\d*)\s*\|\s*(\d*)\s*\|\s*([^|]*)\|\s*([^|]*)\|\s*(\d*)\s*\|\s*(\d*)\s*\|\s*([^|]*)\|\s*([^|]*)\|\s*([^|]*)\|\s*([^|]*)\|\s*([^|]*)\|/i);
        if (!match) continue;
        strategiesByName.set(match[2], {
            label: match[1].trim(),
            strategy: match[2],
            passed: match[3].toUpperCase() === 'PASS',
            currentDay: Number(match[4]),
            phase: match[5].trim(),
            reportCount: Number(match[6] || 0),
            logCount: Number(match[7] || 0),
            expectedEnding: match[8].trim(),
            endingType: match[9].trim(),
            endingReviewId: Number(match[10] || 0),
            keyEventCount: Number(match[11] || 0),
            routeEvidenceTrailCount: Number(match[12] || 0),
            stageReviewPresent: /^PASS|True$/i.test(match[13].trim()),
            stageMilestonesPresent: /^PASS/i.test(match[14].trim()),
            midgameEventsPresent: /^PASS/i.test(match[15].trim()),
            lateGameEventsPresent: /^PASS/i.test(match[16].trim())
        });
    }
    let current = null;
    for (const line of text.split(/\r?\n/)) {
        const heading = line.match(/^###\s+(?:.*?\/\s*)?([a-z_]+)\s*$/i);
        if (heading) {
            current = strategiesByName.get(heading[1]) || null;
            continue;
        }
        if (!current) continue;
        const field = line.match(/^-\s*([^：]+)：(.*)$/);
        if (!field) continue;
        const label = field[1].trim();
        const value = field[2].trim();
        if (label === '运行种子') current.runSeed = value;
        if (label === '最终标题') current.finalTitle = value;
        if (label === '副标题') current.subtitle = value;
        if (label === '结局理由') current.endingReason = value;
        if (label === '证据链') {
            const evidence = value.match(/关键事件\s*(\d+)，路线证据\s*(\d+)，最终日报 Day\s*(\d+)，阶段复盘=(True|False)/i);
            if (evidence) {
                current.keyEventCount = Number(evidence[1]);
                current.routeEvidenceTrailCount = Number(evidence[2]);
                current.finalReportDay = Number(evidence[3]);
                current.stageReviewPresent = evidence[4].toLowerCase() === 'true';
            }
        }
        if (current.endingReason) {
            const route = current.endingReason.match(/当前路线\s*([^，。]+)/);
            if (route) current.routeReviewCurrentRoute = route[1].trim();
        }
    }
    const strategies = [...strategiesByName.values()];
    if (!strategies.length) return null;
    return {
        generatedAt: statSync(filePath).mtime.toISOString(),
        sourcePath: filePath,
        sourceMtimeMs: statSync(filePath).mtimeMs,
        allPassed: strategies.every(item => item.passed),
        canPlayP0: !/canPlayP0=False|配置检查：canPlayP0=False/i.test(text),
        routeScope: strategies.length === requiredStrategies.length ? 'full-30-day-11-route-acceptance' : 'focused-30-day-acceptance',
        isFullRouteRun: strategies.length === requiredStrategies.length,
        strategies
    };
}

function latestAcceptanceEvidence() {
    const jsonCandidates = existsSync(targetDir)
        ? readdirSync(targetDir)
            .filter(name => /^acceptance-routes.*\.json$/i.test(name))
            .map(name => path.join(targetDir, name))
            .sort((a, b) => statSync(b).mtimeMs - statSync(a).mtimeMs)
        : [];
    const candidates = [];
    if (jsonCandidates.length) {
        for (const candidate of jsonCandidates) {
            try {
                const parsed = JSON.parse(readText(candidate));
                parsed.sourcePath = candidate;
                parsed.sourceMtimeMs = statSync(candidate).mtimeMs;
                candidates.push(parsed);
            } catch (error) {
                throw new Error(`Acceptance evidence JSON is invalid: ${path.relative(root, candidate).replaceAll('\\', '/')} (${error.message})`);
            }
        }
    }

    const markdownCandidates = [targetDir, docsDir]
        .filter(dir => existsSync(dir))
        .flatMap(dir => readdirSync(dir)
            .filter(name => /^acceptance-routes.*\.md$/i.test(name))
            .map(name => path.join(dir, name)))
        .sort((a, b) => statSync(b).mtimeMs - statSync(a).mtimeMs);
    for (const candidate of markdownCandidates) {
        const parsed = parseAcceptanceMarkdownFallback(candidate);
        if (parsed) candidates.push(parsed);
    }
    if (!candidates.length) return null;
    return candidates
        .sort((a, b) => {
            const aComplete = completeAcceptanceEvidence(a) ? 1 : 0;
            const bComplete = completeAcceptanceEvidence(b) ? 1 : 0;
            if (aComplete !== bComplete) return bComplete - aComplete;
            const scoreDelta = acceptanceEvidenceScore(b) - acceptanceEvidenceScore(a);
            if (scoreDelta !== 0) return scoreDelta;
            return Number(b.sourceMtimeMs || 0) - Number(a.sourceMtimeMs || 0);
        })[0];
}

function completeAcceptanceEvidence(evidence) {
    const strategies = Array.isArray(evidence?.strategies) ? evidence.strategies : [];
    const strategyNames = new Set(strategies.map(item => item.strategy).filter(Boolean));
    return requiredStrategies.every(strategy => strategyNames.has(strategy));
}

function latestStageProbeEvidence() {
    if (!existsSync(stageProbePath)) return null;
    try {
        const parsed = JSON.parse(readText(stageProbePath));
        parsed.sourcePath = stageProbePath;
        return parsed;
    } catch (error) {
        throw new Error(`Stage flow probe evidence JSON is invalid: ${path.relative(root, stageProbePath).replaceAll('\\', '/')} (${error.message})`);
    }
}

function acceptanceEvidenceScore(evidence) {
    const strategies = Array.isArray(evidence?.strategies) ? evidence.strategies : [];
    const strategyNames = new Set(strategies.map(item => item.strategy).filter(Boolean));
    const endingTypes = new Set(strategies.map(item => item.endingType).filter(Boolean));
    const targetDay = Number(evidence?.targetDay || 30);
    const passedRows = strategies.filter(item => item.passed && Number(item.currentDay) === targetDay && item.phase === 'ENDING_READY').length;
    const structuredRows = strategies.filter(item =>
        'stageMilestoneDays' in item
        && 'midgameEventWindows' in item
        && 'lateGameEventWindows' in item
    ).length;
    return strategyNames.size * 100
        + endingTypes.size * 50
        + passedRows * 10
        + structuredRows * 20
        + (evidence?.allPassed ? 5 : 0)
        + (evidence?.canPlayP0 ? 5 : 0);
}

function checkRouteAcceptanceCoverage() {
    const evidence = latestAcceptanceEvidence();
    const stageProbe = latestStageProbeEvidence();
    const actionServiceSource = readText(actionServicePath);
    const formalEventPresenterSource = readText(formalEventPresenterPath);
    const midgameServiceSource = readText(midgameContentServicePath);
    const commercialServiceSource = readText(commercialContentServicePath);
    const reportServiceSource = readText(reportServicePath);
    const strategies = evidence?.strategies || [];
    const targetDay = Number(evidence?.targetDay || 30);
    const strategiesByName = new Map(strategies.map(item => [item.strategy, item]));
    const strategyNames = new Set(strategiesByName.keys());
    const endingTypes = new Set(requiredStrategies
        .map(strategy => strategiesByName.get(strategy)?.endingType)
        .filter(Boolean));
    const missingStrategies = requiredStrategies.filter(strategy => !strategyNames.has(strategy));
    const missingEndings = fullRouteExpectedEndings.filter(type => !endingTypes.has(type));
    const routeRows = requiredStrategies.map(strategy => strategiesByName.get(strategy)).filter(Boolean);
    const endingMismatches = routeRows.filter(item => {
        const expected = item.expectedEnding || fullRouteSpecs.get(item.strategy)?.expectedEnding;
        return expected && item.endingType !== expected;
    });
    const requiredMilestoneDays = [7, 15, 23].filter(day => day <= targetDay);
    const rowsWithStageMilestones = routeRows.filter(item => {
        const days = Array.isArray(item.stageMilestoneDays) ? item.stageMilestoneDays.map(Number) : [];
        return item.stageMilestonesPresent === true && requiredMilestoneDays.every(day => days.includes(day));
    });
    const rowsMissingStageMilestones = routeRows
        .filter(item => !rowsWithStageMilestones.includes(item))
        .map(item => {
            const days = Array.isArray(item.stageMilestoneDays) ? item.stageMilestoneDays.map(Number) : [];
            const missing = requiredMilestoneDays.filter(day => !days.includes(day));
            return `${item.strategy}:${missing.join('/') || 'flag false'}`;
        });
    const requiredMidgameWindows = [
        ['FIRST_NAMING', 12],
        ['MID_BACKLASH', 18],
        ['LOCK_WARNING', 25]
    ].filter(([, day]) => day <= targetDay).map(([window]) => window);
    const rowsWithMidgameEvents = routeRows.filter(item => {
        const windows = Array.isArray(item.midgameEventWindows) ? item.midgameEventWindows.map(String) : [];
        return item.midgameEventsPresent === true && requiredMidgameWindows.every(window => windows.includes(window));
    });
    const rowsMissingMidgameEvents = routeRows
        .filter(item => !rowsWithMidgameEvents.includes(item))
        .map(item => {
            const windows = Array.isArray(item.midgameEventWindows) ? item.midgameEventWindows.map(String) : [];
            const missing = requiredMidgameWindows.filter(window => !windows.includes(window));
            return `${item.strategy}:${missing.join('/') || 'flag false'}`;
        });
    const requiredLateWindows = [
        ['FINAL_PRESSURE', 25],
        ['FINAL_LOCK', 27],
        ['FINAL_SCREENSHOT', 29]
    ].filter(([, day]) => day <= targetDay).map(([window]) => window);
    const rowsWithLateGameEvents = routeRows.filter(item => {
        const windows = Array.isArray(item.lateGameEventWindows) ? item.lateGameEventWindows.map(String) : [];
        return item.lateGameEventsPresent === true && requiredLateWindows.every(window => windows.includes(window));
    });
    const rowsMissingLateGameEvents = routeRows
        .filter(item => !rowsWithLateGameEvents.includes(item))
        .map(item => {
            const windows = Array.isArray(item.lateGameEventWindows) ? item.lateGameEventWindows.map(String) : [];
            const missing = requiredLateWindows.filter(window => !windows.includes(window));
            return `${item.strategy}:${missing.join('/') || 'flag false'}`;
        });
    let midgameContentSummary = 'missing';
    let midgameContentPass = false;
    if (existsSync(midgameContentPath)) {
        const content = JSON.parse(readText(midgameContentPath));
        const events = Array.isArray(content.events) ? content.events : [];
        const windows = new Set((content.windows || []).map(item => item.key));
        const groups = new Set(events.map(item => item.routeGroup));
        const cellSet = new Set(events.map(item => `${item.routeGroup}:${item.window}`));
        midgameContentPass = content.version === 'midgame-events-v1'
            && events.length >= 18
            && ['FIRST_NAMING', 'MID_BACKLASH', 'LOCK_WARNING'].every(window => windows.has(window))
            && ['steady', 'burst', 'heat', 'singing', 'relationship', 'social', 'unknown'].every(group => groups.has(group))
            && cellSet.size >= 21
            && existsSync(midgameValidationPath);
        midgameContentSummary = `${events.length} events, ${cellSet.size} group-window cells, validationScript=${existsSync(midgameValidationPath)}`;
    }
    let commercialContentSummary = 'missing';
    let commercialContentPass = false;
    if (existsSync(commercialContentPath)) {
        const content = JSON.parse(readText(commercialContentPath));
        const identities = Array.isArray(content.routeIdentities) ? content.routeIdentities : [];
        const events = Array.isArray(content.lateGameEvents) ? content.lateGameEvents : [];
        const windows = Array.isArray(content.lateGameWindows) ? content.lateGameWindows : [];
        const windowKeys = new Set(windows.map(item => item.key));
        const identityRoutes = new Set(identities.map(item => item.routeType));
        const routeCoverage = new Set(events.flatMap(item => Array.isArray(item.routeTypes) ? item.routeTypes : []));
        const cellSet = new Set(events.map(item => `${item.routeGroup}:${item.window}`));
        commercialContentPass = content.version === 'commercial-route-content-v1'
            && identities.length === requiredCommercialRouteTypes.length
            && requiredCommercialRouteTypes.every(route => identityRoutes.has(route))
            && requiredLateGameWindows.every(window => windowKeys.has(window))
            && requiredCommercialGroups.every(group => events.some(event => event.routeGroup === group))
            && requiredCommercialRouteTypes.every(route => routeCoverage.has(route))
            && events.length === requiredLateGameWindows.length * requiredCommercialGroups.length
            && cellSet.size === requiredLateGameWindows.length * requiredCommercialGroups.length
            && existsSync(commercialValidationPath);
        commercialContentSummary = `${identities.length} identities, ${events.length} events, ${cellSet.size} group-window cells, validationScript=${existsSync(commercialValidationPath)}`;
    }
    const allRowsPassed = routeRows.length === requiredStrategies.length && routeRows.every(item =>
        item.passed
        && Number(item.currentDay) === targetDay
        && (!item.phase || item.phase === 'ENDING_READY')
        && (!('reportCount' in item) || Number(item.reportCount) >= targetDay)
        && (!('logCount' in item) || Number(item.logCount) >= targetDay)
        && (!(item.expectedEnding || fullRouteSpecs.get(item.strategy)?.expectedEnding)
            || (item.expectedEnding || fullRouteSpecs.get(item.strategy)?.expectedEnding) === item.endingType)
    );
    const checks = [
        { label: '存在本地路线验收证据', pass: Boolean(evidence), detail: evidence ? path.relative(root, evidence.sourcePath).replaceAll('\\', '/') : '未找到' },
        { label: '完整目标11路线验收通过', pass: Boolean(evidence?.allPassed) && evidence?.canPlayP0 !== false, detail: `allPassed=${Boolean(evidence?.allPassed)} canPlayP0=${evidence?.canPlayP0 !== false}` },
        {
            label: 'OFF_STREAM_READY/下播流程',
            pass: stageProbe?.offStreamCovered === true
                && Number(stageProbe?.offStreamOptionCount || 0) > 0
                && (stageProbe?.phaseAfterTitle === 'REPORT_READY' || stageProbe?.phaseAfterTitle === 'ENDING_READY')
                && stageProbe?.reportLoaded === true,
            detail: stageProbe
                ? `covered=${stageProbe.offStreamCovered === true} options=${Number(stageProbe.offStreamOptionCount || 0)} choice=${stageProbe.offStreamChoice || 'NONE'} phase=${stageProbe.phaseAfterTitle || 'UNKNOWN'} reportLoaded=${stageProbe.reportLoaded === true}`
                : 'NOT_COVERED：未找到 target/probe-stage-flow-latest.json，请先运行 scripts\\probe-stage-flow.cmd -Port 18087'
        },
        { label: '覆盖11条完整目标验收路线', pass: missingStrategies.length === 0, detail: missingStrategies.length ? `缺策略：${missingStrategies.join(', ')}` : requiredStrategies.map(strategy => fullRouteSpecs.get(strategy)?.label || strategy).join('、') },
        { label: '覆盖完整目标主结局模板', pass: missingEndings.length === 0, detail: missingEndings.length ? `缺结局：${missingEndings.join(', ')}` : `${endingTypes.size}/${fullRouteExpectedEndings.length}个结局` },
        { label: '非随机路线命中预期结局', pass: endingMismatches.length === 0 && routeRows.length === requiredStrategies.length, detail: endingMismatches.length ? endingMismatches.map(item => `${item.strategy}:${item.endingType}!=${item.expectedEnding || fullRouteSpecs.get(item.strategy)?.expectedEnding}`).join('；') : '全部命中' },
        { label: `每条验收路线推进到第${targetDay}天ENDING_READY`, pass: allRowsPassed, detail: `${routeRows.filter(item => item.passed).length}/${requiredStrategies.length}通过` },
        { label: '后端生成Day 7/15/23阶段爆点', pass: reportServiceSource.includes('stage_milestone') && reportServiceSource.includes('STAGE_MILESTONE_V1'), detail: reportServiceSource.includes('stage_milestone') ? 'ReportService structured milestone present' : 'missing ReportService stage_milestone' },
        { label: '11路线验收覆盖Day 7/15/23阶段爆点', pass: routeRows.length === requiredStrategies.length && rowsWithStageMilestones.length === requiredStrategies.length, detail: rowsMissingStageMilestones.length ? rowsMissingStageMilestones.join('；') : `${rowsWithStageMilestones.length}/${requiredStrategies.length} routes · days ${requiredMilestoneDays.join('/')}` }
    ];
    checks.push(
        { label: 'Midgame content pack covers Day 10-25 pipeline', pass: midgameContentPass, detail: midgameContentSummary },
        {
            label: 'Backend reserves MIDGAME_EVENT through formal event slot',
            pass: midgameServiceSource.includes('midgame_content_pack')
                && actionServiceSource.includes('pauseForMidgameEventIfNeeded')
                && actionServiceSource.includes('"MIDGAME_EVENT"')
                && formalEventPresenterSource.includes('"MIDGAME_EVENT"'),
            detail: 'MidgameEventContentService + ActionService + FormalEventPresenter'
        },
        {
            label: '11 routes acceptance covers Day 10-25 midgame windows',
            pass: routeRows.length === requiredStrategies.length && rowsWithMidgameEvents.length === requiredStrategies.length,
            detail: rowsMissingMidgameEvents.length ? rowsMissingMidgameEvents.join('；') : `${rowsWithMidgameEvents.length}/${requiredStrategies.length} routes windows ${requiredMidgameWindows.join('/')}`
        },
        {
            label: 'Commercial 11-route identity and Day 24-29 content pack',
            pass: commercialContentPass,
            detail: commercialContentSummary
        },
        {
            label: 'Backend reserves LATE_GAME_EVENT through formal event slot',
            pass: commercialServiceSource.includes('commercial_route_content_pack')
                && actionServiceSource.includes('pauseForLateGameEventIfNeeded')
                && actionServiceSource.includes('"LATE_GAME_EVENT"')
                && formalEventPresenterSource.includes('"LATE_GAME_EVENT"'),
            detail: 'CommercialRouteContentService + ActionService + FormalEventPresenter'
        },
        {
            label: '11 routes acceptance covers Day 24-29 late-game windows',
            pass: routeRows.length === requiredStrategies.length && rowsWithLateGameEvents.length === requiredStrategies.length,
            detail: rowsMissingLateGameEvents.length ? rowsMissingLateGameEvents.join('；') : `${rowsWithLateGameEvents.length}/${requiredStrategies.length} routes windows ${requiredLateWindows.join('/')}`
        }
    );
    const pass = checks.every(check => check.pass);
    const score = Math.round((checks.filter(check => check.pass).length / checks.length) * weights.routeAcceptanceCoverage * 10) / 10;
    return { key: 'routeAcceptanceCoverage', title: '完整目标11路线验收覆盖', pass, score, checks, evidence, strategies: routeRows, missingStrategies, missingEndings, endingMismatches };
}

function parseRandomEvents() {
    const source = readText(randomEventServicePath);
    const events = [];
    const pattern = /new RandomEventDTO\(\s*"([^"]+)"[\s\S]*?"(POSITIVE|NEUTRAL|MIXED|NEGATIVE)"\s*\)/g;
    for (const match of source.matchAll(pattern)) {
        events.push({ id: match[1], type: match[2] });
    }
    return events;
}

function checkReplayabilityAndShare() {
    const events = parseRandomEvents();
    const randomEventSource = readText(randomEventServicePath);
    const eventExpansionSource = readText(eventExpansionServicePath);
    const endingServiceSource = readText(endingServicePath);
    const fanTopicServiceSource = readText(fanTopicServicePath);
    const ids = events.map(event => event.id);
    const duplicateIds = [...new Set(ids.filter((id, index, list) => list.indexOf(id) !== index))];
    const typeCounts = new Map();
    for (const event of events) {
        typeCounts.set(event.type, (typeCounts.get(event.type) || 0) + 1);
    }
    const appJs = readFrontendJsSource();
    const appCss = readText(appCssPath);
    const checks = [
        { label: '随机事件池至少32条', pass: events.length >= 32, detail: `${events.length}条` },
        { label: '随机事件ID全部唯一', pass: duplicateIds.length === 0, detail: duplicateIds.length ? duplicateIds.join(', ') : '无重复' },
        { label: '四类事件各至少6条', pass: ['POSITIVE', 'NEUTRAL', 'MIXED', 'NEGATIVE'].every(type => (typeCounts.get(type) || 0) >= 6), detail: ['POSITIVE', 'NEUTRAL', 'MIXED', 'NEGATIVE'].map(type => `${type}:${typeCounts.get(type) || 0}`).join('，') },
        { label: '事件导演暴露候选偏置解释', pass: randomEventSource.includes('candidateBiasesFor') && randomEventSource.includes('CandidateBias'), detail: 'RandomEventService.candidateBiasesFor' },
        { label: '扩展事件选择可复现', pass: eventExpansionSource.includes('Objects.hash(day, fans, watchHeat, reputation') && !eventExpansionSource.includes('new Random()') && !eventExpansionSource.includes('random.nextInt'), detail: 'EventExpansionService deterministic hash' },
        { label: '扩展事件暴露候选解释', pass: eventExpansionSource.includes('candidateEventsFor') && eventExpansionSource.includes('candidateReasonsFor') && eventExpansionSource.includes('CandidateReason'), detail: 'candidateEventsFor + candidateReasonsFor' },
        { label: '结局副标题使用本局个性化种子', pass: endingServiceSource.includes('subtitleSeedData') && endingServiceSource.includes('representativeActions') && endingServiceSource.includes('representativeResultSignals'), detail: 'subtitleSeedData + representative evidence' },
        { label: '关键结局用内容metadata保留传播锚点', pass: endingServiceSource.includes('"signature".equals(entry.subKey())') && readText(dataSqlPath).includes("'signature', '录播组") && readText(dataSqlPath).includes("'signature', '陪伴感") && readText(dataSqlPath).includes("'signature', '这次没有被抬走"), detail: 'ENDING_SUBTITLE sub_key=signature' },
        { label: '结局原因记录副标题来源种子', pass: endingServiceSource.includes('"seed", copyDecision.seedData()') && endingServiceSource.includes('"candidatePoolSize", copyDecision.candidatePoolSize()') && endingServiceSource.includes('"catalogPoolSize", copyDecision.catalogPoolSize()'), detail: 'subtitleSource.seed + pool sizes' },
        { label: '副标题种子不依赖数据库自增ID', pass: !endingServiceSource.includes('seedData.put("vupId"') && !endingServiceSource.includes('endingRuleName('), detail: 'no vupId seed / no duplicate rule name helper' },
        { label: '结局复盘提供可复制战报', pass: appJs.includes('copyEndingShare') && appJs.includes('endingShareText') && appJs.includes('copy-ending-share'), detail: 'copyEndingShare + copy-ending-share' },
        { label: '战报块有专用视觉样式', pass: appCss.includes('.ending-share-card') && appCss.includes('.ending-share-button'), detail: 'ending-share-card / ending-share-button' },
        { label: '主播口播友好句可见', pass: appJs.includes('streamerFriendlyEndingLine') && appJs.includes('ending-streamer-line') && appJs.includes('口播：'), detail: 'streamerFriendlyEndingLine + ending-streamer-line' },
        { label: '结局战报截图/保存验收锚点', pass: appJs.includes('downloadEndingShareCard') && appJs.includes('endingShareCardSvg') && appJs.includes('data-screenshot-anchor="ending-streamer-line"'), detail: 'downloadEndingShareCard + screenshot anchor' },
        {
            label: '结局战报导出覆盖16:9和9:16',
            pass: appJs.includes('ENDING_SHARE_CARD_FORMATS')
                && appJs.includes('label: "16:9"')
                && appJs.includes('label: "9:16"')
                && appJs.includes('label: "4:5"')
                && appJs.includes('width="1920" height="1080"')
                && appJs.includes('width="${spec.width}" height="${spec.height}"')
                && appJs.includes('保存16:9')
                && appJs.includes('保存9:16'),
            detail: '16:9 + 9:16 required, 4:5 optional'
        },
        {
            label: '关键写操作复用稳定幂等键',
            pass: appJs.includes('function stableIdempotencyKey')
                && appJs.includes('function currentRunKeyParts')
                && [
                    'day-action',
                    'stream-title',
                    'title-reroll',
                    'event-choice',
                    'interaction-choice',
                    'next-day',
                    'fan-topic-choice',
                    'risk-tool-use',
                    'cancel-action',
                    'restart'
                ].every(scope => appJs.includes(`stableIdempotencyKey('${scope}'`))
                && [
                    '`action-${Date.now()}`',
                    '`title-${Date.now()}`',
                    '`reroll-${Date.now()}`',
                    '`event-${Date.now()}`',
                    '`interaction-${Date.now()}`',
                    '`next-${Date.now()}`',
                    '`fan-topic-${Date.now()}`',
                    '`risk-tool-${Date.now()}`',
                    '`cancel-${Date.now()}`',
                    '`restart-${Date.now()}`'
                ].every(pattern => !appJs.includes(pattern)),
            detail: 'stableIdempotencyKey covers action/title/event/fan-topic/risk/next/restart writes'
        },
        {
            label: 'Fan topic service rejects timestamp idempotency keys',
            pass: fanTopicServiceSource.includes('requireStableFanTopicKey')
                && fanTopicServiceSource.includes('fan-topic-\\\\d+')
                && fanTopicServiceSource.includes('粉丝群议题写接口必须使用稳定幂等键'),
            detail: 'FanTopicService rejects fan-topic timestamp keys'
        }
    ];
    const pass = checks.every(check => check.pass);
    const score = Math.round((checks.filter(check => check.pass).length / checks.length) * weights.replayabilityAndShare * 10) / 10;
    return { key: 'replayabilityAndShare', title: '复玩/传播契约', pass, score, checks, events, typeCounts, duplicateIds };
}

function normalizedCopy(value) {
    return String(value || '')
        .trim()
        .replace(/\s+/g, '')
        .replace(/[，。！？、,.!?:；;：]/g, '');
}

function duplicateRate(values) {
    const normalized = values.map(normalizedCopy).filter(Boolean);
    if (!normalized.length) return 1;
    return 1 - (new Set(normalized).size / normalized.length);
}

function checkPremiumPlayabilityEvidence() {
    const evidence = latestAcceptanceEvidence();
    const strategies = evidence?.strategies || [];
    const targetDay = Number(evidence?.targetDay || 30);
    const subtitles = strategies.map(item => item.subtitle).filter(Boolean);
    const titles = strategies.map(item => item.finalTitle).filter(Boolean);
    const subtitleDuplicateRate = duplicateRate(subtitles);
    const titleDuplicateRate = duplicateRate(titles);
    const rowsWithNewFields = strategies.filter(item =>
        'keyEventCount' in item
        && 'routeEvidenceTrailCount' in item
        && 'stageReviewPresent' in item
        && 'stageMilestoneDays' in item
    ).length;
    const rowsWithEvidence = strategies.filter(item =>
        Number(item.keyEventCount || 0) >= 3
        && Number(item.routeEvidenceTrailCount || 0) >= 3
        && Number(item.finalReportDay || 0) === targetDay
    ).length;
    const rowsWithStageReview = strategies.filter(item => item.stageReviewPresent === true).length;
    const rowsWithStageMilestones = strategies.filter(item =>
        item.stageMilestonesPresent === true
        && Array.isArray(item.stageMilestoneDays)
        && [7, 15, 23].filter(day => day <= targetDay).every(day => item.stageMilestoneDays.map(Number).includes(day))
    ).length;
    const rowsWithEndingCopy = strategies.filter(item =>
        normalizedCopy(item.finalTitle).length >= 2
        && normalizedCopy(item.subtitle).length >= 8
        && normalizedCopy(item.endingReason).length >= 8
    ).length;
    const rowsWithRouteMemory = strategies.filter(item =>
        item.routeReviewCurrentRoute
        && item.endingType
        && Number(item.routeEvidenceTrailCount || 0) >= 3
    ).length;
    const checks = [
        { label: 'new acceptance evidence fields exist', pass: strategies.length > 0 && rowsWithNewFields === strategies.length, detail: `${rowsWithNewFields}/${strategies.length}` },
        { label: 'each route has key events and target-day route evidence trail', pass: strategies.length > 0 && rowsWithEvidence === strategies.length, detail: `${rowsWithEvidence}/${strategies.length} targetDay=${targetDay}` },
        { label: 'each route has stage review or final wrap evidence', pass: strategies.length > 0 && rowsWithStageReview === strategies.length, detail: `${rowsWithStageReview}/${strategies.length}` },
        { label: 'each route has Day 7/15/23 stage milestone beats', pass: strategies.length > 0 && rowsWithStageMilestones === strategies.length, detail: `${rowsWithStageMilestones}/${strategies.length}` },
        { label: 'ending title subtitle and reason are non-empty copy', pass: strategies.length > 0 && rowsWithEndingCopy === strategies.length, detail: `${rowsWithEndingCopy}/${strategies.length}` },
        { label: 'route memory is recorded in routeReview', pass: strategies.length > 0 && rowsWithRouteMemory === strategies.length, detail: `${rowsWithRouteMemory}/${strategies.length}` },
        { label: 'ending subtitle duplicate rate <= 20%', pass: subtitles.length >= requiredStrategies.length && subtitleDuplicateRate <= 0.2, detail: `${(subtitleDuplicateRate * 100).toFixed(1)}%` },
        { label: 'final title duplicate rate <= 45%', pass: titles.length >= requiredStrategies.length && titleDuplicateRate <= 0.45, detail: `${(titleDuplicateRate * 100).toFixed(1)}%` }
    ];
    const pass = checks.every(check => check.pass);
    const score = Math.round((checks.filter(check => check.pass).length / checks.length) * weights.premiumPlayabilityEvidence * 10) / 10;
    return { key: 'premiumPlayabilityEvidence', title: '90pt premium playability evidence', pass, score, checks, subtitleDuplicateRate, titleDuplicateRate, strategies };
}

function sectionChecks(section) {
    if (section.rows) {
        return section.rows
            .map(row => `| ${escapeMarkdown(row[0])} | ${row[1]} | ${escapeMarkdown(row[2])} |`)
            .join('\n');
    }
    return section.checks
        .map(check => `| ${escapeMarkdown(check.label)} | ${status(check.pass)} | ${escapeMarkdown(check.detail)} |`)
        .join('\n');
}

function buildReport(sections, totalScore, readinessPass) {
    const generatedAt = new Date().toLocaleString('zh-CN', { hour12: false });
    const blockers = sections.flatMap(section => {
        if (section.checks) {
            return section.checks
                .filter(check => !check.pass)
                .map(check => `${section.title}：${check.label}（${check.detail}）`);
        }
        if (section.rows && !section.pass) {
            return section.rows
                .filter(row => row[1] !== status(true))
                .map(row => `${section.title}：${row[0]}（${row[2]}）`);
        }
        return [];
    });
    const routeSection = sections.find(section => section.key === 'routeAcceptanceCoverage');
    const subtitleSection = sections.find(section => section.key === 'endingSubtitleDensity');
    const gallerySection = sections.find(section => section.key === 'galleryReferences');
    const replaySection = sections.find(section => section.key === 'replayabilityAndShare');
    const premiumSection = sections.find(section => section.key === 'premiumPlayabilityEvidence');
    const coreLoopPass = routeSection?.checks?.find(check => check.label.startsWith('完整目标11路线验收通过'))?.pass === true;
    const offStreamPass = routeSection?.checks?.find(check => check.label === 'OFF_STREAM_READY/下播流程')?.pass === true;

    const lines = [];
    lines.push('# Quality Readiness Report');
    lines.push('');
    lines.push(`生成时间：${generatedAt}`);
    lines.push('');
    lines.push(`目标：30 天完整局、11 条演示路线、下播链路和结局证据链可验收；本地/受控环境优先，公开发布仍按发行工程另行处理`);
    lines.push('');
    lines.push(`总分：${(totalScore / 10).toFixed(1)}/10`);
    lines.push('');
    lines.push(`完整目标本地门禁：${readinessPass ? 'PASS' : 'FAIL'}`);
    lines.push(`11路线30天闭环：${coreLoopPass ? 'PASS' : 'UNKNOWN'}`);
    lines.push('');
    lines.push(`发布边界：当前报告只证明本地 30 天完整局、11 条演示路线、日报、结局、下播链路和 action registry 兼容迁移闭环；公开发布、支付、云存档和商店发行工程不在本报告内。/api/system/config-check 在 prod 下已裁剪为最小诊断响应，但接口访问控制/网关策略仍需单独确认；OFF_STREAM_READY / 下播流程${offStreamPass ? '已有本地探针覆盖' : '当前仍为 NOT_COVERED'}。`);
    lines.push('');
    lines.push('公开资源策略：/gallery/v4/** 只服务 v4 运行时资产。背景音乐由 src/main/resources/static/sounds 下的 bgm-*.mp3 提供，短音效继续保留 wav。');
    lines.push('');
    lines.push('## 仪表盘');
    lines.push('');
    lines.push('| 模块 | 权重 | 得分 | 结果 |');
    lines.push('|------|------|------|------|');
    for (const section of sections) {
        lines.push(`| ${section.title} | ${weights[section.key]} | ${section.score.toFixed(1)} | ${status(section.pass)} |`);
    }
    lines.push('');
    lines.push('## 阻断项');
    lines.push('');
    if (blockers.length) {
        for (const blocker of blockers) lines.push(`- ${escapeMarkdown(blocker)}`);
    } else {
        lines.push('- 无');
    }
    lines.push('');

    for (const section of sections) {
        lines.push(`## ${section.title}`);
        lines.push('');
        lines.push('| 检查项 | 结果 | 证据 |');
        lines.push('|--------|------|------|');
        lines.push(sectionChecks(section));
        lines.push('');
    }

    lines.push('## 结局副标题密度明细');
    lines.push('');
    lines.push(`平均有效字符：${subtitleSection.averageLength.toFixed(1)}`);
    lines.push('');
    lines.push('| 结局 | 条数 | 长度范围 | 结果 | 示例副标题 |');
    lines.push('|------|------|----------|------|------------|');
    for (const item of subtitleSection.details) {
        lines.push(`| ${item.type} | ${item.count} | ${item.minLength}-${item.maxLength} | ${status(item.dense)} | ${escapeMarkdown(item.sample)} |`);
    }
    lines.push('');

    lines.push('## 复玩/传播明细');
    lines.push('');
    lines.push(`随机事件总数：${replaySection.events.length}`);
    lines.push('');
    lines.push('| 类型 | 数量 |');
    lines.push('|------|------|');
    for (const type of ['POSITIVE', 'NEUTRAL', 'MIXED', 'NEGATIVE']) {
        lines.push(`| ${type} | ${replaySection.typeCounts.get(type) || 0} |`);
    }
    lines.push('');

    lines.push('## 图库引用明细');
    lines.push('');
    lines.push(`前端图库引用：${gallerySection.refs.length}`);
    lines.push('');
    lines.push(`未登记但文件存在的前端引用：${gallerySection.unregisteredRefs.length}`);
    lines.push('');
    if (gallerySection.missingFiles.length) {
        lines.push('缺失文件：');
        for (const ref of gallerySection.missingFiles) {
            lines.push(`- ${ref.file}（${ref.sources.join(', ')}）`);
        }
    } else {
        lines.push('缺失文件：无');
    }
    lines.push('');

    lines.push('## 路线验收明细');
    lines.push('');
    if (routeSection.evidence) {
        lines.push(`证据来源：${path.relative(root, routeSection.evidence.sourcePath).replaceAll('\\', '/')}`);
        lines.push('');
        lines.push(`生成时间：${routeSection.evidence.generatedAt || '未知'}`);
        lines.push('');
        lines.push('| 验收路线 | 策略 | 结果 | 天数 | 阶段 | 预期结局 | 实际结局 | 日报 | 日志 |');
        lines.push('|----------|------|------|------|------|----------|----------|------|------|');
        for (const item of routeSection.strategies) {
            const routeSpec = fullRouteSpecs.get(item.strategy);
            const label = item.label || routeSpec?.label || item.strategy;
            const expectedEnding = item.expectedEnding || routeSpec?.expectedEnding || '';
            lines.push(`| ${escapeMarkdown(label)} | ${item.strategy} | ${status(Boolean(item.passed))} | ${item.currentDay ?? ''} | ${item.phase ?? ''} | ${expectedEnding} | ${item.endingType ?? ''} | ${item.reportCount ?? ''} | ${item.logCount ?? ''} |`);
        }
    } else {
        lines.push('未找到路线验收证据。先运行 `scripts/acceptance-routes.ps1` 生成本地证据。');
    }
    lines.push('');
    lines.push('## 运行方式');
    lines.push('');
    lines.push('```powershell');
    lines.push('node scripts/quality-readiness-check.mjs');
    lines.push('```');
    lines.push('');
    lines.push('该脚本不访问网络，不启动服务，只读取本地源码、图库与既有路线验收结果，并刷新本报告。');
    lines.push('');
    return lines.join('\n');
}

const sections = [
    checkReadmeRunEntry(),
    checkEndingSubtitleDensity(),
    checkGalleryReferences(),
    checkFrontendMarkers(),
    checkActionRegistryMigration(),
    checkReleaseAssetPolicy(),
    checkRouteAcceptanceCoverage(),
    checkReplayabilityAndShare(),
    checkPremiumPlayabilityEvidence()
];
const maxScore = sections.reduce((sum, section) => sum + (weights[section.key] || 0), 0);
const rawScore = sections.reduce((sum, section) => sum + section.score, 0);
const totalScore = maxScore > 0 ? Math.round((rawScore / maxScore) * 1000) / 10 : 0;
const readinessPass = totalScore >= 98 && sections.every(section => section.pass);

mkdirSync(targetDir, { recursive: true });
writeFileSync(reportPath, buildReport(sections, totalScore, readinessPass), 'utf8');

console.log(`Quality readiness: ${(totalScore / 10).toFixed(1)}/10 ${readinessPass ? 'PASS' : 'FAIL'}`);
console.log(`Report written: ${path.relative(root, reportPath).replaceAll('\\', '/')}`);

if (!readinessPass) {
    process.exitCode = 1;
}
