import { readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';

const root = path.resolve(new URL('..', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'));
const appJs = readFileSync(path.join(root, 'src/main/resources/static/app.js'), 'utf8');
const appCss = readFileSync(path.join(root, 'src/main/resources/static/app.css'), 'utf8');
const reportPath = path.join(root, 'target/ending-share-screenshot-check.json');

const checks = [
    {
        label: 'ending share card screenshot anchor',
        pass: appJs.includes('aria-label="结局战报卡"')
            && appJs.includes('data-screenshot-anchor="ending-streamer-line"')
    },
    {
        label: 'streamer-friendly spoken line',
        pass: appJs.includes('function streamerFriendlyEndingLine')
            && appJs.includes('主播口播')
            && appJs.includes('口播：')
    },
    {
        label: 'downloadable battle report image',
        pass: appJs.includes('function downloadEndingShareCard')
            && appJs.includes('function endingShareCardSvg')
            && appJs.includes('ENDING_SHARE_CARD_FORMATS')
            && appJs.includes('download-ending-share-card')
    },
    {
        label: 'required 16:9 battle report export',
        pass: appJs.includes('landscape')
            && appJs.includes('label: "16:9"')
            && appJs.includes('width: 1920')
            && appJs.includes('height: 1080')
            && appJs.includes('保存16:9')
            && appJs.includes('width="1920" height="1080" viewBox="0 0 1920 1080"')
    },
    {
        label: 'required 9:16 battle report export',
        pass: appJs.includes('portrait')
            && appJs.includes('label: "9:16"')
            && appJs.includes('width: 1080')
            && appJs.includes('height: 1920')
            && appJs.includes('保存9:16')
            && appJs.includes('viewBox="0 0 ${spec.width} ${spec.height}"')
    },
    {
        label: 'optional 4:5 feed battle report export',
        pass: appJs.includes('feed')
            && appJs.includes('label: "4:5"')
            && appJs.includes('height: 1350')
            && appJs.includes('保存4:5')
    },
    {
        label: 'screenshot-safe visual style',
        pass: appCss.includes('.ending-share-card')
            && appCss.includes('.ending-streamer-line')
            && appCss.includes('aspect-ratio: 4 / 5')
    }
];

const summary = {
    generatedAt: new Date().toISOString(),
    allPassed: checks.every(check => check.pass),
    checks
};

writeFileSync(reportPath, JSON.stringify(summary, null, 2), 'utf8');
console.log(JSON.stringify(summary, null, 2));

if (!summary.allPassed) {
    process.exit(1);
}
