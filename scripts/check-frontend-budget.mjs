import { readFileSync, statSync, existsSync } from 'fs';
import { gzipSync } from 'zlib';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '..');

const budgets = {
  jsRawKb: 700,
  cssRawKb: 480,
  htmlRawKb: 30,
  firstPaintImageKb: 2500,
  totalFirstPaintImageKb: 5000
};

const files = [
  { path: 'src/main/resources/static/index.html', label: 'HTML', budget: budgets.htmlRawKb, gzip: true },
  { path: 'src/main/resources/static/app.js', label: 'JS', budget: budgets.jsRawKb, gzip: true },
  { path: 'src/main/resources/static/app.css', label: 'CSS', budget: budgets.cssRawKb, gzip: true },
  { path: 'src/main/resources/static/bili-ui.js', label: 'bili-ui.js', budget: null, gzip: true }
];

const images = [
  { path: '图库/v4/live-room/shells/shell-default.png', label: '背景PNG', budget: budgets.firstPaintImageKb },
  { path: '图库/v4/protagonist/avatars/avatar-default.png', label: '头像PNG', budget: budgets.firstPaintImageKb },
  { path: 'src/main/resources/static/gallery-optimized/v4/live-room/shells/shell-default-1280.webp', label: '背景WebP', budget: 220 },
  { path: 'src/main/resources/static/gallery-optimized/v4/protagonist/avatars/avatar-default-512.webp', label: '头像WebP', budget: 120 }
];

let failed = false;
const rows = [];

for (const f of files) {
  const fullPath = resolve(ROOT, f.path);
  if (!existsSync(fullPath)) {
    failed = true;
    rows.push([f.label, f.path, '不存在', '-', '❌ 缺失', f.budget ? `${f.budget}KB` : '-']);
    continue;
  }
  const stat = statSync(fullPath);
  const rawKb = Math.round(stat.size / 1024);
  let gzipKb = '-';
  if (f.gzip) {
    const content = readFileSync(fullPath);
    gzipKb = Math.round(gzipSync(content).length / 1024) + 'KB';
  }
  const over = f.budget && rawKb > f.budget;
  if (over) failed = true;
  rows.push([f.label, f.path, `${rawKb}KB`, gzipKb, over ? '❌ 超预算' : '✅', f.budget ? `${f.budget}KB` : '-']);
}

let totalImageKb = 0;
for (const img of images) {
  const fullPath = resolve(ROOT, img.path);
  if (!existsSync(fullPath)) {
    failed = true;
    rows.push([img.label, img.path, '不存在', '-', '❌ 缺失', `${img.budget}KB`]);
    continue;
  }
  const stat = statSync(fullPath);
  const rawKb = Math.round(stat.size / 1024);
  totalImageKb += rawKb;
  const over = rawKb > img.budget;
  if (over) failed = true;
  rows.push([img.label, img.path, `${rawKb}KB`, '-', over ? '❌ 超预算' : '✅', `${img.budget}KB`]);
}

// 输出表格
const widths = [12, 60, 10, 10, 10, 10];
const header = ['资源', '路径', '原始大小', 'gzip', '状态', '预算'];
const sep = widths.map(w => '-'.repeat(w));

console.log('\n=== 前端体积预算检查 ===\n');
console.log(header.map((h, i) => h.padEnd(widths[i])).join(' | '));
console.log(sep.join('-+-'));
for (const row of rows) {
  console.log(row.map((c, i) => String(c).padEnd(widths[i])).join(' | '));
}
console.log('\n首屏图片总计: ' + totalImageKb + 'KB (预算: ' + budgets.totalFirstPaintImageKb + 'KB)');
if (totalImageKb > budgets.totalFirstPaintImageKb) { console.log('❌ 首屏图片总计超预算!'); failed = true; }
else { console.log('✅ 首屏图片总计在预算内'); }
console.log('');

if (failed) { console.log('❌ 体积检查失败'); process.exit(1); }
else { console.log('✅ 所有资源在预算内'); }
