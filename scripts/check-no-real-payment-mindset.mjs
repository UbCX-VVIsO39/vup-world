import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import path from 'node:path';

const root = path.resolve(new URL('..', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'));

const forbiddenTerms = [
  '充值',
  '氪金',
  '付费解锁',
  '商城',
  '会员',
  '礼包',
  '抽卡',
  '钻石',
  '点券'
];

const scRealPaymentPatterns = [
  {
    label: 'SC must not be explained as real paid/money content',
    pattern: /(?:SC|Super\s*Chat|醒目留言)[^\n\r]{0,80}(?:付费|花钱|付钱|真钱|人民币|支付|充值|购买|真实付费|现实付费)|(?:付费|花钱|付钱|真钱|人民币|支付|充值|购买|真实付费|现实付费)[^\n\r]{0,80}(?:SC|Super\s*Chat|醒目留言)/iu
  }
];

const scanTargets = [
  {
    label: 'static HTML/JS/CSS',
    root: path.join(root, 'src/main/resources/static'),
    extensions: new Set(['.html', '.js', '.css'])
  },
  {
    label: 'data.sql',
    files: [path.join(root, 'src/main/resources/data.sql')]
  },
  {
    label: 'service copy',
    root: path.join(root, 'src/main/java/com/example/vupworld/service'),
    extensions: new Set(['.java'])
  }
];

function relative(filePath) {
  return path.relative(root, filePath).replaceAll('\\', '/');
}

function listFiles(dir, extensions) {
  if (!existsSync(dir)) return [];
  const entries = readdirSync(dir, { withFileTypes: true });
  return entries.flatMap(entry => {
    const absolute = path.join(dir, entry.name);
    if (entry.isDirectory()) return listFiles(absolute, extensions);
    if (!entry.isFile()) return [];
    return extensions.has(path.extname(entry.name).toLowerCase()) ? [absolute] : [];
  });
}

function targetFiles() {
  const files = [];
  for (const target of scanTargets) {
    if (target.files) {
      for (const file of target.files) {
        if (existsSync(file) && statSync(file).isFile()) files.push({ file, label: target.label });
      }
      continue;
    }
    for (const file of listFiles(target.root, target.extensions)) {
      files.push({ file, label: target.label });
    }
  }
  return files.sort((left, right) => relative(left.file).localeCompare(relative(right.file)));
}

function lineNumberAt(text, index) {
  let line = 1;
  for (let i = 0; i < index; i += 1) {
    if (text.charCodeAt(i) === 10) line += 1;
  }
  return line;
}

function lineAt(text, lineNumber) {
  return text.split(/\r?\n/)[lineNumber - 1] ?? '';
}

function compactSnippet(value) {
  return value.trim().replace(/\s+/g, ' ').slice(0, 180);
}

function collectForbiddenTermFindings(file, label, text) {
  const findings = [];
  const lines = text.split(/\r?\n/);
  lines.forEach((line, index) => {
    for (const term of forbiddenTerms) {
      if (!line.includes(term)) continue;
      findings.push({
        file,
        label,
        line: index + 1,
        rule: `forbidden term "${term}"`,
        snippet: compactSnippet(line)
      });
    }
  });
  return findings;
}

function collectScPaymentFindings(file, label, text) {
  const findings = [];
  for (const rule of scRealPaymentPatterns) {
    const pattern = new RegExp(rule.pattern.source, `${rule.pattern.flags.includes('g') ? '' : 'g'}${rule.pattern.flags.replace('g', '')}`);
    for (const match of text.matchAll(pattern)) {
      const line = lineNumberAt(text, match.index ?? 0);
      findings.push({
        file,
        label,
        line,
        rule: rule.label,
        snippet: compactSnippet(lineAt(text, line))
      });
    }
  }
  return findings;
}

const files = targetFiles();
const findings = [];

for (const { file, label } of files) {
  const text = readFileSync(file, 'utf8').replace(/^\uFEFF/, '');
  findings.push(...collectForbiddenTermFindings(file, label, text));
  findings.push(...collectScPaymentFindings(file, label, text));
}

if (findings.length) {
  console.error(`No-real-payment-mindset check: FAIL (${findings.length} finding${findings.length === 1 ? '' : 's'})`);
  console.error('Forbidden real-payment terms are not allowed in formal player-visible source.');
  console.error('SC is allowed only as an in-game simulated/highlight interaction, not as real paid/money content.');
  console.error('');
  for (const finding of findings) {
    console.error(`- ${relative(finding.file)}:${finding.line} [${finding.label}] ${finding.rule}`);
    console.error(`  ${finding.snippet}`);
  }
  process.exit(1);
}

console.log(`No-real-payment-mindset check: PASS (${files.length} files scanned)`);
