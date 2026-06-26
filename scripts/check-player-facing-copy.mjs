import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import path from 'node:path';

const root = path.resolve(new URL('..', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'));

const appJsPath = path.join(root, 'src/main/resources/static/app.js');
const indexHtmlPath = path.join(root, 'src/main/resources/static/index.html');
const contentRoot = path.join(root, 'src/main/resources/content');

const forbiddenRules = [
  { term: '开发高级', label: 'internal validation wording' },
  { term: '开发验证', label: 'internal validation wording' },
  { term: '答辩', label: 'internal validation wording' },
  { term: '课堂', label: 'teaching/demo wording' },
  { term: 'Demo', label: 'demo wording' },
  { term: '调试', label: 'debug wording' },
  { term: '体验模式', label: 'trial-mode wording' },
  { term: '移动端', label: 'mobile wording' },
  { term: '触屏', label: 'mobile/touch wording' },
  { term: '手机端', label: 'mobile wording' },
  { term: '手机版', label: 'mobile wording' },
  { term: '充值', label: 'real-payment wording' },
  { term: '氪金', label: 'real-payment wording' },
  { term: '内购', label: 'real-payment wording' },
  { term: '付费解锁', label: 'real-payment wording' },
  { term: 'safe-area', label: 'mobile safe-area wording' },
  { term: 'touch-action', label: 'mobile touch-action wording' }
];

const contextualRules = [
  {
    term: '安全区',
    label: 'mobile safe-area wording',
    allowLine: line => /口碑|体力|旧账|风险|舆情|路线|拉回/.test(line)
  }
];

const internalAppFunctionNames = [
  'demoTargetDay',
  'demoErrorCodeText',
  'demoErrorMessageText',
  'demoResultLabel',
  'demoBriefText',
  'renderDefenseEvidenceStrip',
  'renderDefenseEvidencePanel',
  'refreshDemoStatus',
  'demoStrategyName',
  'profileLabelFor',
  'demoToolsEnabled',
  'demoStrategyOptions',
  'demoStrategyValue',
  'syncDemoStrategySelects',
  'normalizeInfoTab',
  'syncDemoTabAvailability',
  'renderDemo',
  'demoReset',
  'demoRun',
  'demoResetAndRun',
  'demoFastForward',
  'renderDefenseCockpit'
];

const explicitAppAllowMarkers = [
  'DEV_TOOL_DISABLED',
  '/api/dev/demo',
  "state.infoTab === 'demo'",
  'data-tab="demo"',
  'open-defense-demo',
  'demoToolsEnabled()',
  'refreshDemoStatus',
  'syncDemoTabAvailability',
  'renderDemo()'
];

function relative(filePath) {
  return path.relative(root, filePath).replaceAll('\\', '/');
}

function listFiles(dir, predicate) {
  if (!existsSync(dir)) return [];
  const entries = readdirSync(dir, { withFileTypes: true });
  return entries.flatMap(entry => {
    const absolute = path.join(dir, entry.name);
    if (entry.isDirectory()) return listFiles(absolute, predicate);
    if (!entry.isFile()) return [];
    return predicate(absolute) ? [absolute] : [];
  });
}

function targetFiles() {
  const files = [];
  for (const file of [appJsPath, indexHtmlPath]) {
    if (existsSync(file) && statSync(file).isFile()) {
      files.push(file);
    }
  }
  files.push(...listFiles(contentRoot, file => path.extname(file).toLowerCase() === '.json'));
  return files.sort((left, right) => relative(left).localeCompare(relative(right)));
}

function lineNumberAt(text, index) {
  let line = 1;
  for (let i = 0; i < index; i += 1) {
    if (text.charCodeAt(i) === 10) line += 1;
  }
  return line;
}

function functionRanges(text, names) {
  const ranges = [];
  const lines = text.split(/\r?\n/);
  const starts = [];
  lines.forEach((line, index) => {
    const match = line.match(/^(?:async\s+)?function\s+([A-Za-z0-9_]+)\s*\(/);
    if (match) {
      starts.push({ name: match[1], line: index + 1 });
    }
  });
  starts.forEach((start, index) => {
    if (!names.includes(start.name)) return;
    const next = starts.find((candidate, candidateIndex) => candidateIndex > index && !names.includes(candidate.name));
    ranges.push({
      name: start.name,
      start: start.line,
      end: next ? next.line - 1 : lines.length
    });
  });
  return ranges;
}

function compactSnippet(value) {
  return value.trim().replace(/\s+/g, ' ').slice(0, 180);
}

function inRange(line, ranges) {
  return ranges.some(range => line >= range.start && line <= range.end);
}

function isAllowedFinding(file, lineNumber, line, rule, appInternalRanges) {
  if (file === appJsPath) {
    if (inRange(lineNumber, appInternalRanges)) return true;
    if (explicitAppAllowMarkers.some(marker => line.includes(marker))) return true;
  }
  if (typeof rule.allowLine === 'function' && rule.allowLine(line)) return true;
  return false;
}

function collectFindings(file, text, appInternalRanges) {
  const findings = [];
  const lines = text.split(/\r?\n/);
  lines.forEach((line, index) => {
    const lineNumber = index + 1;
    for (const rule of [...forbiddenRules, ...contextualRules]) {
      if (!line.includes(rule.term)) continue;
      if (isAllowedFinding(file, lineNumber, line, rule, appInternalRanges)) continue;
      findings.push({
        file,
        line: lineNumber,
        rule: rule.label,
        term: rule.term,
        snippet: compactSnippet(line)
      });
    }
  });
  return findings;
}

const appText = existsSync(appJsPath) ? readFileSync(appJsPath, 'utf8').replace(/^\uFEFF/, '') : '';
const appInternalRanges = functionRanges(appText, internalAppFunctionNames);
const files = targetFiles();
const findings = [];

for (const file of files) {
  const text = readFileSync(file, 'utf8').replace(/^\uFEFF/, '');
  findings.push(...collectFindings(file, text, appInternalRanges));
}

if (findings.length) {
  console.error(`Player-facing copy hygiene: FAIL (${findings.length} finding${findings.length === 1 ? '' : 's'})`);
  console.error('Formal player-facing copy must not expose internal validation, mobile, trial-mode, or real-payment wording.');
  console.error('');
  for (const finding of findings) {
    console.error(`- ${relative(finding.file)}:${finding.line} [${finding.rule}] "${finding.term}"`);
    console.error(`  ${finding.snippet}`);
  }
  process.exit(1);
}

console.log(`Player-facing copy hygiene: PASS (${files.length} files scanned, ${appInternalRanges.length} internal app ranges allowed)`);
