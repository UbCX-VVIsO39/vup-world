import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

const sourceFiles = {
  gameController: 'src/main/java/com/example/vupworld/web/GameController.java',
  gameService: 'src/main/java/com/example/vupworld/service/core/GameService.java',
  authService: 'src/main/java/com/example/vupworld/service/AuthService.java',
  appJs: 'src/main/resources/static/app.js'
};

const sources = Object.fromEntries(
  Object.entries(sourceFiles).map(([key, relativePath]) => [key, readSource(relativePath)])
);

const checks = [];

function readSource(relativePath) {
  const absolutePath = path.join(root, relativePath);
  if (!existsSync(absolutePath)) return null;
  return readFileSync(absolutePath, 'utf8').replace(/^\uFEFF/, '');
}

function record(label, pass, detail) {
  checks.push({ label, pass, detail });
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function stripComments(source) {
  let result = '';
  let state = 'normal';

  for (let index = 0; index < source.length; index += 1) {
    const char = source[index];
    const next = source[index + 1];

    if (state === 'lineComment') {
      if (char === '\n') {
        result += char;
        state = 'normal';
      }
      continue;
    }

    if (state === 'blockComment') {
      if (char === '*' && next === '/') {
        index += 1;
        state = 'normal';
      } else if (char === '\n') {
        result += char;
      }
      continue;
    }

    if (state === 'singleQuote' || state === 'doubleQuote' || state === 'template') {
      result += char;
      if (char === '\\') {
        index += 1;
        result += source[index] || '';
        continue;
      }
      if (state === 'singleQuote' && char === "'") state = 'normal';
      if (state === 'doubleQuote' && char === '"') state = 'normal';
      if (state === 'template' && char === '`') state = 'normal';
      continue;
    }

    if (char === '/' && next === '/') {
      index += 1;
      state = 'lineComment';
      continue;
    }
    if (char === '/' && next === '*') {
      index += 1;
      state = 'blockComment';
      continue;
    }
    if (char === "'") state = 'singleQuote';
    if (char === '"') state = 'doubleQuote';
    if (char === '`') state = 'template';
    result += char;
  }

  return result;
}

function annotationHasPath(source, annotation, expectedPath) {
  if (!source) return false;
  const expected = escapeRegExp(expectedPath);
  const pattern = new RegExp(`@${annotation}\\s*\\(([^)]*)\\)`, 'g');
  let match;
  while ((match = pattern.exec(source)) !== null) {
    if (new RegExp(`["']${expected}["']`).test(match[1])) return true;
  }
  return false;
}

function extractBlock(source, openBraceIndex) {
  if (!source || openBraceIndex < 0 || source[openBraceIndex] !== '{') return null;

  let depth = 0;
  let state = 'normal';
  const bodyStart = openBraceIndex + 1;

  for (let index = openBraceIndex; index < source.length; index += 1) {
    const char = source[index];
    const next = source[index + 1];

    if (state === 'lineComment') {
      if (char === '\n') state = 'normal';
      continue;
    }

    if (state === 'blockComment') {
      if (char === '*' && next === '/') {
        index += 1;
        state = 'normal';
      }
      continue;
    }

    if (state === 'singleQuote' || state === 'doubleQuote' || state === 'template') {
      if (char === '\\') {
        index += 1;
        continue;
      }
      if (state === 'singleQuote' && char === "'") state = 'normal';
      if (state === 'doubleQuote' && char === '"') state = 'normal';
      if (state === 'template' && char === '`') state = 'normal';
      continue;
    }

    if (char === '/' && next === '/') {
      index += 1;
      state = 'lineComment';
      continue;
    }
    if (char === '/' && next === '*') {
      index += 1;
      state = 'blockComment';
      continue;
    }
    if (char === "'") {
      state = 'singleQuote';
      continue;
    }
    if (char === '"') {
      state = 'doubleQuote';
      continue;
    }
    if (char === '`') {
      state = 'template';
      continue;
    }

    if (char === '{') depth += 1;
    if (char === '}') {
      depth -= 1;
      if (depth === 0) return source.slice(bodyStart, index);
    }
  }

  return null;
}

function extractJavaMethodBody(source, methodName) {
  if (!source) return null;
  const signature = new RegExp(`\\b${escapeRegExp(methodName)}\\s*\\(`, 'g');
  let match;
  while ((match = signature.exec(source)) !== null) {
    const openBraceIndex = source.indexOf('{', match.index);
    if (openBraceIndex === -1) return null;
    const prefix = source.slice(Math.max(0, match.index - 80), match.index);
    if (/\b(public|private|protected)\s+[\w<>, ?]+\s*$/.test(prefix)) {
      return extractBlock(source, openBraceIndex);
    }
  }
  return null;
}

function extractJsFunctionBody(source, functionName) {
  if (!source) return null;
  const signature = new RegExp(`\\b(?:async\\s+)?function\\s+${escapeRegExp(functionName)}\\s*\\(`);
  const match = signature.exec(source);
  if (!match) return null;

  const openBraceIndex = source.indexOf('{', match.index);
  return extractBlock(source, openBraceIndex);
}

for (const [key, relativePath] of Object.entries(sourceFiles)) {
  record(`source exists: ${relativePath}`, sources[key] !== null, relativePath);
}

const controllerSource = stripComments(sources.gameController || '');
const hasApiPrefix = annotationHasPath(controllerSource, 'RequestMapping', '/api');
const requiredEndpoints = [
  { label: 'GET /api/game/bootstrap', annotation: 'GetMapping', relative: '/game/bootstrap', full: '/api/game/bootstrap' },
  { label: 'POST /api/game/quick-start', annotation: 'PostMapping', relative: '/game/quick-start', full: '/api/game/quick-start' },
  { label: 'GET /api/game/continue', annotation: 'GetMapping', relative: '/game/continue', full: '/api/game/continue' },
  { label: 'POST /api/game/continue', annotation: 'PostMapping', relative: '/game/continue', full: '/api/game/continue' },
  { label: 'GET /api/save-slots', annotation: 'GetMapping', relative: '/save-slots', full: '/api/save-slots' },
  { label: 'POST /api/save-slots/{slotNumber}/start', annotation: 'PostMapping', relative: '/save-slots/{slotNumber}/start', full: '/api/save-slots/{slotNumber}/start' },
  { label: 'GET /api/save-slots/{slotNumber}/continue', annotation: 'GetMapping', relative: '/save-slots/{slotNumber}/continue', full: '/api/save-slots/{slotNumber}/continue' },
  { label: 'POST /api/save-slots/{slotNumber}/continue', annotation: 'PostMapping', relative: '/save-slots/{slotNumber}/continue', full: '/api/save-slots/{slotNumber}/continue' },
  { label: 'POST /api/save-slots/{slotNumber}/save', annotation: 'PostMapping', relative: '/save-slots/{slotNumber}/save', full: '/api/save-slots/{slotNumber}/save' },
  { label: 'GET /api/save/export', annotation: 'GetMapping', relative: '/save/export', full: '/api/save/export' },
  { label: 'POST /api/save/import', annotation: 'PostMapping', relative: '/save/import', full: '/api/save/import' },
  { label: 'GET /api/save-slots/{slotNumber}/export', annotation: 'GetMapping', relative: '/save-slots/{slotNumber}/export', full: '/api/save-slots/{slotNumber}/export' },
  { label: 'POST /api/save-slots/{slotNumber}/import', annotation: 'PostMapping', relative: '/save-slots/{slotNumber}/import', full: '/api/save-slots/{slotNumber}/import' },
  { label: 'POST /api/save-slots/{slotNumber}/rename', annotation: 'PostMapping', relative: '/save-slots/{slotNumber}/rename', full: '/api/save-slots/{slotNumber}/rename' },
  { label: 'POST /api/save-slots/{slotNumber}/restart', annotation: 'PostMapping', relative: '/save-slots/{slotNumber}/restart', full: '/api/save-slots/{slotNumber}/restart' }
];

for (const endpoint of requiredEndpoints) {
  const hasFullMapping = annotationHasPath(controllerSource, endpoint.annotation, endpoint.full);
  const hasRelativeMapping = hasApiPrefix && annotationHasPath(controllerSource, endpoint.annotation, endpoint.relative);
  record(`GameController exposes ${endpoint.label}`, hasFullMapping || hasRelativeMapping, endpoint.full);
}

const quickStartBody = stripComments(extractJavaMethodBody(sources.gameService || '', 'quickStart') || '');
record(
  'GameService.quickStart enters slot 1',
  /\bstartSlot\s*\(\s*user\s*,\s*LOCAL_SLOT\s*\)/.test(quickStartBody),
  'quickStart(...) must enter the default local slot'
);
record(
  'GameService has restartSlot',
  /\bGameStartDTO\s+restartSlot\s*\(/.test(stripComments(sources.gameService || '')),
  'restartSlot(...)'
);
record(
  'GameService has renameSlot',
  /\bSaveSlotDTO\s+renameSlot\s*\(/.test(stripComments(sources.gameService || '')),
  'renameSlot(...)'
);
record(
  'GameService has multi-slot startSlot and continueSlot',
  /\bGameStartDTO\s+startSlot\s*\(/.test(stripComments(sources.gameService || ''))
    && /\bGameStartDTO\s+continueSlot\s*\(/.test(stripComments(sources.gameService || '')),
  'startSlot(...) + continueSlot(...)'
);
record(
  'GameService has save archive export and import',
  /\bSaveArchiveDTO\s+exportSlot\s*\(/.test(stripComments(sources.gameService || ''))
    && /\bGameStartDTO\s+importSlot\s*\(/.test(stripComments(sources.gameService || ''))
    && (sources.gameService || '').includes('singleplayer-save-v1'),
  'exportSlot(...) + importSlot(...) + saveVersion'
);

const authSource = stripComments(sources.authService || '');
record(
  'AuthService has localPlayer',
  /\bUserDTO\s+localPlayer\s*\(/.test(authSource),
  'localPlayer(...)'
);
record(
  'AuthService has localPlayerIfExists',
  /\bUserDTO\s+localPlayerIfExists\s*\(/.test(authSource),
  'localPlayerIfExists(...)'
);

const appSource = stripComments(sources.appJs || '');
for (const endpoint of ['/start', '/continue']) {
  record(`app.js references ${endpoint}`, appSource.includes(endpoint), endpoint);
}
record(
  'app.js references /api/save-slots/1/restart',
  appSource.includes('/restart'),
  '/restart'
);
record(
  'app.js references /api/save-slots/1/rename',
  appSource.includes('/rename'),
  '/rename'
);
record(
  'app.js renders three local save slot cards',
  appSource.includes('renderLocalSaveSlotCards') && appSource.includes('local-save-slot-grid'),
  'renderLocalSaveSlotCards + local-save-slot-grid'
);

const quickStartGuestBody = extractJsFunctionBody(sources.appJs || '', 'quickStartGuest');
record('app.js has quickStartGuest function', quickStartGuestBody !== null, 'quickStartGuest');

if (quickStartGuestBody !== null) {
  const quickStartGuestSource = stripComments(quickStartGuestBody);
  const forbiddenEndpoints = ['/api/auth/register', '/api/auth/login', '/api/vup/create'];
  for (const endpoint of forbiddenEndpoints) {
    record(
      `quickStartGuest does not call ${endpoint}`,
      !quickStartGuestSource.includes(endpoint),
      endpoint
    );
  }
}

record(
  'app.js renders local save summaries through localSaveSlotSummary',
  /\bfunction\s+localSaveSlotSummary\s*\(\s*slot\s*\)/.test(appSource),
  'localSaveSlotSummary(slot)'
);

record(
  'completed local slots display the earned ending title',
  appSource.includes('slot.endingTitle') && appSource.includes('已结局'),
  'slot.endingTitle'
);

record(
  'landing page exposes restart current slot action',
  appSource.includes('restartLocalSlot') && appSource.includes('重新开始本槽'),
  'restartLocalSlot + 重新开始本槽'
);
record(
  'landing page exposes rename current slot action',
  appSource.includes('renameLocalSlot') && appSource.includes('重命名存档'),
  'renameLocalSlot + 重命名存档'
);
record(
  'landing page exposes save archive export/import actions',
  appSource.includes('exportLocalSlot') && appSource.includes('importLocalSlot')
    && appSource.includes('/export') && appSource.includes('/import')
    && appSource.includes('JSON.stringify(payload, null, 2)'),
  'exportLocalSlot + importLocalSlot'
);

const failed = checks.filter(check => !check.pass);

console.log('singleplayer-entry-check');
for (const check of checks) {
  const status = check.pass ? 'PASS' : 'FAIL';
  console.log(`${status} ${check.label} - ${check.detail}`);
}

if (failed.length > 0) {
  console.error(`FAIL singleplayer-entry-check: ${failed.length}/${checks.length} checks failed.`);
  process.exit(1);
}

console.log(`PASS singleplayer-entry-check: ${checks.length}/${checks.length} checks passed.`);
