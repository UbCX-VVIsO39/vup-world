import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const cdpBase = process.argv[2] || 'http://localhost:9342';
const appUrl = process.argv[3] || 'http://localhost:18080/';
const screenshotPath = resolve(process.argv[4] || 'target/desktop-cockpit-check.png');
const viewportWidth = Number(process.argv[5] || 1440);
const viewportHeight = Number(process.argv[6] || 900);
mkdirSync(dirname(screenshotPath), { recursive: true });

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

async function connectToPage() {
    const targets = await fetch(`${cdpBase}/json/list`).then(response => response.json());
    const page = targets.find(target => target.type === 'page' && target.url.startsWith(appUrl))
        || targets.find(target => target.type === 'page');
    if (!page?.webSocketDebuggerUrl) {
        throw new Error('No debuggable page target found');
    }

    const socket = new WebSocket(page.webSocketDebuggerUrl);
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

async function evaluate(send, expression) {
    const result = await send('Runtime.evaluate', {
        expression,
        awaitPromise: true,
        returnByValue: true
    });
    if (result.exceptionDetails) {
        throw new Error(result.exceptionDetails.exception?.description || result.exceptionDetails.text || 'Runtime evaluation failed');
    }
    return result.result?.value;
}

async function setDesktopWindowSize(send, width, height) {
    try {
        const { windowId } = await send('Browser.getWindowForTarget');
        if (typeof windowId === 'number') {
            let boundsWidth = width;
            let boundsHeight = height;
            for (let attempt = 0; attempt < 4; attempt++) {
                await send('Browser.setWindowBounds', {
                    windowId,
                    bounds: {
                        windowState: 'normal',
                        left: 0,
                        top: 0,
                        width: boundsWidth,
                        height: boundsHeight
                    }
                });
                await sleep(150);
                const current = await readViewportSize(send);
                if (!current) return;
                const deltaWidth = width - current.width;
                const deltaHeight = height - current.height;
                if (Math.abs(deltaWidth) <= 1 && Math.abs(deltaHeight) <= 1) return;
                boundsWidth += deltaWidth;
                boundsHeight += deltaHeight;
            }
        }
    } catch {
        // External CDP targets may not expose browser-window bounds.
    }
}

async function readViewportSize(send) {
    try {
        const result = await send('Runtime.evaluate', {
            expression: '({ width: window.innerWidth, height: window.innerHeight })',
            returnByValue: true
        });
        return result.result?.value || null;
    } catch {
        return null;
    }
}

const { socket, send } = await connectToPage();
try {
    await send('Page.enable');
    await send('Runtime.enable');
    await setDesktopWindowSize(send, viewportWidth, viewportHeight);
    await send('Page.navigate', { url: appUrl });
    await sleep(800);

    const metrics = await evaluate(send, String.raw`
        (async () => {
            const waitFor = async (predicate, timeout = 14000) => {
                const start = performance.now();
                while (performance.now() - start < timeout) {
                    try {
                        if (predicate()) return true;
                    } catch (error) {}
                    await new Promise(resolve => setTimeout(resolve, 100));
                }
                return false;
            };
            const visible = selector => {
                const node = document.querySelector(selector);
                const rect = node?.getBoundingClientRect();
                return Boolean(rect && rect.width > 0 && rect.height > 0 && rect.top < innerHeight && rect.bottom > 0);
            };

            await waitFor(() => typeof register === 'function' && document.getElementById('username'));
            const suffix = Date.now().toString(36);
            document.getElementById('username').value = 'desktop_cockpit_' + suffix;
            document.getElementById('password').value = 'pass1234';
            document.getElementById('nickname').value = '桌面验收员';
            await register();
            await waitFor(() => typeof applyCreationStylePreset === 'function' && document.querySelector('input[name="creationStyle"]'));
            applyCreationStylePreset('clip_machine');
            await createVup();
            await waitFor(() => document.querySelector('.operation-cockpit') && document.querySelector('.action-list') && state?.vup);
            window.scrollTo(0, 0);
            await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));

            const cockpit = document.querySelector('.operation-cockpit');
            const debtPanel = document.querySelector('.cockpit-debt-card');
            const goalPanel = document.querySelector('.cockpit-goal-card');
            const stagePanel = document.querySelector('.cockpit-stage-card');
            const actionPanel = document.querySelector('#actionPanel');
            const actionPanelBox = actionPanel?.getBoundingClientRect();

            return {
                viewport: { width: innerWidth, height: innerHeight },
                route: state.vup?.currentRoute || '',
                cockpitText: cockpit?.innerText || '',
                debtText: debtPanel?.innerText || '',
                actionPanelBottom: actionPanelBox?.bottom || 0,
                overflowX: document.body.scrollWidth - document.documentElement.clientWidth,
                checks: {
                    cockpitVisible: visible('.operation-cockpit'),
                    goalVisible: visible('.cockpit-goal-card'),
                    debtVisible: visible('.cockpit-debt-card'),
                    stageVisible: visible('.cockpit-stage-card'),
                    actionListVisible: visible('.action-list'),
                    primaryCueVisible: visible('.ready-primary-cue'),
                    cockpitNamesGameConcepts: Boolean(cockpit?.innerText.includes('运营驾驶舱')
                        && cockpit?.innerText.includes('结局目标')
                        && cockpit?.innerText.includes('旧账')
                        && cockpit?.innerText.includes('阶段')),
                    debtLifecycleReadable: Boolean(debtPanel?.innerText.includes('到期') || debtPanel?.innerText.includes('暂无旧账')),
                    noHorizontalOverflow: document.body.scrollWidth <= document.documentElement.clientWidth + 1,
                    mainActionFitsFirstScreen: Boolean(actionPanelBox && actionPanelBox.top >= 0 && actionPanelBox.bottom <= innerHeight + 1)
                }
            };
        })()
    `);

    const screenshot = await send('Page.captureScreenshot', { format: 'png', fromSurface: true });
    writeFileSync(screenshotPath, Buffer.from(screenshot.data, 'base64'));

    const failedChecks = Object.entries(metrics.checks)
        .filter(([, passed]) => !passed)
        .map(([name]) => name);
    console.log(JSON.stringify({ metrics, failedChecks, screenshotPath }, null, 2));
    if (failedChecks.length > 0) {
        throw new Error(`Desktop cockpit check failed: ${failedChecks.join(', ')}`);
    }
} finally {
    socket.close();
}
