#!/usr/bin/env node

import { existsSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { spawn } from 'node:child_process';
import { setTimeout as sleep } from 'node:timers/promises';
import os from 'node:os';

const options = parseArgs(process.argv.slice(2));
const appUrl = normalizeUrl(options.app || 'http://localhost:18087/');
const outDir = resolve(options.out || 'target/browser-ui-evidence');
const cdpPort = Number(options.port || process.env.CDP_PORT || 9342);
const cdpBase = normalizeUrl(options.cdp || `http://127.0.0.1:${cdpPort}/`);
const launchBrowser = options.launch !== false && !options.cdp;
const browserPath = options.browser || process.env.BROWSER || process.env.CHROME_PATH || process.env.EDGE_PATH || findBrowserExecutable();

const VIEWPORTS = [
    { width: 1366, height: 768 },
    { width: 1600, height: 900 },
    { width: 1920, height: 1080 },
    { width: 2560, height: 1440 }
];
const ZOOMS = [1, 1.25];
const PAGE_SCENARIOS = [
    {
        name: 'first',
        label: '首屏入口',
        screenshotPrefix: 'first-entry',
        setup: firstEntryExpression
    },
    {
        name: 'action',
        label: '行动页',
        screenshotPrefix: 'action-ready',
        setup: actionReadyExpression
    },
    {
        name: 'report',
        label: '日报页',
        screenshotPrefix: 'report-ready',
        setup: reportReadyExpression
    },
    {
        name: 'ending',
        label: '结局页',
        screenshotPrefix: 'ending-ready',
        setup: endingReadyExpression
    },
    {
        name: 'battle-card',
        label: '战报卡',
        screenshotPrefix: 'battle-card',
        setup: battleCardExpression
    }
];

mkdirSync(outDir, { recursive: true });

let browserProcess = null;
let profileDir = null;

try {
    await ensureAppReady(appUrl);
    if (launchBrowser) {
        if (!browserPath) {
            throw new Error('No Chromium/Edge executable found. Pass --browser "path\\to\\chrome.exe" or start a browser with --remote-debugging-port and pass --cdp.');
        }
        ({ browserProcess, profileDir } = await startBrowser(browserPath, cdpPort));
    } else {
        await waitForCdp(cdpBase, 3000);
    }

    const results = [];
    for (const viewport of VIEWPORTS) {
        for (const zoom of ZOOMS) {
            for (const scenario of PAGE_SCENARIOS) {
                results.push(await runScenario({ scenario, viewport, zoom }));
            }
        }
    }

    const failedChecks = results.flatMap(result => result.failedChecks.map(check => `${result.name}:${check}`));
    const summary = {
        generatedAt: new Date().toISOString(),
        appUrl,
        cdpBase,
        browserPath: launchBrowser ? browserPath : null,
        viewportMatrix: VIEWPORTS,
        zoomMatrix: ZOOMS,
        scenarioCount: PAGE_SCENARIOS.length,
        resetWarning: 'This script calls /api/dev/demo/reset and /api/dev/demo/run-script. Run it on a disposable manual/test port, not during a preserved manual playtest.',
        allPassed: failedChecks.length === 0,
        failedChecks,
        results
    };
    const reportPath = join(outDir, 'browser-ui-evidence-report.json');
    writeFileSync(reportPath, JSON.stringify(summary, null, 2), 'utf8');
    console.log(JSON.stringify({
        allPassed: summary.allPassed,
        screenshots: results.length,
        failedChecks,
        report: reportPath
    }, null, 2));
    if (!summary.allPassed) {
        process.exitCode = 1;
    }
} finally {
    if (browserProcess && !browserProcess.killed) {
        browserProcess.kill();
    }
    if (profileDir) {
        try {
            rmSync(profileDir, { recursive: true, force: true });
        } catch {}
    }
}

function parseArgs(argv) {
    const parsed = {};
    for (let index = 0; index < argv.length; index++) {
        const arg = argv[index];
        if (!arg.startsWith('--')) continue;
        const key = arg.slice(2);
        if (key === 'no-launch') {
            parsed.launch = false;
            continue;
        }
        const next = argv[index + 1];
        if (!next || next.startsWith('--')) {
            parsed[key] = true;
            continue;
        }
        parsed[key] = next;
        index++;
    }
    return parsed;
}

function normalizeUrl(value) {
    const text = String(value || '').trim();
    return text.endsWith('/') ? text : `${text}/`;
}

async function ensureAppReady(baseUrl) {
    let lastError;
    for (let attempt = 0; attempt < 20; attempt++) {
        try {
            const response = await fetch(baseUrl);
            if (response.ok) return;
            lastError = new Error(`HTTP ${response.status}`);
        } catch (error) {
            lastError = error;
        }
        await sleep(250);
    }
    throw new Error(`App is not ready at ${baseUrl}. Start the manual server first, for example: scripts\\run-latest-manual.cmd -Port 18087. Last error: ${lastError?.message}`);
}

function findBrowserExecutable() {
    const candidates = [
        join(process.env.LOCALAPPDATA || '', 'ms-playwright', 'chromium-1228', 'chrome-win64', 'chrome.exe'),
        join(process.env.PROGRAMFILES || '', 'Google', 'Chrome', 'Application', 'chrome.exe'),
        join(process.env['PROGRAMFILES(X86)'] || '', 'Google', 'Chrome', 'Application', 'chrome.exe'),
        join(process.env.LOCALAPPDATA || '', 'Google', 'Chrome', 'Application', 'chrome.exe'),
        join(process.env.PROGRAMFILES || '', 'Microsoft', 'Edge', 'Application', 'msedge.exe'),
        join(process.env['PROGRAMFILES(X86)'] || '', 'Microsoft', 'Edge', 'Application', 'msedge.exe'),
        join(process.env.LOCALAPPDATA || '', 'Programs', 'OCS Desktop', 'resources', 'bin', 'chrome', 'chrome', 'chrome.exe')
    ];
    return candidates.find(candidate => candidate && existsSync(candidate)) || null;
}

async function startBrowser(executable, port) {
    const profile = join(os.tmpdir(), `vupworld-cdp-${Date.now()}-${Math.random().toString(16).slice(2)}`);
    const args = [
        '--headless=new',
        '--disable-gpu',
        '--disable-extensions',
        '--disable-background-networking',
        '--disable-sync',
        '--no-first-run',
        '--no-default-browser-check',
        '--window-size=2560,1440',
        `--remote-debugging-port=${port}`,
        `--user-data-dir=${profile}`,
        'about:blank'
    ];
    const child = spawn(executable, args, { stdio: 'ignore' });
    child.unref();
    await waitForCdp(`http://127.0.0.1:${port}/`, 12000);
    return { browserProcess: child, profileDir: profile };
}

async function waitForCdp(baseUrl, timeoutMs) {
    const start = Date.now();
    let lastError;
    while (Date.now() - start < timeoutMs) {
        try {
            const response = await fetch(new URL('/json/version', baseUrl));
            if (response.ok) return;
            lastError = new Error(`HTTP ${response.status}`);
        } catch (error) {
            lastError = error;
        }
        await sleep(150);
    }
    throw new Error(`CDP endpoint is not ready at ${baseUrl}. Last error: ${lastError?.message}`);
}

async function runScenario({ scenario, viewport, zoom }) {
    const zoomSuffix = `z${Math.round(zoom * 100)}`;
    const viewportSuffix = `${viewport.width}x${viewport.height}`;
    const screenshot = join(outDir, `${scenario.screenshotPrefix}-${viewportSuffix}-${zoomSuffix}.png`);
    const name = `${scenario.name}-${viewportSuffix}-${zoomSuffix}`;
    const page = await openPage();
    const { socket, send } = page;
    try {
        await send('Page.enable');
        await send('Runtime.enable');
        await setDesktopWindowSize(send, viewport);
        await setDesktopMetrics(send, viewport, zoom);
        await send('Page.navigate', { url: appUrl });
        await sleep(350);
        const metrics = await evaluate(send, scenario.setup({ viewport, zoom, name }));
        await sleep(900);
        mkdirSync(dirname(screenshot), { recursive: true });
        const shot = await send('Page.captureScreenshot', { format: 'png', fromSurface: true });
        const imageBytes = Buffer.from(shot.data, 'base64');
        writeFileSync(screenshot, imageBytes);
        if (scenario.name === 'action' && viewport.width === 1600 && viewport.height === 900 && zoom === 1) {
            writeFileSync(join(outDir, 'desktop-ready.png'), imageBytes);
        }
        const checks = {
            ...(metrics.checks || {}),
            screenshotNonBlank: imageBytes.length > 16000
        };
        const failedChecks = Object.entries(checks)
            .filter(([, passed]) => !passed)
            .map(([check]) => check);
        return {
            name,
            scenario: scenario.name,
            label: scenario.label,
            viewport,
            zoom,
            cssViewport: cssViewportFor(viewport, zoom),
            screenshot,
            metrics: { ...metrics, checks },
            failedChecks
        };
    } finally {
        try {
            await send('Emulation.clearDeviceMetricsOverride');
        } catch {}
        socket.close();
    }
}

async function openPage() {
    let target = null;
    try {
        const response = await fetch(new URL(`/json/new?${encodeURIComponent('about:blank')}`, cdpBase), { method: 'PUT' });
        if (response.ok) {
            target = await response.json();
        }
    } catch {}
    if (!target) {
        const targets = await fetch(new URL('/json/list', cdpBase)).then(response => response.json());
        target = targets.find(item => item.type === 'page');
    }
    if (!target?.webSocketDebuggerUrl) {
        throw new Error('No debuggable page target found.');
    }

    const socket = new WebSocket(target.webSocketDebuggerUrl);
    await new Promise((resolve, reject) => {
        socket.addEventListener('open', resolve, { once: true });
        socket.addEventListener('error', reject, { once: true });
    });

    let id = 0;
    const pending = new Map();
    socket.addEventListener('message', event => {
        const message = JSON.parse(event.data);
        if (!message.id || !pending.has(message.id)) return;
        const { resolve, reject } = pending.get(message.id);
        pending.delete(message.id);
        if (message.error) {
            reject(new Error(message.error.message));
        } else {
            resolve(message.result || {});
        }
    });

    const send = (method, params = {}) => new Promise((resolve, reject) => {
        const messageId = ++id;
        pending.set(messageId, { resolve, reject });
        socket.send(JSON.stringify({ id: messageId, method, params }));
    });

    return { socket, send };
}

async function setDesktopWindowSize(send, viewport) {
    try {
        const { windowId } = await send('Browser.getWindowForTarget');
        if (typeof windowId === 'number') {
            await send('Browser.setWindowBounds', {
                windowId,
                bounds: {
                    windowState: 'normal',
                    left: 0,
                    top: 0,
                    width: viewport.width,
                    height: viewport.height
                }
            });
            await sleep(120);
        }
    } catch {
        // Some externally supplied CDP targets do not expose browser-window bounds.
    }
}

async function setDesktopMetrics(send, viewport, zoom) {
    const css = cssViewportFor(viewport, zoom);
    await send('Emulation.setDeviceMetricsOverride', {
        width: css.width,
        height: css.height,
        deviceScaleFactor: zoom,
        mobile: false,
        screenWidth: viewport.width,
        screenHeight: viewport.height,
        scale: 1
    });
}

function cssViewportFor(viewport, zoom) {
    return {
        width: Math.max(320, Math.floor(viewport.width / zoom)),
        height: Math.max(240, Math.floor(viewport.height / zoom))
    };
}

async function evaluate(send, expression) {
    const result = await send('Runtime.evaluate', {
        expression,
        awaitPromise: true,
        returnByValue: true
    });
    if (result.exceptionDetails) {
        const description = result.exceptionDetails.exception?.description
            || result.exceptionDetails.text
            || 'Runtime evaluation failed';
        throw new Error(description);
    }
    return result.result?.value;
}

function sharedBrowserHelpers() {
    return String.raw`
        const waitFor = async (predicate, timeout = 18000) => {
            const start = performance.now();
            while (performance.now() - start < timeout) {
                try {
                    if (predicate()) return true;
                } catch (error) {}
                await new Promise(resolve => setTimeout(resolve, 100));
            }
            return false;
        };
        const apiPost = async (path, body = {}) => {
            const response = await fetch(path, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify(body)
            });
            const payload = await response.json().catch(() => ({}));
            if (!response.ok || payload.success === false) {
                throw new Error(payload.message || payload.code || path);
            }
            return payload.data;
        };
        const box = selector => {
            const node = document.querySelector(selector);
            const rect = node?.getBoundingClientRect();
            if (!rect) return null;
            return {
                top: rect.top,
                right: rect.right,
                bottom: rect.bottom,
                left: rect.left,
                width: rect.width,
                height: rect.height,
                visible: rect.width > 0 && rect.height > 0 && rect.bottom > 0 && rect.top < innerHeight && rect.right > 0 && rect.left < innerWidth
            };
        };
        const isVisible = selector => Boolean(box(selector)?.visible);
        const visibleCount = selector => [...document.querySelectorAll(selector)].filter(node => {
            const rect = node.getBoundingClientRect();
            return rect.width > 0 && rect.height > 0 && rect.bottom > 0 && rect.top < innerHeight && rect.right > 0 && rect.left < innerWidth;
        }).length;
        const text = selector => document.querySelector(selector)?.innerText?.trim() || '';
        const noHorizontalOverflow = () => document.documentElement.scrollWidth <= document.documentElement.clientWidth + 2
            && document.body.scrollWidth <= document.documentElement.clientWidth + 2;
        const visibleTextContent = () => {
            const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
                acceptNode(node) {
                    const value = String(node.nodeValue || '').trim();
                    if (!value) return NodeFilter.FILTER_REJECT;
                    const parent = node.parentElement;
                    if (!parent) return NodeFilter.FILTER_REJECT;
                    const style = getComputedStyle(parent);
                    if (style.display === 'none' || style.visibility === 'hidden') return NodeFilter.FILTER_REJECT;
                    const range = document.createRange();
                    range.selectNodeContents(node);
                    const rect = range.getBoundingClientRect();
                    range.detach();
                    return rect.width > 0
                        && rect.height > 0
                        && rect.bottom > 0
                        && rect.top < innerHeight
                        && rect.right > 0
                        && rect.left < innerWidth
                        ? NodeFilter.FILTER_ACCEPT
                        : NodeFilter.FILTER_REJECT;
                }
            });
            const chunks = [];
            let node = walker.nextNode();
            while (node) {
                chunks.push(node.nodeValue);
                node = walker.nextNode();
            }
            return chunks.join('\n');
        };
        const forbiddenVisibleCopy = () => {
            const forbidden = [
                '开发验证', '开发高级', '答辩驾驶舱', '调试', 'Demo',
                '移动端', '手机端', '触屏', 'safe-area',
                '充值', '氪金', '内购', '付费解锁'
            ];
            const visibleText = visibleTextContent();
            return forbidden.filter(word => visibleText.includes(word));
        };
        const textFitIssues = () => [...document.querySelectorAll('button, .guest-start-box, .simple-decision-card, .daily-plan-card, .report-three-beat, .report-primary-deltas, .ending-share-button, .ending-hero')]
            .filter(node => {
                const rect = node.getBoundingClientRect();
                if (rect.width <= 0 || rect.height <= 0) return false;
                if (rect.bottom < 0 || rect.top > innerHeight) return false;
                if (node.matches('.sidebar-toggle-btn, .sfx-toggle, .voice-toggle')) return false;
                const style = getComputedStyle(node);
                if (style.overflowX === 'auto' || style.overflowY === 'auto') return false;
                return node.scrollWidth > node.clientWidth + 3 || node.scrollHeight > node.clientHeight + 6;
            })
            .slice(0, 8)
            .map(node => node.className || node.dataset.action || node.tagName);
        const focusProbe = async () => {
            const focusable = [...document.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])')]
                .find(node => {
                    const rect = node.getBoundingClientRect();
                    return !node.disabled
                        && rect.width > 0
                        && rect.height > 0
                        && rect.bottom > 0
                        && rect.top < innerHeight
                        && rect.right > 0
                        && rect.left < innerWidth;
                });
            if (!focusable) return false;
            focusable.focus();
            await new Promise(resolve => setTimeout(resolve, 20));
            return document.activeElement === focusable;
        };
        const baseChecks = async () => {
            const forbidden = forbiddenVisibleCopy();
            const fitIssues = textFitIssues();
            return {
                cssViewportMatches: innerWidth === expectedCssWidth && innerHeight === expectedCssHeight,
                desktopWidth: innerWidth >= 1000,
                hasReadableText: (document.body.innerText || '').trim().length > 80,
                noHorizontalOverflow: noHorizontalOverflow(),
                noForbiddenVisibleCopy: forbidden.length === 0,
                noTextFitIssues: fitIssues.length === 0,
                focusProbePasses: await focusProbe(),
                forbiddenVisibleCopy: forbidden,
                textFitIssues: fitIssues
            };
        };
        const waitForApp = async () => {
            const ok = await waitFor(() => typeof hydrate === 'function'
                && typeof render === 'function'
                && typeof state === 'object'
                && document.getElementById('gameContainer'));
            if (!ok) throw new Error('App globals did not become ready.');
        };
        const resetDemo = async (scenario, seed) => {
            await apiPost('/api/dev/demo/reset', { scenario, runSeed: seed });
            await hydrate();
        };
        const runDemo = async (strategy, targetDay, seed) => {
            await apiPost('/api/dev/demo/run-script', { strategy, targetDay, runSeed: seed, stopOnError: true });
            await hydrate();
            if (typeof hideEndingFullscreen === 'function') {
                hideEndingFullscreen(true);
            }
            await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
        };
    `;
}

function scenarioEnvelope({ viewport, zoom, name }, body) {
    const css = cssViewportFor(viewport, zoom);
    return String.raw`
        (async () => {
            const expectedCssWidth = ${css.width};
            const expectedCssHeight = ${css.height};
            ${sharedBrowserHelpers()}
            await waitForApp();
            const seed = 'browser-ui-${name}-' + Date.now().toString(36);
            ${body}
        })()
    `;
}

function firstEntryExpression(context) {
    return scenarioEnvelope(context, String.raw`
        await apiPost('/api/auth/logout', {});
        if (typeof resetGameState === 'function') resetGameState();
        await hydrate();
        await waitFor(() => isVisible('.guest-landing-panel') && isVisible('[data-action="quick-start-guest"]'));
        window.scrollTo(0, 0);
        const base = await baseChecks();
        const primary = box('[data-action="quick-start-guest"]');
        return {
            scenario: 'first',
            phase: state.session?.phase || 'NO_SESSION',
            bodyClass: document.body.className,
            ctaText: text('[data-action="quick-start-guest"]'),
            checks: {
                ...base,
                firstPanelVisible: isVisible('.guest-landing-panel'),
                mainCtaVisible: Boolean(primary?.visible),
                mainCtaInFirstViewport: Boolean(primary && primary.top >= 0 && primary.bottom <= innerHeight + 1),
                saveSlotControlsVisible: visibleCount('.slot-card, .local-slot-card, [data-action="continue-local-run"], [data-action="quick-start-guest"]') > 0
            }
        };
    `);
}

function actionReadyExpression(context) {
    return scenarioEnvelope(context, String.raw`
        await resetDemo('steady', seed);
        await waitFor(() => state?.vup && state?.session?.phase === 'READY' && document.querySelector('#actionPanel'));
        await waitFor(() => document.querySelector('.opening-style-choice-card, .daily-plan-board, .simple-decision-card, .create-style-card'));
        window.scrollTo(0, 0);
        const base = await baseChecks();
        const ctaSelector = '[data-action="daily-plan-next-step"], [data-action="simple-primary-action"], [data-action="opening-style-choice"], .opening-style-choice-card, [data-action="select-creation-style"], [data-action="daily-plan-submit"], [data-action="quick-submit-action"], #actionPanel button';
        const cta = [...document.querySelectorAll(ctaSelector)].find(node => {
            const rect = node.getBoundingClientRect();
            return rect.width > 0 && rect.height > 0 && rect.top >= 0 && rect.bottom <= innerHeight + 1;
        });
        return {
            scenario: 'action',
            day: state.session?.day,
            phase: state.session?.phase,
            bodyClass: document.body.className,
            ctaText: cta?.innerText || '',
            actionCount: Array.isArray(state.actions) ? state.actions.length : 0,
            checks: {
                ...base,
                readyPhase: state.session?.phase === 'READY',
                actionPanelVisible: isVisible('#actionPanel'),
                dailyPlanOrDecisionVisible: isVisible('.daily-plan-board') || isVisible('.simple-decision-card') || isVisible('.create-style-card') || isVisible('.opening-style-choice-card'),
                routeGuidancePresent: Boolean(document.querySelector('.ready-goal-board, .route-playbook-card, .create-style-card, .opening-style-choice-card')),
                keyCtaVisible: Boolean(cta),
                keyCtaKeyboardCapable: Boolean(cta && (cta.tagName === 'BUTTON' || cta.getAttribute('role') === 'button' || cta.matches('label')) && !cta.disabled)
            }
        };
    `);
}

function reportReadyExpression(context) {
    return scenarioEnvelope(context, String.raw`
        await resetDemo('clip', seed);
        await runDemo('clip', 1, seed);
        await waitFor(() => state?.session?.phase === 'REPORT_READY' && state?.report && isVisible('#reportPanel'));
        window.scrollTo(0, 0);
        const base = await baseChecks();
        const cta = document.querySelector('#reportPanel [data-action="next-day"]');
        return {
            scenario: 'report',
            day: state.session?.day,
            phase: state.session?.phase,
            bodyClass: document.body.className,
            ctaText: cta?.innerText || '',
            checks: {
                ...base,
                reportPhase: state.session?.phase === 'REPORT_READY',
                reportPanelVisible: isVisible('#reportPanel'),
                reportLoopVisible: isVisible('.report-loop'),
                reportThreeBeatVisible: isVisible('.report-three-beat'),
                reportPrimaryDeltasPresent: Boolean(document.querySelector('.report-primary-deltas')),
                nextDayCtaVisible: Boolean(box('#reportPanel [data-action="next-day"]')?.visible),
                nextDayCtaKeyboardCapable: Boolean(cta && cta.tagName === 'BUTTON' && !cta.disabled)
            }
        };
    `);
}

function endingReadyExpression(context) {
    return scenarioEnvelope(context, String.raw`
        await resetDemo('main_stage_king', seed);
        await runDemo('main_stage_king', 30, seed);
        await waitFor(() => state?.session?.phase === 'ENDING_READY' && state?.ending && isVisible('#endingPanel'));
        if (typeof hideEndingFullscreen === 'function') hideEndingFullscreen(true);
        window.scrollTo(0, 0);
        const base = await baseChecks();
        const cta = document.querySelector('#endingPanel [data-action="restart"]');
        return {
            scenario: 'ending',
            day: state.session?.day,
            phase: state.session?.phase,
            endingType: state.ending?.endingType,
            bodyClass: document.body.className,
            ctaText: cta?.innerText || '',
            checks: {
                ...base,
                endingPhase: state.session?.phase === 'ENDING_READY',
                endingPanelVisible: isVisible('#endingPanel'),
                endingHeroVisible: isVisible('.ending-hero'),
                endingShareCardPresent: Boolean(document.querySelector('.ending-share-card')),
                endingRestartCtaPresent: Boolean(cta),
                endingRestartKeyboardCapable: Boolean(cta && cta.tagName === 'BUTTON' && !cta.disabled)
            }
        };
    `);
}

function battleCardExpression(context) {
    return scenarioEnvelope(context, String.raw`
        await resetDemo('main_stage_king', seed);
        await runDemo('main_stage_king', 30, seed);
        await waitFor(() => state?.session?.phase === 'ENDING_READY' && state?.ending && isVisible('.ending-share-card'));
        if (typeof hideEndingFullscreen === 'function') hideEndingFullscreen(true);
        document.querySelector('.ending-share-card')?.scrollIntoView({ block: 'center' });
        await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
        const base = await baseChecks();
        const formats = [...document.querySelectorAll('[data-action="download-ending-share-card"]')]
            .map(button => button.dataset.shareFormat)
            .filter(Boolean)
            .sort();
        const requiredFormats = ['feed', 'landscape', 'portrait'];
        return {
            scenario: 'battle-card',
            day: state.session?.day,
            phase: state.session?.phase,
            endingType: state.ending?.endingType,
            bodyClass: document.body.className,
            formats,
            checks: {
                ...base,
                battleCardVisible: isVisible('.ending-share-card'),
                copyBattleReportPresent: Boolean(document.querySelector('[data-action="copy-ending-share"]')),
                battleCardFormatsReady: requiredFormats.every(format => formats.includes(format)),
                battleCardRestartPresent: Boolean(document.querySelector('[data-action="restart-from-share-card"]')),
                battleButtonsUseRegistry: [...document.querySelectorAll('.ending-share-card [data-action]')].every(button => !button.hasAttribute('onclick')),
                battleCardReadableInViewport: Boolean(box('.ending-share-card')?.visible)
            }
        };
    `);
}
