import { existsSync, mkdirSync, statSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '..');

async function main() {
  let sharp;
  try {
    sharp = (await import('sharp')).default;
  } catch (e) {
    console.error('❌ sharp 未安装。请运行: npm install sharp');
    console.error('   或: npm install sharp --save-dev');
    process.exit(1);
  }

  const targets = [
    {
      src: resolve(ROOT, '图库/v4/live-room/shells/shell-default.png'),
      outputs: [
        { name: 'shell-default-1280.webp', width: 1280, quality: 78 },
        { name: 'shell-default-768.webp', width: 768, quality: 76 }
      ],
      outDir: resolve(ROOT, 'src/main/resources/static/gallery-optimized/v4/live-room/shells')
    },
    {
      src: resolve(ROOT, '图库/v4/protagonist/avatars/avatar-default.png'),
      outputs: [
        { name: 'avatar-default-512.webp', width: 512, quality: 82 },
        { name: 'avatar-default-256.webp', width: 256, quality: 82 }
      ],
      outDir: resolve(ROOT, 'src/main/resources/static/gallery-optimized/v4/protagonist/avatars')
    }
  ];

  const results = [];

  for (const t of targets) {
    if (!existsSync(t.src)) {
      console.log(`⚠️  源文件不存在: ${t.src}`);
      continue;
    }

    const srcStat = statSync(t.src);
    const srcKb = Math.round(srcStat.size / 1024);
    console.log(`\n处理: ${t.src} (${srcKb}KB)`);

    mkdirSync(t.outDir, { recursive: true });

    for (const out of t.outputs) {
      const outPath = resolve(t.outDir, out.name);
      try {
        await sharp(t.src)
          .resize(out.width, null, { withoutEnlargement: true })
          .webp({ quality: out.quality })
          .toFile(outPath);

        const outStat = statSync(outPath);
        const outKb = Math.round(outStat.size / 1024);
        const ratio = Math.round((1 - outKb / srcKb) * 100);
        results.push({ name: out.name, srcKb, outKb, ratio, path: outPath });
        console.log(`  ✅ ${out.name}: ${srcKb}KB → ${outKb}KB (压缩 ${ratio}%)`);
      } catch (e) {
        console.log(`  ❌ ${out.name}: ${e.message}`);
      }
    }
  }

  console.log('\n=== 优化结果汇总 ===\n');
  console.log('文件'.padEnd(30) + '原始'.padEnd(10) + '优化后'.padEnd(10) + '压缩率');
  console.log('-'.repeat(60));
  for (const r of results) {
    console.log(r.name.padEnd(30) + `${r.srcKb}KB`.padEnd(10) + `${r.outKb}KB`.padEnd(10) + `${r.ratio}%`);
  }

  // 验证
  console.log('\n=== 验证 ===');
  let ok = true;
  for (const r of results) {
    if (r.name.includes('1280') && r.outKb > 220) { console.log(`❌ ${r.name} 超过220KB`); ok = false; }
    if (r.name.includes('512') && r.outKb > 120) { console.log(`❌ ${r.name} 超过120KB`); ok = false; }
  }
  if (ok) console.log('✅ 所有图片在预算内');
}

main().catch(e => { console.error(e); process.exit(1); });
