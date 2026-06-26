import { readFileSync } from 'node:fs';
import path from 'node:path';

const root = path.resolve(new URL('..', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'));
const contentPath = path.join(root, 'src/main/resources/content/midgame-events.json');

const requiredWindows = ['FIRST_NAMING', 'MID_BACKLASH', 'LOCK_WARNING'];
const requiredGroups = ['steady', 'burst', 'heat', 'singing', 'relationship', 'social', 'unknown'];
const requiredChoiceTypes = ['safe', 'traffic', 'meme'];
const routeTargets = [
    'ELECTRONIC_PICKLE',
    'SLICE_SAINT',
    'BLACK_RED_MAIN_STAGE',
    'DD_BUS_STOP',
    'SINGING_IDOL',
    'CYBER_GIRLFRIEND',
    'MAIN_STAGE_KING',
    'GLORIOUS_GRADUATION',
    'SOCIAL_COLLAB',
    'DANCE_MEME',
    'UNKNOWN'
];

function fail(message) {
    console.error(`Midgame content validation failed: ${message}`);
    process.exit(1);
}

function assert(condition, message) {
    if (!condition) fail(message);
}

function readJson(filePath) {
    return JSON.parse(readFileSync(filePath, 'utf8').replace(/^\uFEFF/, ''));
}

function visibleLength(value) {
    return String(value || '').replace(/\s/g, '').length;
}

const content = readJson(contentPath);
const windows = Array.isArray(content.windows) ? content.windows : [];
const events = Array.isArray(content.events) ? content.events : [];

assert(content.version === 'midgame-events-v1', 'version must be midgame-events-v1');
assert(events.length >= requiredWindows.length * requiredGroups.length, `expected at least ${requiredWindows.length * requiredGroups.length} events`);

const windowKeys = new Set(windows.map(item => item.key));
for (const key of requiredWindows) {
    assert(windowKeys.has(key), `missing window ${key}`);
}

const ids = new Set();
const duplicateIds = [];
const coverage = new Map();
const routeCoverage = new Set();

for (const event of events) {
    assert(event.id && /^MID_[A-Z0-9_]+$/.test(event.id), `invalid id ${event.id}`);
    if (ids.has(event.id)) duplicateIds.push(event.id);
    ids.add(event.id);

    assert(requiredWindows.includes(event.window), `${event.id} has invalid window ${event.window}`);
    assert(requiredGroups.includes(event.routeGroup), `${event.id} has invalid routeGroup ${event.routeGroup}`);
    assert(Array.isArray(event.routeTypes) && event.routeTypes.length > 0, `${event.id} missing routeTypes`);
    event.routeTypes.forEach(route => routeCoverage.add(route));

    assert(['POSITIVE', 'NEUTRAL', 'MIXED', 'NEGATIVE'].includes(event.eventType), `${event.id} invalid eventType`);
    assert(visibleLength(event.title) >= 6 && visibleLength(event.title) <= 24, `${event.id} title length out of range`);
    assert(visibleLength(event.description) >= 28, `${event.id} description too short`);
    assert(visibleLength(event.effect) >= 18, `${event.id} effect too short`);

    for (const choiceType of requiredChoiceTypes) {
        const choice = event.choices?.[choiceType];
        assert(choice, `${event.id} missing ${choiceType} choice`);
        assert(visibleLength(choice.label) >= 6, `${event.id}.${choiceType} label too short`);
        assert(visibleLength(choice.costPreview) >= 3, `${event.id}.${choiceType} costPreview too short`);
        assert(visibleLength(choice.riskPreview) >= 3, `${event.id}.${choiceType} riskPreview too short`);
        assert(visibleLength(choice.effectPreview) >= 3, `${event.id}.${choiceType} effectPreview too short`);
    }

    const key = `${event.routeGroup}:${event.window}`;
    coverage.set(key, (coverage.get(key) || 0) + 1);
}

assert(duplicateIds.length === 0, `duplicate ids: ${duplicateIds.join(', ')}`);

const missingCells = [];
for (const group of requiredGroups) {
    for (const window of requiredWindows) {
        const key = `${group}:${window}`;
        if (!coverage.has(key)) missingCells.push(key);
    }
}
assert(missingCells.length === 0, `missing group/window coverage: ${missingCells.join(', ')}`);

const missingRoutes = routeTargets.filter(route => !routeCoverage.has(route));
assert(missingRoutes.length === 0, `missing route coverage: ${missingRoutes.join(', ')}`);

console.log(`Midgame content validation: PASS (${events.length} events, ${coverage.size} group-window cells, ${routeCoverage.size} routes)`);
