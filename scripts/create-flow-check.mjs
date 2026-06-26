import { readFileSync } from 'node:fs';

const app = readFileSync('src/main/resources/static/app.js', 'utf8');
const css = readFileSync('src/main/resources/static/app.css', 'utf8');

const fail = message => {
    console.error(message);
    process.exit(1);
};

if (!app.includes('create-loadout-panel')) {
    fail('Create flow must use a compact loadout panel.');
}

if (!app.includes('create-style-more')) {
    fail('Create flow must collapse extra style presets behind details.');
}

if (!app.includes('creationStylePresets.slice(0, 4)')) {
    fail('Create flow should show four primary presets before the folded extras.');
}

if (!app.includes('开局卡组')) {
    fail('Create flow needs game-like copy for the initial choice.');
}

if (!app.includes('data-action="create-loadout-summary"') || !app.includes('create-loadout-summary-grid')) {
    fail('Create flow must render a playable loadout summary, not only a text form preview.');
}

if (!app.includes('首日打法') || !app.includes('适合路线') || !app.includes('风险提示')) {
    fail('Create loadout summary must explain first move, fit route, and risk.');
}

if (!/\.pregame-mode \.create-loadout-panel\s*\{[\s\S]*?max-height:\s*calc\(100dvh - 220px\)/.test(css)) {
    fail('Create loadout panel must be height-budgeted for one-screen pregame use.');
}

if (!/\.pregame-mode \.create-style-card-copy\s*\{[\s\S]*?display:\s*none/.test(css)) {
    fail('Create preset cards should not show long copy in pregame mode.');
}

if (!/\.pregame-mode \.create-style-grid\s*\{[\s\S]*?grid-template-columns:\s*repeat\(4, minmax\(0, 1fr\)\)/.test(css)) {
    fail('Desktop pregame create presets should fit as a compact four-column row.');
}

if (!/\.pregame-mode \.create-loadout-summary\s*\{[\s\S]*?min-height:\s*86px/.test(css)) {
    fail('Create loadout summary must have a fixed pregame height budget.');
}

if (!/\.create-loadout-summary-grid\s*\{[\s\S]*?grid-template-columns:\s*repeat\(3, minmax\(0, 1fr\)\)/.test(css)) {
    fail('Create loadout summary should scan as three compact gameplay facts.');
}

console.log('Create flow check passed.');
