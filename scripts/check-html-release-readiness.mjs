import { readFileSync, existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '..');
const htmlPath = resolve(ROOT, 'src/main/resources/static/index.html');

if (!existsSync(htmlPath)) { console.error('index.html 不存在'); process.exit(1); }

const html = readFileSync(htmlPath, 'utf-8');
const lines = html.split('\n');
let warnings = 0;
let errors = 0;

console.log('\n=== HTML 发布检查 ===\n');

// 检查必需元素
const checks = [
  { pattern: /<meta charset="UTF-8"/i, name: 'charset meta' },
  { pattern: /<meta name="viewport"/i, name: 'viewport meta' },
  { pattern: /<title>/i, name: 'title tag' }
];
for (const c of checks) {
  if (c.pattern.test(html)) console.log(`  ✅ ${c.name} 存在`);
  else { console.log(`  ❌ ${c.name} 缺失`); errors++; }
}

// 检查内联事件
const inlineEvents = ['onclick', 'oninput', 'onload', 'onerror', 'onsubmit', 'onchange'];
console.log('\n--- 内联事件检查 (警告) ---');
for (const event of inlineEvents) {
  const regex = new RegExp(`\\s${event}=`, 'gi');
  let match;
  while ((match = regex.exec(html)) !== null) {
    const lineNum = html.substring(0, match.index).split('\n').length;
    const line = lines[lineNum - 1]?.trim().substring(0, 80);
    console.log(`  ⚠️  第${lineNum}行: ${event} → ${line}`);
    warnings++;
  }
}

// 检查外部CDN
console.log('\n--- 外部CDN检查 (警告) ---');
const cdnPattern = /https?:\/\/(cdn|unpkg|cdnjs|jsdelivr)/gi;
let cdnMatch;
while ((cdnMatch = cdnPattern.exec(html)) !== null) {
  const lineNum = html.substring(0, cdnMatch.index).split('\n').length;
  const line = lines[lineNum - 1]?.trim().substring(0, 80);
  console.log(`  ⚠️  第${lineNum}行: 外部CDN → ${line}`);
  warnings++;
}

console.log(`\n总计: ${warnings} 个警告, ${errors} 个错误`);
if (errors > 0) { console.log('❌ 检查失败'); process.exit(1); }
else if (warnings > 0) { console.log('⚠️  有警告，但不阻塞'); }
else { console.log('✅ 全部通过'); }
