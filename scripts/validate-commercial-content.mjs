import { readFileSync } from 'node:fs';
import path from 'node:path';

const root = path.resolve(new URL('..', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'));
const contentPath = path.join(root, 'src/main/resources/content/commercial-route-content.json');

const requiredRouteTypes = [
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
const requiredLateWindows = ['FINAL_PRESSURE', 'FINAL_LOCK', 'FINAL_SCREENSHOT'];
const expectedWindowRanges = {
  FINAL_PRESSURE: [24, 25],
  FINAL_LOCK: [26, 27],
  FINAL_SCREENSHOT: [28, 29]
};
const requiredGroups = ['steady', 'burst', 'heat', 'singing', 'relationship', 'social', 'unknown'];
const requiredChoiceTypes = ['safe', 'traffic', 'meme'];

function fail(message) {
  console.error(`Commercial content validation failed: ${message}`);
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
const identities = Array.isArray(content.routeIdentities) ? content.routeIdentities : [];
const windows = Array.isArray(content.lateGameWindows) ? content.lateGameWindows : [];
const events = Array.isArray(content.lateGameEvents) ? content.lateGameEvents : [];

assert(content.version === 'commercial-route-content-v1', 'version must be commercial-route-content-v1');

const identityRoutes = new Set(identities.map(item => item.routeType));
assert(identities.length === requiredRouteTypes.length, `expected exactly ${requiredRouteTypes.length} route identities`);
assert(identityRoutes.size === identities.length, 'duplicate route identities');
const missingIdentityRoutes = requiredRouteTypes.filter(route => !identityRoutes.has(route));
assert(missingIdentityRoutes.length === 0, `missing route identities: ${missingIdentityRoutes.join(', ')}`);

for (const identity of identities) {
  assert(requiredRouteTypes.includes(identity.routeType), `invalid route identity ${identity.routeType}`);
  assert(visibleLength(identity.label) >= 3, `${identity.routeType} label too short`);
  assert(visibleLength(identity.fantasy) >= 12, `${identity.routeType} fantasy too short`);
  assert(visibleLength(identity.cost) >= 12, `${identity.routeType} cost too short`);
  assert(visibleLength(identity.playerPromise) >= 12, `${identity.routeType} playerPromise too short`);
  assert(visibleLength(identity.streamerHook) >= 12, `${identity.routeType} streamerHook too short`);
}

const windowKeys = new Set(windows.map(item => item.key));
for (const key of requiredLateWindows) {
  assert(windowKeys.has(key), `missing late-game window ${key}`);
}
for (const window of windows) {
  const range = expectedWindowRanges[window.key];
  assert(range, `invalid late-game window ${window.key}`);
  assert(window.startDay === range[0] && window.endDay === range[1], `${window.key} must cover Day${range[0]}-${range[1]}`);
}

assert(events.length === requiredLateWindows.length * requiredGroups.length, `expected exactly ${requiredLateWindows.length * requiredGroups.length} late-game events`);

const ids = new Set();
const duplicateIds = [];
const coverage = new Map();
const routeCoverage = new Set();

for (const event of events) {
  assert(event.id && /^LATE_[A-Z0-9_]+$/.test(event.id), `invalid late-game id ${event.id}`);
  if (ids.has(event.id)) duplicateIds.push(event.id);
  ids.add(event.id);

  assert(requiredLateWindows.includes(event.window), `${event.id} has invalid window ${event.window}`);
  assert(requiredGroups.includes(event.routeGroup), `${event.id} has invalid routeGroup ${event.routeGroup}`);
  assert(Array.isArray(event.routeTypes) && event.routeTypes.length > 0, `${event.id} missing routeTypes`);
  event.routeTypes.forEach(route => {
    assert(requiredRouteTypes.includes(route), `${event.id} references invalid routeType ${route}`);
    routeCoverage.add(route);
  });

  assert(['POSITIVE', 'NEUTRAL', 'MIXED', 'NEGATIVE'].includes(event.eventType), `${event.id} invalid eventType`);
  assert(visibleLength(event.title) >= 6 && visibleLength(event.title) <= 24, `${event.id} title length out of range`);
  assert(visibleLength(event.description) >= 28, `${event.id} description too short`);
  assert(visibleLength(event.effect) >= 18, `${event.id} effect too short`);
  assert(visibleLength(event.screenshotLine) >= 8, `${event.id} screenshotLine too short`);
  assert(visibleLength(event.streamerLine) >= 8, `${event.id} streamerLine too short`);
  assert(visibleLength(event.heckleLine) >= 8, `${event.id} heckleLine too short`);

  for (const choiceType of requiredChoiceTypes) {
    const choice = event.choices?.[choiceType];
    assert(choice, `${event.id} missing ${choiceType} choice`);
    assert(visibleLength(choice.label) >= 4, `${event.id}.${choiceType} label too short`);
    assert(visibleLength(choice.costPreview) >= 3, `${event.id}.${choiceType} costPreview too short`);
    assert(visibleLength(choice.riskPreview) >= 3, `${event.id}.${choiceType} riskPreview too short`);
    assert(visibleLength(choice.effectPreview) >= 3, `${event.id}.${choiceType} effectPreview too short`);
  }

  const key = `${event.routeGroup}:${event.window}`;
  coverage.set(key, (coverage.get(key) || 0) + 1);
}

assert(duplicateIds.length === 0, `duplicate late-game ids: ${duplicateIds.join(', ')}`);

const missingCells = [];
for (const group of requiredGroups) {
  for (const window of requiredLateWindows) {
    const key = `${group}:${window}`;
    if (!coverage.has(key)) missingCells.push(key);
  }
}
assert(missingCells.length === 0, `missing late-game group/window coverage: ${missingCells.join(', ')}`);

const missingRoutes = requiredRouteTypes.filter(route => !routeCoverage.has(route));
assert(missingRoutes.length === 0, `missing late-game route coverage: ${missingRoutes.join(', ')}`);

console.log(`Commercial content validation: PASS (${identities.length} route identities, ${events.length} late-game events, ${coverage.size} group-window cells)`);
