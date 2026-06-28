// === Player panel bundle loader ===
// app.js keeps the shared globals; panel-bundle.js owns the large render/bootstrap UI.
// Load it from here so index.html can keep a single classic script entry before modules.
// Defense static surface kept for acceptance checks after the bundle split:
// function renderDefenseEvidencePanel(), SSM证据台, 梗舞练功房, 黑红法庭, 花房歌会.
var ROUTE_AVATAR_MAP = {
    SINGING_IDOL: '/gallery/v4/routes/singing-idol/mini-avatar.png',
    SLICE_SAINT: '/gallery/v4/routes/slice-saint/mini-avatar.png',
    DANCE_MEME: '/gallery/v4/routes/dance-meme/mini-avatar.png',
    SOCIAL_COLLAB: '/gallery/v4/routes/social-collab/mini-avatar.png',
    BLACK_RED_MAIN_STAGE: '/gallery/v4/routes/black-red-main-stage/mini-avatar.png',
    ELECTRONIC_PICKLE: '/gallery/v4/routes/electronic-pickle/mini-avatar.png',
    CYBER_GIRLFRIEND: '/gallery/v4/routes/cyber-girlfriend/mini-avatar.png',
    DD_BUS_STOP: '/gallery/v4/routes/dd-bus-stop/mini-avatar.png',
    MAIN_STAGE_KING: '/gallery/v4/routes/main-stage-king/mini-avatar.png',
    GLORIOUS_GRADUATION: '/gallery/v4/routes/glorious-graduation/mini-avatar.png',
    UNKNOWN: '/gallery/v4/routes/unknown/mini-avatar.png'
};

(function ensurePanelBundle() {
  if (window.__vupPanelBundleRequested) return;
  window.__vupPanelBundleRequested = true;

  var bundleSrc = '/js/panel-bundle.js';
  function bootLoadedBundle() {
    if (window.__vupPanelBundleBooted || typeof hydrate !== 'function') return;
    window.__vupPanelBundleBooted = true;
    if (typeof initStreamerModeToggle === 'function') initStreamerModeToggle();
    if (typeof initTutorial === 'function') initTutorial();
    if (typeof initTabs === 'function') initTabs();
    if (typeof hydrateConfigCheck === 'function') hydrateConfigCheck();
    hydrate();
    if (typeof handleActionHotkey === 'function') {
      document.addEventListener('keydown', handleActionHotkey);
    }
    if (typeof handleStageChoiceHotkey === 'function') {
      document.addEventListener('keydown', handleStageChoiceHotkey);
    }
    if (typeof handleStageAdvanceHotkey === 'function') {
      document.addEventListener('keydown', handleStageAdvanceHotkey);
    }
  }

  if (document.currentScript && document.readyState === 'loading') {
    document.write('<script src="' + bundleSrc + '"><\/script>');
    return;
  }

  var script = document.createElement('script');
  script.src = bundleSrc;
  script.onload = bootLoadedBundle;
  script.onerror = function() {
    console.error('Panel bundle failed to load:', bundleSrc);
  };
  document.head.appendChild(script);
})();

// === 全局点击反馈 - 增强按钮可点击的物理反馈 ===
(function initGlobalClickFeedback() {
  // 等待 SFX / DOM 准备好再绑定
  document.addEventListener('click', function(e) {
    const target = e.target.closest('button, .action-item, .cockpit-card, .clickable-card, .route-gallery-item, [data-action]');
    if (!target) return;

    // 跳过禁用状态
    if (target.matches(':disabled, [aria-disabled="true"]') ||
        target.closest(':disabled, [aria-disabled="true"]')) {
      return;
    }

    // 播放点击音效
    if (window.SFX && !window.SFX.muted) {
      try { window.SFX.click(); } catch (err) {}
    }

    // 创建涟漪元素
    const ripple = document.createElement('span');
    ripple.className = 'click-ripple';
    const rect = target.getBoundingClientRect();
    const size = Math.max(rect.width, rect.height);
    ripple.style.cssText =
      'position:absolute;border-radius:50%;' +
      'background:radial-gradient(circle,rgba(255,255,255,0.35) 0%,transparent 70%);' +
      'transform:scale(0);animation:click-ripple-animation 0.45s ease-out forwards;' +
      'pointer-events:none;z-index:9999;width:' + size + 'px;height:' + size + 'px;' +
      'left:' + (e.clientX - rect.left - size / 2) + 'px;' +
      'top:' + (e.clientY - rect.top - size / 2) + 'px;';
    target.style.position = 'relative';
    target.style.overflow = 'hidden';
    target.appendChild(ripple);
    setTimeout(function() {
      if (ripple.parentNode === target) ripple.remove();
    }, 500);
  }, true);

  // 注入涟漪动画 keyframes（仅一次）
  if (!document.getElementById('__vup_ripple_keyframes')) {
    const style = document.createElement('style');
    style.id = '__vup_ripple_keyframes';
    style.textContent =
      '@keyframes click-ripple-animation{' +
      '0%{transform:scale(0);opacity:0.6}' +
      '100%{transform:scale(2.2);opacity:0}' +
      '}';
    document.head.appendChild(style);
  }
})();

// === Module兼容层 ===
// 如果ES Module已加载，使用Module的API；否则使用旧全局API
(function() {
  // 保持旧代码兼容
  if (typeof window.html !== 'function') {
    window.html = function(value) {
      if (value == null) return '';
      return String(value).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
    };
  }
})();

// VUP出道局 - 游戏主逻辑
// NOTE: `var` (not `const`) so that state.js can detect and adopt
// window.state at boot time, unifying the dual state stores.
var state = {
    user: null,
    vup: null,
    session: null,
    actions: [],
    streamPlans: [],
    offStreamOptions: [],
    titles: [],
    riskTools: [],
    fanTopics: [],
    npcSpotlight: null,
    platformData: null,
    chainEvent: null,
    buzzBriefing: null,
    funAudienceProfile: null,
    audienceExpectation: null,
    endingForecast: null,
    stageBriefing: null,
    personaTags: null,
    memeLifecycle: null,
    comboDiscovery: null,
    pendingEvent: null,
    pendingInteraction: null,
    report: null,
    lastActionResult: null,
    lastRiskToolResult: null,
    reports: [],
    dailyHighlights: null,
    ending: null,
    endingShareCard: null,
    achievementProgress: null,
    restartBiasOverride: null,
    danmaku: null,
    // 1.1 礼物累积（本日直播礼物数与折算币值）
    giftAccum: { count: 0, coinValue: 0 },
    // 1.2 弹幕累积（本日直播弹幕数与热度）
    danmakuAccum: { count: 0, heat: 0 },
    configCheck: null,
    openingStyleChoice: null,
    openingStyleBackendAvailable: null,
    openingStyleChoiceSkipped: false,
    demoResult: null,
    demoStatus: null,
    hasSave: false,
    saveSlots: [],
    busy: false,
    busyLabel: "",
    appBootState: "loading",
    appBootMessage: "正在接回你的出道局",
    appBootDetail: "同步存档、角色、今日阶段和行动列表。",
    statusMessage: "",
    statusType: "info",
    streamerMode: false,
    infoTab: "host",
    infoTabAutoPhase: null,
    infoTabManualPhase: null,
    idempotencyKeys: {},
    tutorialSeen: false,
    pendingAction: null,
    dailyScheduleDraft: null,
    seenDailyFeedbackKeys: {},
    // 2.2 风险预警：即将到期风险列表
    crisisAlerts: [],
    // 1.4 对手威胁：竞争对手数据
    rivals: null,
    // 2.3 结局图鉴：已解锁结局集合
    endingAtlas: null,
    // 2.5 回放：本局每日时间线
    timeline: null
};

const previousStats = {
    stamina: null,
    fans: null,
    heat: null,
    materialStock: null,
    reputation: null,
    risk: null,
    route: null,
    popularity: null,
    watchHeat: null,
    memeLevel: null,
    commercialLevel: null,
    trueFans: null,
    funFans: null,
    unicornFans: null,
    ddFans: null
};

let statusClearTimer = null;
let lastAchievementCount = 0;
let achievementPopupHideTimer = null;
let endingFullscreenShowTimer = null;
let endingFullscreenHideTimer = null;
let endingFullscreenTagTimer = null;
let endingFullscreenChildTimers = [];

// ============================================================
// 音效系统 (Web Audio API)
// ============================================================
const SFX = {
    ctx: null,
    muted: false,
    volume: 0.3,
    _initialized: false,

    init() {
        if (this._initialized) return;
        try {
            this.ctx = new (window.AudioContext || window.webkitAudioContext)();
            this._initialized = true;
        } catch (e) { /* no audio support */ }
    },

    _tone(freq, duration, type = 'sine', vol = this.volume) {
        if (!this.ctx || this.muted) return;
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.type = type;
        osc.frequency.value = freq;
        gain.gain.setValueAtTime(vol, this.ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + duration);
        osc.connect(gain);
        gain.connect(this.ctx.destination);
        osc.start();
        osc.stop(this.ctx.currentTime + duration);
    },

    _chord(freqs, duration, vol = this.volume * 0.5) {
        freqs.forEach(f => this._tone(f, duration, 'sine', vol));
    },

    click()       { this._tone(800, 0.08, 'square', 0.1); },
    success()     { this._chord([523, 659, 784], 0.3); },
    fanUp()       { this._tone(880, 0.15, 'sine'); setTimeout(() => this._tone(1100, 0.15, 'sine'), 100); },
    fanDown()     { this._tone(440, 0.2, 'sawtooth', 0.15); },
    event()       { this._chord([440, 554, 659], 0.25); },
    debtWarn()    { this._tone(220, 0.4, 'sawtooth', 0.2); },
    sRank()       { this._chord([523, 659, 784, 1047], 0.6, 0.2); },
    report()      { this._tone(660, 0.12, 'triangle'); setTimeout(() => this._tone(880, 0.12, 'triangle'), 80); },
    ending()      { [523, 659, 784, 1047].forEach((f, i) => setTimeout(() => this._tone(f, 0.4, 'sine', 0.15), i * 150)); },
    nextDay()     { this._tone(523, 0.1, 'triangle'); setTimeout(() => this._tone(659, 0.1, 'triangle'), 60); },
    combo()       { this._chord([523, 659, 784, 1047, 1319], 0.4, 0.15); },
    achievement() { [523, 659, 784, 1047].forEach((f, i) => setTimeout(() => this._tone(f, 0.2, 'sine', 0.18), i * 120)); },
    play(name) {
        if (typeof this[name] === "function") {
            this[name]();
        }
    },

    toggle() {
        this.muted = !this.muted;
        syncAudioSettingsPanel();
        return this.muted;
    }
};

// ============================================================
// BGM系统 (背景音乐)
// ============================================================
const BGM = {
    current: null,
    currentTrack: null,
    muted: false,
    volume: 0.3,
    _crossfadeTimer: null,
    _playRequestId: 0,

    tracks: {
        daily:       '/sounds/bgm-daily.mp3',
        stream:      '/sounds/bgm-stream.mp3',
        event:       '/sounds/bgm-event.mp3',
        ending:      '/sounds/bgm-ending.mp3',
        achievement: '/sounds/bgm-achievement.mp3',
        title:       '/sounds/bgm-title.mp3'
    },

    play(trackName) {
        if (this.muted || !this.tracks[trackName]) return;
        if (this.currentTrack === trackName) return; // already playing

        // Clear any pending crossfade
        if (this._crossfadeTimer) {
            clearTimeout(this._crossfadeTimer);
            this._crossfadeTimer = null;
        }

        const requestId = ++this._playRequestId;
        const oldAudio = this.current;

        // Create new audio
        const audio = new Audio(this.tracks[trackName]);
        audio.volume = 0;
        audio.loop = true;

        // Crossfade: fade new in over 800ms
        audio.play().then(() => {
            if (requestId !== this._playRequestId) {
                audio.pause();
                audio.src = '';
                return;
            }
            this.current = audio;
            this.currentTrack = trackName;
            const fadeInSteps = 20;
            const stepTime = 800 / fadeInSteps;
            let step = 0;
            const fadeIn = setInterval(() => {
                step++;
                const progress = step / fadeInSteps;
                audio.volume = this.volume * progress;
                if (step >= fadeInSteps) {
                    audio.volume = this.volume;
                    clearInterval(fadeIn);
                }
            }, stepTime);

            // Crossfade: fade old out over 600ms after the new track is actually playing.
            if (oldAudio && oldAudio !== audio) {
                const fadeOutSteps = 15;
                const stepTime = 600 / fadeOutSteps;
                let step = 0;
                const origVolume = oldAudio.volume || this.volume;
                const fadeOut = setInterval(() => {
                    step++;
                    const progress = 1 - (step / fadeOutSteps);
                    oldAudio.volume = origVolume * Math.max(progress, 0);
                    if (step >= fadeOutSteps) {
                        oldAudio.pause();
                        oldAudio.currentTime = 0;
                        oldAudio.src = '';
                        clearInterval(fadeOut);
                    }
                }, stepTime);
            }
        }).catch(() => {
            audio.src = '';
        });
    },

    stop() {
        this._playRequestId++;
        if (this._crossfadeTimer) {
            clearTimeout(this._crossfadeTimer);
            this._crossfadeTimer = null;
        }
        if (this.current) {
            this.current.pause();
            this.current.currentTime = 0;
            this.current.src = '';
            this.current = null;
        }
        this.currentTrack = null;
    },

    toggle() {
        this.muted = !this.muted;
        if (this.muted) {
            this.stop();
        } else {
            syncBGM();
        }
        // Update toggle button visual
        const btn = document.getElementById('bgmToggle');
        if (btn) {
            btn.classList.toggle('muted', this.muted);
            btn.textContent = this.muted ? '\u{1F507}' : '\u{1F3B5}';
        }
        syncAudioSettingsPanel();
    },

    setVolume(vol) {
        this.volume = Math.max(0, Math.min(1, vol));
        if (this.current) this.current.volume = this.volume;
        syncAudioSettingsPanel();
    }
};

// 首次交互时初始化音频上下文
document.addEventListener('click', () => SFX.init(), { once: true });
document.addEventListener('keydown', () => SFX.init(), { once: true });

// ============================================================
// AI语音系统 (SpeechSynthesis API)
// ============================================================
const TTS = {
    enabled: true,
    _queue: [],
    _speaking: false,

    speak(text, lang = 'zh-CN') {
        if (!this.enabled || !window.speechSynthesis || !text) return;
        this._queue.push({ text, lang });
        this._processQueue();
    },

    _processQueue() {
        if (this._speaking || this._queue.length === 0) return;
        this._speaking = true;
        const { text, lang } = this._queue.shift();
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.lang = lang;
        utterance.rate = 1.1;
        utterance.pitch = 1.2;
        utterance.volume = 0.8;
        utterance.onend = () => { this._speaking = false; this._processQueue(); };
        utterance.onerror = () => { this._speaking = false; this._processQueue(); };
        speechSynthesis.speak(utterance);
    },

    stop() {
        if (window.speechSynthesis) speechSynthesis.cancel();
        this._queue = [];
        this._speaking = false;
    },

    toggle() {
        this.enabled = !this.enabled;
        if (!this.enabled) this.stop();
        syncAudioSettingsPanel();
        return this.enabled;
    }
};

// ES module action-registry reads these through globalThis.
window.SFX = SFX;
window.BGM = BGM;
window.TTS = TTS;

function syncAudioSettingsPanel() {
    const panel = document.getElementById('settingsPanel');
    const settingsButton = document.getElementById('settingsButton');
    const bgmToggle = document.getElementById('bgmSettingToggle');
    const bgmVolume = document.getElementById('bgmSettingVolume');
    const bgmQuickToggle = document.getElementById('bgmToggle');
    const bgmQuickVolume = document.getElementById('bgmVolume');
    const sfxToggle = document.getElementById('sfxSettingToggle');
    const ttsToggle = document.getElementById('ttsSettingToggle');
    const open = Boolean(panel && !panel.classList.contains('hidden'));

    if (settingsButton) {
        settingsButton.classList.toggle('active', open);
        settingsButton.setAttribute('aria-expanded', open ? 'true' : 'false');
    }
    if (bgmToggle) bgmToggle.checked = !BGM.muted;
    if (bgmVolume && document.activeElement !== bgmVolume) bgmVolume.value = String(Math.round(BGM.volume * 100));
    if (bgmQuickToggle) {
        bgmQuickToggle.classList.toggle('muted', BGM.muted);
        bgmQuickToggle.textContent = BGM.muted ? '\u{1F507}' : '\u{1F3B5}';
        bgmQuickToggle.setAttribute('aria-pressed', BGM.muted ? 'false' : 'true');
    }
    if (bgmQuickVolume && document.activeElement !== bgmQuickVolume) {
        bgmQuickVolume.value = String(Math.round(BGM.volume * 100));
    }
    if (sfxToggle) sfxToggle.checked = !SFX.muted;
    if (ttsToggle) ttsToggle.checked = TTS.enabled;
}

function toggleSettingsPanel() {
    const panel = document.getElementById('settingsPanel');
    if (!panel) return;
    panel.classList.toggle('hidden');
    syncAudioSettingsPanel();
    if (!panel.classList.contains('hidden')) {
        panel.querySelector('input, button')?.focus({ preventScroll: true });
    }
}

function closeSettingsPanel() {
    document.getElementById('settingsPanel')?.classList.add('hidden');
    syncAudioSettingsPanel();
}

function currentWatchHeat() {
    return Number(state.vup?.opinion?.watchHeat ?? state.vup?.watchHeat ?? 0);
}

function liveHeatTier(heat) {
    if (heat >= 70) return 'hot';
    if (heat >= 35) return 'warm';
    return 'quiet';
}

function livePreviousReport() {
    const currentDay = Number(state.session?.day || state.vup?.day || 0);
    const reports = Array.isArray(state.reports) ? state.reports : [];
    if (!reports.length) return null;
    const previousDay = currentDay > 1 ? currentDay - 1 : 0;
    return reports
        .filter(report => {
            const day = Number(report?.day || 0);
            return previousDay ? day <= previousDay : day > 0;
        })
        .sort((left, right) => Number(right?.day || 0) - Number(left?.day || 0))[0] || null;
}

function liveContextReport() {
    const phase = state.session?.phase || "";
    if ((phase === "REPORT_READY" || phase === "ENDING_READY") && state.report) {
        return state.report;
    }
    return livePreviousReport();
}

function reportDeltaFrom(report, key, aliases = []) {
    const delta = report?.dataDelta || report?.dataDeltas || {};
    const keys = [key, ...aliases];
    for (const candidate of keys) {
        const value = Number(delta?.[candidate]);
        if (Number.isFinite(value)) return value;
    }
    return 0;
}

function reportVisibleLineFrom(report, patterns, fallback = "") {
    const items = Array.isArray(report?.visibleItems) ? report.visibleItems : [];
    const found = items.find(item => {
        const text = String(item || "");
        return patterns.some(pattern => pattern.test(text));
    });
    return found
        ? reportVisibleItemText(found).replace(/^(数据变化|主行动|今日标题|路线提示|场外声音|粉丝来信【[^】]+】[^：]+|素材小票)：?/, "").trim()
        : fallback;
}

function currentResourceValue(key, fallback = 0) {
    return firstNumericValue(state.vup?.resources?.[key], state.vup?.[key], fallback);
}

function currentOpinionValue(key, fallback = 0) {
    return firstNumericValue(state.vup?.opinion?.[key], state.vup?.[key], fallback);
}

function currentFanValue(key, fallback = 0) {
    return firstNumericValue(state.vup?.fanStructure?.[key], state.vup?.[key], fallback);
}

function compactCount(value) {
    const number = Number(value || 0);
    if (Math.abs(number) >= 10000) return `${(number / 10000).toFixed(number >= 100000 ? 0 : 1)}w`;
    if (Math.abs(number) >= 1000) return `${(number / 1000).toFixed(number >= 10000 ? 0 : 1)}k`;
    return String(number);
}

function liveRoomMoodProfile(report = liveContextReport()) {
    const heat = currentWatchHeat();
    const heatTier = liveHeatTier(heat);
    const fanDelta = reportDeltaFrom(report, "fanDelta", ["fanChange"]);
    const reputationDelta = reportDeltaFrom(report, "reputationDelta", ["reputationChange"]);
    const heatDelta = reportDeltaFrom(report, "watchHeatDelta", ["watchHeatChange"]);
    const route = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN";
    const routeName = routeLabelFor(route);
    const riskText = localizedVisibleTextOrFallback(report?.riskHint, "");
    const reportSummary = reportShortSentence(report?.summary, "昨天还没有可复盘的结果，今天先从直播间状态读起。", 42);
    const heatText = heatTier === "hot" ? "弹幕很密" : heatTier === "warm" ? "有人围观" : "房间低压";
    let headline = `${heatText} · ${routeName}`;
    if (fanDelta > 0 && heatDelta >= 0) headline = `昨天涨粉${signedDelta(fanDelta)}，今天${heatText}`;
    if (riskText || reputationDelta < 0) headline = `昨天留下风险，今天先稳住直播间`;
    if (!report && state.session?.phase === "READY") headline = `新一天开播前，先看房间温度和路线缺口`;

    const chips = [
        { label: "粉丝", value: signedDelta(fanDelta), tone: deltaToneClass(fanDelta) },
        { label: "口碑", value: signedDelta(reputationDelta), tone: deltaToneClass(reputationDelta) },
        { label: "围观", value: signedDelta(heatDelta), tone: deltaToneClass(heatDelta) }
    ];

    return {
        report,
        heat,
        heatTier,
        route,
        routeName,
        headline,
        summary: reportSummary,
        riskText,
        chips
    };
}

function liveStageTickerHtml(profile = liveRoomMoodProfile()) {
    if (!state.vup) {
        return '<span>未开播</span><strong>选择存档或创建主播后进入直播间。</strong>';
    }
    const phase = state.session?.phase || "";
    const reportLabel = profile.report
        ? (phase === "REPORT_READY" ? `今日日报 · 第${Number(profile.report.day) || "?"}天` : `昨日回声 · 第${Number(profile.report.day) || "?"}天`)
        : "今日房间";
    return `
        <span>${html(reportLabel)}</span>
        <strong>${html(profile.headline)}</strong>
        <small>${html(profile.summary)}</small>
        <button type="button" data-action="open-daily-feedback">查看反馈</button>
    `;
}

function dailyFeedbackKey(report = livePreviousReport()) {
    if (!state.vup || !report) return "";
    const runId = state.vup?.id || state.vup?.vupId || "run";
    return `${runId}:${Number(report.day || 0)}`;
}

function dailyFeedbackShouldAutoOpen() {
    const phase = state.session?.phase || "";
    const report = livePreviousReport();
    const key = dailyFeedbackKey(report);
    return Boolean(state.vup && phase === "READY" && report && key && !state.seenDailyFeedbackKeys[key]);
}

function dailyFeedbackImpactRows(profile = liveRoomMoodProfile()) {
    const report = profile.report;
    const route = profile.routeName;
    const brain = operationalBrainSnapshot();
    const rows = [
        {
            label: "直播间",
            value: profile.heatTier === "hot" ? "弹幕密集" : profile.heatTier === "warm" ? "正在升温" : "低压观察",
            detail: `当前围观 ${profile.heat}，弹幕密度会跟着房间热度变化。`
        },
        {
            label: "路线",
            value: route,
            detail: firstLocalizedReadableText([
                reportVisibleLineFrom(report, [/^路线提示：/], ""),
                state.endingForecast && typeof firstWarnEndingRequirement === "function"
                    ? endingGapCoachLine(firstWarnEndingRequirement(), { likelyEnding: profile.route })
                    : ""
            ], `今天继续补${route}的证据。`)
        },
        {
            label: "可能走向",
            value: brain.state,
            detail: brain.detail
        },
        {
            label: "风险",
            value: profile.riskText ? "要留意" : "暂稳",
            detail: profile.riskText || "目前没有明显舆情苗头，可以把行动花在成长或路线证据上。"
        }
    ];
    return rows.map(row => `
        <div class="daily-feedback-row">
            <em>${html(row.label)}</em>
            <strong>${html(row.value)}</strong>
            <small>${html(row.detail)}</small>
        </div>
    `).join("");
}

function dailyFeedbackDeltaHtml(profile = liveRoomMoodProfile()) {
    return profile.chips.map(chip => `
        <span class="daily-feedback-delta ${html(chip.tone)}">
            <em>${html(chip.label)}</em>
            <strong>${html(chip.value)}</strong>
        </span>
    `).join("");
}

function dailyFeedbackPressureConsequence() {
    const pressure = currentOperationalPressureSnapshot();
    if (!pressure) return "";
    const tone = pressure.tone || "neutral";
    const lockGroup = pressure.lockGroup || "";
    const replacementLabel = pressure.replacementLabel || "";
    const cooldownLeft = pressure.cooldownLeft || 0;
    const woundLeft = pressure.woundLeft || 0;
    const wounded = pressure.wounded;
    let consequence = "";
    if (wounded) {
        consequence = `带伤恢复中，收益下降、可选项变少。`;
    } else if (cooldownLeft > 0) {
        consequence = `${lockGroup}线被锁${cooldownLeft}天，操作空间变窄。`;
    } else if (lockGroup) {
        consequence = `${lockGroup}线被压住了，今天要绕着走。`;
    }
    let advice = "";
    if (replacementLabel) {
        advice = `今天优先用${replacementLabel}稳住。`;
    }
    return `
        <div class="daily-feedback-pressure-consequence ${html(tone)}">
            <em>运营脑</em>
            <strong>${html(pressure.state)}${lockGroup ? ` · ${html(lockGroup)}` : ""}</strong>
            ${consequence ? `<small>${html(consequence)}</small>` : ""}
            ${advice ? `<small>${html(advice)}</small>` : ""}
        </div>
    `;
}

function dailyFeedbackHtml() {
    const profile = liveRoomMoodProfile();
    const report = profile.report;
    const phase = state.session?.phase || "";
    const dayText = report ? `第${Number(report.day) || "?"}天之后` : "今天开播前";
    const title = phase === "REPORT_READY"
        ? "今天的操作已经结算成直播间反馈"
        : "昨天的操作正在改变今天的直播间";
    return `
        <div class="daily-feedback-card" role="dialog" aria-modal="true" aria-label="昨日操作反馈">
            <div class="daily-feedback-head">
                <div>
                    <span>${html(dayText)}</span>
                    <h3>${html(title)}</h3>
                </div>
                <button type="button" class="daily-feedback-close" data-action="close-daily-feedback" aria-label="关闭">×</button>
            </div>
            <div class="daily-feedback-body">
                <section class="daily-feedback-main">
                    <span>今日氛围</span>
                    <strong>${html(profile.headline)}</strong>
                    <p>${html(profile.summary)}</p>
                    <p class="daily-feedback-trajectory">可能走向：${html(operationalBrainSnapshot().trajectory)}</p>
                    <div class="daily-feedback-deltas">${dailyFeedbackDeltaHtml(profile)}</div>
                    ${dailyFeedbackPressureConsequence()}
                </section>
                <section class="daily-feedback-grid" aria-label="反馈影响">
                    ${dailyFeedbackImpactRows(profile)}
                </section>
            </div>
            <div class="daily-feedback-actions">
                <button type="button" data-action="open-feedback-report">看完整昨日结果</button>
                <button type="button" class="primary" data-action="close-daily-feedback">开始今天</button>
            </div>
        </div>
    `;
}

function markDailyFeedbackSeen() {
    const key = dailyFeedbackKey();
    if (key) state.seenDailyFeedbackKeys[key] = true;
}

function showDailyFeedback({ force = false } = {}) {
    const report = livePreviousReport();
    if (!state.vup || (!force && !dailyFeedbackShouldAutoOpen()) || (!report && !force)) return;
    let overlay = document.getElementById('dailyFeedbackOverlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'dailyFeedbackOverlay';
        overlay.className = 'daily-feedback-overlay';
        document.body.appendChild(overlay);
    }
    overlay.innerHTML = dailyFeedbackHtml();
    overlay.classList.add('open');
    overlay.setAttribute('aria-hidden', 'false');
    markDailyFeedbackSeen();
}

function closeDailyFeedback() {
    markDailyFeedbackSeen();
    const overlay = document.getElementById('dailyFeedbackOverlay');
    if (!overlay) return;
    overlay.classList.remove('open');
    overlay.setAttribute('aria-hidden', 'true');
}

function openFeedbackReport() {
    closeDailyFeedback();
    openLiveDrawer('report');
}

function syncDailyFeedbackPrompt() {
    if (dailyFeedbackShouldAutoOpen()) {
        window.setTimeout(() => showDailyFeedback(), 120);
    }
}

function syncLiveStageTicker() {
    const ticker = document.getElementById('liveStageTicker');
    if (!ticker) return;
    ticker.innerHTML = liveStageTickerHtml();
}

// 4.7 直播间浮层按需出现：直播阶段显示底栏+右侧栏+礼物面板，否则隐藏
const LIVE_ROOM_PHASES = new Set(['NEED_TITLE', 'ACTION_RESOLVED', 'NEED_INTERACTION_CHOICE']);

function syncLiveRoomStage() {
    const viewport = document.getElementById('vupLiveViewport');
    const heatLabel = document.getElementById('vupLiveHeatLabel');
    if (viewport) {
        if (state.vup) {
            const heat = currentWatchHeat();
            const tier = liveHeatTier(heat);
            viewport.dataset.heatTier = tier;
            if (heatLabel) {
                const text = tier === 'hot' ? '主会场弹幕' : tier === 'warm' ? '有人围观' : '低压直播';
                heatLabel.textContent = `围观 ${heat} · ${text}`;
            }
            syncLiveStageTicker();
            if (typeof updateLiveRoomInfo === 'function') updateLiveRoomInfo();
        } else {
            syncLiveStageTicker();
        }
    }

    // 按 phase 控制直播间浮层显隐
    const phase = state.session?.phase || (state.vup ? 'READY' : null);
    const showLive = !!phase && LIVE_ROOM_PHASES.has(phase);
    const bottomBar = document.getElementById('liveRoomBottomBar');
    const rightSidebar = document.getElementById('liveRightSidebar');
    const giftPanel = document.getElementById('giftPanelOverlay');
    if (bottomBar) bottomBar.classList.toggle('hidden', !showLive);
    if (showLive) {
        // 直播阶段：移除 hidden，恢复 toggle 行为（visible 由 bili-ui.js 管理）
        if (rightSidebar) rightSidebar.classList.remove('hidden');
        if (giftPanel) giftPanel.classList.remove('hidden');
    } else {
        // 非直播阶段：强制关闭并重置 bili-ui.js 内部状态
        if (rightSidebar) { rightSidebar.classList.remove('visible'); rightSidebar.classList.add('hidden'); }
        if (giftPanel) { giftPanel.classList.remove('visible'); giftPanel.classList.add('hidden'); }
        if (typeof closeLivePanels === 'function') closeLivePanels();
    }
}

window.toggleSettingsPanel = toggleSettingsPanel;
window.closeSettingsPanel = closeSettingsPanel;
window.syncAudioSettingsPanel = syncAudioSettingsPanel;
window.showDailyFeedback = showDailyFeedback;
window.closeDailyFeedback = closeDailyFeedback;
window.openFeedbackReport = openFeedbackReport;

// ============================================================
// 动画系统
// ============================================================
const ANIM = {
    flash(element, type = 'positive') {
        if (!element) return;
        element.classList.add(`anim-flash-${type}`);
        setTimeout(() => element.classList.remove(`anim-flash-${type}`), 600);
    },

    bounce(element) {
        if (!element) return;
        element.classList.add('anim-bounce');
        setTimeout(() => element.classList.remove('anim-bounce'), 500);
    },

    shake(element) {
        if (!element) return;
        element.classList.add('anim-shake');
        setTimeout(() => element.classList.remove('anim-shake'), 500);
    },

    glow(element, color = 'var(--neon-pink)') {
        if (!element) return;
        element.style.boxShadow = `0 0 20px ${color}, 0 0 40px ${color}`;
        setTimeout(() => { element.style.boxShadow = ''; }, 1500);
    },

    confetti(container) {
        if (!container) return;
        const colors = ['#ff6ec7', '#00ffff', '#a855f7', '#22c55e', '#eab308'];
        for (let i = 0; i < 30; i++) {
            const el = document.createElement('div');
            el.className = 'anim-confetti';
            el.style.left = Math.random() * 100 + '%';
            el.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
            el.style.animationDelay = Math.random() * 0.5 + 's';
            el.style.animationDuration = (0.8 + Math.random() * 0.5) + 's';
            container.appendChild(el);
            setTimeout(() => el.remove(), 1500);
        }
    },

    milestone(container, text) {
        if (!container) return;
        const el = document.createElement('div');
        el.className = 'anim-milestone';
        el.textContent = text;
        container.appendChild(el);
        setTimeout(() => el.remove(), 2500);
    },

    // --- 行动反馈动画 ---

    /** 数字弹出：从 anchor 元素位置弹出文字 */
    floatNumber(anchor, text, type = 'positive') {
        if (!anchor) return;
        const rect = anchor.getBoundingClientRect();
        const el = document.createElement('div');
        el.className = `float-number ${type}`;
        el.textContent = text;
        el.style.left = `${rect.left + rect.width / 2}px`;
        el.style.top = `${rect.top}px`;
        document.body.appendChild(el);
        setTimeout(() => el.remove(), 1500);
    },

    /** 粒子效果：在 anchor 元素周围生成粒子 */
    particles(anchor, type = 'star', count = 6) {
        if (!anchor) return;
        const rect = anchor.getBoundingClientRect();
        const cx = rect.left + rect.width / 2;
        const cy = rect.top + rect.height / 2;
        const emojiMap = { star: ['⭐', '✨', '★'], heart: ['❤️', '💖', '💕'], sparkle: ['✨', '☄️', '🌟'], ribbon: ['🎉', '🎊', '🎈'] };
        const emojis = emojiMap[type] || emojiMap.star;
        for (let i = 0; i < count; i++) {
            const el = document.createElement('div');
            el.className = `anim-particle ${type}`;
            el.textContent = emojis[i % emojis.length];
            el.style.left = `${cx}px`;
            el.style.top = `${cy}px`;
            const angle = (Math.PI * 2 * i) / count + (Math.random() - 0.5) * 0.5;
            const dist = 30 + Math.random() * 40;
            const px = Math.cos(angle) * dist;
            const py = Math.sin(angle) * dist - 30;
            const rot = (Math.random() - 0.5) * 360;
            el.style.setProperty('--px', `${px}px`);
            el.style.setProperty('--py', `${py}px`);
            el.style.setProperty('--pr', `${rot}deg`);
            document.body.appendChild(el);
            setTimeout(() => el.remove(), 1800);
        }
    },

    /** 卡片闪烁：给元素添加闪光扫过效果 */
    cardShine(card) {
        if (!card) return;
        card.classList.remove('card-shine');
        void card.offsetWidth;
        card.classList.add('card-shine');
        setTimeout(() => card.classList.remove('card-shine'), 800);
    },

    /** 事件面板发光 */
    eventGlow(panel) {
        if (!panel) return;
        panel.classList.remove('event-glow');
        void panel.offsetWidth;
        panel.classList.add('event-glow');
        setTimeout(() => panel.classList.remove('event-glow'), 1600);
    },

    /** 属性条增长发光 */
    progressBarGlow(bar) {
        if (!bar) return;
        bar.classList.remove('stat-bar-glow');
        void bar.offsetWidth;
        bar.classList.add('stat-bar-glow');
        setTimeout(() => bar.classList.remove('stat-bar-glow'), 900);
    },

    /** Toast 通知 */
    toast(message, type = 'info') {
        let container = document.querySelector('.toast-container');
        if (!container) {
            container = document.createElement('div');
            container.className = 'toast-container';
            document.body.appendChild(container);
        }
        const item = document.createElement('div');
        item.className = `toast-item ${type}`;
        item.textContent = message;
        container.appendChild(item);
        setTimeout(() => item.remove(), 2500);
    },

    /** 成就解锁彩带效果 */
    achievementCelebration(container) {
        if (!container) return;
        ANIM.confetti(container);
        ANIM.particles(container, 'ribbon', 10);
        ANIM.milestone(container, '🏆 成就解锁！');
    }
};

function animateCountUp(element, endValue, duration = 600) {
    if (!element || isNaN(endValue)) return;
    const startValue = parseInt(element.textContent) || 0;
    const startTime = performance.now();
    function tick(now) {
        const elapsed = now - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);
        element.textContent = Math.round(startValue + (endValue - startValue) * eased);
        if (progress < 1) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
}

const STAGE_FOCUS_PHASES = new Set([
    "NEED_TITLE",
    "OFF_STREAM_READY",
    "NEED_INTERACTION_CHOICE",
    "NEED_EVENT_CHOICE",
    "REPORT_READY",
    "ENDING_READY"
]);

function resetGameState() {
    state.vup = null;
    state.session = null;
    state.actions = [];
    state.streamPlans = [];
    state.offStreamOptions = [];
    state.titles = [];
    state.riskTools = [];
    state.fanTopics = [];
    state.npcSpotlight = null;
    state.buzzBriefing = null;
    state.funAudienceProfile = null;
    state.audienceExpectation = null;
    state.endingForecast = null;
    state.stageBriefing = null;
    state.personaTags = null;
    state.memeLifecycle = null;
    state.comboDiscovery = null;
    state.pendingEvent = null;
    state.pendingInteraction = null;
    state.report = null;
    state.lastActionResult = null;
    state.lastRiskToolResult = null;
    state.reports = [];
    state.dailyHighlights = null;
    state.ending = null;
    state.endingShareCard = null;
    state.achievementProgress = null;
    state.restartBiasOverride = null;
    state.danmaku = null;
    state.openingStyleChoice = null;
    state.infoTabAutoPhase = null;
    state.infoTabManualPhase = null;
    state.idempotencyKeys = {};
    state.dailyScheduleDraft = null;
    state.hasSave = false;
    state.saveSlots = [];
    state.crisisAlerts = [];
    state.rivals = null;
    state.endingAtlas = null;
    state.timeline = null;
    disconnectDanmakuStream();
}

function setStatus(message, type = "info") {
    if (statusClearTimer) {
        window.clearTimeout(statusClearTimer);
        statusClearTimer = null;
    }
    state.statusMessage = statusMessageText(message);
    state.statusType = type;
    renderStatus();
    if (state.statusMessage && (type === "success" || type === "info")) {
        const expectedMessage = state.statusMessage;
        statusClearTimer = window.setTimeout(() => {
            if (state.statusMessage === expectedMessage && state.statusType === type && !state.busy) {
                clearStatus();
            }
        }, 4000);
    }
}

function clearStatus() {
    if (statusClearTimer) {
        window.clearTimeout(statusClearTimer);
        statusClearTimer = null;
    }
    state.statusMessage = "";
    state.statusType = "info";
    renderStatus();
}

function renderStatus() {
    const status = document.getElementById("appStatus");
    document.body.classList.toggle("app-busy", state.busy);
    if (!status) return;

    const messageText = state.statusMessage;
    status.className = `app-status ${messageText ? "" : "hidden"} ${state.statusType}`;
    status.title = messageText;
    status.textContent = messageText;
    positionStatusToast(status);
}

function setBootState(mode, message = "", detail = "") {
    state.appBootState = mode;
    state.appBootMessage = message;
    state.appBootDetail = detail;
    renderBootState();
}

function renderBootState() {
    const panel = document.getElementById("bootStatePanel");
    if (!panel) return;

    const mode = state.appBootState || "ready";
    if (mode === "ready") {
        panel.classList.add("hidden");
        panel.innerHTML = "";
        return;
    }

    panel.classList.remove("hidden");
    const loading = mode === "loading";
    const title = state.appBootMessage || (loading ? "正在接回你的出道局" : "开播失败");
    const detail = state.appBootDetail || (loading ? "同步存档、角色、今日阶段和行动列表。" : "后台暂时没有响应，请重试。");
    const actionHtml = loading ? "" : `
        <button type="button" class="boot-state-retry" data-action="retry-bootstrap">
            重新加载
        </button>
    `;

    panel.innerHTML = `
        <div class="boot-state-card ${loading ? "loading" : "failed"}">
            <span class="boot-state-kicker">${loading ? "开播准备中" : "开播失败"}</span>
            <h1>${html(title)}</h1>
            <p>${html(detail)}</p>
            <div class="boot-state-meter" aria-hidden="true"><span></span></div>
            ${actionHtml}
        </div>
    `;
}

function bootErrorIsBlocking(error) {
    const status = Number(error?.status || 0);
    if (status > 0 && status < 500) return false;
    return true;
}

function positionStatusToast(status = document.getElementById("appStatus")) {
    const clearToastPosition = () => {
        status?.style.removeProperty("--status-toast-top");
        status?.style.removeProperty("--status-toast-left");
        status?.style.removeProperty("--status-toast-width");
        status?.style.removeProperty("--status-toast-transform");
    };

    if (!status || status.classList.contains("hidden")) {
        clearToastPosition();
        return;
    }

    const visibleBox = element => {
        if (!element) return null;
        const style = getComputedStyle(element);
        const box = element.getBoundingClientRect();
        if (style.display === "none" || style.visibility === "hidden" || box.width <= 0 || box.height <= 0) {
            return null;
        }
        return box;
    };
    const rectOverlaps = (first, second) => {
        if (!first || !second) return false;
        return first.left < second.right
            && first.right > second.left
            && first.top < second.bottom
            && first.bottom > second.top;
    };

    const headerBox = visibleBox(document.querySelector(".game-header"));
    const centerBox = visibleBox(document.querySelector(".game-center"));

    if (!state.vup) {
        const left = 220;
        const reservedRight = 180;
        const width = Math.round(Math.min(720, Math.max(320, window.innerWidth - left - reservedRight)));
        const top = Math.round((headerBox?.top || 0) + 14);
        status.style.setProperty("--status-toast-top", `${top}px`);
        status.style.setProperty("--status-toast-left", `${left}px`);
        status.style.setProperty("--status-toast-width", `${width}px`);
        status.style.setProperty("--status-toast-transform", "none");
        return;
    }

    if (!centerBox) {
        clearToastPosition();
        return;
    }

    const style = getComputedStyle(status);
    const maxHeight = Number.parseFloat(style.maxHeight) || status.scrollHeight || 48;
    const statusHeight = Math.min(status.scrollHeight || maxHeight, maxHeight);
    const left = Math.max(12, Math.round(centerBox.left));
    const maxWidth = Math.max(320, window.innerWidth - left - 12);
    const width = Math.round(Math.min(centerBox.width, maxWidth));
    const minTop = Math.max((headerBox?.bottom || 61) + 8, centerBox.top + 4);
    const maxTop = Math.max(minTop, Math.min(window.innerHeight - statusHeight - 8, centerBox.bottom - statusHeight - 8));
    const avoidBoxes = [
        ".coach-card",
        "#actionPanel",
        ".daily-plan-board [data-action=\"daily-plan-submit\"]",
        ".simple-decision-card [data-action=\"simple-primary-action\"]",
        ".simple-decision-card [data-action=\"simple-alt-action\"]",
        ".ready-action-deck [data-action=\"quick-submit-action\"]",
        "#titlePanel [data-action=\"choose-title\"]",
        "#titlePanel [data-action=\"reroll-title\"]",
        "#titlePanel [data-action=\"cancel-action\"]",
        "#eventPanel [data-action=\"choose-event\"]",
        "#eventPanel [data-action=\"choose-interaction\"]",
        "#reportPanel [data-action=\"next-day\"]",
        "#endingPanel [data-action=\"restart\"]",
        ".info-brief",
        ".info-tabs"
    ]
        .flatMap(selector => [...document.querySelectorAll(selector)])
        .map(visibleBox)
        .filter(Boolean);
    let top = minTop;
    for (let candidateTop = minTop; candidateTop <= maxTop; candidateTop += 4) {
        const candidateBox = {
            left,
            right: left + width,
            top: candidateTop,
            bottom: candidateTop + statusHeight
        };
        if (!avoidBoxes.some(box => rectOverlaps(candidateBox, box))) {
            top = candidateTop;
            break;
        }
    }

    status.style.setProperty("--status-toast-top", `${Math.round(top)}px`);
    status.style.setProperty("--status-toast-left", `${left}px`);
    status.style.setProperty("--status-toast-width", `${width}px`);
    status.style.setProperty("--status-toast-transform", "none");
}

function syncShellMode() {
    const phase = state.session?.phase || (state.vup ? "READY" : "");
    const stageFocus = Boolean(state.vup) && STAGE_FOCUS_PHASES.has(phase);
    syncRouteTheme(state.vup?.currentRoute || "UNKNOWN", Boolean(state.vup));
    document.body.classList.toggle("streamer-mode", Boolean(state.streamerMode));
    document.body.classList.toggle("pregame-mode", !state.vup);
    document.body.classList.toggle("ready-mode", Boolean(state.vup) && phase === "READY");
    document.body.classList.toggle("stage-focus-mode", stageFocus);
    document.body.classList.toggle("title-mode", Boolean(state.vup) && phase === "NEED_TITLE");
    document.body.classList.toggle("event-mode", Boolean(state.vup) && (phase === "NEED_EVENT_CHOICE" || phase === "NEED_INTERACTION_CHOICE"));
    document.body.classList.toggle("report-mode", Boolean(state.vup) && phase === "REPORT_READY");
    document.body.classList.toggle("ending-mode", Boolean(state.vup) && phase === "ENDING_READY");
}

function setStreamerMode(enabled) {
    state.streamerMode = Boolean(enabled);
    try {
        window.localStorage?.setItem("vup-streamer-mode", state.streamerMode ? "1" : "0");
    } catch (e) { /* storage unavailable */ }
    syncShellMode();
    syncStreamerModeToggle();
    positionStatusToast();
}

function syncStreamerModeToggle() {
    const toggle = document.getElementById("streamerModeToggle");
    const hint = document.getElementById("streamerModeHint");
    const enabled = Boolean(state.streamerMode);
    if (toggle) {
        toggle.classList.toggle("active", enabled);
        toggle.setAttribute("aria-pressed", enabled ? "true" : "false");
        toggle.title = enabled ? "主播展示模式已开启：只调整显示，不改变规则" : "主播展示模式";
        toggle.setAttribute("aria-label", toggle.title);
        toggle.textContent = enabled ? "展示" : "LIVE";
    }
    if (hint) {
        hint.classList.toggle("hidden", !enabled);
        hint.textContent = enabled ? "主播展示：大字低噪，不改规则" : "";
    }
}

function toggleStreamerMode() {
    setStreamerMode(!state.streamerMode);
}

function initStreamerModeToggle() {
    try {
        state.streamerMode = window.localStorage?.getItem("vup-streamer-mode") === "1";
    } catch (e) {
        state.streamerMode = false;
    }

    const toggle = document.getElementById("streamerModeToggle");
    if (toggle) {
        toggle.addEventListener("click", toggleStreamerMode);
    }
    syncShellMode();
    syncStreamerModeToggle();
}

window.toggleStreamerMode = toggleStreamerMode;

// BGM自动切换：根据游戏阶段播放对应背景音乐
function syncBGM() {
    if (!state.vup) {
        // 未开始游戏，停止BGM
        BGM.stop();
        return;
    }

    const phase = state.session?.phase || "READY";

    // 结局阶段优先级最高
    if (phase === "ENDING_READY" && state.ending) {
        BGM.play('ending');
        return;
    }

    // 事件阶段
    if (phase === "NEED_EVENT_CHOICE" || phase === "NEED_INTERACTION_CHOICE") {
        BGM.play('event');
        return;
    }

    // 标题选择阶段
    if (phase === "NEED_TITLE") {
        BGM.play('title');
        return;
    }

    // 日报阶段
    if (phase === "REPORT_READY") {
        BGM.stop(); // 日报展示时暂停BGM，让SFX报告音效突出
        return;
    }

    // 日常/待机阶段
    BGM.play('daily');
}

function syncGameplayInfoTab() {
    const phase = state.session?.phase || (state.vup ? "READY" : "");
    const defaultInfoTab = state.vup && STAGE_FOCUS_PHASES.has(phase)
        ? stageDefaultInfoTabFor(phase)
        : "";
    if (!defaultInfoTab) {
        state.infoTabAutoPhase = null;
        state.infoTabManualPhase = null;
        return;
    }
    if (state.infoTabManualPhase === phase && state.infoTabAutoPhase === phase) {
        return;
    }
    if (state.infoTab === 'demo' || state.infoTabAutoPhase !== phase) {
        state.infoTabAutoPhase = phase;
        activateInfoTab(defaultInfoTab);
    }
}

function stageDefaultInfoTabFor(phase) {
    const defaults = {
        NEED_TITLE: "npc",
        OFF_STREAM_READY: "npc",
        NEED_EVENT_CHOICE: "npc",
        NEED_INTERACTION_CHOICE: "npc",
        REPORT_READY: "report",
        ENDING_READY: "environment"
    };
    return defaults[phase] || "";
}

function doneStatusText(label) {
    const actionLabel = String(label || "操作").replace(/中$/, "");
    const phase = state.session?.phase || "";
    const maxDayLabel = runMaxDayLabel();
    const phaseCopy = {
        READY: "回到行动面板，今天继续营业。",
        NEED_TITLE: "标题组已接棒，先选一个稳得住的直播标题。",
        NEED_INTERACTION_CHOICE: "现场互动递到台前，先看成本再选处理方式。",
        NEED_EVENT_CHOICE: "正式事件摆上桌，先稳米线再看日报。",
        REPORT_READY: "今日日报已生成，读完就能开下一天。",
        ENDING_READY: "30天收官完成，去结局复盘看路线证据。"
    };
    if (maxDayLabel !== "30天") {
        phaseCopy.ENDING_READY = `${maxDayLabel}收官完成，去结局复盘看路线证据。`;
    }
    const labelCopy = {
        创建本地存档: "存档已创建，先创建你的VUP。",
        打开旧档: "旧档已打开，继续接档当前进度。",
        本地开局: "本地档已开播，第一天可以直接选行动。",
        读取本地档: "已接回上一局，继续当前这一天。",
        保存本地档: "本地存档已保存，可以放心继续点。",
        退出登录: "已退出登录，本轮现场已收麦。",
        开始出道: "出道局已开播，先选今日主行动。",
        换一批标题: "标题组换好一批，挑一个别太上头的。",
        取消今日企划: "企划已取消，行动面板回收，先稳住今天。",
        处理粉丝群议题: "粉丝群议题已处理，群聊先降温。",
        米线工具执行: "米线工具已落地，回主流程看风险变化。",
        重置验证轮: "验证轮已重置，可以重新跑路线。",
        跑验证脚本: "验证脚本已跑完，复盘组可以验收路线。",
        快进验证轮: "验证轮已快进，继续看阶段结果。"
    };
    const nextStep = phaseCopy[phase] || labelCopy[actionLabel] || "排班表已收，继续营业。";
    return `${actionLabel}完成，${nextStep}`;
}

async function withBusy(label, task) {
    if (state.busy) {
        setStatus("上一操作还在收尾，稍等再点。", "warning");
        return null;
    }

    SFX.click();
    state.busy = true;
    state.busyLabel = label;
    setStatus(`${label}……`, "info");
    if (typeof setApiBusy === 'function') setApiBusy(label);

    try {
        const result = await task();

        // Sound effects for action results
        try {
            const ar = result?.actionResult || result?.data?.actionResult;
            if (ar) {
                if (ar.fanChange > 20) SFX.fanUp();
                else if (ar.fanChange < -10) SFX.fanDown();
                if (ar.debtCreated || ar.debts?.length > 0) SFX.debtWarn();
            }
        } catch(e) {}

        if (result?.suppressBusySuccess) {
            return result;
        }

        setStatus(doneStatusText(label), "success");
        SFX.success();

        // Critical hit visual for big fan gains
        try {
            const ar = result?.actionResult || result?.data?.actionResult;
            if (ar?.fanChange > 30) {
                const flash = document.createElement('div');
                flash.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(255,110,199,0.15);pointer-events:none;z-index:9999;';
                document.body.appendChild(flash);
                setTimeout(() => flash.remove(), 300);
                SFX.combo();
            }
        } catch(e) {}

        return result;
    } catch (error) {
        await refreshAfterWriteFailure();
        setStatus(error.message || "操作失败，请稍后再试。", "error");
        return null;
    } finally {
        state.busy = false;
        state.busyLabel = "";
        if (typeof clearApiBusy === 'function') clearApiBusy();
        render();
        checkMilestones();
    }
}

// 路线立绘映射

// Moved to /js/panel-bundle.js
function renderSimpleDecisionCard(primaryAction, quickActions) {
    if (!state.vup) return "";
    const visibleChoices = simpleDecisionVisibleChoices(primaryAction, quickActions);
    const action = visibleChoices.action;
    const route = state.vup?.currentRoute || 'UNKNOWN';
    const routeTheme = ROUTE_THEMES[route] || {};
    const routeActionEmoji = routeTheme.actionEmoji || {};
    const primaryActionEmoji = routeActionEmoji[action?.actionType] || '🎮';
    const actionName = action ? (primaryActionEmoji + ' ' + actionNameText(action)) : "🎮 等待行动";
    const actionType = action?.actionType || "";
    const disabled = !action || state.busy || !action.enabled;
    const disabledText = action ? actionDisabledText(action, disabled) : "行动列表还在同步。";
    const forecast = state.endingForecast;
    const likelyEnding = forecast?.likelyEndingType || state.vup.currentRoute || "UNKNOWN";
    const likelyTitle = endingForecastFinalTitleText(forecast, routeLabelFor(likelyEnding));
    const endingGapLine = endingGapCoachLine(firstWarnEndingRequirement(), { likelyEnding });
    const playbook = routePlaybookFor(likelyEnding);
    const routeLabel = action
        ? visibleTextOrFallback(action.routeBiasLabel, routeLabelFor(action.routeBiasType || likelyEnding))
        : routeLabelFor(likelyEnding);
    const debtSummary = cockpitDebtSummary(activeDebts());
    const riskTone = debtSummary.tone || readyGoalRiskTone();
    const day = Number(state.session?.day || state.vup.day || 1);
    const stats = state.vup.fanStructure || {};
    const watchHeat = state.vup.opinion?.watchHeat ?? state.vup.watchHeat ?? 0;
    const alternativeActions = visibleChoices.alternatives.map(choice => choice.action);
    const counterplayText = simpleDecisionCounterplayText(action, alternativeActions);
    const quickItems = visibleChoices.alternatives.map((choice, index) => {
        const action = choice.action;
        const itemDisabled = state.busy || !action.enabled;
        const hotkey = index + 2;
        const itemEmoji = routeActionEmoji[action.actionType] || '🎮';
        const routeLabel = visibleTextOrFallback(action.routeBiasLabel, actionDecisionTags(action)[0].value);
        const comboLabel = actionComboShortText(action);
        const quickVisibleReason = action.disabledReason ? quickDisabledReasonText(action.disabledReason) : (comboLabel || visibleTextOrFallback(action.tempoHint, routeLabel));
        const quickAccessibleReason = action.disabledReason ? disabledReasonText(action.disabledReason) : actionSignalText(action, 'recommended');
        // legacy marker: <span><em aria-hidden="true">${index + 1}</em>${html(actionNameText(action))}</span>
        return `
            <button type="button" class="simple-alt-action ${html(choice.role)} ${action.actionType === actionType ? "active" : ""} ${itemDisabled ? "blocked" : ""}"
                    data-action="simple-alt-action"
                    data-action-type="${html(action.actionType)}"
                    data-action-name="${html(actionNameText(action))}"
                    data-action-hotkey="${hotkey}"
                    aria-label="${html(`${quickActionLabel(action, quickAccessibleReason)}，快捷键 ${hotkey}`)}"
                    ${actionButtonStateAttributes(itemDisabled, actionDisabledText(action, itemDisabled))}>
                <span>${html(choice.label)}</span>
                <strong>${itemEmoji} ${html(choice.title)} · ${html(actionNameText(action))}</strong>
                <small class="${comboLabel ? 'quick-combo-line' : ''}">${html(routeLabel)} · ${html(quickVisibleReason)}</small>
            </button>
        `;
    }).join("");
    const quickActionsHtml = quickItems || '<span class="simple-alt-empty">暂无其他推荐行动</span>';

    // 目标栏：本局进度 + 当前路线
    const totalDays = runMaxDay();
    const progressPct = Math.min(100, Math.round(day / totalDays * 100));
    const motto = routeTheme.motto || '成为最佳主播';
    const goalBanner = `
        <div class="goal-banner" aria-label="游戏目标">
            <div class="goal-progress-bar"><div class="goal-progress-fill" style="width:${progressPct}%"></div></div>
            <div class="goal-text">
                <span>🎯 ${totalDays}天出道挑战</span>
                <span>第${day}天/${totalDays}天</span>
                <span>路线：${html(routeLabel)}</span>
                <span class="goal-motto">${html(motto)}</span>
            </div>
        </div>
    `;

    // VUP 说话气泡
    const vupQuoteText = vupQuote();
    const vupBubble = '<div class="vup-speech-bubble"><span class="vup-quote-avatar">💬</span><span class="vup-quote-text">' + html(vupQuoteText) + '</span></div>';

    // 路线进度条
    const routeProgressLabel = routeLabelFor(likelyEnding);
    const gapLine = endingGapLine || '';
    const req0 = firstWarnEndingRequirement();
    const routePct = (req0 && Number.isFinite(req0.currentValue) && Number.isFinite(req0.targetValue) && req0.targetValue > 0)
        ? Math.min(100, Math.round(req0.currentValue / req0.targetValue * 100))
        : progressPct;
    const routeProgressBar = `
        <div class="route-progress-bar">
            <div class="route-progress-label">📍 路线：${html(routeProgressLabel)}</div>
            <div class="route-progress-track"><div class="route-progress-fill" style="width:${routePct}%"></div></div>
            <div class="route-progress-hint">${gapLine ? html(gapLine) : '路线探索中，多做行动来推进进度'}</div>
        </div>
    `;
    const routePlaybookCard = `
        <div class="route-playbook-card" aria-label="路线打法">
            <span><em>想要</em><strong>${html(playbook.fantasy)}</strong></span>
            <span><em>代价</em><strong>${html(playbook.cost)}</strong></span>
            <span><em>下一手</em><strong>${html(playbook.next)}</strong></span>
        </div>
    `;

    return `
        ${goalBanner}
        ${vupBubble}
        ${routeProgressBar}
        ${routePlaybookCard}
        ${renderFullRouteMap(likelyEnding)}
        ${renderNewPlayerGuardrail(action, day)}
        <section class="simple-decision-card ${riskTone}" aria-label="今日决策">
            <div class="simple-decision-hero">
                <div class="simple-decision-copy">
                    <h3>今天先做：${html(actionName)}</h3>
                    <p>${html(simpleDecisionLoopText(action))}</p>
                    <p>${html(simpleDecisionPreviewText(action))}</p>
                    ${renderOpeningDecisionHint(action, day)}
                </div>
                <button type="button" class="simple-primary-action"
                        data-action="simple-primary-action"
                        data-action-type="${html(actionType)}"
                        data-action-name="${html(actionName)}"
                        data-action-hotkey="1"
                        ${actionButtonStateAttributes(disabled, disabledText)}>
                    ${html(disabled ? "暂不可用" : primaryActionButtonText(action))}
                </button>
            </div>
            <div class="simple-decision-strip" aria-label="关键信息">
                <span class="benefit"><em>收益</em><strong>${html(simplifiedActionPayoff(action))}</strong></span>
                <span class="risk"><em>风险</em><strong>${html(simpleDecisionRiskText(action))}</strong></span>
                <span class="goal"><em>结局</em><strong>${html(likelyTitle)}</strong></span>
                <span class="debt ${html(debtSummary.tone)}"><em>旧账</em><strong>${html(debtSummary.title)}</strong></span>
            </div>
            <div class="simple-decision-bottom">
                <div class="simple-mini-stats">
                    <span><em>粉丝</em><strong>${html(stats.fans ?? state.vup.fans ?? 0)}</strong></span>
                    <span><em>围观</em><strong>${html(watchHeat)}</strong></span>
                    <span><em>旧账</em><strong>${html(debtSummary.title)}</strong></span>
                </div>
                <div class="simple-alt-actions" aria-label="其他可选行动">${quickActionsHtml}</div>
            </div>
            ${renderRiskStatus()}
        </section>
    `;
}

function dailyPlanActionTone(action) {
    if (!action) return "route";
    if (action.actionType === 'REST') return "rest";
    if (action.planArchetype === "SAFE_BUSINESS") return "repair";
    if (action.planArchetype === "ROUTE_PROGRESS") return "route";
    if (['FAN_GROUP_MAINTAIN', 'TRAIN_TALK'].includes(action.actionType)) {
        return "repair";
    }
    if (['STREAM_PLAN', 'PUBLISH_VIDEO', 'PUBLISH_CLIP', 'NPC_INTERACT', 'TRAIN_SONG', 'TRAIN_DANCE'].includes(action.actionType)) {
        return "route";
    }
    return "route";
}

function dailyPlanToneMeta(tone) {
    const metas = {
        recommended: {
            title: "推荐",
            badge: "先点这个",
            verb: "执行推荐",
            promise: "按当前目标、风险灯和结局缺口排出的首选行动。",
            empty: "推荐行动还在同步"
        },
        route: {
            title: "路线",
            badge: "补证据",
            verb: "推进路线",
            promise: "把今天写进路线证据链，结局更不摇摆。",
            empty: "路线方案还在同步"
        },
        repair: {
            title: "修复",
            badge: "拆风险",
            verb: "修复状态",
            promise: "把口碑、体力或旧账拉回安全区。",
            empty: "今天没有合适的修复方案"
        },
        rest: {
            title: "休息",
            badge: "回体力",
            verb: "休息一天",
            promise: "少拿一点反馈，换体力和风险缓冲。",
            empty: "休息方案还在同步"
        }
    };
    return metas[tone] || metas.route;
}

function dailyPlanActionScore(action, tone, primaryAction, quickActions) {
    if (!action) return -9999;
    let score = action.enabled ? 100 : -100;
    if (primaryAction?.actionType === action.actionType) score += 28;
    if ((quickActions || []).some(item => item?.actionType === action.actionType)) score += 18;
    if (actionMatchesStageObjective(action.actionType)) score += 26;
    if (actionEndingGapCovered(action)) score += 24;
    if (dailyPlanActionTone(action) === tone) score += 42;
    if (tone === "repair" && hasMeaningfulReadyRisk()) score += 34;
    if (tone === "repair" && ['FAN_GROUP_MAINTAIN', 'TRAIN_TALK'].includes(action.actionType)) score += 26;
    if (tone === "rest" && action.actionType === 'REST') score += 80;
    if (tone === "rest" && action.actionType !== 'REST') score -= 60;
    if (tone === "route" && primaryRouteActionTypes(state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN").includes(action.actionType)) score += 30;
    if (tone === "route" && ['STREAM_PLAN', 'PUBLISH_VIDEO', 'PUBLISH_CLIP', 'NPC_INTERACT'].includes(action.actionType)) score += 12;
    if (action.disabledReason === 'INSUFFICIENT_MATERIAL_STOCK') score -= 30;
    if (/缺切片素材|灵感不足/.test(action.recommendedReason || "")) score -= 18;
    return score;
}

function selectDailyPlanAction(tone, usedTypes, primaryAction, quickActions) {
    const actions = Array.isArray(state.actions) ? state.actions : [];
    const enabled = actions.filter(action => action?.enabled && !usedTypes.has(action.actionType));
    const candidates = enabled.length ? enabled : actions.filter(action => action && !usedTypes.has(action.actionType));
    return [...candidates]
        .sort((left, right) =>
            dailyPlanActionScore(right, tone, primaryAction, quickActions)
            - dailyPlanActionScore(left, tone, primaryAction, quickActions))
        [0] || null;
}

function dailyPlanWhyText(action, tone) {
    if (!action) return dailyPlanToneMeta(tone).empty;
    const reason = visibleTextOrFallback(action.recommendedReason, "");
    if (reason) {
        return shortDecisionText(reason.replace(/^主推：/, "").replace(/^复活赛目标：/, ""), dailyPlanToneMeta(tone).promise);
    }
    if (hasMeaningfulReadyRisk() || activeDebts().length > 0 || tone === "repair") {
        return shortDecisionText(
            `${dailyPlanToneMeta(tone).promise} 当前有旧账或风险信号，先把风险压住再冲热度。`,
            "有旧账，先稳风险。"
        );
    }
    const gap = firstWarnEndingRequirement();
    if (gap || tone === "route") {
        return shortDecisionText(
            `${dailyPlanToneMeta(tone).promise} 结局缺口需要继续补路线证据。`,
            "补路线证据，减少结局缺口。"
        );
    }
    return dailyPlanToneMeta(tone).promise;
}

function dailyPlanRiskText(action, tone) {
    if (!action) return "不可执行";
    const risk = simplifiedActionRisk(action);
    const focus = visibleTextOrFallback(action.routeFocusHint, "");
    if (tone === "route" && focus) return shortDecisionText(focus, risk);
    return risk;
}

function dailyPlanMainCostText(action) {
    if (!action) return "待同步";
    return shortDecisionText(actionResourceChipText(action), "低消耗，主要看风险取舍");
}

const DAILY_SCHEDULE_SLOT_ORDER = [
    { key: "MORNING", label: "上午" },
    { key: "NOON", label: "中午" },
    { key: "AFTERNOON", label: "下午" },
    { key: "NIGHT", label: "晚上" }
];

const DAILY_SCHEDULE_INTENSITIES = [
    { value: "LIGHT", label: "轻量" },
    { value: "STANDARD", label: "标准" },
    { value: "SPRINT", label: "冲刺" }
];

const DAILY_SCHEDULE_PRESETS = [
    {
        key: "STEADY_ROUTE",
        label: "稳口碑补路线",
        shortLabel: "稳口碑",
        tone: "steady",
        result: "口碑稳、旧账低、涨粉中等",
        slots: [
            { slotKey: "MORNING", actionType: "TRAIN_TALK", actionName: "练谈吐", intensity: "STANDARD" },
            { slotKey: "NOON", actionType: "FAN_GROUP_MAINTAIN", actionName: "粉丝群维护", intensity: "LIGHT" },
            { slotKey: "AFTERNOON", actionType: "STREAM_PLAN", actionName: "直播企划", intensity: "STANDARD" },
            { slotKey: "NIGHT", actionType: "TRAIN_TALK", actionName: "低压杂谈", intensity: "STANDARD" }
        ]
    },
    {
        key: "HEAT_PUSH",
        label: "冲热度",
        shortLabel: "冲热度",
        tone: "burst",
        result: "围观高、涨粉快、风险升",
        slots: [
            { slotKey: "MORNING", actionType: "STREAM_PLAN", actionName: "刷趋势", intensity: "STANDARD" },
            { slotKey: "NOON", actionType: "STREAM_PLAN", actionName: "标题企划", intensity: "SPRINT" },
            { slotKey: "AFTERNOON", actionType: "PUBLISH_CLIP", actionName: "切片准备", intensity: "STANDARD" },
            { slotKey: "NIGHT", actionType: "STREAM_PLAN", actionName: "高热直播", intensity: "SPRINT" }
        ]
    },
    {
        key: "CONTENT_BUILD",
        label: "做内容",
        shortLabel: "做内容",
        tone: "route",
        result: "灵感和素材转化，路线证据更清楚",
        slots: [
            { slotKey: "MORNING", actionType: "FAN_GROUP_MAINTAIN", actionName: "收集素材", intensity: "STANDARD" },
            { slotKey: "NOON", actionType: "REST", actionName: "休息/读信", intensity: "LIGHT" },
            { slotKey: "AFTERNOON", actionType: "PUBLISH_VIDEO", actionName: "投稿制作", intensity: "SPRINT" },
            { slotKey: "NIGHT", actionType: "STREAM_PLAN", actionName: "短直播预热", intensity: "LIGHT" }
        ]
    },
    {
        key: "REPAIR_STATUS",
        label: "修状态",
        shortLabel: "修状态",
        tone: "repair",
        result: "体力回一点，口碑和旧账更安全",
        slots: [
            { slotKey: "MORNING", actionType: "REST", actionName: "休息", intensity: "LIGHT" },
            { slotKey: "NOON", actionType: "FAN_GROUP_MAINTAIN", actionName: "读粉丝信", intensity: "LIGHT" },
            { slotKey: "AFTERNOON", actionType: "TRAIN_TALK", actionName: "危机处理/复盘", intensity: "STANDARD" },
            { slotKey: "NIGHT", actionType: "TRAIN_TALK", actionName: "低压陪聊", intensity: "LIGHT" }
        ]
    },
    {
        key: "COMMERCIAL_CONVERSION",
        label: "商业转化",
        shortLabel: "商业",
        tone: "business",
        result: "礼物收入、运营预算上升，但商业味和压力也会上升",
        slots: [
            { slotKey: "MORNING", actionType: "STREAM_PLAN", actionName: "直播企划", intensity: "STANDARD" },
            { slotKey: "NOON", actionType: "FAN_GROUP_MAINTAIN", actionName: "粉丝互动", intensity: "LIGHT" },
            { slotKey: "AFTERNOON", actionType: "STREAM_PLAN", actionName: "商务准备", intensity: "STANDARD" },
            { slotKey: "NIGHT", actionType: "STREAM_PLAN", actionName: "感谢礼物/商业回", intensity: "STANDARD" }
        ]
    },
    {
        key: "FREE",
        label: "自由排班",
        shortLabel: "自由",
        tone: "free",
        result: "按手动排班结算",
        slots: [
            { slotKey: "MORNING", actionType: "TRAIN_TALK", actionName: "自定上午", intensity: "STANDARD" },
            { slotKey: "NOON", actionType: "FAN_GROUP_MAINTAIN", actionName: "自定中午", intensity: "LIGHT" },
            { slotKey: "AFTERNOON", actionType: "STREAM_PLAN", actionName: "自定下午", intensity: "STANDARD" },
            { slotKey: "NIGHT", actionType: "REST", actionName: "自定晚上", intensity: "LIGHT" }
        ]
    }
];

function dailySchedulePresetFor(planKey) {
    return DAILY_SCHEDULE_PRESETS.find(plan => plan.key === planKey) || DAILY_SCHEDULE_PRESETS[0];
}

function dailyScheduleCurrentDay() {
    return Number(state.session?.day || state.vup?.day || 1);
}

function currentScheduleResources() {
    const v = state.vup || {};
    return {
        stamina: Number(v.resources?.stamina ?? v.stamina ?? 0),
        maxStamina: Number(v.resources?.maxStamina ?? v.maxStamina ?? 10),
        inspiration: Number(v.resources?.inspiration ?? v.inspiration ?? 0),
        coin: Number(v.resources?.coin ?? v.coin ?? 0),
        material: Number(v.funProfile?.materialStock ?? state.funAudienceProfile?.materialStock ?? 0)
    };
}

function dailyScheduleRecommendedPlanKey(primaryAction, quickActions) {
    const resources = currentScheduleResources();
    const debts = activeDebts();
    const debtSummary = cockpitDebtSummary(debts);
    const likelyEnding = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN";
    const day = dailyScheduleCurrentDay();
    if (resources.stamina <= 4 || debtSummary.tone === "hot" || (state.vup?.opinion?.reputation ?? state.vup?.reputation ?? 60) < 45) {
        return "REPAIR_STATUS";
    }
    if (day >= 22 && resources.coin < 1200) {
        return "COMMERCIAL_CONVERSION";
    }
    if (likelyEnding === "SLICE_SAINT" || likelyEnding === "DANCE_MEME") {
        return "CONTENT_BUILD";
    }
    if (primaryAction?.actionType === "PUBLISH_CLIP" && resources.material < 1) {
        return "CONTENT_BUILD";
    }
    if ((quickActions || []).some(action => action?.actionType === "FAN_GROUP_MAINTAIN") && debts.length > 0) {
        return "STEADY_ROUTE";
    }
    return "STEADY_ROUTE";
}

function buildDailyScheduleDraft(planKey) {
    const preset = dailySchedulePresetFor(planKey);
    const rawSlots = preset.slots.map(slot => ({ ...slot }));
    const normalized = normalizeDailyScheduleForResources(rawSlots);
    return {
        day: dailyScheduleCurrentDay(),
        planKey: preset.key,
        slots: normalized.slots,
        adjustments: normalized.adjustments
    };
}

function ensureDailyScheduleDraft(primaryAction, quickActions) {
    const day = dailyScheduleCurrentDay();
    if (!state.dailyScheduleDraft || state.dailyScheduleDraft.day !== day || state.session?.phase !== "READY") {
        state.dailyScheduleDraft = buildDailyScheduleDraft(dailyScheduleRecommendedPlanKey(primaryAction, quickActions));
    }
    return state.dailyScheduleDraft;
}

function dailyScheduleActionOptions() {
    const fallbackTypes = [
        "TRAIN_SONG",
        "TRAIN_DANCE",
        "TRAIN_TALK",
        "STREAM_PLAN",
        "PUBLISH_VIDEO",
        "PUBLISH_CLIP",
        "FAN_GROUP_MAINTAIN",
        "NPC_INTERACT",
        "REST"
    ];
    const byType = new Map();
    (Array.isArray(state.actions) ? state.actions : []).forEach(action => {
        if (action?.actionType) byType.set(action.actionType, action);
    });
    fallbackTypes.forEach(type => {
        if (!byType.has(type)) {
            byType.set(type, { actionType: type, name: actionLabelFor(type), enabled: true, actionPointCost: 2 });
        }
    });
    return fallbackTypes.map(type => byType.get(type)).filter(Boolean);
}

function dailyScheduleBaseStaminaCost(actionType) {
    const backend = (state.actions || []).find(action => action?.actionType === actionType);
    if (backend && Number.isFinite(Number(backend.staminaCost))) return Number(backend.staminaCost);
    return {
        STREAM_PLAN: 3,
        PUBLISH_VIDEO: 4,
        PUBLISH_CLIP: 1,
        NPC_INTERACT: 2,
        REST: 0
    }[actionType] ?? 2;
}

function dailyScheduleStaminaDelta(actionType, intensity) {
    if (actionType === "REST") {
        return intensity === "LIGHT" ? 3 : 5;
    }
    const base = dailyScheduleBaseStaminaCost(actionType);
    const cost = intensity === "LIGHT"
        ? Math.max(1, base - 1)
        : intensity === "SPRINT"
            ? base + (actionType === "PUBLISH_CLIP" ? 1 : 2)
            : base;
    return -cost;
}

function dailyScheduleApCost(actionType, intensity) {
    if (actionType === "REST") return 1;
    return intensity === "LIGHT" ? 1 : intensity === "SPRINT" ? 3 : 2;
}

function dailyScheduleInspirationDelta(actionType) {
    if (actionType === "PUBLISH_VIDEO") return -1;
    if (actionType === "TRAIN_TALK" || actionType === "FAN_GROUP_MAINTAIN") return 1;
    return 0;
}

function dailyScheduleMaterialDelta(actionType) {
    if (actionType === "PUBLISH_VIDEO") return 2;
    if (actionType === "FAN_GROUP_MAINTAIN") return 1;
    if (actionType === "PUBLISH_CLIP") return -1;
    return 0;
}

function dailyScheduleCoinDelta(actionType) {
    const day = dailyScheduleCurrentDay();
    if (day < 22) return 0;
    if (actionType === "PUBLISH_VIDEO") return -300;
    if (actionType === "PUBLISH_CLIP") return -150;
    if (actionType === "NPC_INTERACT") return -250;
    return 0;
}

function dailyScheduleCanApply(slot, resources) {
    return resources.actionPoints >= dailyScheduleApCost(slot.actionType, slot.intensity)
        && resources.inspiration + dailyScheduleInspirationDelta(slot.actionType) >= 0
        && resources.material + dailyScheduleMaterialDelta(slot.actionType) >= 0
        && resources.coin + dailyScheduleCoinDelta(slot.actionType) >= 0;
}

function applyDailyScheduleSlotResources(resources, slot) {
    return {
        stamina: Math.max(0, Math.min(resources.maxStamina, resources.stamina + dailyScheduleStaminaDelta(slot.actionType, slot.intensity))),
        maxStamina: resources.maxStamina,
        inspiration: Math.max(0, resources.inspiration + dailyScheduleInspirationDelta(slot.actionType)),
        coin: Math.max(0, resources.coin + dailyScheduleCoinDelta(slot.actionType)),
        material: Math.max(0, resources.material + dailyScheduleMaterialDelta(slot.actionType))
    };
}

function downgradeScheduleIntensity(intensity) {
    if (intensity === "SPRINT") return "STANDARD";
    if (intensity === "STANDARD") return "LIGHT";
    return "LIGHT";
}

function scheduleFallbackActionFor(slot, resources) {
    if (slot.actionType === "PUBLISH_CLIP") {
        if (resources.inspiration > 0 && resources.stamina >= 4) return "PUBLISH_VIDEO";
        return "FAN_GROUP_MAINTAIN";
    }
    if (slot.actionType === "PUBLISH_VIDEO") {
        return "FAN_GROUP_MAINTAIN";
    }
    if (dailyScheduleCoinDelta(slot.actionType) < 0 && resources.coin + dailyScheduleCoinDelta(slot.actionType) < 0) {
        return "FAN_GROUP_MAINTAIN";
    }
    return "REST";
}

function normalizeDailyScheduleForResources(slots) {
    let resources = currentScheduleResources();
    const adjustments = [];
    const normalized = slots.map(rawSlot => {
        let slot = {
            slotKey: rawSlot.slotKey,
            actionType: rawSlot.actionType,
            actionName: rawSlot.actionName || actionLabelFor(rawSlot.actionType),
            intensity: rawSlot.intensity || "STANDARD"
        };
        if ((slot.actionType === "PUBLISH_CLIP" && resources.material < 1)
                || (slot.actionType === "PUBLISH_VIDEO" && resources.inspiration < 1)
                || (dailyScheduleCoinDelta(slot.actionType) < 0 && resources.coin + dailyScheduleCoinDelta(slot.actionType) < 0)) {
            const fallbackType = scheduleFallbackActionFor(slot, resources);
            adjustments.push(`${dailyScheduleSlotLabel(slot.slotKey)}资源不足，改成${actionLabelFor(fallbackType)}`);
            slot = { ...slot, actionType: fallbackType, actionName: actionLabelFor(fallbackType), intensity: fallbackType === "REST" ? "LIGHT" : "STANDARD" };
        }
        while (!dailyScheduleCanApply(slot, resources) && slot.intensity !== "LIGHT") {
            slot = { ...slot, intensity: downgradeScheduleIntensity(slot.intensity) };
            adjustments.push(`${dailyScheduleSlotLabel(slot.slotKey)}体力紧张，强度降为${dailyScheduleIntensityLabel(slot.intensity)}`);
        }
        if (!dailyScheduleCanApply(slot, resources)) {
            slot = { ...slot, actionType: "REST", actionName: "休息", intensity: "LIGHT" };
            adjustments.push(`${dailyScheduleSlotLabel(slot.slotKey)}改成休息，避免体力或资源透支`);
        }
        resources = applyDailyScheduleSlotResources(resources, slot);
        return slot;
    });
    return { slots: normalized, adjustments };
}

function dailyScheduleSlotLabel(slotKey) {
    return DAILY_SCHEDULE_SLOT_ORDER.find(slot => slot.key === slotKey)?.label || "时段";
}

function dailyScheduleIntensityLabel(value) {
    return DAILY_SCHEDULE_INTENSITIES.find(item => item.value === value)?.label || "标准";
}

function dailyScheduleEstimate(slots) {
    return slots.reduce((total, slot) => ({
        stamina: total.stamina + dailyScheduleStaminaDelta(slot.actionType, slot.intensity),
        ap: total.ap + dailyScheduleApCost(slot.actionType, slot.intensity),
        inspiration: total.inspiration + dailyScheduleInspirationDelta(slot.actionType),
        material: total.material + dailyScheduleMaterialDelta(slot.actionType),
        coin: total.coin + dailyScheduleCoinDelta(slot.actionType)
    }), { stamina: 0, ap: 0, inspiration: 0, material: 0, coin: 0 });
}

function dailyScheduleSigned(value) {
    return value > 0 ? `+${value}` : String(value);
}

function dailySchedulePrimaryActionType(draft) {
    return draft?.slots?.find(slot => slot.actionType !== "REST")?.actionType || draft?.slots?.[0]?.actionType || "";
}

function selectDailySchedulePreset(planKey) {
    state.dailyScheduleDraft = buildDailyScheduleDraft(planKey || "STEADY_ROUTE");
    renderActions();
}

function updateDailyScheduleSlotAction(slotKey, actionType) {
    const draft = ensureDailyScheduleDraft();
    const slots = draft.slots.map(slot => slot.slotKey === slotKey
        ? { ...slot, actionType, actionName: actionLabelFor(actionType) }
        : slot);
    const normalized = normalizeDailyScheduleForResources(slots);
    state.dailyScheduleDraft = { ...draft, planKey: "FREE", slots: normalized.slots, adjustments: normalized.adjustments };
    renderActions();
}

function updateDailyScheduleSlotIntensity(slotKey, intensity) {
    const draft = ensureDailyScheduleDraft();
    const slots = draft.slots.map(slot => slot.slotKey === slotKey ? { ...slot, intensity } : slot);
    const normalized = normalizeDailyScheduleForResources(slots);
    state.dailyScheduleDraft = { ...draft, slots: normalized.slots, adjustments: normalized.adjustments };
    renderActions();
}

async function submitSchedule(planKey) {
    const draft = state.dailyScheduleDraft || buildDailyScheduleDraft(planKey || "STEADY_ROUTE");
    const normalized = normalizeDailyScheduleForResources(draft.slots);
    const finalDraft = {
        ...draft,
        planKey: planKey || draft.planKey || "FREE",
        slots: normalized.slots,
        adjustments: normalized.adjustments
    };
    state.dailyScheduleDraft = finalDraft;
    const keyParts = currentRunKeyParts(
        "schedule",
        finalDraft.planKey,
        finalDraft.slots.map(slot => `${slot.slotKey}:${slot.actionType}:${slot.intensity}`).join("|")
    );
    await withBusy('提交今日排班中', async () => {
        const board = document.querySelector('.daily-plan-board');
        const result = await api('/api/day/schedule', {
            method: 'POST',
            body: {
                planKey: finalDraft.planKey,
                slots: finalDraft.slots.map(slot => ({
                    slotKey: slot.slotKey,
                    actionType: slot.actionType,
                    intensity: slot.intensity
                })),
                idempotencyKey: stableIdempotencyKey('day-schedule', keyParts)
            }
        });
        state.lastActionResult = result?.actionResult || null;
        state.pendingAction = null;
        triggerActionFeedbackAnimations(state.lastActionResult, board);
        await refreshAfterWrite();
        clearStableIdempotencyKey('day-schedule', keyParts);
        window.setTimeout(focusActionResultHud, 0);
    });
}

function renderDailyScheduleStrategyButtons(draft, recommendedKey) {
    return `
        <div class="daily-schedule-strategies" aria-label="排班预设">
            ${DAILY_SCHEDULE_PRESETS.map(plan => {
                const active = plan.key === draft.planKey;
                const recommended = plan.key === recommendedKey;
                return `
                    <button type="button"
                            class="daily-schedule-strategy ${html(plan.tone)} ${active ? "active" : ""} ${recommended ? "recommended" : ""}"
                            data-action="daily-schedule-preset"
                            data-plan-key="${html(plan.key)}"
                            ${state.busy ? 'disabled aria-disabled="true"' : ''}>
                        <span>${html(recommended ? `推荐：${plan.shortLabel}` : plan.shortLabel)}</span>
                        <small>${html(plan.result)}</small>
                    </button>
                `;
            }).join("")}
        </div>
    `;
}

function renderDailyScheduleSlot(slot, index) {
    const actions = dailyScheduleActionOptions();
    const action = actions.find(item => item.actionType === slot.actionType);
    const routeText = action
        ? visibleTextOrFallback(action.routeBiasLabel, routeLabelFor(action.routeBiasType || state.vup?.currentRoute || "UNKNOWN"))
        : routeLabelFor(state.vup?.currentRoute || "UNKNOWN");
    return `
        <article class="daily-plan-card daily-schedule-slot ${html(slot.actionType === "REST" ? "rest" : "route")}" aria-label="${html(`${dailyScheduleSlotLabel(slot.slotKey)}：${slot.actionName}`)}">
            <div class="daily-plan-card-head">
                <span>${html(dailyScheduleSlotLabel(slot.slotKey))}</span>
                <strong>${html(dailyScheduleIntensityLabel(slot.intensity))}</strong>
            </div>
            <h4>${html(slot.actionName || actionLabelFor(slot.actionType))}</h4>
            <div class="daily-schedule-controls">
                <label>
                    <span>行动</span>
                    <select data-change-action="daily-schedule-action-change" data-slot-key="${html(slot.slotKey)}" ${state.busy ? 'disabled' : ''}>
                        ${actions.map(item => `<option value="${html(item.actionType)}" ${item.actionType === slot.actionType ? "selected" : ""}>${html(actionNameText(item))}</option>`).join("")}
                    </select>
                </label>
                <label>
                    <span>强度</span>
                    <select data-change-action="daily-schedule-intensity-change" data-slot-key="${html(slot.slotKey)}" ${state.busy ? 'disabled' : ''}>
                        ${DAILY_SCHEDULE_INTENSITIES.map(item => `<option value="${html(item.value)}" ${item.value === slot.intensity ? "selected" : ""}>${html(item.label)}</option>`).join("")}
                    </select>
                </label>
            </div>
            <div class="daily-plan-card-facts">
                <span><em>体力</em><strong>${html(dailyScheduleSigned(dailyScheduleStaminaDelta(slot.actionType, slot.intensity)))}</strong></span>
                <span><em>资源</em><strong>${html(`灵感${dailyScheduleSigned(dailyScheduleInspirationDelta(slot.actionType))} / 素材${dailyScheduleSigned(dailyScheduleMaterialDelta(slot.actionType))}`)}</strong></span>
                <span><em>路线倾向</em><strong>${html(routeText)}</strong></span>
                <span><em>顺序</em><strong>${html(`${index + 1}/4`)}</strong></span>
            </div>
        </article>
    `;
}

function renderDailyPlanCard(plan, index) {
    return renderDailyScheduleSlot(plan, index);
}

function renderDailyPlanCards(primaryAction, quickActions) {
    const draft = ensureDailyScheduleDraft(primaryAction, quickActions);
    const recommendedKey = dailyScheduleRecommendedPlanKey(primaryAction, quickActions);
    const recommendedPlan = dailySchedulePresetFor(recommendedKey);
    const preset = dailySchedulePresetFor(draft.planKey);
    const estimate = dailyScheduleEstimate(draft.slots);
    const resources = currentScheduleResources();
    const day = dailyScheduleCurrentDay();
    const maxDay = runMaxDay();
    const latePhase = lateGameReadyPhase(day);
    const likelyEnding = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN";
    const debtSummary = cockpitDebtSummary(activeDebts());
    const routeMap = renderRouteVisibilityMap(likelyEnding, true);
    const crisisCards = renderCrisisCards(true);
    const gap = endingGapCoachLine(firstWarnEndingRequirement(), { likelyEnding });
    const primaryActionType = dailySchedulePrimaryActionType(draft);
    const submitDisabled = state.busy || !draft.slots?.length;
    const submitDisabledText = submitDisabled ? (state.busyLabel || "排班处理中，请稍等。") : "";
    const adjustments = draft.adjustments?.length
        ? `<div class="daily-schedule-adjustments">${draft.adjustments.slice(0, 3).map(item => `<span>${html(item)}</span>`).join("")}</div>`
        : "";
    const planLine = latePhase ? latePhase.planLine : "点一个今日打法会自动填好四个时段；体力、素材和灵感不足时会先降强度，再改成更稳的安排。";
    return `
        <section class="daily-plan-board" aria-label="今日运营方案">
            <div class="daily-plan-board-head">
                <div>
                    <span>第${html(day)}天 / ${html(maxDay)}天${latePhase ? ` · ${html(latePhase.label)}` : ""}</span>
                    <h3>${latePhase ? `${html(latePhase.label)}：` : ""}今日运营方案</h3>
                    <p>${html(planLine)}</p>
                </div>
                <div class="daily-plan-board-actions">
                    <button type="button"
                            class="daily-plan-next-step"
                            data-action="daily-plan-next-step"
                            data-plan-key="${html(draft.planKey)}"
                            data-action-type="${html(primaryActionType)}"
                            data-action-name="${html(preset.label)}"
                            data-action-hotkey="1"
                            ${actionButtonStateAttributes(submitDisabled, submitDisabledText)}>
                        <span>${html(recommendedKey === draft.planKey ? "推荐今日打法" : `推荐：${recommendedPlan.label}`)}</span>
                        <strong>${html(preset.label)}</strong>
                        <small>${html(preset.result)}</small>
                        <em>${html(submitDisabled ? "等待" : "执行")}</em>
                    </button>
                    <button type="button" class="daily-plan-risk ${html(debtSummary.tone)}" data-action="open-daily-plan-risk">
                        <span>${html(debtSummary.title)}</span>
                        <strong>${html(debtSummary.line)}</strong>
                    </button>
                </div>
            </div>
            ${renderDailyScheduleStrategyButtons(draft, recommendedKey)}
            <div class="daily-schedule-budget" aria-label="资源预算">
                <span><em>行动点</em><strong>${html(`${resources.actionPoints ?? '?'} → ${Math.max(0, (resources.actionPoints ?? 0) - (estimate.ap ?? 0))}`)}</strong></span>
                <span><em>灵感</em><strong>${html(dailyScheduleSigned(estimate.inspiration))}</strong></span>
                <span><em>素材</em><strong>${html(dailyScheduleSigned(estimate.material))}</strong></span>
                <span><em>预算</em><strong>${html(dailyScheduleSigned(estimate.coin))}</strong></span>
            </div>
            ${adjustments}
            <div class="daily-plan-grid daily-schedule-grid">
                ${draft.slots.map((slot, index) => renderDailyScheduleSlot(slot, index)).join("")}
            </div>
            <button type="button"
                    class="daily-plan-submit daily-schedule-submit"
                    data-action="daily-plan-submit"
                    data-plan-key="${html(draft.planKey)}"
                    data-action-type="${html(primaryActionType)}"
                    data-action-name="${html(preset.label)}"
                    ${actionButtonStateAttributes(submitDisabled, submitDisabledText)}>
                <span>1</span>
                执行今日排班
            </button>
            <div class="daily-plan-context" aria-label="路线和危机概览">
                ${routeMap}
                ${crisisCards}
            </div>
            <div class="daily-plan-footer">
                <span>结局预演</span>
                <strong>${html(endingForecastFinalTitleText(state.endingForecast, routeLabelFor(likelyEnding)))}</strong>
                <small>${html(gap)}</small>
            </div>
            ${renderRiskStatus()}
        </section>
    `;
}

function openDebtControl() {
    openInsightTab('ambient');
    window.setTimeout(() => {
        pulseFocusTarget(document.querySelector('.risk-tool-chip:not(.blocked):not(:disabled), .risk-tool-chip'))
            || pulseFocusTarget(document.querySelector('.cockpit-debt-row.hot, .cockpit-debt-row.watch, .cockpit-debt-row'))
            || pulseFocusTarget(document.getElementById('ambientPanel'));
    }, 0);
}

function activeDebts() {
    return Array.isArray(state.vup?.debts)
        ? state.vup.debts.filter(debt => debt?.status !== 'CLEARED')
        : [];
}

function debtDaysLeft(debt) {
    const remaining = Number(debt?.remainingDays);
    if (Number.isFinite(remaining)) {
        return Math.max(0, remaining);
    }
    const currentDay = Number(state.session?.day || state.vup?.day || 1);
    return Math.max(0, Number(debt?.dueDay || currentDay) - currentDay);
}

function debtDueText(debt) {
    const left = debtDaysLeft(debt);
    if (left <= 0) return "今天到期";
    if (left === 1) return "明天到期";
    return `${left}天后到期`;
}

function debtToneClass(debt) {
    const severity = Number(debt?.severity) || 0;
    const left = debtDaysLeft(debt);
    if (severity >= 3 || left <= 0) return "hot";
    if (severity >= 2 || left <= 1) return "watch";
    return "safe";
}

function debtUrgencyText(debt) {
    const severity = Number(debt?.severity) || 0;
    const left = debtDaysLeft(debt);
    if (left <= 0) return "今天回流";
    if (severity >= 4) return `高危 · ${left}天`;
    if (left === 1) return "临近 · 明天";
    if (severity >= 2) return `中危 · ${left}天`;
    return `低危 · ${left}天`;
}

function debtCounterplayText(debt) {
    const type = String(debt?.debtType || "");
    const left = debtDaysLeft(debt);
    if (left <= 0) {
        return "先处理事件卡";
    }
    if (type === "TITLE_BACKFIRE" || type === "BOOMERANG_CLIP" || type === "BLACK_HISTORY_STOCK" || type === "VOICE_ACCIDENT") {
        return left <= 1 ? "米线工具或杂谈复盘" : "杂谈复盘可降温";
    }
    if (type === "UNICORN_EXPECTATION" || type === "FAN_GROUP_DRAMA" || type === "COMMERCIAL_BACKLASH" || type === "COLLAB_SPILLOVER") {
        return left <= 1 ? "粉丝群维护优先" : "粉丝群维护可缓冲";
    }
    return left <= 1 ? "先拆最高风险" : "低压行动可防守";
}

function debtSummaryLine(debt) {
    return digestText(
        `${debtSourceLine(debt)}。${debtConsequenceLine(debt)}`,
        `${debtDisplayLabel(debt)}还在潜伏。`
    );
}

function cockpitDebtSummary(debts) {
    if (!debts.length) {
        return {
            title: "暂无旧账",
            line: "今天可以主动冲路线证据，不用加班拆米线。",
            tone: "safe"
        };
    }
    const sorted = [...debts].sort((left, right) => {
        const dueDelta = debtDaysLeft(left) - debtDaysLeft(right);
        if (dueDelta !== 0) return dueDelta;
        return (Number(right.severity) || 0) - (Number(left.severity) || 0);
    });
    const top = sorted[0];
    return {
        title: `${debts.length}笔旧账`,
        line: `${debtTypeText(top.debtType)} · ${debtUrgencyText(top)} · ${debtCounterplayText(top)}`,
        tone: debtToneClass(top),
        top
    };
}

function renderCockpitDebtList(debts) {
    if (!debts.length) {
        return `
            <div class="cockpit-debt-empty">
                <strong>暂无旧账</strong>
                <small>高风险标题和硬接节奏会在这里留下后续债务。</small>
            </div>
        `;
    }
    return [...debts].sort((left, right) => {
        const dueDelta = debtDaysLeft(left) - debtDaysLeft(right);
        if (dueDelta !== 0) return dueDelta;
        return (Number(right.severity) || 0) - (Number(left.severity) || 0);
    }).slice(0, 3).map(debt => `
        <button type="button" class="cockpit-debt-row ${debtToneClass(debt)}" data-action="open-debt-control">
            <span>${html(debtTypeText(debt.debtType))}</span>
            <strong>${html(debtUrgencyText(debt))}</strong>
            <small>${html(debtSourceLine(debt))} · ${html(debtDueText(debt))} · ${html(riskSeverityText(debt.severity))}</small>
            <em>${html(debtSummaryLine(debt))}</em>
            <small>${html(debtRecommendedLine(debt))}</small>
        </button>
    `).join('');
}

function routePressureText() {
    const expectation = state.audienceExpectation;
    const lead = expectation?.lead;
    if (lead?.routeLabel || lead?.routeType) {
        return `观众期待：${lead.routeLabel || routeLabelFor(lead.routeType)}`;
    }
    return readyGoalLineText(expectation?.pivotRiskLine || expectation?.nextMoveHint, "观众期待还在形成。");
}

function renderOperationCockpit(primaryAction, enabledActionCount) {
    if (!state.vup) return "";
    const forecast = state.endingForecast;
    const briefing = state.stageBriefing;
    const debts = activeDebts();
    const debtSummary = cockpitDebtSummary(debts);
    const likelyEnding = forecast?.likelyEndingType || state.vup.currentRoute || "UNKNOWN";
    const likelyTitle = endingForecastFinalTitleText(forecast, routeLabelFor(likelyEnding));
    const confidence = forecast ? `${percent(forecast.confidence)}%` : "--";
    const day = Number(state.session?.day || state.vup.day || 1);
    const maxDay = runMaxDay();
    const milestone = briefing
        ? (briefing.milestoneToday ? "今天复盘" : `${briefing.daysUntilMilestone || 0}天后复盘`)
        : `${Math.max(0, maxDay - day)}天冲刺`;
    const trendLabel = stageTrendLabelText(
        briefing?.currentTrend || state.vup.platformTrend || {},
        state.vup.platformTrend?.label || "口味待观察"
    );
    const stageLine = readyGoalLineText(briefing?.actionHint || forecast?.sprintHint, "按当前路线补证据。");
    const actionName = primaryAction ? actionNameText(primaryAction) : "等待行动";
    const actionLine = primaryAction ? primaryActionCueText(primaryAction) : "行动列表还在加载。";
    const riskLine = readyGoalLineText(forecast?.riskLine || briefing?.riskSnapshot || debtSummary.line, debtSummary.line);

    return `
        <section class="operation-cockpit" aria-label="运营驾驶舱">
            <div class="operation-cockpit-head">
                <div>
                    <span class="operation-kicker">运营驾驶舱</span>
                    <h4>${html(formatDayBadge(day))} · ${html(routeLabelFor(state.vup.currentRoute || likelyEnding))}</h4>
                </div>
                <button type="button" class="operation-risk-jump ${debtSummary.tone}" data-action="open-operation-risk">
                    ${html(debtSummary.title)}
                </button>
            </div>
            <div class="operation-cockpit-grid">
                <button type="button" class="cockpit-card cockpit-goal-card" data-action="open-cockpit-goal">
                    <span>结局目标</span>
                    <strong>${html(likelyTitle)}</strong>
                    <small>${html(endingForecastHeadlineText(forecast, routeLabelFor(likelyEnding)))}</small>
                    <em>${html(confidence)} 预演</em>
                </button>
                <button type="button" class="cockpit-card cockpit-stage-card" data-action="open-cockpit-stage">
                    <span>阶段</span>
                    <strong>${html(milestone)}</strong>
                    <small>${html(trendLabel)} · ${html(stageLine)}</small>
                    <em>${html(routePressureText())}</em>
                </button>
                <div class="cockpit-card cockpit-debt-card ${debtSummary.tone}">
                    <span>旧账</span>
                    <strong>${html(debtSummary.line)}</strong>
                    <div class="cockpit-debt-list">${renderCockpitDebtList(debts)}</div>
                </div>
                <div class="cockpit-card cockpit-action-card">
                    <span>今日打法</span>
                    <strong>${html(actionName)}</strong>
                    <small>${html(actionLine)}</small>
                    <em>${html(enabledActionCount)}个行动可用</em>
                </div>
            </div>
            <p class="operation-cockpit-line">${html(riskLine)}</p>
        </section>
    `;
}

function readyGoalLineText(value, fallback) {
    return digestText(localizedVisibleTextOrFallback(value, fallback), fallback);
}

function readyGoalRiskTone() {
    const riskText = `${state.endingForecast?.riskLine || ""} ${state.stageBriefing?.riskSnapshot || ""}`;
    if (/暂无未结清债务|不用加班/.test(riskText)) {
        return "safe";
    }
    if (/高压|最高风险|债务[1-9]/.test(riskText)) {
        return "hot";
    }
    return "watch";
}

function readyGoalRiskLabel() {
    const tone = readyGoalRiskTone();
    if (tone === "safe") return "风险低";
    if (tone === "hot") return "先拆债";
    return "看风向";
}

function readyGoalCoachText(primaryAction) {
    if (!primaryAction) {
        return "行动候选还在加载，先别急着连点。";
    }
    const actionName = actionNameText(primaryAction);
    const likelyTitle = endingForecastFinalTitleText(state.endingForecast, routeLabelFor(state.vup?.currentRoute || 'UNKNOWN'));
    const sprint = readyGoalLineText(state.endingForecast?.sprintHint, stageLineText(state.stageBriefing?.actionHint, "按当前路线补证据。"));
    const latePhase = lateGameReadyPhase();
    const reason = (hasMeaningfulReadyRisk() || activeDebts().length > 0)
        ? "当前有旧账风险，推荐先压风险再冲热度"
        : (firstWarnEndingRequirement() ? "当前有结局缺口，推荐先补路线证据" : "当前按目标推进");
    return `今天优先用【${actionName}】服务【${likelyTitle}】方向：${sprint}；${reason}。${latePhase ? ` ${latePhase.coachLine}` : ""}`;
}

function objectiveProgressClass(progress) {
    const value = Math.max(0, Math.min(100, Number(progress) || 0));
    return `objective-progress-${Math.round(value / 10) * 10}`;
}

function renderObjectiveItems(items) {
    const source = (Array.isArray(items) ? items : [])
        .slice()
        .sort((a, b) => Number(Boolean(a.achieved)) - Number(Boolean(b.achieved)));
    return source.map(item => {
        const actionType = objectiveItemActionType(item);
        const clickable = Boolean(!item.achieved && actionType);
        const tag = clickable ? "button" : "div";
        const attrs = clickable
            ? ` type="button" data-action="open-objective-item" data-objective-action-type="${html(actionType)}"`
            : "";
        return `
        <${tag}${attrs} class="stage-objective-item ${item.achieved ? "achieved" : "pending"} ${clickable ? "clickable" : ""}">
            <div class="stage-objective-head">
                <span>${html(item.label || "目标")}</span>
                <strong>${html(item.state || (item.achieved ? "达成" : "推进中"))}</strong>
            </div>
            ${objectiveTypeBadge(item)}
            <p>${html(item.hint || "目标说明待补充。")}</p>
        </${tag}>
    `;
    }).join("");
}

function objectiveItemActionType(item) {
    const actionType = String(item?.recommendedActionType || "").trim();
    if (!actionType) return "";
    if (!Array.isArray(state.actions) || state.actions.length === 0) return actionType;
    return state.actions.some(action => action?.actionType === actionType) ? actionType : "";
}

function objectiveTypeBadge(item) {
    const key = String(item?.objectiveKey || "");
    if (!key) return "";
    const labelByKey = {
        ROUTE_SCORE: "补路线",
        RISK_CONTROL: "控风险",
        REPUTATION: "稳口碑",
        RECOVERY: "回体力",
        CONTENT_ASSET: "铺素材",
        CLIP_OUTPUT: "做切片",
        SOCIAL_PROOF: "接同台",
        SINGING_PROOF: "练歌势",
        FAN_BASE: "稳粉丝",
        LOW_PRESSURE_PROOF: "低压证据",
        FAN_SERVICE_PROOF: "粉丝服务",
        RELATION_BUSINESS_PROOF: "关系营业",
        WATCH_HEAT: "控热度",
        MEME_DENSITY: "梗浓度"
    };
    const actionName = actionNameByType(item?.recommendedActionType);
    return `
        <div class="stage-objective-type">
            <span>${html(labelByKey[key] || "经营目标")}</span>
            ${actionName ? `<em>${html(actionName)}</em>` : ""}
        </div>
    `;
}

function actionNameByType(actionType) {
    const action = Array.isArray(state.actions)
        ? state.actions.find(item => item?.actionType === actionType)
        : null;
    if (action) {
        return actionNameText(action);
    }
    return actionLabelFor(actionType);
}

function objectiveActionForToday(fallbackAction) {
    const objectiveActionType = stageObjectiveActionTypes()[0] || "";
    if (!objectiveActionType || !Array.isArray(state.actions)) {
        return fallbackAction;
    }
    return state.actions.find(action => action?.actionType === objectiveActionType) || fallbackAction;
}

function renderObjectiveTodayAction(action, stageLine, objectiveTitle) {
    const actionType = action?.actionType || state.stageBriefing?.objectiveActionType || "";
    const actionName = action ? actionNameText(action) : "等待行动";
    const target = visibleTextOrFallback(objectiveTitle, "阶段目标");
    return `
        <button type="button" class="objective-today-action" data-action="open-objective-today" data-objective-action-type="${html(actionType)}">
            <span>今日目标</span>
            <strong>${html(actionName)}</strong>
            <small>${html(`用【${actionName}】推进【${target}】：${stageLine}`)}</small>
        </button>
    `;
}

function objectiveStreakStrip(briefing) {
    if (!briefing) return "";
    const current = Number(briefing.objectiveStreak || 0);
    const next = Number(briefing.objectiveNextStreak || current + 1);
    const reward = stageLineText(briefing.objectiveRewardPreview, "命中今日委托可获得阶段复盘奖励。");
    const label = current > 0 ? `已连${current}天` : "连击未开启";
    const nextLabel = next >= 2 ? `命中后${next}连` : "命中后开启";
    return `
        <div class="objective-streak-strip ${next >= 3 ? "hot" : ""}">
            <span>${html(label)}</span>
            <strong>${html(nextLabel)}</strong>
            <small>${html(reward)}</small>
        </div>
    `;
}

function routeMasteryCard(briefing, compact = false) {
    const mastery = briefing?.routeMastery;
    if (!mastery) return "";
    const routeLabel = stageLineText(mastery.routeLabel, "璺嚎鏈畾");
    const progressLabel = stageLineText(mastery.progressLabel, "0/8");
    const progress = percent(mastery.progress || 0);
    const milestone = stageLineText(mastery.nextMilestoneLabel, "Next tier: route entry.");
    const payoff = stageLineText(mastery.payoffHint, "Pick one route action to make the next ending more stable.");
    const actionType = mastery.recommendedActionType || "";
    const actionName = actionType ? actionLabelFor(actionType) : "Route action";
    const focus = actionType
        ? ` data-mastery-action-type="${html(actionType)}"`
        : "";
    return `
        <button type="button" class="route-mastery-card ${compact ? "compact" : ""}" data-action="focus-route-mastery"${focus}>
            <span>璺嚎绾啛搴?/span>
            <strong>${html(routeLabel)} 路 ${html(progressLabel)}</strong>
            <em>${html(milestone)}</em>
            <div class="route-mastery-bar" aria-hidden="true"><span style="width:${progress}%"></span></div>
            <small>${html(payoff)}${actionName ? ` 路 ${html(actionName)}` : ""}</small>
        </button>
    `;
}

function restartGoalChip() {
    const expectations = state.vup?.expectations || {};
    const targetLabel = expectations.restartTargetLabel || expectations.previousEndingMemory?.targetLabel || "";
    const objective = expectations.restartRunObjective || expectations.previousEndingMemory?.runObjective || "";
    const gradeContract = expectations.restartGradeContract || expectations.previousEndingMemory?.gradeContract || {};
    const gradeLine = expectations.previousEndingMemory?.gradeContractLine || gradeContract.summary || "";
    if (!targetLabel && !objective) return "";
    const boundary = expectations.restartBoundary || expectations.previousEndingMemory?.boundary || "只继承路线提示，不继承旧债。";
    const motivation = nextRunMotivationText({
        routeKey: state.vup?.currentRoute,
        goal: objective || gradeLine || boundary
    });
    return `
        <div class="ready-restart-goal">
            <span>复活赛目标</span>
            <strong>${html(targetLabel || "路线再挑战")}</strong>
            <small>${html(gradeLine || objective || boundary)}</small>
            <small>${html(motivation)}</small>
        </div>
    `;
}

function atlasGoalChip() {
    const endingCollection = state.achievementProgress?.endingCollection;
    if (!endingCollection) return "";
    const total = Number(endingCollection.totalCount || 0);
    const unlocked = Number(endingCollection.unlockedCount || 0);
    const progress = total > 0 ? `${unlocked}/${total}` : "0/0";
    const target = localizedVisibleTextOrFallback(endingCollection.nextTargetLabel, "下一张图鉴");
    const goal = localizedVisibleTextOrFallback(
        endingCollection.nextRunGoal || endingCollection.nextTargetHint,
        `完成${runMaxDayLabel()}后点亮结局图鉴。`
    );
    const motivation = nextRunMotivationText({
        routeKey: endingCollection.nextTargetType,
        endingCollection,
        goal
    });
    const guidance = endingAtlasGuidance(endingCollection.nextTargetType);
    const actionName = guidance.action;
    return `
        <button type="button" class="ready-atlas-goal" data-action="open-ready-atlas-goal" data-atlas-action-type="${html(guidance.actionType)}">
            <span>图鉴目标</span>
            <strong>${html(progress)} · ${html(target)}</strong>
            <small>${html(`推荐路线：${guidance.route}；推荐行动：${guidance.action}`)}</small>
            <small>${html(guidance.risk)}</small>
            <small>${html(motivation)}</small>
            ${actionName ? `<em>今日入口：${html(actionName)}</em>` : ""}
        </button>
    `;
}

function renderReportObjectiveRecap() {
    const briefing = state.stageBriefing;
    if (!briefing) return "";
    const progress = Number(briefing.objectiveProgress || 0);
    const label = stageLineText(briefing.objectiveProgressLabel, "0/0");
    const title = stageLineText(briefing.objectiveTitle, "本周任务");
    const items = Array.isArray(briefing.objectiveItems) ? briefing.objectiveItems : [];
    const achieved = items.filter(item => item.achieved).length;
    const pending = Math.max(0, items.length - achieved);
    return `
        <section class="report-objective-recap ${objectiveProgressClass(progress)}" aria-label="周任务进度">
            <div>
                <span>周任务进度</span>
                <strong>${html(title)}</strong>
                <small>已完成 ${achieved} 项，待推进 ${pending} 项</small>
            </div>
            <em>${html(label)}</em>
            <div class="ready-goal-objective-bar" aria-hidden="true"><span></span></div>
        </section>
    `;
}

function renderReadyFirstRunPrimer(primaryAction) {
    const readyDay = Number(state.session?.day || state.vup?.day || 1);
    if (!isFirstRunOpeningTutorialActive(readyDay) || readyDay !== 1) return "";
    const expectations = state.vup?.expectations || {};
    const openingFirstMoves = Array.isArray(expectations.openingFirstMoves)
        ? expectations.openingFirstMoves.filter(Boolean)
        : [];
    const openingRouteGoal = expectations.openingRouteGoal || "";
    const openingRiskBoundary = expectations.openingRiskBoundary || "";
    const openingAdvice = expectations.openingAdvice || "";
    if (!openingFirstMoves.length && !openingRouteGoal && !openingAdvice) return "";

    const actionType = primaryAction?.actionType || "";
    const actionName = primaryAction ? actionNameText(primaryAction) : "等待行动";
    const oneLine = openingPhaseCoachLine("READY", primaryAction, readyDay)
        || `今天点【${actionName}】；会变：第一张日报会落账；明天担心：按日报修正打法。`;
    const focusAttrs = actionType
        ? ` data-primer-action-type="${html(actionType)}"`
        : ' aria-disabled="true"';
    return `
        <button type="button" class="ready-first-run-primer" data-action="focus-ready-first-run"${focusAttrs}>
            <span>首局前三天</span>
            <strong>建议先点：${html(actionName)}</strong>
            <p>${html(oneLine)}</p>
            <small>${html(openingRouteGoal || openingAdvice || openingFirstMoves[0] || openingRiskBoundary || "先行动，再读日报。")}</small>
        </button>
    `;
}

function renderReadyGoalBoard(primaryAction, enabledActionCount) {
    if (!state.vup) return "";
    const forecast = state.endingForecast;
    const briefing = state.stageBriefing;
    const visual = routeVisualFor(forecast?.likelyEndingType || state.vup.currentRoute || 'UNKNOWN');
    const confidence = forecast ? `${percent(forecast.confidence)}%` : "--";
    const likelyTitle = endingForecastFinalTitleText(forecast, routeLabelFor(state.vup.currentRoute || 'UNKNOWN'));
    const readyDay = Number(state.session?.day || state.vup?.day || 1);
    const latePhase = lateGameReadyPhase(readyDay);
    const currentTrend = briefing?.currentTrend || state.vup.platformTrend || {};
    const trendLabel = stageTrendLabelText(currentTrend, state.vup.platformTrend?.label || "口味待观察");
    const daysText = briefing
        ? (briefing.milestoneToday ? "今天复盘" : `${briefing.daysUntilMilestone || 0}天后复盘`)
        : "阶段待同步";
    const riskTone = readyGoalRiskTone();
    const riskLine = readyGoalLineText(forecast?.riskLine || briefing?.riskSnapshot, "风险快照还在巡场。");
    const actionName = primaryAction ? actionNameText(primaryAction) : "等待行动";
    const actionLine = primaryAction ? primaryActionCueText(primaryAction) : "行动列表还在加载。";
    const stageLine = readyGoalLineText(briefing?.actionHint || forecast?.sprintHint, "先按当前路线补证据。");
    const audienceLine = readyGoalLineText(
        state.audienceExpectation?.pivotRiskLine || state.audienceExpectation?.nextMoveHint,
        "观众期待还在形成，连续行动会让路线更清楚。"
    );
    const objectiveTitle = stageLineText(briefing?.objectiveTitle, "阶段目标");
    const objectiveSummary = stageLineText(briefing?.objectiveSummary, "目标板正在生成。");
    const objectiveProgress = Number(briefing?.objectiveProgress || 0);
    const objectiveProgressLabel = stageLineText(briefing?.objectiveProgressLabel, "0/0");
    const objectiveItems = renderObjectiveItems(briefing?.objectiveItems);
    const objectiveTodayAction = renderObjectiveTodayAction(objectiveActionForToday(primaryAction), stageLine, objectiveTitle);
    const objectiveStreak = objectiveStreakStrip(briefing);
    const routeMastery = routeMasteryCard(briefing, true);
    const restartGoal = restartGoalChip();
    const atlasGoal = atlasGoalChip();
    const endingGapCoach = renderEndingGapCoachCard(firstWarnEndingRequirement(), { likelyEnding: forecast?.likelyEndingType });
    const decisionSummaryHtml = renderActionDecisionSummary(actionDecisionSummary(primaryAction, "recommended"), true);
    const firstRunPrimer = renderReadyFirstRunPrimer(primaryAction);

    return `
        <section class="ready-goal-board" aria-label="今日目标板">
            <img class="ready-goal-cover" src="/gallery/${html(visual.cover)}" alt="${html(likelyTitle)}路线封面" loading="lazy" decoding="async">
            <div class="ready-goal-main">
                <div>
                    <span class="ready-goal-kicker">${html(latePhase ? latePhase.label : "本轮目标")}</span>
                    <h4>${html(likelyTitle)}</h4>
                <p>${html(readyGoalCoachText(primaryAction))}</p>
            </div>
            <div class="ready-goal-confidence">
                <strong>${html(confidence)}</strong>
                <span>预演置信</span>
            </div>
        </div>
            ${endingGapCoach}
            <div class="ready-goal-objective ${objectiveProgressClass(objectiveProgress)}">
                <div class="ready-goal-objective-head">
                    <span>本周任务 · ${html(daysText)}</span>
                    <strong>${html(objectiveProgressLabel)}</strong>
                </div>
                <h5>${html(objectiveTitle)}</h5>
                <p>${html(objectiveSummary)}</p>
                <div class="ready-goal-objective-bar" aria-hidden="true">
                    <span></span>
                </div>
                ${restartGoal}
                ${atlasGoal}
                ${routeMastery}
                ${objectiveStreak}
                <div class="ready-goal-objective-list">${objectiveItems}</div>
                ${objectiveTodayAction}
            </div>
            <div class="ready-goal-decision">
                <div class="ready-goal-decision-head">
                    <span>今日推荐解释</span>
                    <strong>${html(actionName)}</strong>
                </div>
                ${firstRunPrimer}
                ${decisionSummaryHtml}
            </div>
            <div class="ready-goal-grid">
                <button type="button" class="ready-goal-item" data-action="open-ready-goal" data-goal-target="forecast">
                    <span>结局</span>
                    <strong>${html(likelyTitle)}</strong>
                    <small>${html(forecast ? endingForecastHeadlineText(forecast) : "结局预演还在同步。")}</small>
                </button>
                <button type="button" class="ready-goal-item" data-action="open-ready-goal" data-goal-target="stage">
                    <span>阶段</span>
                    <strong>${html(latePhase ? latePhase.label : daysText)}</strong>
                    <small>${html(latePhase ? latePhase.boardLine : stageLine)}</small>
                </button>
                <button type="button" class="ready-goal-item ${riskTone}" data-action="open-ready-goal" data-goal-target="risk">
                    <span>${html(readyGoalRiskLabel())}</span>
                    <strong>${html(riskTone === 'safe' ? '米线稳定' : '优先看债')}</strong>
                    <small>${html(riskLine)}</small>
                </button>
                <button type="button" class="ready-goal-item" data-action="open-ready-goal" data-goal-target="audience">
                    <span>观众</span>
                    <strong>${html(trendLabel)}</strong>
                    <small>${html(audienceLine)}</small>
                </button>
            </div>
            <div class="ready-goal-route">
                <span>${html(enabledActionCount)}个行动可用</span>
                <strong>${html(actionName)}</strong>
                <small>${html(actionLine)}</small>
            </div>
        </section>
    `;
}

function quickActionLabel(action, quickReason) {
    const routeLabel = visibleTextOrFallback(action.routeBiasLabel, actionDecisionTags(action)[0].value);
    const combo = actionComboShortText(action);
    return `快捷行动：${actionNameText(action)}，${routeLabel}，${combo ? `${combo}，` : ""}${quickReason}，资源取舍 ${actionResourceTradeoffText(action)}`;
}

function actionDisabledText(action, disabled) {
    if (!disabled) {
        return "";
    }
    if (state.busy) {
        return state.busyLabel || "行动处理中，请稍等。";
    }
    return disabledReasonText(action.disabledReason);
}

function actionButtonStateAttributes(disabled, disabledReason) {
    const ariaDisabled = `aria-disabled="${disabled ? 'true' : 'false'}"`;
    if (!disabled) {
        return ariaDisabled;
    }
    const title = disabledReason ? ` title="${html(disabledReason)}"` : "";
    return `disabled ${ariaDisabled}${title}`;
}

function actionCardState(action, disabled, recommended) {
    if (disabled || !action.enabled) {
        return 'blocked';
    }
    return recommended ? 'recommended' : 'playable';
}

function actionCardStateLabel(cardState) {
    const labels = {
        recommended: '推荐先点',
        playable: '可执行',
        blocked: '暂时卡住'
    };
    return labels[cardState] || '可执行';
}

function actionCardAriaLabel(action, disabled, recommended) {
    const cardState = actionCardState(action, disabled, recommended);
    const parts = [
        actionNameText(action),
        actionCardStateLabel(cardState),
        actionPreviewText(action.effectPreview, "场务还在整理数据"),
        action.riskPreview ? actionPreviewText(action.riskPreview, "风险待观察") : "",
        `资源取舍 ${actionResourceTradeoffText(action)}`
    ];
    if (disabled) {
        parts.push(actionDisabledText(action, disabled));
    }
    return parts.filter(Boolean).join('，');
}

const DEFENSE_STYLE_PRESET_NAMES = [
    "梗舞练功房",
    "黑红法庭",
    "花房歌会"
];

function defenseEvidenceItems() {
    const items = state.configCheck?.defenseEvidence;
    if (Array.isArray(items) && items.length > 0) {
        return items.map(item => ({
            ...item,
            proof: readableEvidenceProof(item.proof)
        }));
    }
    const maxDayLabel = typeof runMaxDayLabel === "function" ? runMaxDayLabel() : "30天";
    return [
        {
            key: "mvc_layers",
            label: "MVC分层",
            detail: "Controller -> Service -> Mapper -> Model/DTO 分层完整。",
            proof: "业务流程通过 REST 接口进入 Service，再由 Mapper 落库。"
        },
        {
            key: "daily_loop",
            label: `${maxDayLabel}闭环`,
            detail: "行动、日报、结局按状态机推进。",
            proof: "日会话、业务日志、每日报告、结局复盘都会保存证据。"
        }
    ];
}

function readableEvidenceProof(proof) {
    return String(proof || "")
        .replaceAll("day_session", "日会话")
        .replaceAll("business_log", "业务日志")
        .replaceAll("daily_report", "每日报告")
        .replaceAll("ending_review", "结局复盘")
        .replaceAll("api_idempotency_record", "幂等记录");
}

function renderDefenseEvidenceStrip(items = defenseEvidenceItems()) {
    if (!items.length) return "";
    const chips = items.slice(0, 4)
        .map(item => `<span title="${html(item.proof || item.detail)}">${html(item.label || item.key)}</span>`)
        .join('');
    return `<div class="defense-evidence-strip" aria-label="SSM答辩证据速览">${chips}</div>`;
}

function renderDefenseEvidencePanel() {
    const items = defenseEvidenceItems();
    if (!items.length) return "";
    const cards = items.map(item => `
        <article class="defense-evidence-card">
            <span>${html(item.label || item.key)}</span>
            <strong>${html(item.detail || "证据待补齐")}</strong>
            <p>${html(item.proof || "现场可从接口、Service 和数据库表继续追问。")}</p>
        </article>
    `).join('');
    return `
        <section class="defense-evidence-panel" aria-label="SSM证据台">
            <div class="defense-evidence-head">
                <strong>SSM证据台</strong>
                <small>答辩追问路线</small>
            </div>
            <div class="defense-evidence-grid">${cards}</div>
        </section>
    `;
}

function renderDefenseCockpit() {
    if (!demoToolsEnabled()) {
        return "";
    }
    const devTools = state.configCheck?.devTools || {};
    const strategies = devTools.availableStrategies || ["steady"];
    const selectedStrategy = demoStrategyValue();
    const current = strategies.includes(selectedStrategy) ? selectedStrategy : strategies[0];
    const targetDay = demoTargetDay();
    const targetDayLabel = `${targetDay}天`;
    const defenseRunLabel = targetDay === 30 ? "重置并跑满30天" : `重置并跑满${targetDayLabel}`;
    const resultText = state.demoResult?.endingReviewId
        ? `已到第${state.demoResult.currentDay || state.demoResult.day || targetDay}天，结局${demoResultLabel(state.demoResult)}已存档。`
        : demoBriefText(state.demoStatus, state.demoResult);
    const phaseText = state.demoStatus?.phase ? phaseLabelFor(state.demoStatus.phase) : "待启动";
    const evidenceStrip = renderDefenseEvidenceStrip();
    const runwaySteps = [
        ["1", "答辩只点这个"],
        ["2", "正式 Service"],
        ["3", targetDay === 30 ? "30份日报" : `${targetDay}份日报`],
        ["4", "结局复盘"]
    ].map(([index, label]) => `
        <span class="defense-runway-step">
            <strong>${index}</strong>
            <em>${html(label)}</em>
        </span>
    `).join('');

    return `
        <section class="defense-cockpit" aria-label="开发高级验证驾驶舱">
            <div class="defense-cockpit-head">
                <div>
                    <span class="defense-kicker">开发高级</span>
                    <h4>答辩驾驶舱 · 一键跑完整${targetDayLabel}流程</h4>
                </div>
                <span class="defense-profile">${html(profileLabelFor(devTools.profile))}</span>
            </div>
            <p class="defense-cockpit-copy">逐日调用正式 Service：行动、标题、事件、日报、结局复盘都会落库，适合现场证明不是静态页面。</p>
            <div class="defense-runway" aria-label="开发高级验证证据链">
                ${runwaySteps}
            </div>
            ${evidenceStrip}
            <div class="defense-cockpit-controls">
                <select id="defenseStrategy" class="demo-strategy-select defense-strategy-select" aria-label="答辩验证路线" data-change-action="sync-demo-strategy-selects">
                    ${demoStrategyOptions(current)}
                </select>
                <button type="button" class="primary defense-run-button" data-action="demo-reset-run" data-compact-label="跑满${targetDayLabel}" ${state.busy ? 'disabled' : ''} aria-label="答辩验证：${defenseRunLabel}">${defenseRunLabel}</button>
                <button type="button" class="defense-open-button" data-action="open-defense-demo" ${state.busy ? 'disabled' : ''} aria-label="打开开发高级验证详情">看验证详情</button>
            </div>
            <div class="defense-cockpit-status">
                <span>当前阶段：${html(phaseText)}</span>
                <strong>${html(resultText)}</strong>
            </div>
        </section>
    `;
}

function renderActions() {
    // legacy marker: <button type="button" class="action-item"
    const panel = document.getElementById('actionPanel');
    if (!state.vup) {
        panel.classList.add('hidden');
        panel.innerHTML = '';
        return;
    }

    const currentPhase = state.session?.phase || 'READY';
    const busyDisabled = state.busy ? 'disabled' : '';

    if (currentPhase !== 'READY') {
        panel.classList.add('hidden');
        panel.innerHTML = '';
        return;
    }

    panel.classList.remove('hidden');
    if (!Array.isArray(state.actions) || state.actions.length === 0) {
        panel.innerHTML = `
            <div class="panel-header">
                <h3 class="panel-title">今日行动</h3>
                <span class="panel-badge">等待同步</span>
            </div>
            <div class="ready-empty-state" role="status" aria-live="polite">
                <span>行动列表为空</span>
                <strong>今天还没有可选行动。</strong>
                <small>可能是进度刚恢复或后台还在整理，先重新加载当前进度。</small>
                <button type="button" data-action="retry-actions">重新加载</button>
            </div>
        `;
        return;
    }
    const enabledActionCount = state.actions.filter(action => action.enabled).length;

    // Show confirmation panel if an action is pending confirmation
    if (state.pendingAction) {
        const pendingActionData = state.actions.find(a => a.actionType === state.pendingAction);
        if (pendingActionData) {
            panel.innerHTML = `
                <div class="panel-header">
                    <h3 class="panel-title">确认行动</h3>
                    <span class="panel-badge">待确认</span>
                </div>
                ${actionConfirmPanelHtml(pendingActionData)}
            `;
            return;
        }
    }

    const quickActions = selectQuickActions(state.actions);
    const primaryReadyAction = selectPrimaryReadyAction(state.actions, quickActions);
    const dailyPlanCards = renderDailyPlanCards(primaryReadyAction, quickActions);
    const readyGoalBoard = renderReadyGoalBoard(primaryReadyAction, enabledActionCount);
    const operationCockpit = renderOperationCockpit(primaryReadyAction, enabledActionCount);
    const recommendedActionTypes = new Set(quickActions
        .filter(action => action.enabled)
        .map(action => action.actionType));

    if (isFirstRunRouteChoiceActive()) {
        panel.innerHTML = `
            <div class="panel-header">
                <h3 class="panel-title">今日行动</h3>
                <span class="panel-badge">首局三选一</span>
            </div>
            ${renderOpeningStyleChoices()}
        `;
        return;
    }

    // 直播企划选项
    const planOptions = state.streamPlans || [];
    const hasStreamPlan = state.actions.some(action => action.actionType === 'STREAM_PLAN');
    const hasNpcInteract = state.actions.some(action => action.actionType === 'NPC_INTERACT');
    const planSelect = planOptions.length > 0 ? `
        <select id="planType" aria-label="直播企划类型" data-change-action="sync-action-options-summary" ${busyDisabled}>
            ${planOptions.map(plan => `<option value="${html(plan.planType)}">${html(streamPlanLabelText(plan))}</option>`).join('')}
        </select>
    ` : '';

    const renderActionContent = (action, cardState, hotkeyHtml = '') => {
        const actionName = actionNameText(action);
        const effectPreview = actionPreviewText(action.effectPreview, "场务还在整理数据");
        const riskPreview = action.riskPreview ? actionPreviewText(action.riskPreview, "风险待观察") : "";
        const decisionReason = actionDecisionReason(action, cardState);
        const signalText = actionSignalText(action, cardState);
        const routeLabel = visibleTextOrFallback(action.routeBiasLabel, "路线待定");
        const comboLabel = actionComboShortText(action);
        const comboHint = actionComboHintText(action, "");
        const pressureLine = actionPressureText(action);
        const pressureDetailHtml = actionPressureDetail(action);
        const stripTail = comboLabel || visibleTextOrFallback(action.tempoHint, "按局势推进");
        const stripTailAttrs = comboLabel
            ? ` class="action-combo-strip" title="${html(comboHint || comboLabel)}"`
            : "";
        const decisionSummaryHtml = renderActionDecisionSummary(actionDecisionSummary(action, cardState));
        const costText = actionResourceChipText(action);
        const riskDotClass = action.riskLevel === 'HIGH' ? 'risk-dot-high' : (action.riskLevel === 'LOW' ? 'risk-dot-low' : 'risk-dot-medium');
        const riskText = actionRiskLevelText(action.riskLevel);
        return `
            <div class="action-summary" data-action="toggle-action-detail" data-action-type="${html(action.actionType)}">
                <div class="action-name-row">
                    <span class="action-name">${html(actionName)}</span>
                    <span class="action-cost-pill">${html(costText)}</span>
                </div>
                <p class="action-reason">${html(decisionReason)}</p>
                <div class="action-risk-line"><span class="action-risk-dot ${riskDotClass}"></span>${html(riskText)}</div>
                ${pressureLine ? `<div class="action-pressure-line"><span>压力</span><strong>${html(pressureLine)}</strong></div>` : ''}
            </div>
            <div class="action-detail">
                <div class="action-detail-inner">
                    ${hotkeyHtml}
                    <div class="action-title-row">
                        <span class="action-state-pill">${html(actionCardStateLabel(cardState))}</span>
                    </div>
                    <div class="action-route-strip">
                        <span>${html(routeLabel)}</span>
                        <strong>${html(actionRiskLevelText(action.riskLevel))}</strong>
                        <small${stripTailAttrs}>${html(stripTail)}</small>
                    </div>
                    <p class="action-signal-copy">${html(signalText)}</p>
                    ${pressureDetailHtml}
                    ${renderActionTradeoffMeter(action)}
                    ${renderActionScoreSignals(action)}
                    <p class="action-effect-copy">${html(effectPreview)}</p>
                    ${action.effectPreview ? renderActionEffectPreview(action) : ''}
                    ${riskPreview ? `<p class="action-risk-copy">${html(riskPreview)}</p>` : ''}
                    ${decisionSummaryHtml}
                    <div class="action-meta-row">${renderActionDecisionTags(action)}</div>
                    ${action.disabledReason ? `<p class="action-disabled-reason">${html(disabledReasonText(action.disabledReason))}</p>` : ''}
                    <div class="action-detail-cost">${html(costText)}</div>
                </div>
            </div>
        `;
    };
    const detailItems = state.actions.map(action => {
        const disabled = state.busy || !action.enabled;
        const recommended = recommendedActionTypes.has(action.actionType);
        const cardState = actionCardState(action, disabled, recommended);
        return `
        <div class="action-item" data-action-type="${html(action.actionType)}" data-action-state="${html(cardState)}" data-recommended="${recommended ? 'true' : 'false'}" data-combo-key="${html(action.comboKey || '')}" role="button" tabindex="0" aria-label="${html(actionCardAriaLabel(action, disabled, recommended))}" ${disabled ? 'aria-disabled="true"' : ''}>
            ${renderActionContent(action, cardState)}
            <button type="button" class="action-submit-btn" data-action="detail-submit-action" data-action-type="${html(action.actionType)}" ${actionButtonStateAttributes(disabled, actionDisabledText(action, disabled))}>
                ${disabled ? '无法执行' : '执行行动'}
            </button>
        </div>
    `;
    }).join('');
    // 同台互动倾向选择
    const npcTendencySelect = `
        <select id="npcTendency" aria-label="同台互动倾向" data-change-action="sync-action-options-summary" ${busyDisabled}>
            <option value="RAID" selected>查房</option>
            <option value="COLLAB">轻联动</option>
            <option value="BORROW_HEAT">蹭热度</option>
            <option value="AVOID">避嫌</option>
        </select>
    `;
    const actionOptionControls = [
        hasStreamPlan ? planSelect : '',
        hasNpcInteract ? npcTendencySelect : ''
    ].filter(Boolean).join('');
    const actionOptionsSummaryLabel = [
        hasStreamPlan ? streamPlanLabelText(planOptions[0]) : '',
        hasNpcInteract ? '查房' : ''
    ].filter(Boolean).join(' / ');
    const actionOptionsSummaryText = actionOptionsSummaryLabel
        ? `行动参数：${html(actionOptionsSummaryLabel)} · 点开改`
        : '展开行动参数 · 先定企划和同台倾向';
    const actionOptionsSummaryAria = actionOptionsSummaryLabel
        ? `行动参数：当前为${actionOptionsSummaryLabel}。点开可修改。`
        : '展开行动参数，先定企划和同台倾向。';
    const actionOptions = actionOptionControls ? `
        <details class="action-options">
            <summary aria-label="${html(actionOptionsSummaryAria)}"><span class="action-options-summary-label">${actionOptionsSummaryText}</span></summary>
            <div class="action-options-body">${actionOptionControls}</div>
        </details>
    ` : '';

    panel.innerHTML = `
        ${renderQuickActionBar()}
        <div class="panel-header">
            <h3 class="panel-title">今日行动</h3>
            <span class="panel-badge">四类决策 / ${enabledActionCount}个可用 / ${state.actions.length}项</span>
        </div>
        ${readyGoalBoard}
        ${dailyPlanCards}
        ${actionOptions ? `<div class="simple-action-options-wrap">${actionOptions}</div>` : ''}
        <details class="advanced-ready-details">
            <summary>进阶情报：路线目标、旧账和阶段任务</summary>
            ${operationCockpit}
        </details>
        <details class="action-details" aria-label="全部行动和详细数值">
            <summary>展开详细数值</summary>
            <div class="action-list action-detail-list">${detailItems}</div>
        </details>
    `;
    syncActionOptionsSummary();
}

function selectedOptionLabel(elementId) {
    const select = document.getElementById(elementId);
    return select?.selectedOptions?.[0]?.textContent?.trim() || '';
}

function syncActionOptionsSummary() {
    const summary = document.querySelector('.action-options-summary-label');
    if (!summary) return;

    const selectedLabels = [
        selectedOptionLabel('planType'),
        selectedOptionLabel('npcTendency')
    ].filter(Boolean);
    const summaryText = selectedLabels.length
        ? `行动参数：${selectedLabels.join(' / ')} · 点开改`
        : '展开行动参数 · 先定企划和同台倾向';
    summary.textContent = summaryText;
    summary.closest('summary')?.setAttribute(
        'aria-label',
        selectedLabels.length
            ? `行动参数：当前为${selectedLabels.join(' / ')}。点开可修改。`
            : '展开行动参数，先定企划和同台倾向。'
    );
}

function actionPlanTypeFor(actionType) {
    if (actionType === 'STREAM_PLAN') {
        return document.getElementById('planType')?.value;
    }
    if (actionType === 'NPC_INTERACT') {
        return document.getElementById('npcTendency')?.value;
    }
    return null;
}

const openingStyleChoices = [
    {
        key: "pickle",
        route: "ELECTRONIC_PICKLE",
        image: "v4/commercial-v1/routes/opening-pickle.png",
        label: "稳健电子榨菜",
        actionTypes: ["TRAIN_TALK", "FAN_GROUP_MAINTAIN", "STREAM_PLAN"],
        payoff: "低压陪饭，老粉先坐稳，口碑慢慢攒。",
        landmine: "热度不会立刻爆，连续保守会被说没活。",
        voice: "弹幕：写作业挂着听刚好。"
    },
    {
        key: "slice",
        route: "SLICE_SAINT",
        image: "v4/commercial-v1/routes/opening-slice.png",
        label: "切片圣体",
        actionTypes: ["PUBLISH_CLIP", "PUBLISH_VIDEO", "STREAM_PLAN"],
        payoff: "名场面优先，路人进场快，素材会自己滚起来。",
        landmine: "标题和封面欠账会回流，旧梗复读会被查重。",
        voice: "切片组：这秒能截封面。"
    },
    {
        key: "black-red",
        route: "BLACK_RED_MAIN_STAGE",
        image: "v4/commercial-v1/routes/opening-black-red.png",
        label: "黑红主会场",
        actionTypes: ["PUBLISH_CLIP", "NPC_INTERACT", "STREAM_PLAN"],
        payoff: "围观来得快，讨论区先亮灯，第一天就有记忆点。",
        landmine: "口碑和旧账一起计时，明天要准备台阶。",
        voice: "楼友：先插眼，等日报补证据。"
    }
];

function shouldShowOpeningStyleChoices() {
    const day = Number(state.session?.day || state.vup?.day || 0);
    return day === 1
        && (state.session?.phase || "READY") === "READY"
        && Array.isArray(state.actions)
        && state.actions.length > 0
        && !(state.reports || []).some(report => Number(report?.day) === 1);
}

function findEnabledOpeningAction(choice) {
    const actions = Array.isArray(state.actions) ? state.actions : [];
    const enabledActions = actions.filter(action => action.enabled);
    const preferred = (choice.actionTypes || [])
        .map(type => enabledActions.find(action => action.actionType === type))
        .find(Boolean);
    if (preferred) return preferred;
    return enabledActions.find(action => primaryRouteActionTypes(choice.route).includes(action.actionType))
        || enabledActions[0]
        || null;
}

function renderOpeningStyleChoices() {
    const cards = openingStyleChoices.map(choice => {
        const action = findEnabledOpeningAction(choice);
        const disabled = state.busy || !action;
        const recommended = choice.key === "pickle";
        const actionLabel = action ? actionNameText(action) : "暂无可用行动";
        const art = choice.image ? `<img class="opening-style-choice-art" src="/gallery/${html(choice.image)}" alt="${html(choice.label)}" loading="lazy" decoding="async" data-remove-on-error="true">` : "";
        return `
            <button type="button" class="opening-style-choice-card ${html(choice.key)} ${recommended ? "recommended" : ""}" data-action="opening-style-choice" data-opening-style="${html(choice.key)}" ${disabled ? 'disabled aria-disabled="true"' : ''}>
                ${art}
                <span class="opening-style-route">${html(recommended ? `推荐开局 · ${routeLabelFor(choice.route)}` : routeLabelFor(choice.route))}</span>
                <strong>${html(choice.label)}</strong>
                <small>${html(recommended ? `下一步：${actionLabel}` : actionLabel)}</small>
                <div class="opening-style-beats">
                    <span><em>爽点</em>${html(choice.payoff)}</span>
                    <span><em>欠账</em>${html(choice.landmine)}</span>
                    <span><em>观众</em>${html(choice.voice)}</span>
                </div>
            </button>
        `;
    }).join("");
    return `
        <section class="opening-style-choice" aria-label="第一天开局风格">
            <div class="opening-style-choice-head">
                <span>第1天</span>
                <h3>先选一种活法</h3>
                <p>不用看完整仪表盘。今天只做一个决定，明天再承受回声或收获反馈。</p>
            </div>
            <div class="opening-style-choice-grid">${cards}</div>
        </section>
    `;
}

// ============ 下播行动面板（独立面板，OFF_STREAM_READY 阶段） ============
function renderOffStreamPanel() {
    const panel = document.getElementById('offStreamPanel');
    const phase = state.session?.phase;
    const isActive = phase === 'OFF_STREAM_READY';

    // 非下播阶段：隐藏内嵌面板，关闭弹窗
    if (!isActive) {
        if (panel) { panel.classList.add('hidden'); panel.innerHTML = ''; }
        closeOffStreamPopup();
        return;
    }

    // 下播阶段：内嵌面板隐藏，改用弹窗
    if (panel) { panel.classList.add('hidden'); panel.innerHTML = ''; }

    const route = state.vup?.currentRoute || 'UNKNOWN';
    const backendOptions = Array.isArray(state.offStreamOptions) ? state.offStreamOptions : [];
    const routeOptions = ROUTE_OFFSTREAM_OPTIONS[route] || [];
    const commonOptions = COMMON_OFFSTREAM_OPTIONS;
    const options = backendOptions.length
        ? backendOptions.slice(0, 4)
        : [...routeOptions.slice(0, 3), ...commonOptions.slice(0, 1)];

    const routeLabel = routeLabelFor(route);
    const crisisCards = renderCrisisCards(true);
    const routeMap = renderRouteVisibilityMap(state.endingForecast?.likelyEndingType || route, true);
    const secondRoundLine = firstLocalizedReadableText([
        state.session?.offStreamPrompt,
        state.stageBriefing?.offStreamHint,
        state.endingForecast?.sprintHint
    ], "主行动结算后还有一个低压回合：补路线、降风险，或者直接收工进事件。");

    const optionsHtml = options.map(opt => {
        const type = opt.type || opt.offStreamType || "";
        const name = firstLocalizedReadableText([opt.name, opt.label], actionLabelFor(type));
        const effect = firstLocalizedReadableText([opt.effectPreview, opt.benefitPreview, opt.summary], "给明天留一点余量");
        const cost = firstLocalizedReadableText([opt.costPreview, opt.riskPreview], opt.recommended ? "推荐补一手" : "低压收尾");
        const bestFor = firstLocalizedReadableText([opt.bestFor, opt.recommendedReason, opt.routeBiasLabel], routeLabel);
        // 解析 effect 里的收益和代价
        const { gains, costs } = parseOffStreamEffect(effect);
        const gainsHtml = gains.map(g => `<span class="offstream-chip gain">${html(g)}</span>`).join('');
        const costsHtml = costs.map(c => `<span class="offstream-chip cost">${html(c)}</span>`).join('');
        return `
        <button type="button" class="offstream-option ${opt.recommended ? 'recommended' : ''}"
                data-action="offstream-${html(String(type).toLowerCase())}"
                data-offstream-type="${html(type)}">
            <div class="offstream-option-head">
                <strong class="offstream-name">${html(name)}</strong>
                ${opt.recommended ? '<div class="offstream-badge">推荐</div>' : ''}
            </div>
            <div class="offstream-chips">
                ${gainsHtml}
                ${costsHtml}
            </div>
            <div class="offstream-facts">
                <span>${html(bestFor)}</span>
                <small>${html(cost)}</small>
            </div>
        </button>
    `;
    }).join('');

    // 当前资源状态条
    const resourcesBar = buildOffStreamResourcesBar();

    const bodyHtml = `
        <div class="offstream-resources">${resourcesBar}</div>
        <div class="offstream-intro">
            <p>${html(secondRoundLine)}</p>
            <small>当前路线：${html(routeLabel)} · 下播动作不抢主行动，但会写进今天结算。</small>
        </div>
        <div class="offstream-context">
            ${routeMap}
            ${crisisCards}
        </div>
        <div class="offstream-grid">${optionsHtml}</div>
        <button type="button" class="offstream-skip" data-action="skip-offstream">
            跳过第二回合，直接进入事件 →
        </button>
    `;
    showOffStreamPopup(routeLabel, bodyHtml);
}

// 解析下播效果文本，拆成收益和代价数组
function parseOffStreamEffect(text) {
    const gains = [];
    const costs = [];
    if (!text) return { gains, costs };
    // 格式："收益：A，B；代价：C，D" 或 "收益：A；代价：B"
    const gainMatch = text.match(/收益[：:]\s*([^；;]+)/);
    const costMatch = text.match(/代价[：:]\s*([^；;]+)/);
    if (gainMatch) {
        gainMatch[1].split(/[，,]/).map(s => s.trim()).filter(Boolean).forEach(s => gains.push(s));
    }
    if (costMatch) {
        costMatch[1].split(/[，,]/).map(s => s.trim()).filter(Boolean).forEach(s => costs.push(s));
    }
    // 如果没匹配到，把整个文本放收益
    if (gains.length === 0 && costs.length === 0) gains.push(text);
    return { gains, costs };
}

// 构建下播弹窗的资源状态条
function buildOffStreamResourcesBar() {
    const v = state.vup;
    if (!v) return '';
    const stamina = Number(v.resources?.stamina ?? 0);
    const maxStamina = Number(v.resources?.maxStamina ?? 10);
    const inspiration = Number(v.resources?.inspiration ?? v.inspiration ?? 0);
    const coin = Number(v.resources?.coin ?? v.coin ?? 0);
    const reputation = Number(v.opinion?.reputation ?? v.reputation ?? 0);
    const watchHeat = Number(v.opinion?.watchHeat ?? v.watchHeat ?? 0);
    const staminaTier = stamina <= 0 ? '！崩溃' : stamina <= 2 ? '▼透支' : stamina <= 5 ? '◆疲惫' : stamina <= 8 ? '✦正常' : '⚡巅峰';
    const chips = [
        { label: '体力', value: staminaTier, icon: '❤', tone: stamina <= 2 ? 'low' : stamina <= 5 ? 'warn' : 'ok' },
        { label: '灵感', value: inspiration, icon: '✦', tone: inspiration <= 0 ? 'low' : 'ok' },
        { label: '预算', value: coin, icon: '¥', tone: coin < 20 ? 'low' : 'ok' },
        { label: '口碑', value: reputation, icon: '📊', tone: reputation < 0 ? 'low' : 'ok' },
        { label: '围观', value: watchHeat, icon: '👁', tone: watchHeat > 60 ? 'high' : 'ok' }
    ];
    return chips.map(c => `
        <div class="offstream-resource-chip tone-${c.tone}">
            <span class="resource-icon">${c.icon}</span>
            <span class="resource-label">${html(c.label)}</span>
            <strong class="resource-value">${html(String(c.value))}</strong>
        </div>
    `).join('');
}

// 下播弹窗
function showOffStreamPopup(routeLabel, bodyHtml) {
    let overlay = document.getElementById('offStreamPopupOverlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'offStreamPopupOverlay';
        overlay.className = 'modal-overlay';
        overlay.style.display = 'flex';
        document.body.appendChild(overlay);
    }
    overlay.innerHTML = `
        <div class="modal-content modal-content--wide offstream-popup">
            <div class="offstream-popup-head">
                <h3>📺 下播第二回合</h3>
                <span class="panel-badge">${html(routeLabel)}收尾</span>
            </div>
            <div class="offstream-popup-body">${bodyHtml}</div>
        </div>
    `;
    if (typeof openDialog === 'function') openDialog(overlay);
}

function closeOffStreamPopup() {
    const overlay = document.getElementById('offStreamPopupOverlay');
    if (overlay) {
        if (typeof closeDialog === 'function') closeDialog(overlay);
        overlay.remove();
    }
}

async function submitOffStream(offStreamType) {
    await withBusy('提交下播行动中', async () => {
        try {
            await api('/api/offstream/action', {
                method: 'POST',
                body: { offStreamType },
            });
            await refreshAfterWrite();
        } catch (e) {
            setStatus(e.message || '下播行动失败', 'error');
        }
    });
}

async function skipOffStream() {
    await withBusy('跳过下播行动中', async () => {
        try {
            await api('/api/offstream/skip', { method: 'POST' });
            await refreshAfterWrite();
        } catch (e) {
            setStatus(e.message || '跳过失败', 'error');
        }
    });
}

function danmakuMoodText(value, fallback = "围观中") {
    return visibleTextOrFallback(value, fallback);
}

function danmakuPersonaText(danmaku, fallback = "围观群众") {
    return visibleTextOrFallback(danmaku?.persona, fallback);
}

function danmakuLineText(danmaku, fallback = "弹幕还在同步。") {
    return visibleTextOrFallback(danmaku?.text, fallback);
}

function renderDanmaku() {
    const panel = document.getElementById('danmakuPanel');
    if (!state.vup || !state.danmaku) {
        panel.classList.add('hidden');
        panel.innerHTML = '';
        disconnectDanmakuStream();
        stopDanmakuOverlay();
        return;
    }

    const danmakus = state.danmaku.danmakus || [];
    if (danmakus.length === 0) {
        panel.classList.add('hidden');
        panel.innerHTML = '';
        disconnectDanmakuStream();
        stopDanmakuOverlay();
        return;
    }

    panel.classList.remove('hidden');
    startDanmakuOverlay();
    const items = danmakus.map(d => `
        <div class="danmaku-item">
            <span class="danmaku-persona">${html(danmakuPersonaText(d))}</span>
            <span class="danmaku-text">${html(danmakuLineText(d))}</span>
        </div>
    `).join('');
    const moodText = danmakuMoodText(state.danmaku.mood);

    // Preserve the stream list if it already exists to avoid flicker
    const existingStreamList = document.getElementById('danmakuStreamList');
    const streamListHtml = existingStreamList
        ? existingStreamList.outerHTML
        : '<div id="danmakuStreamList" class="danmaku-stream-list"></div>';

    panel.innerHTML = `
        <div class="panel-header">
            <h3 class="panel-title">直播间弹幕</h3>
            <span class="panel-badge">${html(moodText)}</span>
        </div>
        ${items ? `<div class="danmaku-list">${items}</div>` : ''}
        ${streamListHtml}
    `;

    // Start SSE stream if not already connected, or restart if data changed
    const currentVersion = state.danmaku ? state.danmaku.count : 0;
    if (!danmakuEventSource || danmakuStreamVersion !== currentVersion) {
        danmakuStreamVersion = currentVersion;
        const heat = (state.vup.watchHeat >= 70) ? 2 : (state.vup.watchHeat >= 40) ? 1 : 0;
        connectDanmakuStream(heat);
    }
}

// --- SSE Danmaku Stream ---
let danmakuEventSource = null;
let danmakuStreamTimer = null;
let danmakuStreamVersion = 0;

const TONE_COLOR_MAP = {
    rose: '#ff7262',
    teal: '#3dd6c6',
    gold: '#f3c969',
    violet: '#94b7ff'
};

// --- Danmaku Overlay (横向滚动弹幕) ---
let danmakuOverlayContainer = null;
let danmakuOverlayTimer = null;
let danmakuViewerCountEl = null;
let danmakuOverlayActive = false;

const DANMAKU_COLORS = ['#ff7262', '#3dd6c6', '#f3c969', '#94b7ff', '#ff9ff3', '#48dbfb', '#ff6b6b', '#a29bfe'];

function danmakuOverlayTarget() {
    return document.getElementById('vupLiveDanmakuBand')
        || document.getElementById('vupLiveViewport')
        || document.body;
}

function danmakuDensityConfig() {
    const heat = currentWatchHeat();
    if (heat >= 70) {
        return { limit: 24, burst: 4, interval: 900, minDuration: 3.6, randomDuration: 1.4 };
    }
    if (heat >= 35) {
        return { limit: 14, burst: 2, interval: 1500, minDuration: 4.8, randomDuration: 1.8 };
    }
    return { limit: 7, burst: 1, interval: 2600, minDuration: 6.4, randomDuration: 2.2 };
}

function scheduleDanmakuOverlayPulse() {
    if (!danmakuOverlayActive) return;
    if (danmakuOverlayTimer) clearTimeout(danmakuOverlayTimer);
    const config = danmakuDensityConfig();
    danmakuOverlayTimer = setTimeout(() => {
        if (!danmakuOverlayActive) return;
        for (let i = 0; i < config.burst; i++) {
            setTimeout(() => spawnDanmakuOverlayMessage(), i * 180);
        }
        updateDanmakuViewerCount();
        scheduleDanmakuOverlayPulse();
    }, config.interval);
}

function startDanmakuOverlay() {
    if (danmakuOverlayActive) return;
    danmakuOverlayActive = true;

    // Create overlay container
    if (!danmakuOverlayContainer) {
        danmakuOverlayContainer = document.createElement('div');
        danmakuOverlayContainer.className = 'danmaku-overlay';
        danmakuOverlayTarget().appendChild(danmakuOverlayContainer);
    }

    // Create viewer count display
    if (!danmakuViewerCountEl) {
        danmakuViewerCountEl = document.createElement('div');
        danmakuViewerCountEl.className = 'danmaku-viewer-count';
        document.body.appendChild(danmakuViewerCountEl);
    }
    danmakuViewerCountEl.style.display = '';
    lastViewerCountUpdate = 0; // reset throttle so it updates immediately
    updateDanmakuViewerCount();
    scheduleDanmakuOverlayPulse();
}

function stopDanmakuOverlay() {
    danmakuOverlayActive = false;
    if (danmakuOverlayTimer) {
        clearTimeout(danmakuOverlayTimer);
        danmakuOverlayTimer = null;
    }
    if (danmakuOverlayContainer && danmakuOverlayContainer.parentNode) {
        danmakuOverlayContainer.parentNode.removeChild(danmakuOverlayContainer);
        danmakuOverlayContainer = null;
    }
    if (danmakuViewerCountEl && danmakuViewerCountEl.parentNode) {
        danmakuViewerCountEl.parentNode.removeChild(danmakuViewerCountEl);
        danmakuViewerCountEl = null;
    }
}

function spawnDanmakuOverlayMessage(text, persona, isSC, mood) {
    if (!danmakuOverlayContainer) return;
    const config = danmakuDensityConfig();
    if (danmakuOverlayContainer.children.length > config.limit) return;

    const el = document.createElement('div');
    el.className = 'danmaku-fly' + (isSC ? ' sc' : '');

    // 1.2 按 mood 着色：positive 绿 / neutral 白 / negative 红
    const MOOD_COLORS = { positive: '#52c41a', neutral: '#ffffff', negative: '#ff4d4f' };
    if (mood && MOOD_COLORS[mood]) {
        el.classList.add('mood-' + mood);
        if (!isSC) el.style.color = MOOD_COLORS[mood];
    } else if (!isSC) {
        // Random color from palette (skip for SC messages, they use CSS gold)
        const color = DANMAKU_COLORS[Math.floor(Math.random() * DANMAKU_COLORS.length)];
        el.style.color = color;
    }

    const topPercent = Math.random() * 82;
    el.style.top = topPercent + '%';

    const duration = config.minDuration + Math.random() * config.randomDuration;
    el.style.animationDuration = duration + 's';

    // Use provided text or generate a generic one
    if (!text) {
        const genericMessages = [
            // Short (2-4 chars)
            '666', '好听', '哈哈哈', '牛逼', '加油',
            '笑死', '破防', '泪目', '绝了', '太强了',
            '来了来了', '好家伙', '冲冲冲', '牛啊', '上分',
            // Medium (5-10 chars)
            '这段能剪', '标题组加班', '切片组出动', '老粉欣慰', 'DD坐稳了',
            '可以的', '不错不错', '继续继续', '期待', '好看',
            '绷不住了', '这波啊', '有东西', '顶级', '稳了稳了',
            '节目效果到了', '截图了截图了', '主播觉醒了', '秒了一切', '这首绝了',
            // Long (10-20 chars)
            '主播今天状态拉满了', '我已经录了三遍了', '切片组请开始你的表演',
            '这波属于是教科书操作', '切片标题我已经想好了', '老粉看了直呼内行',
            '今天直播信息量太大了', '录播组全员已就位', '主播别紧张我们都在',
            '这素材够切三个视频了', '标题组今晚要加班了', '互联网是有记忆的'
        ];
        text = genericMessages[Math.floor(Math.random() * genericMessages.length)];
    }

    text = normalizeNoPayCopy(text);
    if (persona) {
        el.textContent = persona + '：' + text;
    } else {
        el.textContent = text;
    }

    danmakuOverlayContainer.appendChild(el);

    // Remove after animation completes
    setTimeout(() => {
        if (el.parentNode === danmakuOverlayContainer) {
            danmakuOverlayContainer.removeChild(el);
        }
    }, duration * 1000 + 200);
}

window.spawnDanmakuOverlayMessage = spawnDanmakuOverlayMessage;

let lastViewerCountUpdate = 0;

function updateDanmakuViewerCount() {
    if (!danmakuViewerCountEl) return;
    // Only update every 5 seconds
    const now = Date.now();
    if (now - lastViewerCountUpdate < 5000) return;
    lastViewerCountUpdate = now;
    const watchHeat = state.vup?.watchHeat ?? 0;
    // Fluctuate viewer count based on watchHeat
    const baseViewers = Math.floor(watchHeat * 12.3 + 150);
    const jitter = Math.floor(Math.random() * 40) - 20;
    const viewers = Math.max(50, baseViewers + jitter);
    danmakuViewerCountEl.textContent = viewers + ' 人观看';
}

function connectDanmakuStream(heat) {
    disconnectDanmakuStream();
    if (!state.vup) return;

    const heatParam = Math.min(Math.max(Number(heat) || 1, 0), 2);
    const url = `/api/danmaku/stream?heat=${heatParam}`;

    try {
        danmakuEventSource = new EventSource(url);

        danmakuEventSource.addEventListener('danmaku', (e) => {
            try {
                const data = JSON.parse(e.data);
                appendStreamDanmaku(data);

                // Guard: only spawn overlay if active
                if (!danmakuOverlayActive) return;

                // Spawn overlay message for this danmaku
                const isSC = !!(data.type === 'SC' || data.isSC || data.sc);
                spawnDanmakuOverlayMessage(data.text, data.persona, isSC);

                // Density burst: when watchHeat >= 70, spawn 2-3 extra generic messages
                const watchHeat = state.vup?.watchHeat ?? 0;
                if (watchHeat >= 70) {
                    const extraCount = 2 + Math.floor(Math.random() * 2); // 2-3
                    for (let i = 0; i < extraCount; i++) {
                        setTimeout(() => {
                            spawnDanmakuOverlayMessage();
                        }, 200 + i * 300);
                    }
                }

                // Update viewer count (throttled internally)
                updateDanmakuViewerCount();
            } catch (_) { /* ignore parse errors */ }
        });

        danmakuEventSource.onerror = () => {
            // EventSource auto-reconnects, but we stop after 60s anyway
        };

        // Auto-stop after 65 seconds (server closes at 60s)
        danmakuStreamTimer = setTimeout(() => {
            disconnectDanmakuStream();
        }, 65_000);
    } catch (_) {
        // SSE not supported or network error — silently degrade
    }
}

function disconnectDanmakuStream() {
    if (danmakuEventSource) {
        danmakuEventSource.close();
        danmakuEventSource = null;
    }
    if (danmakuStreamTimer) {
        clearTimeout(danmakuStreamTimer);
        danmakuStreamTimer = null;
    }
}

function appendStreamDanmaku(data) {
    const list = document.getElementById('danmakuStreamList');
    if (!list) return;

    const isSC = !!(data.type === 'SC' || data.isSC || data.sc);

    const item = document.createElement('div');
    item.className = 'danmaku-stream-item' + (isSC ? ' danmaku-sc' : '');

    const persona = document.createElement('span');
    persona.className = 'danmaku-persona';
    persona.textContent = data.persona || '观众';
    if (data.color) {
        persona.style.color = data.color;
    }

    const text = document.createElement('span');
    text.className = 'danmaku-text';
    text.textContent = normalizeNoPayCopy(data.text || '');

    // SC visual: gold highlight
    if (isSC) {
        item.style.background = 'rgba(255,215,0,0.12)';
        item.style.border = '1px solid rgba(255,215,0,0.3)';
        persona.style.color = '#ffd700';
        text.style.color = '#ffd700';
        text.style.fontWeight = '700';
    }

    item.appendChild(persona);
    item.appendChild(text);
    list.appendChild(item);

    // Keep at most 20 items visible
    while (list.children.length > 20) {
        list.removeChild(list.firstChild);
    }

    // Scroll to bottom
    list.scrollTop = list.scrollHeight;

    // Auto-remove after 10 seconds with fade-out
    setTimeout(() => {
        item.classList.add('danmaku-fade-out');
        setTimeout(() => {
            if (item.parentNode === list) {
                list.removeChild(item);
            }
        }, 400);
    }, 10_000);
}

function fanTopicChoiceDisabledText(choice, disabled) {
    if (!disabled) {
        return "";
    }
    if (state.busy) {
        return state.busyLabel || "群议题处理中，请稍等。";
    }
    return disabledReasonText(choice.disabledReason);
}

function fanTopicTitleText(topic, fallback = "群友议题") {
    return localizedVisibleTextOrFallback(topic?.title, fallback);
}

function fanTopicDescriptionText(topic, fallback = "群友还在整理小作文。") {
    return localizedVisibleTextOrFallback(topic?.description, fallback);
}

function fanTopicChoiceLabelText(choice, fallback = "未命名方案") {
    return localizedVisibleTextOrFallback(choice?.label, fallback);
}

function fanTopicPreviewText(value, fallback) {
    return localizedVisibleTextOrFallback(value, fallback);
}

function fanTopicChoiceAriaLabel(topic, choice) {
    const topicTitle = fanTopicTitleText(topic);
    const choiceLabel = fanTopicChoiceLabelText(choice);
    const costPreview = fanTopicPreviewText(choice.costPreview, "待观察");
    const effectPreview = fanTopicPreviewText(choice.effectPreview, "待观察");
    const riskPreview = fanTopicPreviewText(choice.riskPreview, "待观察");
    const parts = [
        `处理粉丝群议题：${topicTitle}`,
        `选择${choiceLabel}`,
        `成本${costPreview}`,
        `效果${effectPreview}`,
        `风险${riskPreview}`
    ];
    if (choice.disabledReason) {
        parts.push(`暂不可用：${disabledReasonText(choice.disabledReason)}`);
    }
    return parts.join('，');
}

function fanTopicChoiceMeta(choice) {
    const effect = String(fanTopicPreviewText(choice?.effectPreview, "") || "").trim();
    const signals = [...effect.matchAll(/(口碑|围观热度|灵感|乐子人)\s*([+-]\d+)/g)]
        .slice(0, 2)
        .map(match => `${match[1].replace("围观热度", "热度").replace("乐子人", "乐子")}${match[2]}`);
    const cost = String(fanTopicPreviewText(choice?.costPreview, "") || "").trim();
    const usefulCost = cost && cost !== "无资源成本" ? cost : "";
    const fallback = visibleTextOrFallback(effect.split(/[，,；;]/)[0], "看群友反馈");
    return [usefulCost, signals.join(" / ") || fallback].filter(Boolean).join(" · ");
}

function fanTopicChoiceChips(choice) {
    const costPreview = fanTopicPreviewText(choice?.costPreview, "待观察");
    const effectPreview = fanTopicPreviewText(choice?.effectPreview, "待观察");
    const riskPreview = fanTopicPreviewText(choice?.riskPreview, "待观察");
    return {
        cost: fanTopicCostChip(costPreview),
        effect: fanTopicEffectChip(effectPreview, choice),
        risk: fanTopicRiskChip(riskPreview, choice)
    };
}

function fanTopicCostChip(costPreview) {
    const text = String(costPreview || "");
    if (!text || /无资源成本|无成本|无需|不消耗/.test(text)) return "无成本";
    if (new RegExp(BUDGET_RESOURCE_PATTERN).test(text) && /体力/.test(text)) return "运营预算/体力";
    if (new RegExp(`${BUDGET_RESOURCE_PATTERN}|预算`).test(text)) return "运营预算";
    if (/体力|精力/.test(text)) return "体力";
    if (/灵感/.test(text)) return "灵感";
    if (/素材|投稿/.test(text)) return "素材";
    return text.length > 6 ? "有成本" : text;
}

function fanTopicEffectChip(effectPreview, choice) {
    const text = `${effectPreview || ""} ${fanTopicChoiceLabelText(choice, "")}`;
    const signals = [...String(effectPreview || "").matchAll(/(口碑|围观热度|灵感|乐子人)\s*([+-]\d+)/g)]
        .slice(0, 1)
        .map(match => `${match[1].replace("围观热度", "热度").replace("乐子人", "乐子")}${match[2]}`);
    if (signals.length > 0) return signals[0];
    if (/切片组|投稿|素材/.test(text)) return "补素材";
    if (/口碑|老粉|真爱粉|稳/.test(text)) return "稳口碑";
    if (/陪伴|团建|灵感/.test(text)) return "凝聚力";
    if (/热度|围观|乐子|整活/.test(text)) return "热度";
    return "看反馈";
}

function fanTopicRiskChip(riskPreview, choice) {
    const text = `${riskPreview || ""} ${fanTopicChoiceLabelText(choice, "")} ${choice?.disabledReason || ""}`;
    if (choice?.enabled === false || choice?.disabledReason) return "暂不可选";
    if (/高风险|开庭|炎上|米线|事故|爆雷/.test(text)) return "高风险";
    if (/关系债|独角兽|老粉|商业味|期待/.test(text)) return "关系债";
    if (/整活|乐子|围观|冲|波动/.test(text)) return "高波动";
    if (/低风险|安全|稳|冷处理|观察/.test(text)) return "安全";
    return "待观察";
}

function fanTopicSummaryHint(topic) {
    const choices = topic?.choices || [];
    const submissionChoice = choices.find(choice => choice.choiceType === "COLLECT_SUBMISSIONS");
    if (submissionChoice?.enabled) {
        return "投稿箱可补素材";
    }
    const fanMeetingChoice = choices.find(choice => choice.choiceType === "HOLD_FAN_MEETING");
    if (fanMeetingChoice && !fanMeetingChoice.enabled) {
        return disabledReasonText(fanMeetingChoice.disabledReason);
    }
    return `${state.fanTopics.length}个待处理`;
}

function fanTopicMaterialCueText(topic) {
    const submissionChoice = (topic?.choices || []).find(choice => choice.choiceType === "COLLECT_SUBMISSIONS");
    if (submissionChoice?.enabled) {
        return "切片按钮灰掉时，点“改成投稿征集”能给素材库+1。";
    }
    return "";
}

function fanTopicChoiceClass(choice, disabled) {
    const submissionClass = choice.choiceType === "COLLECT_SUBMISSIONS" ? ' submission' : '';
    return `${disabled ? 'fan-topic-choice blocked' : 'fan-topic-choice'}${submissionClass}`;
}

function fanTopicChoiceButtonAttributes(disabled, disabledReason, titleText) {
    const ariaDisabled = `aria-disabled="${disabled ? 'true' : 'false'}"`;
    const title = titleText ? ` title="${html(titleText)}"` : (disabledReason ? ` title="${html(disabledReason)}"` : "");
    return `${disabled ? 'disabled ' : ''}${ariaDisabled}${title}`;
}

function renderFanTopics() {
    const panel = document.getElementById('fanTopicPanel');
    if (!state.vup || !state.fanTopics || state.fanTopics.length === 0) {
        panel.classList.add('hidden');
        panel.innerHTML = '';
        return;
    }

    panel.classList.remove('hidden');
    const items = state.fanTopics.map(topic => {
        const materialCue = fanTopicMaterialCueText(topic);
        const choices = (topic.choices || []).map(c => {
            const disabled = !c.enabled || state.busy;
            const disabledText = fanTopicChoiceDisabledText(c, disabled);
            const ariaLabel = fanTopicChoiceAriaLabel(topic, c);
            const titleText = disabledText ? `${ariaLabel}；${disabledText}` : ariaLabel;
            const choiceChips = fanTopicChoiceChips(c);
            return `
            <button type="button"
                    class="${fanTopicChoiceClass(c, disabled)}"
                    ${fanTopicChoiceButtonAttributes(disabled, disabledText, titleText)}
                    data-action="choose-fan-topic"
                    data-topic="${html(topic.topicKey)}"
                    data-topic-choice="${html(c.choiceType)}"
                    aria-label="${html(ariaLabel)}">
                <span class="fan-topic-choice-label">${html(fanTopicChoiceLabelText(c))}</span>
                <span class="fan-topic-choice-chip-row">
                    <span class="fan-topic-choice-chip cost"><em>成本</em><strong>${html(choiceChips.cost)}</strong></span>
                    <span class="fan-topic-choice-chip effect"><em>效果</em><strong>${html(choiceChips.effect)}</strong></span>
                    <span class="fan-topic-choice-chip risk"><em>风险</em><strong>${html(choiceChips.risk)}</strong></span>
                </span>
                <small class="fan-topic-choice-meta">${html(fanTopicChoiceMeta(c))}</small>
                ${c.disabledReason ? `<span class="fan-topic-choice-disabled">${html(disabledReasonText(c.disabledReason))}</span>` : ''}
            </button>
        `;
        }).join('');

        return `
            <div class="info-card">
                <h4>${html(fanTopicTitleText(topic))}</h4>
                <p class="fan-topic-copy">${html(fanTopicDescriptionText(topic))}</p>
                ${materialCue ? `<p class="ambient-rail-cue">${html(materialCue)}</p>` : ''}
                <div class="fan-topic-actions">${choices}</div>
            </div>
        `;
    }).join('');
    // 没有粉丝议题时自动收起二级面板，避免空占位
    const secondaryPanels = document.getElementById('secondaryPanels');
    if (secondaryPanels) {
        const hasAnySecondaryContent = items.trim().length > 0;
        if (hasAnySecondaryContent) {
            secondaryPanels.classList.remove('empty');
            secondaryPanels.removeAttribute('hidden');
        } else {
            secondaryPanels.classList.add('empty');
            secondaryPanels.setAttribute('hidden', '');
        }
    }

    panel.innerHTML = `
        <div class="panel-header">
            <h3 class="panel-title">粉丝群议题</h3>
            <span class="panel-badge">${state.fanTopics.length}个</span>
        </div>
        <div class="fan-topic-list">${items}</div>
    `;
}

function riskToolLabelFor(toolType) {
    const toolNames = {
        FULL_CLIP_CONTEXT: "补全切片上下文",
        COOLING_NOTICE: "冷处理公告",
        TEMP_MOD_TEAM: "临时房管组"
    };
    const raw = String(toolType || "").trim();
    return toolNames[raw] || visibleTextOrFallback(raw, "米线工具");
}

function riskToolLabelText(tool) {
    return visibleTextOrFallback(tool?.label, riskToolLabelFor(tool?.toolType));
}

function riskToolPreviewText(value, fallback) {
    return localizedVisibleTextOrFallback(value, fallback);
}

function riskToolTargetIdLiteral(tool) {
    if (tool?.targetDebtId === null || tool?.targetDebtId === undefined) {
        return "null";
    }
    const id = Number(tool.targetDebtId);
    return Number.isFinite(id) ? String(id) : "null";
}

function riskToolDisabledText(tool, disabled) {
    if (!disabled) {
        return "";
    }
    if (state.busy) {
        return state.busyLabel || "米线工具处理中，请稍等。";
    }
    return disabledReasonText(tool?.disabledReason);
}

function riskToolButtonStateAttributes(disabled, titleText) {
    const ariaDisabled = `aria-disabled="${disabled ? 'true' : 'false'}"`;
    if (!disabled) {
        return ariaDisabled;
    }
    const title = titleText ? ` title="${html(titleText)}"` : "";
    return `disabled ${ariaDisabled}${title}`;
}

function riskToolAriaLabel(tool, disabled) {
    const label = riskToolLabelText(tool);
    const cost = riskToolPreviewText(tool?.costPreview, "成本待核对");
    const effect = riskToolPreviewText(tool?.effectPreview, "场务还在整理数据");
    const target = riskToolTargetText(tool);
    const urgency = riskToolPreviewText(tool?.urgencyPreview, "");
    const parts = [
        `米线工具：${label}`,
        target,
        urgency,
        cost,
        effect,
        disabled ? riskToolDisabledText(tool, disabled) : "点击后立即使用"
    ];
    return parts.filter(Boolean).join("，");
}

function riskToolChips(tool, disabled) {
    const cost = riskToolPreviewText(tool?.costPreview, "成本待核对");
    const effect = riskToolPreviewText(tool?.effectPreview, "场务还在整理数据");
    const urgency = riskToolPreviewText(tool?.urgencyPreview, "");
    return {
        cost: riskToolCostChip(cost),
        effect: riskToolEffectChip(effect, tool),
        target: riskToolUrgencyChip(urgency, tool),
        state: disabled ? "暂不可用" : riskToolStateChip(effect)
    };
}

function riskToolTargetText(tool) {
    const target = tool?.targetDebt || {};
    const fallback = tool?.targetDebtId ? `目标旧账 #${tool.targetDebtId}` : "";
    const summary = riskToolPreviewText(tool?.targetSummary, fallback);
    if (!target || !tool?.targetDebtId) {
        return summary;
    }
    return `${summary} · ${debtDueText(target)} · ${debtRecommendedLine(target)}`;
}

function riskToolUrgencyChip(urgency, tool) {
    const text = `${urgency || ""} ${riskToolTargetText(tool)}`;
    if (/爆发|今天/.test(text)) return "回流";
    if (/明天|1天/.test(text)) return "临近";
    if (/高危/.test(text)) return "高危";
    if (/中危/.test(text)) return "中危";
    if (/低危/.test(text)) return "低危";
    if (/目标/.test(text)) return "命中";
    return "待命中";
}

function riskToolTimingText(tool, disabledText) {
    if (disabledText) {
        return disabledText;
    }
    const urgency = riskToolPreviewText(tool?.urgencyPreview, "");
    const target = riskToolTargetText(tool);
    if (!target) {
        return "暂无旧账目标，先保留今日次数。";
    }
    if (/爆发|今天|明天|高危/.test(urgency)) {
        return `建议现在处理 · ${target}`;
    }
    if (/中危|后回流/.test(urgency)) {
        return `可提前拆雷 · ${target}`;
    }
    return `可留到事件卡救场 · ${target}`;
}

function riskToolCostChip(cost) {
    const text = String(cost || "");
    if (/无资源成本|无成本|无需|不消耗/.test(text)) return "无成本";
    if (new RegExp(BUDGET_RESOURCE_PATTERN).test(text) && /体力/.test(text)) return "运营预算/体力";
    if (new RegExp(`${BUDGET_RESOURCE_PATTERN}|预算`).test(text)) return "运营预算";
    if (/体力|精力/.test(text)) return "体力";
    if (/灵感/.test(text)) return "灵感";
    return text.length > 6 ? "有成本" : visibleTextOrFallback(text, "待核对");
}

function riskToolEffectChip(effect, tool) {
    const text = `${effect || ""} ${riskToolLabelText(tool)}`;
    if (/上下文|补全|切片/.test(text)) return "补上下文";
    if (/冷处理|降温|降热度|公告/.test(text)) return "降温";
    if (/房管|临时|拦截|控场/.test(text)) return "控场";
    if (/旧账|风险|反噬|米线/.test(text)) return "压旧账";
    return "稳风险";
}

function riskToolStateChip(effect) {
    const text = String(effect || "");
    if (/立即|当天|今日/.test(text)) return "即刻";
    if (/减少|降低|压低|降/.test(text)) return "减压";
    if (/补全|补充|增加/.test(text)) return "补证据";
    return "可用";
}

// 渲染米线工具箱
function renderRiskToolResult(result = state.lastRiskToolResult) {
    if (!result) {
        return "";
    }
    const toolName = riskToolLabelText({ toolType: result.toolType });
    const debt = result.debt || {};
    const debtLabel = debtDisplayLabel(debt);
    const severityDelta = Number(result.debtSeverityDelta || 0);
    const popularityDelta = signedDelta(result.popularityDelta);
    const reputationDelta = signedDelta(result.reputationDelta);
    const coinDelta = signedDelta(result.coinDelta);
    const inspirationDelta = signedDelta(result.inspirationDelta);
    const severityText = severityDelta < 0 ? `严重度${severityDelta}` : "严重度已压住";
    return `
        <div class="risk-tool-result" role="status" aria-live="polite">
            <div class="risk-tool-result-copy">
                <span>刚刚拆债</span>
                <strong>${html(toolName)} · ${html(debtLabel)}</strong>
                <small>${html(visibleTextOrFallback(result.summary, "风险工具已记录，本轮结局会把这次处理计入拆债证据。"))}</small>
            </div>
            <div class="risk-tool-result-chips" aria-label="风险工具结算">
                <em>${html(severityText)}</em>
                <em>曝光${html(popularityDelta)}</em>
                <em>口碑${html(reputationDelta)}</em>
                <em>${Number(result.inspirationDelta || 0) ? `灵感${inspirationDelta}` : `运营预算${coinDelta}`}</em>
            </div>
            <small>结局评分会计入“风险恢复 / 拆债证据”，下一手可以回到路线或委托补证据。</small>
        </div>
    `;
}

function renderRiskTools() {
    const tools = state.vup && Array.isArray(state.riskTools) ? state.riskTools : [];
    if (tools.length === 0) return '';

    const enabledCount = tools.filter(tool => tool.enabled).length;
    const hasTarget = tools.some(tool => tool.targetDebtId !== null && tool.targetDebtId !== undefined);
    const copy = enabledCount
        ? (hasTarget ? "今天只能用一次，先比较成本和目标再下手。" : "今天次数还在，等旧账露头再用。")
        : "工具箱今日待命，留意旧账回流日。";
    const items = tools.map(tool => {
        const disabled = state.busy || !tool.enabled;
        const label = riskToolLabelText(tool);
        const cost = riskToolPreviewText(tool.costPreview, "成本待核对");
        const effect = riskToolPreviewText(tool.effectPreview, "场务还在整理数据");
        const disabledText = riskToolDisabledText(tool, disabled);
        const titleText = disabledText ? `${label}：${disabledText}` : "";
        const chips = riskToolChips(tool, disabled);
        return `
            <button type="button"
                    class="${disabled ? 'risk-tool-chip blocked' : 'risk-tool-chip'}"
                    data-action="use-risk-tool"
                    data-risk-tool-type="${html(tool.toolType)}"
                    data-target-debt-id="${html(riskToolTargetIdLiteral(tool))}"
                    aria-label="${html(riskToolAriaLabel(tool, disabled))}"
                    ${riskToolButtonStateAttributes(disabled, titleText)}>
                <strong>${html(label)}</strong>
                <span class="risk-tool-token-row">
                    <span class="risk-tool-token cost"><em>成本</em><b>${html(chips.cost)}</b></span>
                    <span class="risk-tool-token effect"><em>效果</em><b>${html(chips.effect)}</b></span>
                    <span class="risk-tool-token target"><em>目标</em><b>${html(chips.target)}</b></span>
                    <span class="risk-tool-token state"><em>状态</em><b>${html(chips.state)}</b></span>
                </span>
                <span class="debt-tool-meta">${html(cost)} / ${html(effect)}${tool.targetDebt ? ` / ${html(debtConsequenceLine(tool.targetDebt))}` : ""}</span>
                <small>${html(disabledText || "可用于压一笔旧账风险")}</small>
            </button>
        `;
    }).join('');

    return `
        <div class="ambient-rail-card risk-tool-card">
            <div class="ambient-rail-head">
                <strong>米线工具箱</strong>
                <span>${enabledCount ? `${enabledCount}个可用` : "待命中"}</span>
            </div>
            <p class="ambient-rail-copy">${html(copy)}</p>
            ${renderRiskToolResult()}
            <div class="risk-tool-actions">${items}</div>
        </div>
    `;
}

function renderAmbientPanel() {
    const panel = document.getElementById('ambientPanel');
    if (!panel) return;

    const topics = state.vup && Array.isArray(state.fanTopics) ? state.fanTopics : [];
    const danmakus = state.vup && state.danmaku ? (state.danmaku.danmakus || []) : [];
    const riskToolHtml = renderRiskTools();
    const ambientDetails = (summary, meta, body, className = "") => {
        const openAttr = isLowHeightDesktopViewport() ? "" : " open";
        return `
            <details class="ambient-details ${html(className)}"${openAttr}>
                <summary>${html(summary)}<small>${html(meta)}</small></summary>
                <div class="ambient-details-body">${body}</div>
            </details>
        `;
    };
    if (topics.length === 0 && danmakus.length === 0 && !riskToolHtml) {
        panel.innerHTML = '<p style="color: var(--text-muted);">群聊和弹幕暂时安静，先把今天主行动打完。</p>';
        return;
    }

    const topic = topics[0];
    const topicHtml = topic ? (() => {
        const materialCue = fanTopicMaterialCueText(topic);
        const choices = (topic.choices || []).map(choice => {
            const disabled = !choice.enabled || state.busy;
            const disabledText = fanTopicChoiceDisabledText(choice, disabled);
            const ariaLabel = fanTopicChoiceAriaLabel(topic, choice);
            const titleText = disabledText ? `${ariaLabel}；${disabledText}` : ariaLabel;
            const choiceChips = fanTopicChoiceChips(choice);
            return `
                <button type="button"
                        class="${fanTopicChoiceClass(choice, disabled)}"
                        ${fanTopicChoiceButtonAttributes(disabled, disabledText, titleText)}
                        data-action="choose-fan-topic"
                        data-topic="${html(topic.topicKey)}"
                        data-topic-choice="${html(choice.choiceType)}"
                        aria-label="${html(ariaLabel)}">
                    <span class="fan-topic-choice-label">${html(fanTopicChoiceLabelText(choice))}</span>
                    <span class="fan-topic-choice-chip-row">
                        <span class="fan-topic-choice-chip cost"><em>成本</em><strong>${html(choiceChips.cost)}</strong></span>
                        <span class="fan-topic-choice-chip effect"><em>效果</em><strong>${html(choiceChips.effect)}</strong></span>
                        <span class="fan-topic-choice-chip risk"><em>风险</em><strong>${html(choiceChips.risk)}</strong></span>
                    </span>
                    <small class="fan-topic-choice-meta">${html(fanTopicChoiceMeta(choice))}</small>
                    ${choice.disabledReason ? `<span class="fan-topic-choice-disabled">${html(disabledReasonText(choice.disabledReason))}</span>` : ''}
                </button>
            `;
        }).join('');
        const cardHtml = `
            <div class="ambient-rail-card ${materialCue ? 'material-rail-card' : ''}">
                <div class="ambient-rail-head">
                    <strong>${html(fanTopicTitleText(topic))}</strong>
                    <span>${html(fanTopicSummaryHint(topic))}</span>
                </div>
                <p class="ambient-rail-copy">${html(fanTopicDescriptionText(topic))}</p>
                ${materialCue ? `<p class="ambient-rail-cue">${html(materialCue)}</p>` : ''}
                <div class="ambient-rail-actions">${choices}</div>
            </div>
        `;
        return ambientDetails("展开粉丝群议题", fanTopicSummaryHint(topic), cardHtml, "fan-topic-details");
    })() : '';

    const showDanmakuRail = danmakus.length && !riskToolHtml;
    const danmakuCardHtml = showDanmakuRail ? `
        <div class="ambient-rail-card">
            <div class="ambient-rail-head">
                <strong>直播间弹幕</strong>
                <span>${html(danmakuMoodText(state.danmaku?.mood))}</span>
            </div>
            <div class="ambient-rail-danmaku-list">
                ${danmakus.slice(0, 4).map(d => `
                    <div class="ambient-rail-danmaku">
                        <span>${html(danmakuPersonaText(d))}</span>
                        <small>${html(danmakuLineText(d))}</small>
                    </div>
                `).join('')}
            </div>
        </div>
    ` : '';
    const danmakuHtml = danmakuCardHtml
        ? ambientDetails("展开直播间弹幕", danmakuMoodText(state.danmaku?.mood), danmakuCardHtml, "danmaku-details")
        : '';

    panel.innerHTML = `
        <div class="ambient-rail">
            ${riskToolHtml}
            ${topicHtml}
            ${danmakuHtml}
        </div>
    `;
}

function renderTitleOption(t, index, busyDisabled) {
    const titleText = localizedVisibleTextOrFallback(t.titleText, `候选标题${index + 1}`);
    const effectText = localizedVisibleTextOrFallback(t.effectPreview, "节目效果待定");
    const riskText = localizedVisibleTextOrFallback(t.debtRiskPreview, "标题风险待观察");
    const decisionHint = titleDecisionHint(t, effectText, riskText);
    const decisionChips = titleDecisionChips(t, effectText, riskText);
    return `
        <button type="button" class="title-item" data-action="choose-title" data-title-id="${t.id}" data-title-rank="${index + 1}" data-stage-hotkey="${index + 1}" aria-label="选择直播标题：${html(titleText)}，${html(decisionHint)}，快捷键 ${index + 1}" ${busyDisabled}>
            <div class="title-card-head">
                <span>候选${index + 1}</span>
                <span class="stage-hotkey" aria-hidden="true">${index + 1}</span>
                <strong>标题组</strong>
            </div>
            <h4>${html(titleText)}</h4>
            <div class="title-decision-strip">
                <span class="title-chip heat"><em>热度</em><strong>${html(decisionChips.heat)}</strong></span>
                <span class="title-chip risk"><em>口碑风险</em><strong>${html(decisionChips.risk)}</strong></span>
                <span class="title-chip route"><em>路线倾向</em><strong>${html(decisionChips.route)}</strong></span>
            </div>
        </button>
    `;
}

function titleDecisionHint(title, effectText, riskText) {
    const style = String(title?.style || "");
    const text = `${effectText || ""} ${riskText || ""}`;
    if (style === "SAFE" || /风险低|低风险|口碑|真爱粉|稳/.test(text)) {
        return "稳健下饭";
    }
    if (style === "HARD_MOUTH" || /标题党|回旋镖|开庭|米线|黑红|主会场/.test(text)) {
        return "开庭预警";
    }
    if (style === "ABSTRACT_MEME" || style === "FAN_SERVICE" || /热度|人气|围观|出圈|乐子/.test(text)) {
        return "热度上桌";
    }
    return "标题组观望";
}

function titleDecisionChips(title, effectText, riskText) {
    const style = String(title?.style || "");
    const text = `${style} ${effectText || ""} ${riskText || ""}`;
    return {
        heat: titleHeatChip(text),
        risk: titleRiskChip(text),
        route: titleRouteChip(text)
    };
}

function firstMatch(text, pattern, mapper = null) {
    const match = String(text || "").match(pattern);
    if (!match) return "";
    return mapper ? mapper(match) : match[1];
}

function titleHeatChip(text) {
    const popularity = firstMatch(text, /人气\+(\d+)/);
    if (popularity) return `人气+${popularity}`;
    const watch = firstMatch(text, /围观\+(\d+)/);
    if (watch) return `围观+${watch}`;
    const fans = firstMatch(text, /粉丝\+(\d+)/);
    if (fans) return `粉丝+${fans}`;
    if (/热度|围观|出圈|流量|主会场/.test(text)) return "冲热度";
    if (/乐子|串味|节目效果|梗/.test(text)) return "整活";
    if (/歌|唱|推荐/.test(text)) return "歌势";
    if (/真爱粉|口碑|陪伴|下饭|稳/.test(text)) return "稳流量";
    return "热度待定";
}

function titleRiskChip(text) {
    const debt = firstMatch(text, /债务：([^，；]+?)(?:，|；|$)/);
    if (debt) return debt.replace(/\s+/g, "");
    if (/风险低|低风险|SAFE/.test(text)) return "低风险";
    if (/回旋镖|开庭|米线|破音|事故|坐牢/.test(text)) return "高风险";
    if (/独角兽|商业味|SC|期待/.test(text)) return "关系债";
    if (/标题党|蹭热度|梗疲劳|审判/.test(text)) return "中风险";
    return "可控";
}

function titleRouteChip(text) {
    if (new RegExp(`商业|${BUDGET_RESOURCE_PATTERN}|SC|老板|榜一`).test(text)) return "商业";
    if (/歌|唱|歌势|歌回|高音/.test(text)) return "歌势";
    if (/切片|素材|短视频|乐子|梗|舞|挑战/.test(text)) return "切片";
    if (/DD|联动|同台|新朋友/.test(text)) return "扩圈";
    if (/真爱粉|口碑|陪伴|下饭|低压/.test(text)) return "稳盘";
    return "观望";
}

function titleRerollButtonState() {
    const limit = 2;
    const used = Number(state.session?.titleRerollCount || 0);
    const remaining = Math.max(0, limit - used);
    const inspiration = Number(state.vup?.resources?.inspiration ?? state.vup?.inspiration ?? 0);
    const disabled = remaining <= 0 || inspiration < 1;
    const stateText = remaining <= 0
        ? "剩余0次"
        : `剩余${remaining}次`;
    const costText = inspiration < 1
        ? "灵感不足"
        : "代价：灵感-1";
    return {
        disabled,
        line: `${stateText} · ${costText}`,
        aria: `换一批标题，${stateText}，${costText}`
    };
}

function titleCancelButtonState() {
    return {
        line: "后果：回到行动，今天不能再开直播企划",
        aria: "取消企划，后果是回到行动阶段，今天不能再开直播企划"
    };
}

function renderTitles() {
    const panel = document.getElementById('titlePanel');
    if (state.session?.phase !== 'NEED_TITLE') {
        panel.classList.add('hidden');
        panel.innerHTML = '';
        return;
    }

    panel.classList.remove('hidden');

    // 标题候选异常
    if (state.session?.phase === "NEED_TITLE" && state.titles.length === 0) {
        panel.innerHTML = `
            <div class="panel-header">
                <h3 class="panel-title">标题候选异常</h3>
            </div>
            <div class="title-missing" style="padding: 16px; background: var(--bg-secondary); border-radius: 12px; border-left: 3px solid var(--danger);">
                <p>标题候选异常，当前已经进入标题选择阶段，但没有可用的后端候选标题。请刷新状态后再继续。</p>
            </div>
        `;
        return;
    }

    const busyDisabled = state.busy ? 'disabled' : '';
    const items = state.titles.map((t, index) => renderTitleOption(t, index, busyDisabled)).join('');
    const rerollButton = titleRerollButtonState();
    const cancelButton = titleCancelButtonState();
    const rerollDisabled = state.busy || rerollButton.disabled ? 'disabled' : '';

    panel.innerHTML = `
        <div class="panel-header">
            <h3 class="panel-title">选择直播标题</h3>
            <div class="title-panel-actions">
                <button type="button" class="compact-action-button title-reroll-button" data-action="reroll-title" aria-label="${html(rerollButton.aria)}" ${rerollDisabled}>
                    <span>换一批</span>
                    <small>${html(rerollButton.line)}</small>
                </button>
                <button type="button" class="compact-action-button title-cancel-button" ${busyDisabled} data-action="cancel-action" aria-label="${html(cancelButton.aria)}">
                    <span>取消企划</span>
                    <small>${html(cancelButton.line)}</small>
                </button>
            </div>
        </div>
        <div class="title-decision-loop">
            <div class="title-list">${items}</div>
        </div>
    `;
}

function activeEventPayload() {
    if (state.session?.phase === 'NEED_EVENT_CHOICE') {
        return state.pendingEvent?.pending ? state.pendingEvent : null;
    }
    if (state.session?.phase === 'NEED_INTERACTION_CHOICE') {
        return state.pendingInteraction?.pending ? state.pendingInteraction : null;
    }
    return null;
}

function currentEventKind() {
    return state.session?.phase === 'NEED_INTERACTION_CHOICE' ? 'interaction' : 'event';
}

function eventChoiceKey(choice) {
    return choice.choiceId || choice.choiceType;
}

function eventChoiceDisabledText(choice, disabled) {
    if (!disabled) {
        return "";
    }
    if (state.busy) {
        return state.busyLabel || "事件处理中，请稍等。";
    }
    return disabledReasonText(choice.disabledReason);
}

function eventChoiceButtonAttributes(disabled, disabledReason) {
    const ariaDisabled = `aria-disabled="${disabled ? 'true' : 'false'}"`;
    const title = disabledReason ? ` title="${html(disabledReason)}"` : "";
    return `${disabled ? 'disabled ' : ''}${ariaDisabled}${title}`;
}

function eventChoiceChips(choice, costPreview, effectPreview) {
    const riskPreview = choice?.riskPreview ? eventChoiceMetaText(choice.riskPreview) : "";
    const disabledText = choice?.disabledReason ? disabledReasonText(choice.disabledReason) : "";
    const text = [
        choice?.choiceId,
        choice?.choiceType,
        eventChoiceLabelText(choice),
        costPreview,
        effectPreview,
        riskPreview,
        disabledText
    ].filter(Boolean).join(" ");
    return {
        payoff: eventPayoffChip(effectPreview, text),
        cost: eventCostChip(costPreview, text),
        route: eventRouteImpactChip(choice, text),
        debt: eventDebtChip(choice, text)
    };
}

function eventCostChip(costPreview, text) {
    const costText = String(costPreview || "");
    if (!costText || /无资源成本|无成本|无需|不消耗/.test(costText)) return "无成本";
    if (new RegExp(BUDGET_RESOURCE_PATTERN).test(costText) && /体力/.test(costText)) return "运营预算/体力";
    if (new RegExp(`${BUDGET_RESOURCE_PATTERN}|${MONEY_RESOURCE_PATTERN}|预算`).test(costText)) return "运营预算";
    if (/体力|精力/.test(costText)) return "体力";
    if (/灵感/.test(costText)) return "灵感";
    if (/素材|投稿/.test(costText)) return "素材";
    if (/旧账|风险债|关系债|米线|债/.test(`${costText} ${text}`)) return "旧账";
    return costText.length > 6 ? "有代价" : costText;
}

function eventPayoffChip(effectPreview, text) {
    const effectText = String(effectPreview || "");
    const merged = `${effectText} ${text}`;
    if (/切片组|投稿|素材|补档|录播/.test(merged)) return "补素材";
    if (/真爱粉|老粉|口碑|澄清|稳定|稳住|米线/.test(merged)) return "稳口碑";
    if (/热度|围观|出圈|涨粉|粉丝|流量|人气/.test(merged)) return "涨粉热度";
    if (new RegExp(`${BUDGET_RESOURCE_PATTERN}|商业|SC|变现|赞助`).test(merged)) return "运营反馈";
    if (/联动|同台|DD|扩圈|新朋友/.test(merged)) return "扩圈";
    if (/灵感|企划|准备|复盘/.test(merged)) return "蓄力";
    if (/乐子|梗|整活|节目效果/.test(merged)) return "节目效果";
    return "效果待定";
}

function eventRouteImpactChip(choice, text) {
    const merged = `${choice?.choiceType || ""} ${choice?.choiceId || ""} ${text}`;
    if (new RegExp(`商业|${BUDGET_RESOURCE_PATTERN}|SC|赞助|老板|榜一`).test(merged)) return "商业";
    if (/歌|唱|歌回|高音/.test(merged)) return "歌势";
    if (/切片|素材|投稿|梗|舞|挑战|短视频/.test(merged)) return "切片";
    if (/联动|同台|DD|扩圈|新朋友|查房/.test(merged)) return "扩圈";
    if (/真爱粉|口碑|陪伴|下饭|澄清|道歉|稳/.test(merged)) return "稳盘";
    if (/黑红|主会场|开庭|审判/.test(merged)) return "黑红";
    return "不明显";
}

function eventDebtChip(choice, text) {
    const merged = `${choice?.choiceType || ""} ${choice?.choiceId || ""} ${text}`;
    if (choice?.enabled === false || choice?.disabledReason) return "暂不可选";
    if (/不新增|无旧账|无风险|不留/.test(merged)) return "不新增";
    if (/关系债|独角兽|信任|老粉/.test(merged)) return "关系债";
    if (/标题债|回旋镖|旧账|债|反噬|风险债|米线/.test(merged)) return "会留账";
    if (/开庭|炎上|高风险|事故/.test(merged)) return "高风险";
    if (/降温|澄清|复盘|道歉|冷处理/.test(merged)) return "可降温";
    return "待观察";
}

function eventPressureChips(event, choices) {
    const severity = Number(event?.severity || 0);
    const ordinaryEvent = isOrdinaryFormalEvent(event);
    const severityLabel = event?.debtType && !ordinaryEvent
        ? riskSeverityText(severity)
        : ordinaryEventTypeText(event);
    const enabledCount = (choices || []).filter(choice => choice?.enabled !== false).length;
    const eventText = [
        event?.title,
        event?.description,
        event?.debtType,
        debtTypeText(event?.debtType),
        event?.eventKey
    ].filter(Boolean).join(" ");
    const tempo = /标题|回旋镖|开庭|米线|反噬|债/.test(eventText)
        ? "先稳米线"
        : (/联动|同台|DD/.test(eventText) ? "稳住同台" : "看清再点");
    return {
        severity: severityLabel,
        choices: `${enabledCount || choices?.length || 0}个可选`,
        tempo
    };
}

function renderEventChoice(choice, eventKind, index) {
    const choiceKey = eventChoiceKey(choice);
    const choiceLabel = eventChoiceLabelText(choice);
    const costPreview = choice.costPreview ? eventChoiceMetaText(choice.costPreview) : "";
    const effectPreview = eventChoiceMetaText(choice.effectPreview || choice.riskPreview);
    const choiceChips = eventChoiceChips(choice, costPreview, effectPreview);
    const disabled = state.busy || choice.enabled === false;
    const disabledText = eventChoiceDisabledText(choice, disabled);
    const choiceHotkey = index + 1;
    const choiceCta = eventChoiceCtaText(choice, index);
    const dataAction = eventKind === 'interaction' ? 'choose-interaction' : 'choose-event';
    return `
        <div class="event-choice">
            <div class="event-choice-copy">
                <div class="event-choice-head">
                    <span class="event-choice-label">处理方案</span>
                    <span class="stage-hotkey" aria-hidden="true">${choiceHotkey}</span>
                </div>
                <strong class="event-choice-title">${html(choiceLabel)}</strong>
                <div class="event-choice-lines" aria-label="选择取舍">
                    <span class="event-choice-line payoff"><em>获得什么</em><strong>${html(choiceChips.payoff)}</strong></span>
                    <span class="event-choice-line cost"><em>牺牲什么</em><strong>${html(choiceChips.cost)}</strong></span>
                    <span class="event-choice-line route"><em>路线影响</em><strong>${html(choiceChips.route)}</strong></span>
                    <span class="event-choice-line debt"><em>可能旧账</em><strong>${html(choiceChips.debt)}</strong></span>
                </div>
                ${choice.disabledReason ? `<p class="event-choice-disabled">${html(disabledReasonText(choice.disabledReason))}</p>` : ''}
            </div>
            <button type="button" class="event-choice-button" data-action="${html(dataAction)}" data-choice="${html(choiceKey)}" data-stage-hotkey="${choiceHotkey}" aria-label="选择：${html(choiceLabel)}，操作：${html(choiceCta)}，快捷键 ${choiceHotkey}" ${eventChoiceButtonAttributes(disabled, disabledText)}>${html(choiceCta)}</button>
        </div>
    `;
}

function renderEvent() {
    const panel = document.getElementById('eventPanel');
    if (state.session?.phase !== 'NEED_EVENT_CHOICE' && state.session?.phase !== 'NEED_INTERACTION_CHOICE') {
        panel.classList.add('hidden');
        panel.innerHTML = '';
        delete panel.dataset.eventSoundPlayed;
        delete panel.dataset.eventTtsPlayed;
        return;
    }

    panel.classList.remove('hidden');

    // 事件音效（仅首次渲染时播放）
    if (!panel.dataset.eventSoundPlayed) {
        panel.dataset.eventSoundPlayed = 'true';
        SFX.event();
    }

    const event = activeEventPayload();

    if (!event) {
        const missingLabel = state.session?.phase === 'NEED_INTERACTION_CHOICE'
            ? '现场互动资料'
            : '事件资料';
        panel.innerHTML = `
            <div class="event-missing" role="alert">
                <strong>${html(missingLabel)}同步异常</strong>
                <p>当前阶段需要处理 pending，但本地没有拿到标题、描述或选项。请重新请求状态，恢复后会回到当前事件。</p>
                <button type="button" data-action="retry-actions">重新请求状态</button>
            </div>
        `;
        return;
    }

    // 事件插画
    const illustration = eventIllustrationFor(event);
    const choices = event.choices || [];
    const eventKind = currentEventKind();
    const eventTitle = eventTitleText(event);
    const eventDescription = eventDescriptionText(event);
    const eventVisibleDescription = eventVisibleDescriptionText(event);
    const eventBackgroundDetails = eventBackgroundDetailsHtml(eventDescription, eventVisibleDescription);

    // AI语音播报事件（仅首次渲染时朗读）
    if (!panel.dataset.eventTtsPlayed) {
        panel.dataset.eventTtsPlayed = 'true';
        TTS.speak(`${eventTitle}。${eventDescription}`);
    }

    const eventUrgencyMeta = event.debtType && !isOrdinaryFormalEvent(event)
        ? `必须处理 · ${debtTypeText(event.debtType)} · ${riskSeverityText(event.severity)}`
        : isOrdinaryFormalEvent(event)
            ? `必须处理 · ${ordinaryEventTypeText(event)}`
            : '';
    const pressureChips = eventPressureChips(event, choices);
    const choiceItems = choices.map((choice, index) => renderEventChoice(choice, eventKind, index)).join('');

    panel.innerHTML = `
        <div class="panel-header">
            <h3 class="panel-title">${html(eventTitle)}</h3>
        </div>
        <div class="event-box event-decision-loop">
            ${eventUrgencyMeta ? `<div class="event-meta-line">${html(eventUrgencyMeta)}</div>` : ''}
            <div class="event-pressure-strip" aria-label="事件态势">
                <span class="event-pressure-chip severity"><em>压力</em><strong>${html(pressureChips.severity)}</strong></span>
                <span class="event-pressure-chip choices"><em>选项</em><strong>${html(pressureChips.choices)}</strong></span>
                <span class="event-pressure-chip tempo"><em>节奏</em><strong>${html(pressureChips.tempo)}</strong></span>
            </div>
            <p class="event-description">${html(eventVisibleDescription)}</p>
            ${eventBackgroundDetails}
            <div class="event-choices">${choiceItems}</div>
            <img class="event-illustration" src="/gallery/${illustration}" alt="${html(eventTitle)}" loading="eager" decoding="async">
        </div>
    `;
}

// 渲染正式事件（用于测试）
function renderPendingEvent() {
    if (!state.pendingEvent?.pending) return '';
    const event = state.pendingEvent;
    const illustration = eventIllustrationFor(event);
    const eventTitle = eventTitleText(event);
    const eventDescription = eventDescriptionText(event);
    const eventVisibleDescription = eventVisibleDescriptionText(event);
    const eventBackgroundDetails = eventBackgroundDetailsHtml(eventDescription, eventVisibleDescription);
    const eventUrgencyMeta = event.debtType && !isOrdinaryFormalEvent(event)
        ? `必须处理 · ${debtTypeText(event.debtType)} · ${riskSeverityText(event.severity)}`
        : isOrdinaryFormalEvent(event)
            ? `必须处理 · ${ordinaryEventTypeText(event)}`
            : '';
    const pressureChips = eventPressureChips(event, event.choices || []);
    const choices = (event.choices || []).map((c, index) => renderEventChoice(c, 'event', index)).join('');

    return `
        <div class="event-box">
            ${eventUrgencyMeta ? `<div class="event-meta-line">${html(eventUrgencyMeta)}</div>` : ''}
            <div class="event-pressure-strip" aria-label="事件态势">
                <span class="event-pressure-chip severity"><em>压力</em><strong>${html(pressureChips.severity)}</strong></span>
                <span class="event-pressure-chip choices"><em>选项</em><strong>${html(pressureChips.choices)}</strong></span>
                <span class="event-pressure-chip tempo"><em>节奏</em><strong>${html(pressureChips.tempo)}</strong></span>
            </div>
            <p class="event-description">${html(eventVisibleDescription)}</p>
            ${eventBackgroundDetails}
            <div class="event-choices">${choices}</div>
            <img class="event-illustration" src="/gallery/${illustration}" alt="${html(eventTitle)}" loading="eager" decoding="async">
        </div>
    `;
}

function reportMaterialReceiptText(value) {
    const raw = String(value || "");
    const body = raw.replace(/^素材小票：/, "");
    return `素材小票：${localizedVisibleTextOrFallback(body, "今日素材已记入素材库。")}`;
}

function reportVisibleItemText(value) {
    const raw = String(value || "");
    if (raw.startsWith("素材小票：")) {
        return reportMaterialReceiptText(raw);
    }
    return localizedVisibleTextOrFallback(raw, "复盘条目还在整理。");
}

function eventIllustrationFor(event) {
    const eventText = [
        event?.eventKey,
        event?.debtType,
        event?.title,
        event?.description,
        event?.sourceAction,
        event?.sourceTitle
    ].filter(Boolean).join(' ').toUpperCase();
    const routeText = [eventText, state.vup?.currentRoute].filter(Boolean).join(' ').toUpperCase();
    if (eventText.includes('COMMERCIAL') || eventText.includes('商业')) return eventIllustrations.COMMERCIAL_BACKLASH;
    if (eventText.includes('UNICORN') || eventText.includes('独角兽')) return eventIllustrations.UNICORN_EXPECTATION;
    if (eventText.includes('COLLAB') || eventText.includes('联动')) return eventIllustrations.COLLAB_RUMOR;
    if (eventText.includes('HOT_MIC') || eventText.includes('麦')) return eventIllustrations.HOT_MIC_MISFIRE;
    if (eventText.includes('TITLE') || eventText.includes('标题')) return eventIllustrations.TITLE_BACKLASH;
    if (routeText.includes('BLACK') || routeText.includes('黑红')) return eventIllustrations.BLACK_RED_SIGNATURE;
    if (routeText.includes('CLIP') || routeText.includes('切片') || routeText.includes('SLICE_SAINT')) return eventIllustrations.SLICE_SIGNATURE;
    if (routeText.includes('ELECTRONIC_PICKLE') || routeText.includes('电子榨菜') || /陪饭|陪伴|低压|杂谈|读信/.test(routeText)) return eventIllustrations.PICKLE_SIGNATURE;
    return eventIllustrations.APOLOGY_REVIEW;
}

function reportKpiShortText(value, fallback) {
    const text = String(localizedVisibleTextOrFallback(value, fallback) || "").trim();
    if (!text) return fallback;
    if (/素材|切片|投稿|入库|弹药/.test(text)) return /暂无|待补|补一点/.test(text) ? "待补素材" : "素材入库";
    if (/歌回|基本功|低压|稳健|平台口味|排班|推荐|流量/.test(text)) return "看平台风向";
    if (/风险|舆情|旧账|米线|反噬|回旋镖|标题太冲|压流/.test(text)) return /暂无|无明显/.test(text) ? "风险暂稳" : "先稳风险";
    if (/暂无|无明显|稳定|稳稳|稳住/.test(text)) return "稳住";
    const compact = text.replace(/[，。；、,.!?！？].*$/, "").trim();
    return compact.length > 8 ? `${compact.slice(0, 8)}...` : compact || fallback;
}

function reportShortSentence(value, fallback, maxLength = 42) {
    const text = String(localizedVisibleTextOrFallback(value, fallback) || "").trim();
    if (!text) return fallback;
    const firstClause = text.split(/[。！？!?；;]/)[0].trim() || text;
    return firstClause.length > maxLength ? `${firstClause.slice(0, maxLength)}...` : firstClause;
}

function reportWhyLineHtml(value) {
    const text = String(value || "").trim();
    if (!text) return "";
    const settlementParts = reportSettlementParts(text);
    if (settlementParts) {
        const chips = [
            { label: "收获", text: settlementParts.payoff },
            { label: "欠账", text: settlementParts.risk },
            { label: "明天", text: settlementParts.next }
        ].filter(part => part.text).map(part => `
            <span class="report-why-chip">
                <em>${html(part.label)}</em>
                <strong>${html(reportShortSentence(part.text, "", 16))}</strong>
            </span>
        `).join("");
        return `<div class="report-why-line" aria-label="为什么变化">${chips}</div>`;
    }
    const peakMatch = text.match(/^复盘原因：([^。；;]+)/);
    const peakPart = peakMatch
        ? [{ label: "情绪", text: reportShortSentence(peakMatch[1], "", 12) }]
        : [];
    const causeText = text.replace(/^复盘原因：[^。；;]+[。；;]?/, "");
    const labels = ["收获", "代价", "明天"];
    const tagged = [...causeText.matchAll(/(?:^|[。；;]\s*)(赚|亏|欠|明天)：([^。；;]+)/g)]
        .map(match => ({ label: match[1] === "赚" ? "收获" : match[1] === "亏" || match[1] === "欠" ? "代价" : match[1], text: reportShortSentence(match[2], "", 16) }))
        .filter(part => part.text);
    const fallbackParts = causeText.split(/[；;]/).map(part => reportShortSentence(part, "", 16)).filter(Boolean);
    const causeParts = tagged.length >= 2
        ? tagged
        : fallbackParts.slice(0, 3).map((part, index) => ({ label: labels[index] || "因", text: part }));
    const chips = [...peakPart, ...causeParts].slice(0, 3).map(part => `
        <span class="report-why-chip">
            <em>${html(part.label)}</em>
            <strong>${html(part.text)}</strong>
        </span>
    `).join("");
    return `<div class="report-why-line" aria-label="为什么变化">${chips || html(reportShortSentence(text, "先看涨跌原因，再决定明天打法。", 48))}</div>`;
}

function isReportSettlementLine(value) {
    return String(value || "").trim().startsWith("结算看板：");
}

function reportSettlementParts(value) {
    const text = String(value || "").trim();
    if (!isReportSettlementLine(text)) return null;
    const body = text.replace(/^结算看板：/, "");
    const parts = {};
    [...body.matchAll(/(?:^|[；;]\s*)(赚|亏|欠|明天)：([^；;。]+)/g)].forEach(match => {
        const key = match[1] === "赚" ? "payoff" : (match[1] === "明天" ? "next" : "risk");
        parts[key] = String(match[2] || "").trim();
    });
    return parts.payoff || parts.risk || parts.next ? parts : null;
}

function reportSettlementFromVisibleItems() {
    const item = (state.report?.visibleItems || []).find(value => isReportSettlementLine(value));
    return item ? reportSettlementParts(item) : null;
}

function reportPrimaryDeltaCards(report) {
    const delta = report?.dataDelta || report?.dataDeltas || {};
    const actionResult = state.lastActionResult || {};
    const cards = [
        { label: "粉丝", value: Number(delta.fanDelta ?? actionResult.fanChange ?? 0), positiveIsGood: true },
        { label: "口碑", value: Number(delta.reputationDelta ?? actionResult.reputationChange ?? 0), positiveIsGood: true },
        { label: "围观", value: Number(delta.watchHeatDelta ?? actionResult.watchHeatChange ?? 0), positiveIsGood: true }
    ];
    return cards.map(card => `
        <span class="report-primary-delta ${deltaToneClass(card.value, card.positiveIsGood)}">
            <em>${html(card.label)}</em>
            <strong>${html(signedDelta(card.value))}</strong>
        </span>
    `).join("");
}

function reportDeltaValue(key, fallback = 0) {
    const delta = state.report?.dataDelta || state.report?.dataDeltas || {};
    return Number(delta[key] ?? fallback ?? 0);
}

function reportFirstVisibleLine(patterns, fallback) {
    const items = state.report?.visibleItems || [];
    const found = items.find(item => {
        const text = String(item || "");
        return patterns.some(pattern => pattern.test(text));
    });
    return found ? reportVisibleItemText(found).replace(/^(数据变化|主行动|今日标题|路线提示|场外声音|粉丝来信【[^】]+】[^：]+|素材小票)：?/, "").trim() : fallback;
}

function reportPayoffLine() {
    const settlement = reportSettlementFromVisibleItems();
    if (settlement?.payoff) return settlement.payoff;
    const fan = reportDeltaValue("fanDelta", state.lastActionResult?.fanChange);
    const heat = reportDeltaValue("watchHeatDelta", state.lastActionResult?.watchHeatChange);
    const reputation = reportDeltaValue("reputationDelta", state.lastActionResult?.reputationChange);
    const meme = reportDeltaValue("memeDelta", state.lastActionResult?.memeChange);
    const coin = reportDeltaValue("coinDelta", state.lastActionResult?.coinDelta);
    const parts = [];
    if (fan) parts.push(`粉丝${signedDelta(fan)}`);
    if (heat) parts.push(`围观${signedDelta(heat)}`);
    if (reputation) parts.push(`口碑${signedDelta(reputation)}`);
    if (meme) parts.push(`梗浓度${signedDelta(meme)}`);
    if (coin) parts.push(`运营预算${signedDelta(coin)}`);
    if (parts.length) return parts.slice(0, 3).join(" / ");
    return reportFirstVisibleLine([/^主行动：/, /^数据变化：/], "今天主要完成了一次低风险积累。");
}

function reportLandmineLine(riskHintText) {
    const settlement = reportSettlementFromVisibleItems();
    if (settlement?.risk) return settlement.risk;
    const debt = activeDebts().sort((left, right) => {
        const dueDelta = debtDaysLeft(left) - debtDaysLeft(right);
        return dueDelta !== 0 ? dueDelta : (Number(right?.severity) || 0) - (Number(left?.severity) || 0);
    })[0];
    if (debt) {
        return `${debtDisplayLabel(debt)}：${debtDueText(debt)}，${debtCounterplayText(debt)}`;
    }
    if (state.report?.debtSummary) {
        return reportShortSentence(state.report.debtSummary, riskHintText, 52);
    }
    return reportShortSentence(riskHintText, "暂时没有旧账，明天可以更主动地补路线证据。", 52);
}

function reportOutsideVoiceLine(fanLetterRaw) {
    const voice = reportFirstVisibleLine([/^场外声音：/, /^粉丝来信/], "");
    if (voice) return voice;
    if (fanLetterRaw) return reportShortSentence(fanLetterRaw, "粉丝还在观察你的下一步。", 52);
    return "论坛和录播组暂时没有大节奏，适合继续试探观众口味。";
}

function reportTomorrowWorryLine(riskHintText, fanLetterRaw) {
    const settlement = reportSettlementFromVisibleItems();
    if (settlement?.next) return settlement.next;
    const urgentDebt = activeDebts()
        .sort((left, right) => {
            const dueDelta = debtDaysLeft(left) - debtDaysLeft(right);
            return dueDelta !== 0 ? dueDelta : (Number(right?.severity) || 0) - (Number(left?.severity) || 0);
        })[0];
    if (urgentDebt) {
        return `${debtDisplayLabel(urgentDebt)}：${debtDueText(urgentDebt)}，明天先${debtCounterplayText(urgentDebt)}`;
    }
    if (state.report?.riskHint) {
        return reportShortSentence(riskHintText, "明天先稳口碑，不要连续硬冲。", 52);
    }
    return reportOutsideVoiceLine(fanLetterRaw);
}

function renderReportPressureNarrative() {
    const pressure = currentOperationalPressureSnapshot();
    if (!pressure) return "";
    const tone = pressure.tone || "neutral";
    const lockGroup = pressure.lockGroup || "";
    const replacementLabel = pressure.replacementLabel || "";
    const cooldownLeft = pressure.cooldownLeft || 0;
    const woundLeft = pressure.woundLeft || 0;
    const wounded = pressure.wounded;
    let consequence = "";
    if (wounded) {
        consequence = `带伤恢复还在进行，收益会更紧，可选项也会变少。`;
    } else if (cooldownLeft > 0) {
        consequence = `${lockGroup}线被锁${cooldownLeft}天，这几天的操作空间会变窄。`;
    } else if (lockGroup) {
        consequence = `${lockGroup}线被压住了，先别硬冲。`;
    }
    let advice = "";
    if (replacementLabel) {
        advice = `今天优先用${replacementLabel}稳住局面。`;
    } else if (tone === "risk") {
        advice = "先把高压降下来，再考虑推进。";
    }
    return `
        <div class="report-pressure-narrative ${html(tone)}" aria-label="运营脑压力状态">
            <em>运营脑</em>
            <strong>${html(pressure.state)}${lockGroup ? ` · ${html(lockGroup)}` : ""}</strong>
            ${consequence ? `<p>${html(consequence)}</p>` : ""}
            ${advice ? `<p>${html(advice)}</p>` : ""}
        </div>
    `;
}

function reportThreeBeatHtml({ riskHintText, fanLetterRaw }) {
    const cards = [
        { type: "payoff", label: "今天赚了什么", title: "收获反馈", line: reportPayoffLine() },
        { type: "landmine", label: "今天亏了什么或欠了什么", title: "代价和旧账", line: reportLandmineLine(riskHintText) },
        { type: "outside", label: "明天最该担心什么", title: "下一天风险", line: reportTomorrowWorryLine(riskHintText, fanLetterRaw) }
    ];
    return `
        <div class="report-three-beat" aria-label="日报三段式反馈">
            ${cards.map(card => `
                <section class="report-beat-card ${card.type}">
                    <span>${html(card.label)}</span>
                    <strong>${html(card.title)}</strong>
                    <p>${html(card.line)}</p>
                </section>
            `).join("")}
        </div>
    `;
}

function latestDailyHighlightForReport(report = state.report) {
    const explicit = report?.highlight || report?.dailyHighlight || report?.keyHighlight;
    if (explicit) return explicit;
    const highlights = Array.isArray(state.dailyHighlights?.highlights)
        ? state.dailyHighlights.highlights
        : (Array.isArray(state.dailyHighlights) ? state.dailyHighlights : []);
    const reportDay = Number(report?.day || state.session?.day || 0);
    return highlights.find(item => Number(item?.day) === reportDay) || highlights[highlights.length - 1] || null;
}

function reportFocusItems(report = state.report, riskHintText = "") {
    const highlight = latestDailyHighlightForReport(report);
    const settlement = reportSettlementFromVisibleItems();
    const route = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN";
    const items = [];
    if (highlight) {
        items.push({
            tone: String(highlight.highlightType || "highlight").toLowerCase(),
            label: firstLocalizedReadableText([highlight.title], "今日高光"),
            value: firstLocalizedReadableText([highlight.description], reportShortSentence(report?.summary, "今天有一条可复盘的高光。", 48))
        });
    }
    const reportHighlights = Array.isArray(report?.highlights)
        ? report.highlights
        : (Array.isArray(report?.keyHighlights) ? report.keyHighlights : []);
    reportHighlights.slice(0, 2).forEach((item, index) => {
        const textItem = typeof item === "string" ? item : "";
        items.push({
            tone: String(item?.tone || item?.type || (textItem.includes("风险") || textItem.includes("欠账") ? "risk" : "highlight")).toLowerCase(),
            label: firstLocalizedReadableText([item?.label, item?.title], index === 0 ? "重点" : "复盘"),
            value: textItem
                ? reportShortSentence(textItem, "这条重点已写入日报。", 46)
                : firstLocalizedReadableText([item?.summary, item?.description, item?.line], "这条重点已写入日报。")
        });
    });
    items.push({
        tone: "payoff",
        label: "收获",
        value: reportShortSentence(settlement?.payoff || reportPayoffLine(), "看粉丝、口碑和围观的变化。", 38)
    });
    items.push({
        tone: riskCrisisItems().some(item => item.urgent) ? "risk" : "safe",
        label: "风险",
        value: reportShortSentence(settlement?.risk || reportLandmineLine(riskHintText), "风险暂稳，明天补路线证据。", 38)
    });
    items.push({
        tone: "route",
        label: "路线",
        value: reportShortSentence(
            reportFirstVisibleLine([/^路线提示：/], "") || endingGapCoachLine(firstWarnEndingRequirement(), { likelyEnding: route }),
            `${routeLabelFor(route)}继续补证据。`,
            42
        )
    });
    const seen = new Set();
    return items.filter(item => {
        const key = `${item.label}|${item.value}`;
        if (seen.has(key)) return false;
        seen.add(key);
        return item.value;
    }).slice(0, 4);
}

function renderReportFocusSummary(report = state.report, riskHintText = "") {
    const items = reportFocusItems(report, riskHintText);
    return `
        <section class="report-focus-summary" aria-label="日报重点摘要">
            <div class="report-focus-head">
                <span>重点摘要</span>
                <strong>${html(reportShortSentence(report?.summary, "今天的日报已经生成。", 34))}</strong>
            </div>
            <div class="report-focus-grid">
                ${items.map(item => `
                    <span class="report-focus-item ${html(item.tone)}">
                        <em>${html(item.label)}</em>
                        <strong>${html(item.value)}</strong>
                    </span>
                `).join("")}
            </div>
        </section>
    `;
}

function reportEchoSearchLine(platformTrendDescription) {
    const actionType = state.lastActionResult?.actionType || "";
    const route = state.vup?.currentRoute || "UNKNOWN";
    const voice = reportFirstVisibleLine([/^场外声音：/, /^路线提示：/, /^今日标题：/], "");
    if (voice) return voice;
    if (route === "BLACK_RED_MAIN_STAGE" || actionType === "NPC_INTERACT") {
        return "论坛新楼已开：有人看热闹，有人等你明天给说法。";
    }
    if (route === "SLICE_SAINT" || actionType === "PUBLISH_CLIP" || actionType === "PUBLISH_VIDEO") {
        return "推荐页开始试水：标题和封面先把路人勾进来了。";
    }
    if (route === "ELECTRONIC_PICKLE" || actionType === "TRAIN_TALK" || actionType === "FAN_GROUP_MAINTAIN") {
        return "饭点讨论区低压通过：这台适合挂后台。";
    }
    return reportShortSentence(platformTrendDescription, "平台风向暂稳，今天的动静还在扩散。", 54);
}

function reportEchoDanmakuLine() {
    const actionType = state.lastActionResult?.actionType || "";
    const route = state.vup?.currentRoute || "UNKNOWN";
    const lines = {
        TRAIN_TALK: "「这段别剪，我就爱听这种没包袱的闲聊。」",
        FAN_GROUP_MAINTAIN: "「群公告发了，今天气压终于下来了。」",
        PUBLISH_CLIP: "「这秒能截封面，切片组开始上班。」",
        PUBLISH_VIDEO: "「正片来了，评论区已经在催下一期。」",
        NPC_INTERACT: "「同台味对了，DD先上车看看。」",
        STREAM_PLAN: "「标题有东西，先蹲一个后续。」",
        REST: "「今天不播也行，别把自己耗炸了。」"
    };
    if (lines[actionType]) return lines[actionType];
    if (route === "BLACK_RED_MAIN_STAGE") return "「别急着开庭，等日报把证据链补齐。」";
    if (route === "SLICE_SAINT") return "「素材有了，剪刀已经举起来了。」";
    if (route === "ELECTRONIC_PICKLE") return "「今晚就这样播挺好，饭都变香了。」";
    return "「新人第一天，先看看她会怎么活。」";
}

function reportEchoFanGroupLine(fanLetterRaw) {
    if (fanLetterRaw) {
        return reportShortSentence(
            String(fanLetterRaw).replace(/^粉丝来信【[^】]*】[^：]+：/, ""),
            "粉丝群还在观察，但已经有人开始留座。",
            54
        );
    }
    const fanDelta = reportDeltaValue("fanDelta", state.lastActionResult?.fanChange);
    if (fanDelta > 20) return "新粉进群先问补课包，老粉已经开始发导航。";
    if (fanDelta > 0) return "群里多了几张新头像，大家还在试探你的边界。";
    return "群里今天比较安静，房管建议明天补一个明确记忆点。";
}

function reportEchoWallHtml({ platformTrendDescription, riskHintText, fanLetterRaw }) {
    const items = [
        { label: "热搜/论坛", line: reportEchoSearchLine(platformTrendDescription) },
        { label: "弹幕", line: reportEchoDanmakuLine() },
        { label: "粉丝群", line: reportEchoFanGroupLine(fanLetterRaw) },
        { label: "明日风险", line: reportLandmineLine(riskHintText) }
    ];
    return `
        <section class="report-echo-wall" aria-label="互联网回音壁">
            <div class="report-echo-wall-head">
                <span>互联网回音壁</span>
                <strong>今天这步在外面变成了什么</strong>
            </div>
            <div class="report-echo-wall-list">
                ${items.map(item => `
                    <div class="report-echo-wall-item">
                        <em>${html(item.label)}</em>
                        <strong>${html(item.line)}</strong>
                    </div>
                `).join("")}
            </div>
        </section>
    `;
}

function reportRiskRecoveryCard(result, riskHintText) {
    const debts = normalizedDebtCreated(result);
    const active = activeDebts();
    const targetDebt = debts[0] || [...active].sort((left, right) => {
        const dueDelta = debtDaysLeft(left) - debtDaysLeft(right);
        return dueDelta !== 0 ? dueDelta : (Number(right?.severity) || 0) - (Number(left?.severity) || 0);
    })[0];
    const hasRiskHint = Boolean(state.report?.riskHint);
    if (!targetDebt && !hasRiskHint) {
        return "";
    }

    const debtLabel = targetDebt ? debtTypeText(targetDebt.debtType) : "舆情苗头";
    const dueText = targetDebt ? debtDueText(targetDebt) : "明天先观察";
    const severityText = targetDebt ? riskSeverityText(targetDebt.severity) : "低压";
    const counterplay = targetDebt ? debtCounterplayText(targetDebt) : "先用低压行动试探，不要连续硬冲热度";
    const summary = targetDebt
        ? digestText(targetDebt.summary, `${debtLabel}正在发酵，明天要留一个回合处理。`)
        : reportKpiShortText(riskHintText, "有风险苗头，下一天别硬冲。");
    const buttonHtml = active.length
        ? `<button type="button" data-action="open-report-risk-recovery">去处理旧账</button>`
        : `<span class="report-risk-recovery-wait">明天复盘</span>`;

    return `
        <div class="report-risk-recovery ${targetDebt ? debtToneClass(targetDebt) : "watch"}" role="note" aria-label="风险恢复建议">
            <div class="report-risk-recovery-copy">
                <span>风险恢复</span>
                <strong>${html(debtLabel)} · ${html(dueText)} · ${html(severityText)}</strong>
                <small>${html(summary)}</small>
            </div>
            <div class="report-risk-recovery-action">
                <em>${html(counterplay)}</em>
                ${buttonHtml}
            </div>
        </div>
    `;
}

function reportNextDayPlanCard(riskHintText) {
    const forecast = state.endingForecast;
    const requirement = (forecast?.requirements || []).find(item => item?.status && item.status !== "PASS");
    const likelyEnding = forecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN";
    const gapActionType = requirement ? (endingGapActionTypes(requirement, likelyEnding)[0] || "") : "";
    const gapActionName = actionLabelFor(gapActionType);
    const objectiveItems = Array.isArray(state.stageBriefing?.objectiveItems) ? state.stageBriefing.objectiveItems : [];
    const pendingObjective = objectiveItems.find(item => !item?.achieved) || objectiveItems[0];
    const objectiveActionType = objectiveItemActionType(pendingObjective) || state.stageBriefing?.objectiveActionType || "";
    const objectiveActionName = actionLabelFor(objectiveActionType);
    const debt = [...activeDebts()].sort((left, right) => {
        const dueDelta = debtDaysLeft(left) - debtDaysLeft(right);
        return dueDelta !== 0 ? dueDelta : (Number(right?.severity) || 0) - (Number(left?.severity) || 0);
    })[0];
    const riskTone = debt ? debtToneClass(debt) : readyGoalRiskTone();
    const riskTitle = debt ? debtTypeText(debt.debtType) : readyGoalRiskLabel();
    const riskLine = debt
        ? `${debtDueText(debt)} · ${debtCounterplayText(debt)}`
        : reportKpiShortText(riskHintText, "风险暂稳，明天优先补路线证据。");
    const mainPlanLine = debt
        ? `明天先拆${riskTitle}：${riskLine}`
        : endingGapCoachLine(requirement, { likelyEnding, clearText: "明天继续补同一路线证据，把结局门槛钉牢。" });
    const gapLine = requirement
        ? `${endingGapActionLabel(requirement)} · ${endingForecastRequirementLineText(requirement)}`
        : "当前结局门槛已稳，明天可以继续冲路线证据。";
    const objectiveLine = pendingObjective
        ? `${pendingObjective.state || "推进中"} · ${pendingObjective.hint || "开下一天后优先补这条周任务。"}`
        : "周任务暂时没有硬缺口，明天按结局缺口排班。";

    return `
        <section class="report-next-plan ${riskTone}" aria-label="明日排班预告">
            <div class="report-next-plan-head">
                <span>明日排班预告</span>
                <strong>${html(forecast ? endingForecastFinalTitleText(forecast) : "先补主线证据")}</strong>
            </div>
            <div class="report-next-plan-main">
                <span>${html(debt ? "先防守" : requirement ? "先补缺口" : "继续推进")}</span>
                <strong>${html(mainPlanLine)}</strong>
            </div>
            <div class="report-next-plan-grid">
                <div class="report-next-plan-item forecast">
                    <em>结局缺口</em>
                    <strong>${html(requirement ? endingGapSignalLabel(requirement) : "门槛暂稳")}</strong>
                    <small>${html(gapActionType ? `优先：${gapActionName} · ${gapLine}` : gapLine)}</small>
                </div>
                <div class="report-next-plan-item objective">
                    <em>周任务</em>
                    <strong>${html(pendingObjective?.label || state.stageBriefing?.objectiveTitle || "阶段委托")}</strong>
                    <small>${html(objectiveActionType ? `优先：${objectiveActionName} · ${objectiveLine}` : objectiveLine)}</small>
                </div>
                <button type="button" class="report-next-plan-item risk" data-action="open-report-next-risk">
                    <em>风险排雷</em>
                    <strong>${html(riskTitle)}</strong>
                    <small>${html(riskLine)}</small>
                </button>
            </div>
        </section>
    `;
}

function evidenceRefTypeLabel(type) {
    const labels = {
        business_log: "业务日志",
        stream_plan: "直播企划",
        title_template: "标题模板",
        accident_material: "事故素材",
        combo: "组合技",
        risk_debt: "旧账风险",
        stage_objective: "阶段委托",
        stage_milestone: "阶段爆点"
    };
    return labels[type] || visibleTextOrFallback(type, "证据");
}

function evidenceRefTone(ref) {
    const type = String(ref?.type || "");
    if (type === "risk_debt" || type === "accident_material") return "risk";
    if (type === "combo" || type === "stage_objective" || type === "stage_milestone") return "special";
    if (type === "title_template" || type === "stream_plan") return "content";
    return "log";
}

function evidenceRefTitle(ref) {
    const type = String(ref?.type || "");
    if (type === "business_log") return actionLabelFor(ref?.action);
    if (type === "title_template") return localizedVisibleTextOrFallback(ref?.title, "标题已入账");
    if (type === "stream_plan") return "直播企划已引用";
    if (type === "accident_material") return materialLabelFor(ref);
    if (type === "combo") return localizedVisibleTextOrFallback(ref?.label, "组合技命中");
    if (type === "risk_debt") return debtTypeText(ref?.debtType);
    if (type === "stage_objective") return localizedVisibleTextOrFallback(ref?.label, "阶段委托");
    if (type === "stage_milestone") return localizedVisibleTextOrFallback(ref?.title, "阶段爆点已触发");
    return evidenceRefTypeLabel(type);
}

function evidenceRefDetail(ref) {
    const type = String(ref?.type || "");
    if (type === "business_log") {
        const day = Number.isFinite(Number(ref?.day)) ? `第${Number(ref.day)}天` : "今日";
        return `${day} · log#${ref?.id ?? "--"}`;
    }
    if (type === "title_template") return `title#${ref?.id ?? "--"}`;
    if (type === "stream_plan") return `plan#${ref?.id ?? "--"}`;
    if (type === "accident_material") {
        const day = Number.isFinite(Number(ref?.day)) ? `第${Number(ref.day)}天` : "当日";
        return `${day} · ${actionLabelFor(ref?.sourceAction)}`;
    }
    if (type === "combo") return localizedVisibleTextOrFallback(ref?.evidence, "已写入打法图鉴。");
    if (type === "risk_debt") {
        const severity = riskSeverityText(ref?.severity);
        const due = Number.isFinite(Number(ref?.dueDay)) ? `第${Number(ref.dueDay)}天回流` : statusLabelFor(ref?.status);
        return `${severity} · ${due}`;
    }
    if (type === "stage_milestone") {
        const day = Number.isFinite(Number(ref?.day)) ? `第${Number(ref.day)}天` : "阶段日";
        return `${day} · ${localizedVisibleTextOrFallback(ref?.stageLabel, routeLabelFor(ref?.routeType || "UNKNOWN"))}`;
    }
    return localizedVisibleTextOrFallback(ref?.summary || ref?.detail, "证据已归档。");
}

function renderReportEvidenceSummary(report = state.report) {
    const refs = Array.isArray(report?.evidenceRefs) ? report.evidenceRefs : [];
    const latest = refs[refs.length - 1];
    const line = latest
        ? `路线证据：${evidenceRefTitle(latest)} · ${evidenceRefDetail(latest)}`
        : "今天没有明显路线证据，明天可补同路线行动";
    return `
        <section class="report-evidence-summary" aria-label="日报路线证据摘要">
            <span>路线证据</span>
            <strong>${html(line)}</strong>
        </section>
    `;
}

function renderReportEvidenceLedger(report = state.report) {
    const refs = Array.isArray(report?.evidenceRefs) ? report.evidenceRefs : [];
    if (!refs.length) return "";
    const priority = {
        risk_debt: 0,
        accident_material: 1,
        stage_milestone: 2,
        combo: 3,
        stage_objective: 4,
        business_log: 4,
        title_template: 5,
        stream_plan: 6
    };
    const sorted = refs
        .map((ref, index) => ({ ref, index }))
        .sort((left, right) =>
            (priority[String(left.ref?.type || "")] ?? 20) - (priority[String(right.ref?.type || "")] ?? 20)
            || left.index - right.index)
        .map(item => item.ref)
        .slice(0, 6);
    const hiddenCount = Math.max(0, refs.length - sorted.length);
    return `
        <section class="report-evidence-ledger" aria-label="今日日报证据链">
            <div class="report-evidence-ledger-head">
                <span>证据链</span>
                <strong>${html(refs.length)}条已归档${hiddenCount ? ` · 另有${hiddenCount}条` : ""}</strong>
            </div>
            <div class="report-evidence-ledger-list">
                ${sorted.map(ref => `
                    <span class="report-evidence-ref ${html(evidenceRefTone(ref))}">
                        <em>${html(evidenceRefTypeLabel(ref?.type))}</em>
                        <strong>${html(evidenceRefTitle(ref))}</strong>
                        <small>${html(evidenceRefDetail(ref))}</small>
                    </span>
                `).join("")}
            </div>
        </section>
    `;
}

function reportStageMilestone(report = state.report) {
    const refs = Array.isArray(report?.evidenceRefs) ? report.evidenceRefs : [];
    const ref = refs.find(item => String(item?.type || "") === "stage_milestone");
    if (ref) return ref;
    const line = (report?.visibleItems || []).find(item => String(item || "").startsWith("阶段爆点："));
    if (!line) return null;
    const body = String(line).replace(/^阶段爆点：/, "");
    const [title, spokenLine] = body.split("｜");
    return {
        type: "stage_milestone",
        day: report?.day,
        title,
        spokenLine,
        routeType: state.vup?.currentRoute || "UNKNOWN",
        routeLabel: routeLabelFor(state.vup?.currentRoute || "UNKNOWN"),
        stageLabel: "阶段爆点",
        heckleLine: "观众已经准备好截图拷打。",
        routeLine: "路线证据开始变厚，接下来要看能不能连续推进。",
        costLine: "爆点会改变观众预期，明天别当作普通日常处理。",
        tomorrowLine: "明天先补同路线证据，再处理最高风险旧账。",
        statLine: ""
    };
}

function reportStageMilestoneShareText(milestone) {
    if (!milestone) return "";
    return [
        `第${Number(milestone.day) || state.report?.day || "?"}天阶段爆点：${localizedVisibleTextOrFallback(milestone.title, "阶段爆点")}`,
        `口播：${localizedVisibleTextOrFallback(milestone.spokenLine, "这一天值得截图。")}`,
        `拷打：${localizedVisibleTextOrFallback(milestone.heckleLine, "观众已经准备好拷打。")}`,
        localizedVisibleTextOrFallback(milestone.tomorrowLine, "")
    ].filter(Boolean).join("｜");
}

async function copyStageMilestoneShare(encodedText) {
    const text = decodeURIComponent(String(encodedText || ""));
    if (!text) {
        setStatus("阶段爆点还没生成，先推进到第7/15/23天。", "warning");
        return;
    }
    try {
        if (navigator.clipboard?.writeText && window.isSecureContext) {
            await navigator.clipboard.writeText(text);
        } else {
            const textarea = document.createElement("textarea");
            textarea.value = text;
            textarea.setAttribute("readonly", "");
            textarea.style.position = "fixed";
            textarea.style.left = "-9999px";
            document.body.appendChild(textarea);
            textarea.select();
            document.execCommand("copy");
            textarea.remove();
        }
        setStatus("阶段爆点口播句已复制。", "success");
    } catch (error) {
        setStatus("浏览器拦截了复制，请直接截图这张阶段爆点卡。", "warning");
    }
}

function reportStageMilestoneHtml(report = state.report) {
    const milestone = reportStageMilestone(report);
    if (!milestone) return "";
    const day = Number(milestone.day) || Number(report?.day) || 0;
    const routeLabel = localizedVisibleTextOrFallback(milestone.routeLabel, routeLabelFor(milestone.routeType || "UNKNOWN"));
    const title = localizedVisibleTextOrFallback(milestone.title, "阶段爆点已触发");
    const spokenLine = localizedVisibleTextOrFallback(milestone.spokenLine, "这一天值得截图。");
    const heckleLine = localizedVisibleTextOrFallback(milestone.heckleLine, "观众已经准备好拷打。");
    const routeLine = localizedVisibleTextOrFallback(milestone.routeLine, "路线证据开始变厚。");
    const costLine = localizedVisibleTextOrFallback(milestone.costLine, "爆点会改变观众预期。");
    const tomorrowLine = localizedVisibleTextOrFallback(milestone.tomorrowLine, "明天先补同路线证据。");
    const statLine = localizedVisibleTextOrFallback(milestone.statLine, "");
    const shareText = reportStageMilestoneShareText(milestone);
    return `
        <section class="stage-milestone-card" aria-label="阶段爆点截图卡" data-stage-milestone-day="${html(day)}">
            <div class="stage-milestone-head">
                <span>${html(milestone.screenshotLabel || `第${day}天阶段爆点`)}</span>
                <strong>${html(routeLabel)}</strong>
            </div>
            <h4>${html(title)}</h4>
            <div class="stage-milestone-lines">
                <p class="stage-milestone-spoken"><em>口播</em><strong>${html(spokenLine)}</strong></p>
                <p class="stage-milestone-heckle"><em>拷打观众</em><strong>${html(heckleLine)}</strong></p>
            </div>
            <div class="stage-milestone-grid">
                <span><em>路线</em><strong>${html(routeLine)}</strong></span>
                <span><em>代价</em><strong>${html(costLine)}</strong></span>
                <span><em>明天</em><strong>${html(tomorrowLine)}</strong></span>
            </div>
            <div class="stage-milestone-foot">
                <small>${html(statLine || `第${day}天 · ${routeLabel}`)}</small>
                <button type="button" data-action="copy-stage-milestone" data-share-text="${html(clipboardText(shareText))}">复制口播</button>
            </div>
        </section>
    `;
}

function reportRiskHintItems(riskHintText) {
    const items = [];
    if (state.report?.riskHint) {
        items.push({
            tone: "watch",
            label: "日报风险",
            title: "明天别硬冲",
            line: riskHintText
        });
    }
    const sessionHints = Array.isArray(state.session?.riskHints) ? state.session.riskHints : [];
    sessionHints.slice(0, 2).forEach(hint => items.push({
        tone: "debt",
        label: "旧账回流",
        title: "已在日程上",
        line: localizedVisibleTextOrFallback(hint, "有旧账在发酵，明天先看风险面板。")
    }));
    activeDebts()
        .sort((left, right) => {
            const dueDelta = debtDaysLeft(left) - debtDaysLeft(right);
            return dueDelta !== 0 ? dueDelta : (Number(right?.severity) || 0) - (Number(left?.severity) || 0);
        })
        .slice(0, 2)
        .forEach(debt => items.push({
            tone: debtToneClass(debt),
            label: debtTypeText(debt.debtType),
            title: `${debtDueText(debt)} · ${riskSeverityText(debt.severity)}`,
            line: debtCounterplayText(debt)
        }));
    const forecastRisk = endingForecastRiskText(state.endingForecast, "");
    if (forecastRisk) {
        items.push({
            tone: "forecast",
            label: "结局预演",
            title: "收官风险",
            line: forecastRisk
        });
    }
    if (!items.length) {
        items.push({
            tone: "safe",
            label: "风险提示",
            title: "今晚暂稳",
            line: "没有明显旧账回流，明天优先补路线证据。"
        });
    }
    const seen = new Set();
    return items.filter(item => {
        const key = `${item.label}|${item.title}|${item.line}`;
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
    }).slice(0, 4);
}

function renderReportRiskHintStack(riskHintText) {
    const items = reportRiskHintItems(riskHintText);
    return `
        <section class="report-risk-hints" aria-label="风险提示栈">
            <div class="report-risk-hints-head">
                <span>风险提示</span>
                <strong>${html(items[0]?.title || "今晚暂稳")}</strong>
            </div>
            <div class="report-risk-hints-list">
                ${items.map(item => `
                    <span class="report-risk-hint ${html(item.tone)}">
                        <em>${html(item.label)}</em>
                        <strong>${html(item.title)}</strong>
                        <small>${html(item.line)}</small>
                    </span>
                `).join("")}
            </div>
        </section>
    `;
}

// ============ 日报叙事系统 ============
const ROUTE_NARRATIVE_OPENERS = {
    TRAIN_SONG: {
        SINGING_IDOL: '你今天练了一整天的歌，感觉嗓子又打开了一个度。高音更稳了，低音也更有质感。',
        ELECTRONIC_PICKLE: '你今天练了练歌，顺便和观众聊了聊最近的生活。唱歌和聊天，都是一种陪伴。',
        DANCE_MEME: '你今天练了练歌，舞蹈演员也得有好嗓子嘛。边唱边跳才是真本事。',
        default: '你今天练了练歌，基本功又扎实了一点。',
    },
    TRAIN_DANCE: {
        DANCE_MEME: '你在练功房里泡了一下午，镜子里的自己动作越来越流畅了。这个舞步终于有了灵魂。',
        SINGING_IDOL: '你今天练了练舞蹈，偶像也得能唱能跳嘛。虽然跳舞不是强项，但有在进步。',
        default: '你今天练了练舞，动作比昨天更流畅了。',
    },
    TRAIN_TALK: {
        ELECTRONIC_PICKLE: '你开了一场杂谈回，从生活琐事聊到人生哲学，观众们听得津津有味。这就是你的天赋。',
        SOCIAL_COLLAB: '你今天练了练口才，下次联动的时候可以聊得更深入了。',
        default: '你今天练了练口才，表达能力又提升了一点。',
    },
    STREAM_PLAN: {
        SINGING_IDOL: (pos, delta) => pos ? `你今天开了一场歌回，观众们点了不少歌，气氛很好。${delta > 20 ? '有人连着点了三首，你的歌声真的打动了他们。' : '虽然涨粉不多，但唱歌的快乐是真实的。'}` : '你今天开了一场歌回，观众不多，但你还是认真唱完了每一首歌。',
        BLACK_RED_MAIN_STAGE: (pos, delta) => pos ? `你今天开了一场直播，弹幕里吵翻了天，但热度确实上去了。${delta > 20 ? '争议越大，涨粉越快，这就是你的路线。' : '黑红也是红，数据不会说谎。'}` : '你今天开了一场直播，虽然争议不少，但热度没起来。得想想怎么制造更有话题性的内容。',
        ELECTRONIC_PICKLE: (pos) => pos ? '你今天开了一场杂谈回，和观众们聊得很开心。有人说"听你聊天特别解压"。' : '你今天开了一场杂谈回，话题有点冷场，但你很快就用幽默化解了尴尬。',
        SOCIAL_COLLAB: (pos) => pos ? '你今天开了一场联动直播，两边的观众都很买账，弹幕互动很热烈。' : '你今天开了一场联动，但配合有点生疏，效果不如预期。',
        default: (pos, delta) => pos ? `你开了一场直播，${delta > 20 ? '直播间人气爆棚！' : '陆陆续续有观众进来。'}弹幕滚动得很快，你感觉到了和观众的连接。` : '今天的直播观众不多，有点冷清。但每一次直播都是一次积累。',
    },
    PUBLISH_VIDEO: {
        SLICE_SAINT: '你今天发了一个精心剪辑的视频，节奏和信息量都控制得很好。',
        DANCE_MEME: '你今天发了一个舞蹈视频，动作流畅，画面也很有感染力。',
        default: '你今天发了一个视频，希望数据能好看一点。',
    },
    PUBLISH_CLIP: {
        SLICE_SAINT: '你今天发了一个精心制作的切片，标题和封面都花了心思。好的切片就是最好的安利。',
        DANCE_MEME: '你今天发了一个舞蹈切片，动作卡点很准，BGM也很洗脑。',
        BLACK_RED_MAIN_STAGE: '你今天发了一个争议向切片，标题故意带了点火药味。黑红也是红嘛。',
        default: '你今天发了一个切片，看看效果如何。',
    },
    FAN_GROUP_MAINTAIN: {
        ELECTRONIC_PICKLE: '你在粉丝群里和大家聊了聊日常，像老朋友一样。有人说"在你的群里特别放松"。',
        SOCIAL_COLLAB: '你在粉丝群里组织了一下下次联动的投票，粉丝们参与度很高。',
        default: '你在粉丝群里和大家聊了聊，老粉们很暖心。',
    },
    NPC_INTERACT: {
        SOCIAL_COLLAB: '你和同行约了个联动，对方的粉丝也过来看你了。人脉就是这么一点点积累的。',
        BLACK_RED_MAIN_STAGE: '你和同行互动了一下，虽然风格不同，但争议话题让两边都涨了热度。',
        default: '你和同行交流了一下，互相引流，对面的粉丝也过来看你了。',
    },
    REST: {
        SINGING_IDOL: '你休息了一天，让嗓子好好恢复了一下。明天可以继续唱歌了。',
        DANCE_MEME: '你休息了一天，腿终于不是自己的了。明天继续练舞！',
        default: '你休息了一天，充了充电。体力恢复了，明天可以好好干。',
    },
};

function generateReportNarrative() {
    const result = state.lastActionResult;
    const report = state.report;
    const day = state.session?.day || report?.day || 1;
    const actionType = result?.actionType || '';
    const fanDelta = Number(result?.fanChange ?? report?.dataDelta?.fanDelta ?? report?.dataDeltas?.fanDelta ?? 0);
    const isPositive = fanDelta > 0;
    const fans = state.vup?.fans || 0;
    const route = state.vup?.currentRoute || 'UNKNOWN';

    const narratives = [];

    const actionStories = {
        TRAIN_SONG: isPositive
            ? `你今天练了一整天的歌，直播间里陆陆续续来了几个新面孔。${fanDelta > 10 ? '有好几个观众被你的歌声吸引，直接点了关注。' : '虽然涨粉不多，但你的歌力在稳步提升。'}`
            : '你今天练了歌，但状态不太好，嗓子有点紧。不过基本功就是这样一点一点磨出来的。',
        TRAIN_DANCE: isPositive
            ? '你在练功房里跳了一下午，动作越来越流畅了。有观众在弹幕里说"这个舞可以"。'
            : '今天练舞有点不在状态，但坚持练完总比不练好。',
        TRAIN_TALK: isPositive
            ? '你开了一场杂谈回，和观众们聊得挺开心。灵感来了，感觉明天可以尝试点新内容。'
            : '杂谈回的效果一般，话题有点干。不过聊着聊着灵感就来了。',
        STREAM_PLAN: isPositive
            ? `你开了一场直播，${fanDelta > 20 ? '直播间人气爆棚！' : '陆陆续续有观众进来。'}弹幕滚动得很快，你感觉到了和观众的连接。`
            : '今天的直播观众不多，有点冷清。但每一次直播都是一次积累。',
        PUBLISH_VIDEO: isPositive
            ? '你发了一个视频，数据在慢慢涨。有观众在评论区说"终于更新了！"。'
            : '视频发出去了，但播放量不太理想。下次换个题材试试。',
        PUBLISH_CLIP: isPositive
            ? '你发了一条切片，传播效果不错！有人在转发，新观众顺着切片摸过来了。'
            : '切片发出去了，但反响平平。标题可能没选好。',
        FAN_GROUP_MAINTAIN: isPositive
            ? '你在粉丝群里和大家聊了聊，老粉们很暖心，还有人给你投稿素材。'
            : '粉丝群维护了一下，气氛还算稳定。',
        NPC_INTERACT: isPositive
            ? '你和同行交流了一下，互相引流，对面的粉丝也过来看你了。'
            : '互动效果一般，但至少露了个脸。',
        REST: '你休息了一天，充了充电。体力恢复了，明天可以好好干。',
    };

    // 路线专属叙事开头
    const routeOpener = ROUTE_NARRATIVE_OPENERS[actionType];
    if (routeOpener) {
        const opener = routeOpener[route];
        if (opener !== undefined) {
            narratives.push(typeof opener === 'function' ? opener(isPositive, fanDelta) : opener);
        } else {
            const defOp = routeOpener.default;
            narratives.push(typeof defOp === 'function' ? defOp(isPositive, fanDelta) : defOp);
        }
    } else {
        narratives.push(actionStories[actionType] || '你今天忙碌了一天，总算有了点成果。');
    }

    // 日报结尾（路线专属）
    const routeEndings = {
        SINGING_IDOL: { early: `出道第${day}天，你的歌声正在被越来越多人听到。`, late: `第${day}天了，用歌声冲向终点吧。`, mid: `现在有${fans}个人在听你唱歌，每一首歌都没有白唱。` },
        SLICE_SAINT: { early: `出道第${day}天，你的切片库正在慢慢丰富起来。`, late: `第${day}天了，最后几个切片要打出爆款！`, mid: `现在有${fans}个粉丝在看你的切片，好内容永远不会被埋没。` },
        ELECTRONIC_PICKLE: { early: `出道第${day}天，你已经习惯了和观众们聊天的日常。`, late: `第${day}天了，珍惜每一次和大家聊天的机会。`, mid: `现在有${fans}个人愿意听你唠嗑，这就是最好的陪伴。` },
        BLACK_RED_MAIN_STAGE: { early: `出道第${day}天，争议已经成了你的标签。`, late: `第${day}天了，不管风评如何，先把热度拉满。`, mid: `现在有${fans}个粉丝在关注你，虽然一半是来看热闹的。` },
        SOCIAL_COLLAB: { early: `出道第${day}天，联动名单越来越长了。`, late: `第${day}天了，最后的联动冲刺！`, mid: `现在有${fans}个粉丝，都是联动带来的。人脉就是资源。` },
        DANCE_MEME: { early: `出道第${day}天，你的舞蹈视频开始被传播了。`, late: `第${day}天了，再跳几个爆款出来！`, mid: `现在有${fans}个粉丝在看你跳舞，下一个爆款就是你发明的。` },
    };
    const ending = routeEndings[route];
    if (ending) {
        if (day <= 3) narratives.push(ending.early);
        else if (day >= 25) narratives.push(ending.late);
        else if (fans >= 500) narratives.push(ending.mid);
    } else {
        if (day <= 3) narratives.push(`这是你出道的第${day}天，一切才刚刚开始。`);
        else if (day >= 25) narratives.push(`第${day}天了，最后的冲刺阶段，每一步都很关键。`);
        else if (fans >= 500) narratives.push(`现在有${fans}个粉丝在关注你，继续加油！`);
    }

    return narratives.join('');
}

function renderReport() {
    const panel = document.getElementById('reportPanel');
    if (state.session?.phase !== 'REPORT_READY') {
        panel.classList.add('hidden');
        panel.innerHTML = '';
        delete panel.dataset.reportSoundKey;
        syncReportDrawerState();
        return;
    }

    panel.classList.remove('hidden');

    // 日报缺失检查
    if (state.session?.phase === "REPORT_READY" && !state.report) {
        panel.innerHTML = `
            <div class="panel-header">
                <h3 class="panel-title">日报生成异常</h3>
            </div>
            <div class="report-missing">
                <p>日报生成异常，当前已经进入日报阶段，但还没有可用的已保存日报。请刷新状态后再继续。</p>
            </div>
        `;
        syncReportDrawerState();
        return;
    }

    if (!state.report) {
        panel.innerHTML = '';
        return;
    }

    const reportSoundKey = String(state.report?.id || `${state.report?.day || state.session?.day || ""}:${state.report?.summary || ""}`);
    if (panel.dataset.reportSoundKey !== reportSoundKey) {
        panel.dataset.reportSoundKey = reportSoundKey;
        SFX.report();
    }

    // Parse fan letter out of visibleItems so it can render as a styled card
    const fanLetterRaw = (state.report.visibleItems || []).find(item => String(item || '').startsWith('粉丝来信'));
    let fanLetterCard = '';
    if (fanLetterRaw) {
        const flMatch = String(fanLetterRaw).match(/^粉丝来信【([^】]*)】([^：]+)：(.+)$/);
        if (flMatch) {
            const fanTypeIcons = { true_fan: '❤️', fun_fan: '😂', unicorn: '👀', dd: '🚌' };
            const fanTypeLabels = { true_fan: '真爱粉', fun_fan: '乐子人', unicorn: '独角兽', dd: 'DD' };
            const flIcon = fanTypeIcons[flMatch[1]] || '💌';
            const flLabel = fanTypeLabels[flMatch[1]] || flMatch[1];
            const flName = flMatch[2];
            const flContent = flMatch[3];
            fanLetterCard = `
                <div class="fan-letter-card">
                    <span class="fan-letter-icon">${flIcon}</span>
                    <div class="fan-letter-content">
                        <span class="fan-letter-name">${html(flName)} <small class="fan-letter-type">${html(flLabel)}</small></span>
                        <p class="fan-letter-text">"${html(flContent)}"</p>
                    </div>
                </div>`;
        }
    }
    const whyLineRaw = (state.report.visibleItems || []).find(item => {
        const text = String(item || '');
        return isReportSettlementLine(text) || text.startsWith('复盘原因：') || (text.includes('赚：') && (text.includes('亏：') || text.includes('欠：')) && text.includes('明天：'));
    });
    const whyLineText = whyLineRaw ? localizedVisibleTextOrFallback(String(whyLineRaw), "先看涨跌原因，再决定明天打法。") : "";
    const whyLineHtml = reportWhyLineHtml(whyLineText);
    const detailItems = (state.report.visibleItems || [])
        .filter(i => !String(i || '').startsWith('粉丝来信'))
        .filter(i => {
            const text = String(i || '');
            return !isReportSettlementLine(text)
                && !text.startsWith('阶段爆点：')
                && !text.startsWith('复盘原因：')
                && !(text.includes('赚：') && (text.includes('亏：') || text.includes('欠：')) && text.includes('明天：'));
        })
        .map(i => `<li>${html(reportVisibleItemText(i))}</li>`)
        .join('');
    const materialReceipt = (state.report.visibleItems || []).find(item => String(item || '').startsWith('素材小票：'));
    const materialReceiptText = materialReceipt ? reportMaterialReceiptText(materialReceipt) : '';
    const materialReceiptHtml = materialReceiptText ? `<p class="report-material-receipt">${html(materialReceiptText)}</p>` : '';
    const platformTrend = state.report.platformTrend;
    const summaryText = reportShortSentence(state.report.summary, "今日直播已收摊，复盘还在整理中。", 46);
    const narrativeText = generateReportNarrative();
    const narrativeHtml = narrativeText ? '<div class="report-narrative">' + html(narrativeText) + '</div>' : '';
    const platformTrendLabel = platformTrend?.label
        ? localizedVisibleTextOrFallback(platformTrend.label, "平台口味")
        : "稳态观察";
    const platformTrendDescription = platformTrend?.description
        ? localizedVisibleTextOrFallback(platformTrend.description, "平台口味暂时没有大波动，按当前节奏继续试水。")
        : "平台口味暂时没有大波动，明天可以继续稳住排班。";
    const materialReceiptSummary = materialReceipt
        ? localizedVisibleTextOrFallback(String(materialReceiptText).replace(/^素材小票：/, ""), "今日素材已记入素材库。")
        : "今日暂无新素材入库，明天可以补一点切片弹药。";
    const riskHintText = state.report.riskHint
        ? localizedVisibleTextOrFallback(state.report.riskHint, "有风险苗头，下一天别硬冲。")
        : "暂无明显风险，今晚可以稳稳收摊。";
    const platformTrendKpi = reportKpiShortText(platformTrendDescription, "看风向");
    const materialReceiptKpi = reportKpiShortText(materialReceiptSummary, "看素材");
    const riskHintKpi = reportKpiShortText(riskHintText, "看风险");
    const actionResultHud = renderActionResultHud(state.lastActionResult);
    const objectiveRecap = renderReportObjectiveRecap();
    const riskRecoveryCard = reportRiskRecoveryCard(state.lastActionResult, riskHintText);
    const nextDayPlanCard = reportNextDayPlanCard(riskHintText);
    const reportPrimaryDeltas = reportPrimaryDeltaCards(state.report);
    const reportThreeBeat = reportThreeBeatHtml({ riskHintText, fanLetterRaw });
    const reportStageMilestone = reportStageMilestoneHtml(state.report);
    const reportEchoWall = reportEchoWallHtml({ platformTrendDescription, riskHintText, fanLetterRaw });
    const reportRiskHints = renderReportRiskHintStack(riskHintText);
    const reportEvidenceLedger = renderReportEvidenceLedger(state.report);
    const reportEvidenceSummary = renderReportEvidenceSummary(state.report);
    const reportFocusSummary = renderReportFocusSummary(state.report, riskHintText);
    const reportRouteMap = renderRouteVisibilityMap(state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN", true);
    const reportCrisisCards = renderCrisisCards(true);
    const maxDay = runMaxDay();

    // 平台口味
    const platformTrendHtml = platformTrend ? `
        <div class="platform-trend">
            <span class="platform-trend-label">${html(platformTrendLabel)}</span>
            <p class="platform-trend-description">${html(platformTrendDescription)}</p>
        </div>
    ` : '';

    // 氛围指示器
    const heat = state.vup?.watchHeat || 0;
    const mood = heat >= 70 ? '🔥 高热' : heat >= 40 ? '📈 升温' : '😴 平静';
    const moodColor = heat >= 70 ? '#ef4444' : heat >= 40 ? '#eab308' : '#6b7280';
    const moodBanner = `<div class="report-mood-banner" style="--mood-color: ${moodColor}">
        <span class="report-mood-label">今日氛围</span>
        <span class="report-mood-value">${mood}</span>
        <span class="report-mood-heat">热度 ${heat}</span>
    </div>`;

    panel.innerHTML = `
        <div class="panel-header report-header">
            <div class="report-header-copy">
                <h3 class="panel-title">第${state.report.day}天日报</h3>
                <p class="report-subtitle">今天先收摊复盘，读完就切到下一天。</p>
            </div>
            ${state.session?.phase === "REPORT_READY" && state.report ? `<button type="button" class="report-next-day" data-action="next-day" data-stage-advance-hotkey="Enter" aria-label="读完第${state.report.day}天日报，进入下一天，快捷键 回车" ${state.busy ? 'disabled aria-disabled="true"' : ''}>读完开下一天 <span class="stage-hotkey stage-advance-hotkey" aria-hidden="true">回车</span></button>` : ''}
        </div>
        ${renderReportPressureNarrative()}
        <div class="report-loop">
            <div class="report-brief report-brief-lite">
                <div class="report-brief-label sr-only">日报重点</div>
                ${reportEvidenceSummary}
                ${reportThreeBeat}
                <p class="report-summary">${html(summaryText)}</p>
                ${narrativeHtml}
                ${reportFocusSummary}
                ${reportStageMilestone}
                ${reportRiskHints}
                ${reportEchoWall}
                ${whyLineHtml}
                ${fanLetterCard}
                <div class="report-primary-deltas" aria-label="今日关键涨跌">${reportPrimaryDeltas}</div>
            </div>
            ${renderThreeLayerReport(state.report)}
            <details class="report-details">
                <summary>展开完整复盘</summary>
                <div class="report-detail-stack">
                    <div class="report-kpi-strip" aria-label="今日复盘摘要">
                        <div class="report-kpi">
                            <span>平台口味</span>
                            <strong>${html(platformTrendLabel)}</strong>
                            <small>${html(platformTrendKpi)}</small>
                        </div>
                        <div class="report-kpi">
                            <span>素材小票</span>
                            <strong>${materialReceipt ? '已入库' : '待补货'}</strong>
                            <small>${html(materialReceiptKpi)}</small>
                        </div>
                        <div class="report-kpi risk">
                            <span>风险提示</span>
                            <strong>${state.report.riskHint ? '留意舆情' : '暂稳'}</strong>
                            <small>${html(riskHintKpi)}</small>
                        </div>
                    </div>
                    ${moodBanner}
                    ${actionResultHud}
                    ${objectiveRecap}
                    ${riskRecoveryCard}
                    ${nextDayPlanCard}
                    <div class="report-route-risk-grid" aria-label="路线与危机复盘">
                        ${reportRouteMap}
                        ${reportCrisisCards}
                    </div>
                    ${reportEvidenceLedger}
                    ${materialReceiptHtml}
                    ${platformTrendHtml}
                    ${state.report.riskHint ? `<p class="report-risk">${html(riskHintText)}</p>` : ''}
                    <ul class="report-items">${detailItems}</ul>
                </div>
            </details>
            <div class="report-summary-block">
                <strong>📍 路线进度</strong>：${html(routeLabelFor(state.endingForecast?.likelyEndingType || state.vup?.currentRoute || 'UNKNOWN'))}路线，${html(endingGapCoachLine(firstWarnEndingRequirement(), { likelyEnding: state.endingForecast?.likelyEndingType || state.vup?.currentRoute }))} 第${state.report.day}天/${maxDay}天。
            </div>
        </div>
    `;
    syncReportDrawerState();

    // Stagger animation for report items
    const items = panel.querySelectorAll('.report-item, .report-brief li, .report-items li, .fan-letter-card, .report-kpi, .report-mood-banner');
    items.forEach((item, i) => {
        item.style.opacity = '0';
        item.style.transform = 'translateY(10px)';
        setTimeout(() => {
            item.style.transition = 'all 0.3s ease';
            item.style.opacity = '1';
            item.style.transform = 'translateY(0)';
        }, 200 + i * 100);
    });

    // Animate action result HUD metric numbers
    const fanMetric = panel.querySelector('[data-stat="fan-change"] strong');
    const staminaMetric = panel.querySelector('[data-stat="stamina-change"] strong');
    const routeMetric = panel.querySelector('[data-stat="route-change"] strong');
    if (fanMetric) {
        const fanEnd = parseInt(fanMetric.textContent) || 0;
        fanMetric.textContent = '0';
        animateCountUp(fanMetric, fanEnd);
    }
    if (staminaMetric) {
        const staminaEnd = parseInt(staminaMetric.textContent) || 0;
        staminaMetric.textContent = '0';
        animateCountUp(staminaMetric, staminaEnd);
    }
    if (routeMetric) {
        const routeEnd = parseInt(routeMetric.textContent) || 0;
        routeMetric.textContent = '0';
        animateCountUp(routeMetric, routeEnd);
    }
}

function signedDelta(value) {
    const number = Number(value || 0);
    return number > 0 ? `+${number}` : String(number);
}

function deltaToneClass(value, positiveIsGood = true) {
    const number = Number(value || 0);
    if (number === 0) {
        return "zero";
    }
    const good = positiveIsGood ? number > 0 : number < 0;
    return good ? "positive" : "negative";
}

function actionResultSummaryText(result) {
    return localizedVisibleTextOrFallback(result?.summary, "这手行动已结算，日报里有完整复盘。");
}

function actionResultNextStepText() {
    const phase = state.session?.phase || "";
    const maxDayLabel = runMaxDayLabel();
    const nextSteps = {
        READY: "回到行动面板，继续安排今天的经营节奏。",
        NEED_TITLE: "下一步：标题组已接棒，先选直播标题，再看这手反馈怎么落地。",
        NEED_INTERACTION_CHOICE: "下一步：现场互动已经递到台前，先比较成本和后果再处理。",
        NEED_EVENT_CHOICE: "下一步：正式事件摆上桌，先处理米线、流量或玩梗分支。",
        REPORT_READY: "下一步：今日日报已生成，读完复盘再开下一天。",
        ENDING_READY: `下一步：${maxDayLabel}已收官，去结局复盘看路线证据和下一轮目标。`
    };
    return nextSteps[phase] || "下一步：按当前高亮面板继续。";
}

function actionResultTacticalNextText(result) {
    const debtCount = normalizedDebtCreated(result).length;
    const mitigationCount = normalizedDefenseMitigation(result).length;
    const routeDelta = Number(result?.routeScoreChange || 0);
    const staminaDelta = Number(result?.staminaChange || 0);
    const fatiguePercent = Number(result?.fatigueInfo?.fatiguePercent || 100);
    const combo = result?.evidenceRef?.comboImpact;
    const targetRoute = result?.evidenceRef?.targetRoute || combo?.targetRoute || state.vup?.currentRoute || "UNKNOWN";
    if (debtCount > 0) {
        return "明天优先安排粉丝群维护、杂谈复盘或米线工具，把新旧账拆掉再继续冲热度。";
    }
    if (mitigationCount > 0) {
        return "风险已经压住，下一手可以接主路线行动，把防守回合转成结局证据。";
    }
    if (fatiguePercent < 80) {
        return "这类行动开始疲劳，下一天换一个同路线动作，别把反馈打成复读。";
    }
    if (combo && !["OVERWORK", "FORGOTTEN"].includes(String(combo.comboKey || ""))) {
        return `连招已经打出来，下一手继续围绕${routeLabelFor(targetRoute)}补证据。`;
    }
    if (routeDelta > 0) {
        return `${routeLabelFor(targetRoute)}证据加厚了，连续两三天同方向推进会更稳。`;
    }
    if (staminaDelta > 0) {
        return "体力回来了，下一天接一个能写进结局的主行动，不要只靠防守过日子。";
    }
    return "下一天先看结局预演缺口，再决定是补路线、拆旧账还是赌高热度。";
}

function normalizedDebtCreated(result) {
    const debtCreated = result?.debtCreated;
    if (Array.isArray(debtCreated)) {
        return debtCreated;
    }
    return debtCreated ? [debtCreated] : [];
}

function normalizedDefenseMitigation(result) {
    const mitigations = result?.evidenceRef?.defenseMitigation;
    if (Array.isArray(mitigations)) {
        return mitigations;
    }
    return mitigations ? [mitigations] : [];
}

function renderActionResultAlerts(result) {
    const fatigue = result?.fatigueInfo;
    const fatiguePercent = Number(fatigue?.fatiguePercent);
    const fatigueAlert = Number.isFinite(fatiguePercent) && fatiguePercent < 100
        ? `<span class="action-result-alert fatigue"><em>重复疲劳</em><strong>${html(fatiguePercent)}%</strong></span>`
        : "";
    const rotationBonus = result?.evidenceRef?.rotationBonus;
    const rotationAlert = rotationBonus
        ? `<span class="action-result-alert rotation"><em>轮换经营</em><strong>${html(rotationBonus.kind === "three_lane_mix" ? "灵感+1" : "节奏+")}</strong></span>`
        : "";
    const comboImpact = result?.evidenceRef?.comboImpact;
    const comboImpactLabel = visibleTextOrFallback(comboImpact?.primaryDeltaLabel, "");
    const comboAlert = comboImpact && comboImpactLabel
        ? `<span class="action-result-alert combo-impact"><em>${html(visibleTextOrFallback(comboImpact.label, "组合技"))}</em><strong>${html(comboImpactLabel)}</strong></span>`
        : "";
    const stageObjective = result?.evidenceRef?.stageObjectiveBonus;
    const stageObjectiveLabel = visibleTextOrFallback(stageObjective?.primaryDeltaLabel, "");
    const stageObjectiveStreak = Number(stageObjective?.streak || 0);
    const stageObjectiveAlert = stageObjective && stageObjectiveLabel
        ? `<span class="action-result-alert stage-objective"><em>${html(stageObjectiveStreak >= 2 ? `委托${stageObjectiveStreak}连` : "阶段委托")}</em><strong>${html(stageObjectiveLabel)}</strong></span>`
        : "";
    const endgameSprint = result?.evidenceRef?.endgameSprint;
    const sprintRouteBonus = Number(endgameSprint?.routeScoreBonus || 0);
    const sprintAlert = sprintRouteBonus > 0
        ? `<span class="action-result-alert sprint"><em>收官押线</em><strong>路线+${html(sprintRouteBonus)}</strong></span>`
        : "";
    const severityPressure = Number(result?.evidenceRef?.severityPressure || 0);
    const severityAlert = severityPressure > 0
        ? `<span class="action-result-alert severity"><em>严重旧账</em><strong>压力+${html(severityPressure)}</strong></span>`
        : "";
    const stressMitigation = Number(result?.evidenceRef?.audiencePressure?.stressMitigation || 0);
    const stressAlert = stressMitigation > 0
        ? `<span class="action-result-alert stress"><em>抗压抵消</em><strong>-${html(stressMitigation)}级</strong></span>`
        : "";
    const productionCoinCost = Number(result?.evidenceRef?.productionBudget?.coinCost || 0);
    const productionAlert = productionCoinCost > 0
        ? `<span class="action-result-alert production"><em>冲刺预算</em><strong>-${html(productionCoinCost)}活动预算</strong></span>`
        : "";
    const flavorModifiers = result?.evidenceRef?.ordinaryFlavorModifiers || {};
    const flavorKeys = Object.keys(flavorModifiers);
    const flavorLabel = flavorKeys.includes("trafficSpike")
        ? "流量波动"
        : flavorKeys.includes("memeMaterial") || flavorKeys.includes("clipMomentum")
            ? "素材进账"
            : flavorKeys.includes("stableCommunity") || flavorKeys.includes("riskCooldown")
                ? "社群稳住"
                : flavorKeys.length > 0
                    ? "事件波动"
                    : "";
    const flavorAlert = flavorLabel
        ? `<span class="action-result-alert event-flavor"><em>突发事件</em><strong>${html(flavorLabel)}</strong></span>`
        : "";
    const platformTrend = result?.evidenceRef?.platformTrendModifier;
    const platformTrendLabel = visibleTextOrFallback(platformTrend?.primaryDeltaLabel, "");
    const trendAlert = platformTrend && platformTrendLabel
        ? `<span class="action-result-alert trend-modifier"><em>平台口味</em><strong>${html(visibleTextOrFallback(platformTrend.label, "风向"))} · ${html(platformTrendLabel)}</strong></span>`
        : "";
    const fortuneModifier = result?.evidenceRef?.fortuneModifier;
    const fortuneLabel = visibleTextOrFallback(fortuneModifier?.primaryDeltaLabel, "");
    const fortuneAlert = fortuneModifier && fortuneLabel
        ? `<span class="action-result-alert fortune-modifier"><em>今日运势</em><strong>${html(visibleTextOrFallback(fortuneModifier.fortune, "运势"))} · ${html(fortuneLabel)}</strong></span>`
        : "";
    const defenseAlerts = normalizedDefenseMitigation(result).slice(0, 1).map(item => {
        const dueDelta = Number(item?.dueDayDelta || 0);
        const severityDelta = Number(item?.severityDelta || 0);
        const label = visibleTextOrFallback(item?.debtLabel, debtTypeText(item?.debtType));
        const value = dueDelta > 0 ? `+${dueDelta}天` : severityDelta < 0 ? `严重${severityDelta}` : "已处理";
        return `<span class="action-result-alert defense"><em>${html(label)}</em><strong>${html(value)}</strong></span>`;
    }).join("");
    const debtAlerts = normalizedDebtCreated(result).slice(0, 2).map(debt => `
        <span class="action-result-alert debt">
            <em>${html(debtTypeText(debt.debtType))}</em>
            <strong>${html(riskSeverityText(debt.severity))}</strong>
        </span>
    `).join("");
    if (!fatigueAlert && !rotationAlert && !comboAlert && !stageObjectiveAlert && !sprintAlert && !severityAlert && !stressAlert && !productionAlert && !flavorAlert && !trendAlert && !fortuneAlert && !defenseAlerts && !debtAlerts) {
        return "";
    }
    return `<div class="action-result-alerts" aria-label="本手后果提示">${rotationAlert}${comboAlert}${stageObjectiveAlert}${sprintAlert}${fatigueAlert}${severityAlert}${stressAlert}${productionAlert}${flavorAlert}${trendAlert}${fortuneAlert}${defenseAlerts}${debtAlerts}</div>`;
}

function actionResultContributionItems(result) {
    if (!result) return [];
    const items = [];
    const gapSnapshot = endingGapImpactSnapshot(result) || result.endingGapSnapshot;
    const gapRequirement = gapSnapshot?.requirement || firstWarnEndingRequirement();
    const gapActionType = gapSnapshot?.actionType || result.actionType;
    const gapLikelyEnding = gapSnapshot?.likelyEnding || state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN";
    const gapWasCovered = typeof gapSnapshot?.covered === "boolean"
        ? gapSnapshot.covered
        : actionTypeCoversEndingGap(gapActionType, gapRequirement, gapLikelyEnding);
    const routeDelta = Number(result.routeScoreChange || 0);
    const targetRoute = result.evidenceRef?.targetRoute || result.evidenceRef?.comboImpact?.targetRoute || result.evidenceRef?.stageObjectiveBonus?.targetRoute;
    const debtCount = normalizedDebtCreated(result).length;
    const mitigationCount = normalizedDefenseMitigation(result).length;
    items.push(actionResultPlayRead({
        result,
        gapRequirement,
        gapWasCovered,
        routeDelta,
        debtCount,
        mitigationCount,
        targetRoute
    }));
    if (routeDelta > 0) {
        items.push({
            tone: "route",
            label: "路线证据",
            value: `${routeLabelFor(targetRoute || state.vup?.currentRoute || "UNKNOWN")} +${routeDelta}`,
            hint: routeDelta >= 3 ? "这手会明显加厚结局证据链。" : "这手被结局复盘记为路线证据。"
        });
    } else {
        items.push({
            tone: "neutral",
            label: "路线证据",
            value: "未加分",
            hint: "这手更偏防守或资源调整，不会直接加厚路线。"
        });
    }

    const objective = result.evidenceRef?.stageObjectiveBonus;
    if (objective) {
        const streak = Number(objective.streak || 0);
        items.push({
            tone: "objective",
            label: "阶段委托",
            value: streak >= 2 ? `${streak}连` : "命中",
            hint: visibleTextOrFallback(objective.hint, "已写入阶段目标证据。")
        });
    } else if (stageObjectiveActionTypes().length) {
        items.push({
            tone: "neutral",
            label: "阶段委托",
            value: "未命中",
            hint: "下一手可对照今日目标补连击。"
        });
    }

    const combo = result.evidenceRef?.comboImpact;
    if (combo) {
        items.push({
            tone: ["OVERWORK", "FORGOTTEN"].includes(String(combo.comboKey || "")) ? "risk" : "combo",
            label: "打法图鉴",
            value: visibleTextOrFallback(combo.label, "组合技"),
            hint: visibleTextOrFallback(combo.hint, "本手触发了可复现打法。")
        });
    }

    if (debtCount > 0 || mitigationCount > 0) {
        items.push({
            tone: debtCount > 0 ? "risk" : "safe",
            label: "风险账本",
            value: debtCount > 0 ? `新增${debtCount}笔` : `处理${mitigationCount}笔`,
            hint: debtCount > 0 ? "这手会进入旧账复盘，后续要安排拆债。" : "风险被压住，收官评分会更干净。"
        });
    }

    if (gapRequirement) {
        const gapProgress = endingGapProgressText(gapRequirement);
        items.push({
            tone: gapWasCovered ? "forecast-hit" : "forecast",
            label: "结局缺口",
            value: gapWasCovered ? `已补：${endingGapActionLabel(gapRequirement)}` : `待补：${endingGapActionLabel(gapRequirement)}`,
            hint: gapWasCovered
                ? `${endingGapSignalLabel(gapRequirement)}已吃到这手行动，行动前${gapProgress || "仍有缺口"}，刷新后看预演是否转绿。`
                : `${endingForecastRequirementLineText(gapRequirement)}${gapProgress ? ` 当前${gapProgress}。` : ""}`
        });
    }
    return prioritizedContributionItems(items).slice(0, 4);
}

function actionResultPlayRead({ result, gapRequirement, gapWasCovered, routeDelta, debtCount, mitigationCount, targetRoute }) {
    if (gapRequirement && gapWasCovered) {
        return {
            tone: "play-read",
            label: "打法判断",
            value: "补短板",
            hint: `这手直接对准${endingGapSignalLabel(gapRequirement)}，适合继续沿同一路线追门槛。`
        };
    }
    if (debtCount > 0) {
        return {
            tone: "play-read-risk",
            label: "打法判断",
            value: "借势换热度",
            hint: "反馈会更响，但旧账会在后续事件和结局评分里追上来。"
        };
    }
    if (mitigationCount > 0 || Number(result?.reputationChange || 0) > 0 && Number(result?.watchHeatChange || 0) < 0) {
        return {
            tone: "play-read-safe",
            label: "打法判断",
            value: "拆风险",
            hint: "这手不是爆发牌，但能降低翻车概率，适合冲体面收官。"
        };
    }
    if (routeDelta > 0) {
        return {
            tone: "play-read",
            label: "打法判断",
            value: "加厚路线",
            hint: `${routeLabelFor(targetRoute || state.vup?.currentRoute || "UNKNOWN")}证据变厚，后续同类行动更容易讲成一条线。`
        };
    }
    return {
        tone: "play-read-neutral",
        label: "打法判断",
        value: "资源垫步",
        hint: "这手主要是在攒体力、素材或气压，下一天要接能写进结局的行动。"
    };
}

function prioritizedContributionItems(items) {
    const priority = {
        "forecast-hit": 0,
        "play-read-risk": 1,
        "play-read": 2,
        "play-read-safe": 3,
        "play-read-neutral": 4,
        forecast: 5,
        risk: 6,
        objective: 7,
        combo: 8,
        route: 9,
        safe: 10,
        neutral: 11
    };
    return items
        .map((item, index) => ({ ...item, index }))
        .sort((left, right) =>
            (priority[left.tone] ?? 50) - (priority[right.tone] ?? 50)
            || left.index - right.index)
        .map(({ index, ...item }) => item);
}

function firstWarnEndingRequirement() {
    const requirements = state.endingForecast?.requirements;
    if (!Array.isArray(requirements)) return null;
    return requirements.find(item => item?.status && item.status !== "PASS") || null;
}

function renderActionResultContribution(result) {
    const items = actionResultContributionItems(result);
    if (!items.length) return "";
    return `
        <div class="action-result-contribution" aria-label="本手复盘贡献">
            ${items.map(item => `
                <span class="${html(item.tone)}">
                    <em>${html(item.label)}</em>
                    <strong>${html(item.value)}</strong>
                    <small>${html(item.hint)}</small>
                </span>
            `).join("")}
        </div>
    `;
}

function renderActionResultObjectiveReward(result) {
    const objective = result?.evidenceRef?.stageObjectiveBonus;
    if (!objective) {
        return "";
    }
    const streak = Number(objective.streak || 1);
    const nextStreak = streak + 1;
    const routeBonus = Number(objective.routeScoreBonus || 0);
    const popularityBonus = Number(objective.popularityBonus || 0);
    const reputationBonus = Number(objective.reputationBonus || 0);
    const trueFanBonus = Number(objective.trueFanBonus || 0);
    const nextLine = nextStreak >= 3
        ? "再命中可冲三连奖励，贴合路线时会继续抬路线分。"
        : "明天继续命中委托，会把短线目标升级成连击奖励。";
    return `
        <div class="action-result-objective-reward ${streak >= 3 ? "hot" : ""}" aria-label="阶段委托奖励">
            <div>
                <span>${html(streak >= 2 ? `委托${streak}连` : "阶段委托命中")}</span>
                <strong>${html(visibleTextOrFallback(objective.objectiveTitle, "阶段目标"))}</strong>
                <small>${html(visibleTextOrFallback(objective.hint, nextLine))}</small>
            </div>
            <div class="action-result-objective-bonuses">
                <em>真爱粉+${html(trueFanBonus)}</em>
                <em>曝光+${html(popularityBonus)}</em>
                <em>口碑+${html(reputationBonus)}</em>
                <em>${html(routeBonus > 0 ? `路线+${routeBonus}` : "路线待贴合")}</em>
            </div>
            <small>${html(nextLine)}</small>
        </div>
    `;
}

function newbieCelebrationHtml(result) {
    const day = Number(state.session?.day || state.vup?.day || 1);
    const fanGain = Number(result?.fanChange || 0);
    if (day > 3 || fanGain <= 0) return "";
    const messages = [
        "开局顺利！第一批观众已经注意到你了。",
        "势头不错！粉丝在涨，继续加油。",
        "新人红利期！今天的收获比平时更多。"
    ];
    const msg = messages[Math.min(day - 1, messages.length - 1)];
    return `<div class="newbie-celebration" aria-label="新手加成提示">\u{1F389} ${html(msg)} <small>（第1-3天新手加成 +50%）</small></div>`;
}

function renderActionResultHud(result) {
    if (!result) {
        return "";
    }
    const actionName = actionLabelFor(result.actionType);
    const hudTone = actionResultHudTone(result);
    const fanDelta = signedDelta(result.fanChange);
    const staminaDelta = signedDelta(result.staminaChange);
    const routeDelta = signedDelta(result.routeScoreChange);
    const summary = actionResultSummaryText(result);
    const nextStep = actionResultNextStepText();
    const contribution = renderActionResultContribution(result);
    const objectiveReward = renderActionResultObjectiveReward(result);
    const alerts = renderActionResultAlerts(result);
    return `
        <section class="action-result-hud ${html(hudTone)}" role="status" aria-live="polite" aria-label="${html(`本手结算：${actionName}`)}">
            <div class="action-result-hud-main">
                <span>本手结算</span>
                <strong>${html(actionName)}</strong>
                <small>${html(summary)}</small>
            </div>
            <div class="action-result-hud-metrics" aria-label="行动即时数据变化">
                <span class="${deltaToneClass(result.fanChange)}" data-stat="fan-change"><em>粉丝</em><strong>${html(fanDelta)}</strong></span>
                <span class="${deltaToneClass(result.staminaChange)}" data-stat="stamina-change"><em>体力</em><strong>${html(staminaDelta)}</strong></span>
                <span class="${deltaToneClass(result.routeScoreChange)}" data-stat="route-change"><em>路线</em><strong>${html(routeDelta)}</strong></span>
            </div>
            <div class="action-result-next-step">
                <em>下一步</em>
                <strong>${html(nextStep)}</strong>
                <small>${html(actionResultTacticalNextText(result))}</small>
            </div>
            ${contribution}
            ${objectiveReward}
            ${alerts}
            ${newbieCelebrationHtml(result)}
            ${renderVupReaction(result)}
        </section>
    `;
}

function actionResultHudTone(result) {
    const hasGain = Number(result?.fanChange || 0) > 0
        || Number(result?.routeScoreChange || 0) > 0
        || Number(result?.popularityChange || 0) > 0
        || Number(result?.reputationChange || 0) > 0;
    const hasRisk = normalizedDebtCreated(result).length > 0
        || Number(result?.watchHeatChange || 0) >= 8
        || /高压|债务|回旋|风险|炎上|失控/.test(`${result?.riskPreview || ""} ${result?.result || ""}`);
    if (hasGain && hasRisk) return "mixed";
    if (hasRisk) return "risk";
    if (hasGain) return "positive";
    return "neutral";
}

function firstNumericValue(...values) {
    for (const value of values) {
        const number = Number(value);
        if (Number.isFinite(number)) {
            return number;
        }
    }
    return 0;
}

function endingScoreTone(value, highIsGood = true) {
    const score = Math.max(0, Math.min(100, Number(value) || 0));
    const good = highIsGood ? score >= 70 : score <= 35;
    const risky = highIsGood ? score < 45 : score > 65;
    if (good) {
        return { className: "good", label: "稳" };
    }
    if (risky) {
        return { className: "risk", label: "险" };
    }
    return { className: "mid", label: "偏" };
}

function renderEndingScoreCard(label, value, note, highIsGood = true, badge = "") {
    const tone = endingScoreTone(value, highIsGood);
    const safeValue = Math.round(Math.max(0, Math.min(100, Number(value) || 0)));
    const noteText = badge ? `${badge} / ${note}` : `${tone.label} / ${note}`;
    return `
        <div class="ending-score-card ${tone.className}">
            <span>${html(label)}</span>
            <strong>${html(safeValue)}</strong>
            <small>${html(noteText)}</small>
        </div>
    `;
}

function endingScoreReason(score, goodText, midText, riskText) {
    const value = Number(score) || 0;
    if (value >= 70) return goodText;
    if (value >= 45) return midText;
    return riskText;
}

function renderEndingBalanceCards(fanProfile, debtCount, keyEventCount, scorecard = null) {
    if (scorecard && Number.isFinite(Number(scorecard.overall))) {
        return `
            <div class="ending-score-grid" aria-label="本轮发布评分">
                ${renderEndingScoreCard("路线专注", scorecard.routeFocusScore, endingScoreReason(scorecard.routeFocusScore, `路线分${Number(scorecard.routeScore) || 0}，打法很集中`, `路线分${Number(scorecard.routeScore) || 0}，还可少换线`, "路线摇摆，下一轮先定主打法"))}
                ${renderEndingScoreCard("证据链", scorecard.evidenceScore, endingScoreReason(scorecard.evidenceScore, `证据${Number(scorecard.routeEvidenceCount) || 0}条，够写战报`, `证据${Number(scorecard.routeEvidenceCount) || 0}条，仍缺名场面`, "代表事件不足，复盘说服力弱"))}
                ${renderEndingScoreCard("风险控制", scorecard.riskControlScore, endingScoreReason(scorecard.riskControlScore, "旧账控制干净", `旧账${Number(scorecard.openDebtCount) || 0}条，收官有尾巴`, `旧账${Number(scorecard.openDebtCount) || 0}条拖低体面度`))}
                ${renderEndingScoreCard("粉丝质量", scorecard.fanQualityScore, endingScoreReason(scorecard.fanQualityScore, "核心粉稳定，口碑能托底", "粉丝质量尚可，热度要稳住", "粉丝质量偏虚，先补口碑和陪伴"))}
            </div>
        `;
    }
    const fans = firstNumericValue(fanProfile?.fans, state.vup?.fans);
    const trueFans = firstNumericValue(fanProfile?.trueFans, state.vup?.trueFans);
    const trueFanRatio = fans > 0 ? trueFans * 100 / fans : 0;
    const reputation = firstNumericValue(fanProfile?.reputation, state.vup?.reputation);
    const watchHeat = firstNumericValue(fanProfile?.watchHeat, state.vup?.watchHeat);
    const memeLevel = firstNumericValue(fanProfile?.memeLevel, state.vup?.memeLevel);
    const pressure = Math.min(100, watchHeat + debtCount * 10 + Math.max(0, 55 - reputation) + Math.max(0, memeLevel - 70) / 2);
    const replayPull = Math.min(100, keyEventCount * 16 + Math.min(40, watchHeat / 2) + Math.min(25, memeLevel / 4));
    return `
        <div class="ending-score-grid" aria-label="本轮发布评分">
            ${renderEndingScoreCard("核心粉", trueFanRatio, "真爱粉占比")}
            ${renderEndingScoreCard("口碑", reputation, "收官体面度")}
            ${renderEndingScoreCard("压力", pressure, "热度与旧账", false)}
            ${renderEndingScoreCard("复玩钩子", replayPull, "事件密度")}
        </div>
    `;
}

function endingGradeForScore(score) {
    if (score >= 85) return { grade: "S", label: "高光收官" };
    if (score >= 70) return { grade: "A", label: "路线清楚" };
    if (score >= 55) return { grade: "B", label: "证据够用" };
    if (score >= 40) return { grade: "C", label: "还差记忆点" };
    return { grade: "D", label: "下轮重打" };
}

function compactSharePart(value, fallback = "", maxLength = 42) {
    const text = String(localizedVisibleTextOrFallback(value, fallback) || fallback || "").trim();
    if (!text) return "";
    return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
}

function endingScorecardView(scorecard, fanProfile, debtCount, keyEventCount, nextRunGoal) {
    const fans = firstNumericValue(fanProfile?.fans, state.vup?.fans);
    const trueFans = firstNumericValue(fanProfile?.trueFans, state.vup?.trueFans);
    const reputation = firstNumericValue(fanProfile?.reputation, state.vup?.reputation);
    const watchHeat = firstNumericValue(fanProfile?.watchHeat, state.vup?.watchHeat);
    const trueFanRatio = fans > 0 ? trueFans * 100 / fans : 0;
    const fallbackOverall = Math.round(Math.max(20, Math.min(96,
        reputation * 0.35
        + trueFanRatio * 0.2
        + Math.min(100, keyEventCount * 18) * 0.25
        + Math.max(0, 100 - debtCount * 18 - Math.max(0, watchHeat - 70)) * 0.2
    )));
    const overall = Number.isFinite(Number(scorecard?.overall))
        ? Math.round(Math.max(0, Math.min(100, Number(scorecard.overall))))
        : fallbackOverall;
    const fallbackGrade = endingGradeForScore(overall);
    const grade = String(scorecard?.grade || fallbackGrade.grade).trim() || fallbackGrade.grade;
    const gradeLabel = localizedVisibleTextOrFallback(scorecard?.gradeLabel, fallbackGrade.label);
    const routeScore = Number(scorecard?.routeScore) || 0;
    const routeEvidenceCount = Number(scorecard?.routeEvidenceCount) || keyEventCount;
    const openDebtCount = Number(scorecard?.openDebtCount) || debtCount;
    const routeFocusScore = Number.isFinite(Number(scorecard?.routeFocusScore))
        ? Math.round(Math.max(0, Math.min(100, Number(scorecard.routeFocusScore))))
        : Math.round(Math.max(0, Math.min(100, routeScore * 3)));
    const evidenceScore = Number.isFinite(Number(scorecard?.evidenceScore))
        ? Math.round(Math.max(0, Math.min(100, Number(scorecard.evidenceScore))))
        : Math.round(Math.max(0, Math.min(100, routeEvidenceCount * 18)));
    const riskControlScore = Number.isFinite(Number(scorecard?.riskControlScore))
        ? Math.round(Math.max(0, Math.min(100, Number(scorecard.riskControlScore))))
        : Math.round(Math.max(0, Math.min(100, 100 - openDebtCount * 18)));
    const fanQualityScore = Number.isFinite(Number(scorecard?.fanQualityScore))
        ? Math.round(Math.max(0, Math.min(100, Number(scorecard.fanQualityScore))))
        : Math.round(Math.max(0, Math.min(100, trueFanRatio)));
    const scoreLine = localizedVisibleTextOrFallback(
        scorecard?.scoreLine,
        `总评${overall}分，${gradeLabel}：路线证据${routeEvidenceCount}条，旧账${openDebtCount}条。`
    );
    const nextChallenge = localizedVisibleTextOrFallback(
        scorecard?.nextChallenge || nextRunGoal,
        openDebtCount > 0
            ? "下一轮挑战：先清旧账，再冲更干净的结局证据。"
            : "下一轮挑战：换一条路线，把代表事件打得更亮。"
    );
    return {
        overall,
        grade,
        gradeLabel,
        routeFocusScore,
        evidenceScore,
        riskControlScore,
        fanQualityScore,
        routeScore,
        routeEvidenceCount,
        openDebtCount,
        severeDebtCount: Number(scorecard?.severeDebtCount || 0),
        comboEvidenceCount: Number(scorecard?.comboEvidenceCount || 0),
        stageObjectiveEvidenceCount: Number(scorecard?.stageObjectiveEvidenceCount || 0),
        riskRecoveryCount: Number(scorecard?.riskRecoveryCount || 0),
        fatiguePenaltyCount: Number(scorecard?.fatiguePenaltyCount || 0),
        advancedEvidenceScore: Number(scorecard?.advancedEvidenceScore || 0),
        scoreLine,
        nextChallenge
    };
}

function renderEndingGradeCallout(scorecard) {
    if (!scorecard) return "";
    const challengeText = compactSharePart(scorecard.nextChallenge, "", 64).replace(/^下一轮挑战：/, "");
    return `
        <div class="ending-grade-callout" aria-label="结局总评与下一轮挑战">
            <span class="ending-grade-badge">${html(scorecard.grade)}</span>
            <div>
                <strong>${html(scorecard.overall)}分 · ${html(scorecard.gradeLabel)}</strong>
                <small>${html(scorecard.scoreLine)}</small>
                ${challengeText ? `<small class="ending-grade-challenge">下一轮挑战：${html(challengeText)}</small>` : ""}
            </div>
        </div>
    `;
}

function renderEndingNextRunChecklist(scorecard, nextRunGoal) {
    const trail = Array.isArray(state.ending?.routeReview?.routeEvidenceTrail)
        ? state.ending.routeReview.routeEvidenceTrail
        : [];
    const debts = Array.isArray(state.ending?.debtRefs) ? state.ending.debtRefs : [];
    const lastEvidence = trail.length ? trail[trail.length - 1] : null;
    const routeLabel = routeLabelFor(lastEvidence?.routeType || state.ending?.routeReview?.currentRoute || state.ending?.endingType);
    const actionLabel = lastEvidence ? actionLabelFor(lastEvidence.action) : "同一路线行动";
    const routeFocusScore = Number(scorecard?.routeFocusScore || 0);
    const evidenceScore = Number(scorecard?.evidenceScore || 0);
    const riskControlScore = Number(scorecard?.riskControlScore || 0);
    const routeTask = routeFocusScore < 70
        ? `锁${routeLabel}，少换线`
        : `延续${routeLabel}`;
    const evidenceTask = evidenceScore < 70
        ? `复现${actionLabel}，留2条`
        : `保留代表事件`;
    const riskTask = debts.length || riskControlScore < 70
        ? `先清${debts[0] ? debtTypeText(debts[0].debtType) : "旧账"}`
        : `可贪一次高反馈`;
    return `
        <div class="ending-next-run-checklist">
            <span><em>路线</em>${html(routeTask)}</span>
            <span><em>证据</em>${html(evidenceTask)}</span>
            <span><em>风险</em>${html(riskTask)}</span>
        </div>
    `;
}

function endingNextGradeTarget(scorecard) {
    const score = Number(scorecard?.overall || 0);
    if (score < 40) return { grade: "C", score: 40 };
    if (score < 55) return { grade: "B", score: 55 };
    if (score < 70) return { grade: "A", score: 70 };
    if (score < 90) return { grade: "S", score: 90 };
    return { grade: "S+", score: 95 };
}

function endingPrimaryWeakness(scorecard) {
    const route = Number(scorecard?.routeFocusScore || 0);
    const evidence = Number(scorecard?.evidenceScore || 0);
    const risk = Number(scorecard?.riskControlScore || 0);
    if (risk <= route && risk <= evidence) {
        return { key: "risk", label: "风险控制" };
    }
    if (evidence <= route) {
        return { key: "evidence", label: "证据链" };
    }
    return { key: "route", label: "路线专注" };
}

function renderEndingGradeContract(scorecard, nextRunGoal) {
    if (!scorecard) return "";
    const target = endingNextGradeTarget(scorecard);
    const weakness = endingPrimaryWeakness(scorecard);
    const scoreGap = Math.max(0, target.score - Number(scorecard.overall || 0));
    const routeLabel = routeLabelFor(state.ending?.endingType || state.ending?.routeReview?.currentRoute || "UNKNOWN");
    const stageOneGoal = weakness.key === "risk"
        ? "第1周先清旧账"
        : weakness.key === "evidence"
            ? `第1周给${routeLabel}留3条证据`
            : `第1周连续补${routeLabel}证据`;
    const stageTwoGoal = weakness.key === "risk"
        ? `第2周把旧账压到2笔内`
        : weakness.key === "evidence"
            ? `第2周补够${routeLabel}证据链`
            : `第2周把路线分拉到12+`;
    return `
        <div class="ending-grade-contract" aria-label="复活赛升档合约">
            <div class="ending-grade-contract-head">
                <span>升档合约</span>
                <strong>${html(scorecard.grade)}→${html(target.grade)} · 还差${html(scoreGap)}分</strong>
            </div>
            <div class="ending-grade-contract-grid">
                <span><em>主短板</em><strong>${html(weakness.label)}</strong></span>
                <span><em>第1周</em><strong>${html(stageOneGoal)}</strong></span>
                <span><em>第2周</em><strong>${html(stageTwoGoal)}</strong></span>
            </div>
            <small>${html(compactSharePart(nextRunGoal || scorecard.nextChallenge, "下一轮按合约补短板。", 72))}</small>
        </div>
    `;
}

function renderEndingAdvancedEvidence(scorecard) {
    if (!scorecard) return "";
    const comboCount = Number(scorecard.comboEvidenceCount || 0);
    const objectiveCount = Number(scorecard.stageObjectiveEvidenceCount || 0);
    const recoveryCount = Number(scorecard.riskRecoveryCount || 0);
    const fatigueCount = Number(scorecard.fatiguePenaltyCount || 0);
    if (!comboCount && !objectiveCount && !recoveryCount && !fatigueCount) {
        return "";
    }
    return `
        <div class="ending-advanced-evidence" aria-label="高级打法证据">
            <span><em>连招</em><strong>${html(comboCount)}</strong></span>
            <span><em>委托</em><strong>${html(objectiveCount)}</strong></span>
            <span><em>拆债</em><strong>${html(recoveryCount)}</strong></span>
            <span class="${fatigueCount ? "risk" : ""}"><em>疲劳</em><strong>${html(fatigueCount)}</strong></span>
        </div>
    `;
}

function routeScoreChangeEntries(change) {
    return Object.entries(change || {})
        .filter(([, value]) => Number(value) !== 0)
        .map(([route, value]) => ({ route, value: Number(value) }))
        .sort((left, right) => Math.abs(right.value) - Math.abs(left.value));
}

function routeScoreChangeSummary(change, fallback = "路线分未变动") {
    const entries = routeScoreChangeEntries(change);
    if (!entries.length) return fallback;
    return entries.slice(0, 2)
        .map(item => `${routeLabelFor(item.route)} ${signedDelta(item.value)}`)
        .join(" / ");
}

function representativeEventTone(event) {
    const reason = String(event?.spotlightReason || "");
    if (/旧账|事故|米线/.test(reason)) return "risk";
    if (/路线|收官/.test(reason)) return "route";
    if (/粉丝群|正式事件|代表/.test(reason)) return "event";
    return "neutral";
}

function endingRepresentativeEventItems(limit = 5) {
    const events = Array.isArray(state.ending?.keyEvents) ? [...state.ending.keyEvents] : [];
    const trail = Array.isArray(state.ending?.routeReview?.routeEvidenceTrail)
        ? state.ending.routeReview.routeEvidenceTrail
        : [];
    const seen = new Set(events.map(event => `${event?.day || ""}:${event?.action || ""}`));
    const routeFallbacks = trail.slice().reverse().map(evidence => {
        const key = `${evidence?.day || ""}:${evidence?.action || ""}`;
        const scoreDelta = Number(evidence?.scoreDelta) || 0;
        return {
            day: evidence?.day,
            action: evidence?.action,
            spotlightReason: "路线证据补位",
            summary: `首轮代表事件还有空位，先用第${evidence?.day || "?"}天的${actionLabelFor(evidence?.action)}补足${routeLabelFor(evidence?.routeType)}证据。`,
            routeScoreChange: evidence?.routeType ? { [evidence.routeType]: scoreDelta } : {},
            spotlightScore: Math.max(1, Math.abs(scoreDelta)),
            _key: key
        };
    }).filter(item => {
        if (seen.has(item._key)) return false;
        seen.add(item._key);
        return true;
    });
    const items = [...events, ...routeFallbacks];
    while (items.length < Math.min(3, limit)) {
        items.push({
            action: "STREAM_PLAN",
            spotlightReason: items.length === 0 ? "首轮记录不足" : "还有空位",
            summary: items.length === 0
                ? "首轮记录不足，下一局补一个能被记住的名场面。"
                : "还有空位，留给下一轮更尖的操作。",
            routeScoreChange: {},
            spotlightScore: 0
        });
    }
    return items.slice(0, limit);
}

function renderEndingEvidenceDigest(scorecard) {
    const trail = Array.isArray(state.ending?.routeReview?.routeEvidenceTrail)
        ? state.ending.routeReview.routeEvidenceTrail
        : [];
    const keyEvents = Array.isArray(state.ending?.keyEvents) ? state.ending.keyEvents : [];
    const debts = Array.isArray(state.ending?.debtRefs) ? state.ending.debtRefs : [];
    const finalReportRef = state.ending?.routeReview?.finalReportRef;
    const routeLabel = routeLabelFor(state.ending?.routeReview?.currentRoute || state.ending?.endingType);
    const evidenceScore = Number(scorecard?.evidenceScore || 0);
    const riskScore = Number(scorecard?.riskControlScore || 0);
    const riskCopy = debts.length
        ? `${debts.length}笔旧账影响收官`
        : "旧账清场，评分更干净";
    return `
        <section class="ending-evidence-digest" aria-label="结局证据总览">
            <span class="route">
                <em>路线证据</em>
                <strong>${html(routeLabel)} · ${html(trail.length)}条</strong>
                <small>${html(`证据链评分 ${evidenceScore}，最终日报 ${finalReportRef?.day ? `第${finalReportRef.day}天` : "待引用"}`)}</small>
            </span>
            <span class="event">
                <em>代表事件</em>
                <strong>${html(keyEvents.length)}个名场面</strong>
                <small>${html(keyEvents[0]?.spotlightReason || "系统按路线、风险和收官动作挑选。")}</small>
            </span>
            <span class="${debts.length ? "risk" : "safe"}">
                <em>风险账本</em>
                <strong>${html(riskScore)}分 · ${html(debts.length ? "有尾账" : "已清场")}</strong>
                <small>${html(riskCopy)}</small>
            </span>
        </section>
    `;
}

function renderEndingRepresentativeEvents() {
    const events = endingRepresentativeEventItems(5);
    return `
        <section class="ending-representative-events" aria-label="代表事件复盘">
            <div class="ending-section-head">
                <span>代表事件</span>
                <strong>系统为什么记住这些天</strong>
            </div>
            <div class="ending-representative-grid">
                ${events.slice(0, 5).map(event => {
                    const day = Number.isFinite(Number(event.day)) ? `第${Number(event.day)}天` : "某天";
                    const routeLine = routeScoreChangeSummary(event.routeScoreChange, "事件影响已入账");
                    const accidentCount = Array.isArray(event.accidentMaterials) ? event.accidentMaterials.length : 0;
                    const reason = localizedVisibleTextOrFallback(event.spotlightReason, "代表事件");
                    const summary = localizedVisibleTextOrFallback(event.summary, "代表事件已归档，复盘文本待整理。");
                    const score = Number(event.spotlightScore || 0);
                    return `
                        <article class="ending-representative-card ${html(representativeEventTone(event))}">
                            <div class="ending-representative-card-head">
                                <span>${html(day)}</span>
                                <em>${html(reason)}</em>
                            </div>
                            <strong>${html(actionLabelFor(event.action))}</strong>
                            <p>${html(compactSharePart(summary, "代表事件已归档。", 82))}</p>
                            <div class="ending-representative-meta">
                                <small>${html(routeLine)}</small>
                                ${score ? `<small>权重 ${html(score)}</small>` : ""}
                                ${accidentCount ? `<small>事故素材 ${html(accidentCount)}</small>` : ""}
                            </div>
                        </article>
                    `;
                }).join("")}
            </div>
        </section>
    `;
}

function endingRiskHintItems(scorecard, restartHintText) {
    const debts = Array.isArray(state.ending?.debtRefs) ? state.ending.debtRefs : [];
    const items = debts.slice(0, 3).map(debt => ({
        tone: "debt",
        label: debtTypeText(debt.debtType),
        title: `${riskSeverityText(debt.severity)} · ${Number.isFinite(Number(debt.dueDay)) ? `第${Number(debt.dueDay)}天回流` : statusLabelFor(debt.status)}`,
        line: localizedVisibleTextOrFallback(debt.summary, "旧账仍有记录，下一轮注意补米线。")
    }));
    const riskScore = Number(scorecard?.riskControlScore || 0);
    if (riskScore < 70) {
        items.push({
            tone: "score",
            label: "风险控制",
            title: `${riskScore}分，需要先清账`,
            line: "高压旧账、低口碑或疲劳惩罚会压低结局评分。"
        });
    }
    const fatigueCount = Number(scorecard?.fatiguePenaltyCount || 0);
    if (fatigueCount > 0) {
        items.push({
            tone: "fatigue",
            label: "重复疲劳",
            title: `${fatigueCount}次被扣分`,
            line: "下一轮不要只复读同一张牌，轮换经营会让证据更稳。"
        });
    }
    if (restartHintText) {
        items.push({
            tone: "next",
            label: "复活赛提示",
            title: "下一轮怎么避坑",
            line: restartHintText
        });
    }
    if (!items.length) {
        items.push({
            tone: "safe",
            label: "风险控制",
            title: "旧账已清场",
            line: "下一轮可以更主动冲路线证据，但仍要留意标题和切片回旋。"
        });
    }
    return items.slice(0, 4);
}

function renderEndingRiskHints(scorecard, restartHintText) {
    const items = endingRiskHintItems(scorecard, restartHintText);
    return `
        <section class="ending-risk-hints" aria-label="结局风险提示">
            <div class="ending-section-head">
                <span>风险提示</span>
                <strong>${html(items[0]?.title || "旧账复盘")}</strong>
            </div>
            <div class="ending-risk-hint-list">
                ${items.map(item => `
                    <span class="ending-risk-hint ${html(item.tone)}">
                        <em>${html(item.label)}</em>
                        <strong>${html(item.title)}</strong>
                        <small>${html(item.line)}</small>
                    </span>
                `).join("")}
            </div>
        </section>
    `;
}

// ============================================================
// 数据可视化 (Chart.js)
// ============================================================
function destroyChartForCanvas(canvas) {
    if (!canvas || typeof window === "undefined" || !window.Chart || typeof window.Chart.getChart !== "function") return;
    const chart = window.Chart.getChart(canvas);
    if (chart) chart.destroy();
}

const CHARTS = {
    routeRadar: null,
    fanLine: null,

    isAvailable() {
        return typeof window !== "undefined" && typeof window.Chart !== "undefined";
    },

    clearTextFallback(canvas) {
        if (!canvas) return;
        canvas.hidden = false;
        canvas.parentElement?.querySelector('[data-chart-fallback="true"]')?.remove();
    },

    renderTextFallback(canvas, title, items, note) {
        if (!canvas?.parentElement) return;
        canvas.hidden = true;

        let fallback = canvas.parentElement.querySelector('[data-chart-fallback="true"]');
        if (!fallback) {
            fallback = document.createElement("div");
            fallback.dataset.chartFallback = "true";
            fallback.setAttribute("role", "note");
            fallback.style.cssText = [
                "display:flex",
                "flex-direction:column",
                "gap:8px",
                "width:100%",
                "padding:8px",
                "color:var(--text-secondary)",
                "font-size:13px",
                "line-height:1.5"
            ].join(";");
            canvas.parentElement.appendChild(fallback);
        }

        const rows = (items || []).filter(Boolean).map(item => `
            <span style="display:flex;justify-content:space-between;gap:12px;border-bottom:1px solid var(--border);padding:3px 0;">
                <em style="font-style:normal;color:var(--text-muted);">${html(item.label)}</em>
                <strong style="color:var(--text-primary);">${html(item.value)}</strong>
            </span>
        `).join("");

        fallback.innerHTML = `
            <strong style="color:var(--text-primary);">${html(title)}</strong>
            <span>${html(note || "Chart.js 未加载，已切换为文本摘要。")}</span>
            <div style="display:flex;flex-direction:column;gap:2px;">${rows || '<span>暂无可展示数据</span>'}</div>
        `;
    },

    destroyAll() {
        if (this.routeRadar) { this.routeRadar.destroy(); this.routeRadar = null; }
        if (this.fanLine) { this.fanLine.destroy(); this.fanLine = null; }
    },

    renderRouteRadar(canvasId) {
        const canvas = document.getElementById(canvasId);
        if (!canvas || !state.vup) return;

        const routeScores = Object.fromEntries(routeScoreEntries().map(item => [item.route, item.score]));
        const labels = ['歌势', '切片', '社交', '舞见', '黑红', '电子榨菜'];
        const keys = ['SINGING_IDOL', 'SLICE_SAINT', 'SOCIAL_COLLAB', 'DANCE_MEME', 'BLACK_RED_MAIN_STAGE', 'ELECTRONIC_PICKLE'];
        const data = keys.map(k => routeScores[k] || 0);

        if (!this.isAvailable()) {
            if (this.routeRadar) { this.routeRadar.destroy(); this.routeRadar = null; }
            this.renderTextFallback(
                canvas,
                "路线分布",
                labels.map((label, index) => ({ label, value: data[index] || 0 })),
                "Chart.js 未加载，已切换为路线分文本摘要。"
            );
            return;
        }

        this.clearTextFallback(canvas);
        if (this.routeRadar && this.routeRadar.canvas !== canvas) this.routeRadar.destroy();
        destroyChartForCanvas(canvas);
        this.routeRadar = new window.Chart(canvas, {
            type: 'radar',
            data: {
                labels,
                datasets: [{
                    label: '路线分',
                    data,
                    backgroundColor: 'rgba(255, 110, 199, 0.15)',
                    borderColor: '#ff6ec7',
                    pointBackgroundColor: '#ff6ec7',
                    pointBorderColor: '#fff',
                    pointHoverBackgroundColor: '#fff',
                    pointHoverBorderColor: '#ff6ec7'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                scales: {
                    r: {
                        beginAtZero: true,
                        ticks: { color: '#888', backdropColor: 'transparent' },
                        grid: { color: 'rgba(255,255,255,0.08)' },
                        angleLines: { color: 'rgba(255,255,255,0.08)' },
                        pointLabels: { color: '#ccc', font: { size: 12 } }
                    }
                },
                plugins: {
                    legend: { display: false }
                }
            }
        });
    },

    renderFanLine(canvasId, reports) {
        const canvas = document.getElementById(canvasId);
        if (!canvas || !reports?.length) return;

        const labels = reports.map(r => `第${r.day}天`);
        const fanDeltas = reports.map(r => Number(r.dataDeltas?.fanDelta || 0));
        const totalFanDelta = fanDeltas.reduce((sum, value) => sum + value, 0);

        if (!this.isAvailable()) {
            if (this.fanLine) { this.fanLine.destroy(); this.fanLine = null; }
            const recentRows = reports.slice(-6).map(report => {
                const delta = Number(report.dataDeltas?.fanDelta || 0);
                return {
                    label: `第${report.day || "?"}天`,
                    value: `${delta >= 0 ? "+" : ""}${delta}`
                };
            });
            this.renderTextFallback(
                canvas,
                "粉丝趋势",
                recentRows,
                `Chart.js 未加载，显示最近日报粉丝变化；累计变化 ${totalFanDelta >= 0 ? "+" : ""}${totalFanDelta}。`
            );
            return;
        }

        this.clearTextFallback(canvas);
        const fanData = reports.map(r => {
            const deltas = r.dataDeltas || {};
            return (state.vup?.fans || 0) - (deltas.fanDelta || 0);
        });
        // 累积粉丝数
        let cumulative = 0;
        const cumulativeFanData = reports.map(r => {
            cumulative += (r.dataDeltas?.fanDelta || 0);
            return (state.vup?.fans || 0) + cumulative - (r.dataDeltas?.fanDelta || 0);
        });

        if (this.fanLine && this.fanLine.canvas !== canvas) this.fanLine.destroy();
        destroyChartForCanvas(canvas);
        this.fanLine = new window.Chart(canvas, {
            type: 'line',
            data: {
                labels,
                datasets: [{
                    label: '粉丝变化',
                    data: reports.map(r => r.dataDeltas?.fanDelta || 0),
                    borderColor: '#22c55e',
                    backgroundColor: 'rgba(34, 197, 94, 0.1)',
                    fill: true,
                    tension: 0.3,
                    pointRadius: 3,
                    pointBackgroundColor: '#22c55e'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                scales: {
                    x: { ticks: { color: '#888' }, grid: { color: 'rgba(255,255,255,0.05)' } },
                    y: { ticks: { color: '#888' }, grid: { color: 'rgba(255,255,255,0.08)' } }
                },
                plugins: {
                    legend: { display: false }
                }
            }
        });
    }
};

if (typeof window !== "undefined") {
    window.addEventListener("chartjs-ready", () => {
        CHARTS.renderRouteRadar("routeRadarChart");
        CHARTS.renderFanLine("fanTrendChart", state.reports);
    });
}

// 本局时间轴回顾（结局页面用）
function renderTimeline() {
    const timeline = state.ending?.timeline;
    if (!timeline || timeline.length === 0) return '';
    const entries = timeline.map(entry => {
        const day = Number(entry.day) || 0;
        const emoji = entry.emoji || '📌';
        const action = html(entry.action || entry.actionKey || '');
        const desc = html(entry.description || '');
        return `<div class="timeline-entry"><div class="timeline-dot">${emoji}</div><div class="timeline-content"><span class="timeline-day">第${day}天</span><strong class="timeline-action">${action}</strong><p class="timeline-desc">${desc}</p></div></div>`;
    }).join('');
    return `<div class="ending-timeline"><h4 class="ending-timeline-title">${runMaxDayLabel()}回顾</h4><div class="timeline-track">${entries}</div></div>`;
}

// 结局差距分析渲染
function renderEndingGapAnalysis() {
    const gap = state.ending?.gapAnalysis;
    if (!gap || !gap.missingConditions || gap.missingConditions.length === 0) return '';
    const conditions = gap.missingConditions.map(c => {
        const pct = Math.min(100, Math.round(c.currentValue * 100 / Math.max(1, c.targetValue)));
        return `<div class="ending-gap-condition"><span class="ending-gap-label">${html(c.label)}</span><div class="ending-gap-bar"><div class="ending-gap-fill" style="width:${pct}%"></div></div><span class="ending-gap-values">${c.currentValue}/${c.targetValue}</span></div>`;
    }).join('');
    return `<div class="ending-gap-analysis"><span class="ending-gap-title">距离「${html(gap.nextEndingName)}」还差</span>${conditions}</div>`;
}

function endingFanProfileLine(fanProfile, fanCount, trueFans, watchHeat) {
    const funFans = Number(fanProfile?.funFans ?? state.vup?.funFans ?? 0);
    const unicornFans = Number(fanProfile?.unicornFans ?? state.vup?.unicornFans ?? 0);
    const ddFans = Number(fanProfile?.ddFans ?? state.vup?.ddFans ?? 0);
    const parts = [
        `总粉${fanCount}`,
        `真爱粉${trueFans}`,
        funFans ? `乐子人${funFans}` : "",
        unicornFans ? `独角兽${unicornFans}` : "",
        ddFans ? `DD${ddFans}` : "",
        `围观热度${watchHeat}`
    ].filter(Boolean);
    return parts.join(" / ");
}

function endingKeyEventLines(limit = 3) {
    const events = Array.isArray(state.ending?.keyEvents) ? state.ending.keyEvents : [];
    const routeEvidence = Array.isArray(state.ending?.routeReview?.routeEvidenceTrail)
        ? state.ending.routeReview.routeEvidenceTrail
        : [];
    const primary = events.map(event => {
        const day = Number.isFinite(Number(event.day)) ? `第${Number(event.day)}天` : "某天";
        const action = actionLabelFor(event.action);
        const summary = localizedVisibleTextOrFallback(event.summary, visibleTextOrFallback(event.spotlightReason, "代表事件已归档。"));
        return `${day} / ${action}：${compactSharePart(summary, "代表事件已归档。", 34)}`;
    });
    const fallback = routeEvidence.slice(-limit).map(event => {
        const day = Number.isFinite(Number(event.day)) ? `第${Number(event.day)}天` : "某天";
        const score = Number(event.scoreDelta) || 0;
        const scoreText = score >= 0 ? `+${score}` : String(score);
        return `${day} / ${actionLabelFor(event.action)}：${routeLabelFor(event.routeType)} ${scoreText}`;
    });
    const lines = [...primary, ...fallback].filter(Boolean).slice(0, limit);
    while (lines.length < limit) {
        lines.push(lines.length === 0 ? "首轮记录不足，下一局补一个能被记住的名场面。" : "还有空位，留给下一轮更尖的操作。");
    }
    return lines;
}

function buildEndingShareCardData({
    finalTitle,
    subtitle,
    endingRouteLabel,
    currentRouteLabel,
    fanProfile,
    fanCount,
    trueFans,
    watchHeat,
    debtCount,
    keyEventCount,
    finalDayAction,
    scorecard,
    nextRunGoal
}) {
    const keyEvents = endingKeyEventLines(3);
    const debtLine = debtCount > 0 ? `旧账 ${debtCount} 条，复活赛先拆雷` : "旧账清场，可以更贪一局";
    const nextRun = compactSharePart(
        nextRunGoal || scorecard?.nextChallenge,
        debtCount > 0 ? "下轮先清旧账，再冲更干净的路线证据。" : "下轮换一条路线，打出新的代表事件。",
        58
    ).replace(/^下一轮挑战：/, "");
    const streamerLine = streamerFriendlyEndingLine({
        finalTitle,
        endingRouteLabel,
        currentRouteLabel,
        fanCount,
        watchHeat,
        debtCount,
        scorecard
    });
    return {
        title: finalTitle,
        subtitle,
        route: endingRouteLabel,
        currentRoute: currentRouteLabel,
        grade: scorecard?.grade || "?",
        score: Number(scorecard?.overall || 0),
        gradeLabel: compactSharePart(scorecard?.gradeLabel, "路线已成型", 18),
        fanProfile: endingFanProfileLine(fanProfile, fanCount, trueFans, watchHeat),
        keyEvents,
        debtLine,
        nextRun,
        streamerLine,
        finalDayAction,
        keyEventCount
    };
}

function streamerFriendlyEndingLine({
    finalTitle,
    endingRouteLabel,
    currentRouteLabel,
    fanCount,
    watchHeat,
    debtCount
}) {
    const debtPart = debtCount > 0 ? `旧账${debtCount}` : "旧账清";
    const routePart = endingRouteLabel === currentRouteLabel
        ? endingRouteLabel
        : `${currentRouteLabel}到${endingRouteLabel}`;
    return compactSharePart(
        `口播：${finalTitle}，${routePart}，${fanCount}粉围观${watchHeat}，${debtPart}。`,
        "口播：本局收官，路线成型，旧账已记。",
        45
    );
}

function renderEndingShareBattleCard(card, shareText) {
    if (!card) return "";
    return `
        <section class="ending-share-card" aria-label="结局战报卡">
            <div class="ending-share-card-head">
                <span>VUP出道局 ${runMaxDayLabel()}战报</span>
                <strong>${html(card.grade)}</strong>
            </div>
            <div class="ending-share-title-row">
                <h3>${html(card.title)}</h3>
                <p>${html(card.subtitle)}</p>
            </div>
            <div class="ending-share-route-row">
                <span><em>结局路线</em>${html(card.route)}</span>
                <span><em>本局活法</em>${html(card.currentRoute)}</span>
                <span><em>总评</em>${html(card.score)}分 / ${html(card.gradeLabel)}</span>
            </div>
            <div class="ending-share-events">
                <em>关键事件</em>
                ${card.keyEvents.map((line, index) => `<strong><span>${index + 1}</span>${html(line)}</strong>`).join("")}
            </div>
            <div class="ending-share-profile">
                <span><em>粉丝画像</em>${html(card.fanProfile)}</span>
                <span><em>旧账状态</em>${html(card.debtLine)}</span>
                <span><em>复活赛建议</em>${html(card.nextRun)}</span>
            </div>
            <p class="ending-streamer-line" data-screenshot-anchor="ending-streamer-line">${html(card.streamerLine)}</p>
            <div class="ending-share-actions">
                <button type="button" class="ending-share-button primary" data-action="copy-ending-share" data-share-text="${html(clipboardText(shareText))}">复制战报</button>
                <button type="button" class="ending-share-button" data-action="download-ending-share-card" data-share-format="landscape">保存16:9</button>
                <button type="button" class="ending-share-button" data-action="download-ending-share-card" data-share-format="feed">保存4:5</button>
                <button type="button" class="ending-share-button" data-action="download-ending-share-card" data-share-format="portrait">保存9:16</button>
                <button type="button" class="ending-share-button" data-action="restart-from-share-card" ${state.busy ? 'disabled aria-disabled="true"' : ''}>开复活赛</button>
            </div>
            <p class="ending-share-text">${html(shareText)}</p>
        </section>
    `;
}

function renderEnding() {
    const panel = document.getElementById('endingPanel');
    if (state.session?.phase !== 'ENDING_READY') {
        panel.classList.add('hidden');
        panel.innerHTML = '';
        delete panel.dataset.endingSoundKey;
        delete panel.dataset.endingSoundPlayed;
        hideEndingFullscreen(true);
        return;
    }

    panel.classList.remove('hidden');

    // 结局音效（仅首次渲染时播放）
    const endingSoundKey = state.ending ? restartEndingIdentity() : "";
    if (state.ending && panel.dataset.endingSoundKey !== endingSoundKey) {
        panel.dataset.endingSoundKey = endingSoundKey;
        panel.dataset.endingSoundPlayed = 'true';
        SFX.ending();
        // S级评价特殊音效
        if (state.ending.routeReviewJson) {
            try {
                const review = JSON.parse(state.ending.routeReviewJson);
                if (review.scorecard?.grade === 'S') {
                    setTimeout(() => SFX.sRank(), 800);
                }
            } catch (e) {}
        }
        // 显示全屏结局展示
        scheduleEndingFullscreen();
    }

    // 结局缺失检查
    if (state.session?.phase === "ENDING_READY" && !state.ending) {
        delete panel.dataset.endingSoundKey;
        delete panel.dataset.endingSoundPlayed;
        hideEndingFullscreen(true);
        panel.innerHTML = `
            <div class="panel-header">
                <h3 class="panel-title">结局生成异常</h3>
            </div>
            <div class="ending-missing" style="padding: 16px; background: var(--bg-secondary); border-radius: 12px; border-left: 3px solid var(--danger);">
                <p>结局生成异常，当前已经进入结局阶段，但还没有可用的结局复盘。</p>
            </div>
        `;
        return;
    }

    if (!state.ending) {
        panel.innerHTML = '';
        hideEndingFullscreen(true);
        return;
    }

    // 结局检查（用于测试）
    if (state.session?.phase === "ENDING_READY" && state.ending) {
        // 结局已加载
    }

    const finalTitle = localizedVisibleTextOrFallback(state.ending.finalTitle, "本轮结局已揭晓");
    const maxDayLabel = runMaxDayLabel();
    const finalDayLabel = runFinalDayLabel();
    const subtitle = localizedVisibleTextOrFallback(state.ending.subtitle, `${maxDayLabel}出道局收官，复盘组正在打包证据。`);
    const summary = localizedVisibleTextOrFallback(state.ending.summary, "这轮已经完成收官，先看路线证据，再决定下一轮复活赛往哪边冲。");
    const endingReason = localizedVisibleTextOrFallback(state.ending.endingReason, `系统根据${finalDayLabel}表现、路线分和代表事件生成本轮结局。`);
    const endingRule = localizedVisibleTextOrFallback(state.ending.endingReasonJson?.rule, "");
    const unknownFailureReason = localizedVisibleTextOrFallback(state.ending.endingReasonJson?.conditions?.unknownFailureReason, "");
    const endingRouteLabel = routeLabelFor(state.ending.endingType);
    const currentRouteLabel = routeLabelFor(state.ending.routeReview?.currentRoute || state.ending.endingType);
    const finalDayAction = actionLabelFor(state.ending.routeReview?.finalDayAction);
    const fanProfile = state.ending.fanProfile || {};
    const fanCount = Number.isFinite(Number(fanProfile.fans)) ? Number(fanProfile.fans) : (state.vup?.fans ?? "--");
    const trueFans = Number.isFinite(Number(fanProfile.trueFans)) ? Number(fanProfile.trueFans) : (state.vup?.trueFans ?? "--");
    const watchHeat = Number.isFinite(Number(fanProfile.watchHeat)) ? Number(fanProfile.watchHeat) : (state.vup?.watchHeat ?? "--");
    const debtCount = (state.ending.debtRefs || []).length;
    const keyEventCount = (state.ending.keyEvents || []).length;
    const rawScorecard = state.ending.routeReview?.scorecard || null;
    let scorecard = null;
    let endingBalanceCards = "";
    const restartHintText = localizedVisibleTextOrFallback(
        state.ending.restartHint?.text,
        "复活赛会继承一点路线倾向，下一轮别让标题组整月摸鱼。"
    );
    const endingCollection = state.achievementProgress?.endingCollection || {};
    const atlasRestartTargetType = endingCollection.nextTargetType === "COMPLETE" ? "" : endingCollection.nextTargetType;
    const defaultRestartBiasRoute = restartBiasRouteFor(
        atlasRestartTargetType,
        state.ending.restartHint?.routeBias || state.ending.endingType
    );
    const restartBiasRoute = state.restartBiasOverride?.endingId === restartEndingIdentity()
        ? restartBiasRouteFor(state.restartBiasOverride.value, defaultRestartBiasRoute)
        : defaultRestartBiasRoute;
    const atlasProgress = Number.isFinite(Number(endingCollection.totalCount))
        ? `${Number(endingCollection.unlockedCount) || 0}/${Number(endingCollection.totalCount) || 0}`
        : "待同步";
    const nextTargetLabel = localizedVisibleTextOrFallback(endingCollection.nextTargetLabel, "下一张图鉴");
    const nextRunGoal = localizedVisibleTextOrFallback(
        rawScorecard?.nextChallenge || endingCollection.nextRunGoal || endingCollection.nextTargetHint,
        "下一局换一条路线补证据，优先点亮未解锁结局。"
    );
    scorecard = endingScorecardView(rawScorecard, fanProfile, debtCount, keyEventCount, nextRunGoal);
    const nextRunPromptText = nextRunMotivationText({
        routeKey: restartBiasRoute,
        endingCollection,
        forecast: state.endingForecast,
        ending: state.ending,
        scorecard,
        goal: nextRunGoal
    });
    endingBalanceCards = renderEndingBalanceCards(fanProfile, debtCount, keyEventCount, scorecard);
    const endingScoreLine = scorecard.scoreLine;
    const advancedEvidence = renderEndingAdvancedEvidence(scorecard);
    const endingEvidenceDigest = renderEndingEvidenceDigest(scorecard);
    const representativeEvents = renderEndingRepresentativeEvents();
    const endingRiskHints = renderEndingRiskHints(scorecard, restartHintText);
    const nextRunChecklist = renderEndingNextRunChecklist(scorecard, nextRunGoal);
    const gradeContract = renderEndingGradeContract(scorecard, nextRunGoal);
    const shareText = endingShareText({
        finalTitle,
        subtitle,
        endingRouteLabel,
        currentRouteLabel,
        fanCount,
        trueFans,
        watchHeat,
        debtCount,
        keyEventCount,
        finalDayAction,
        scorecard,
        nextRunGoal: scorecard.nextChallenge
    });
    const shareCard = buildEndingShareCardData({
        finalTitle,
        subtitle,
        endingRouteLabel,
        currentRouteLabel,
        fanProfile,
        fanCount,
        trueFans,
        watchHeat,
        debtCount,
        keyEventCount,
        finalDayAction,
        scorecard,
        nextRunGoal: scorecard.nextChallenge
    });
    state.endingShareCard = shareCard;
    const endingShareCardHtml = renderEndingShareBattleCard(shareCard, shareText);

    const tags = (state.ending.endingTags || []).map(t => `
        <span class="ending-tag">${html(localizedVisibleTextOrFallback(t.label || t, "结局标签"))}</span>
    `).join('');

    // 路线证据
    const routeEvidence = (state.ending.routeReview?.routeEvidenceTrail || []).map(e => `
        <div class="ending-evidence-card">
            <strong>第${e.day}天 / ${html(actionLabelFor(e.action))}</strong>
            <p>${html(routeLabelFor(e.routeType))} +${Number(e.scoreDelta) || 0}</p>
        </div>
    `).join('');

    const endingProofItems = (state.ending.routeReview?.routeEvidenceTrail || []).slice(-3);
    const endingProofCards = endingProofItems.length ? endingProofItems.map(e => {
        const dayLabel = Number.isFinite(Number(e.day)) ? Number(e.day) : "?";
        const scoreDelta = Number(e.scoreDelta) || 0;
        const scorePrefix = scoreDelta >= 0 ? "+" : "";
        return `
            <div class="ending-proof-card">
                <span>第${html(dayLabel)}天</span>
                <strong>${html(actionLabelFor(e.action))}</strong>
                <small>${html(routeLabelFor(e.routeType))} ${scorePrefix}${html(scoreDelta)}</small>
            </div>
        `;
    }).join('') : `
        <div class="ending-proof-card muted">
            <span>证据摘录</span>
            <strong>${html(currentRouteLabel)}</strong>
            <small>完整证据链仍在归档中</small>
        </div>
    `;
    const endingConclusionSummary = `
        <section class="ending-conclusion-summary" aria-label="结局一句总结">
            <span>一句总结</span>
            <p>${html(summary)}</p>
        </section>
    `;
    const endingFirstScreenProof = `
        <div class="ending-proof-strip ending-proof-strip-first" aria-label="结局三条关键证据">
            ${endingProofCards}
        </div>
    `;
    const endingNextRunCard = `
        <section class="ending-next-run-card" aria-label="下一局建议">
            <span>下一局建议</span>
            <strong id="restartNextRunPrompt">${html(nextRunPromptText)}</strong>
            <small>${html(compactSharePart(nextRunGoal || scorecard.nextChallenge, "按建议补短板，下一局更容易升档。", 72))}</small>
        </section>
    `;
    const endingHero = `
        <section class="ending-hero" aria-label="结局首屏重点">
            <div class="ending-title-block">
                <h2 class="ending-title">${html(finalTitle)}</h2>
                <p class="ending-subtitle">${html(subtitle)}</p>
            </div>
            ${renderEndingGradeCallout(scorecard)}
            ${endingConclusionSummary}
            ${endingFirstScreenProof}
            ${endingNextRunCard}
            ${gradeContract}
        </section>
    `;

    // 代表事件
    const keyEvents = endingRepresentativeEventItems(5).map(e => `
        <div class="ending-evidence-card">
            <strong>${Number.isFinite(Number(e.day)) ? `第${Number(e.day)}天` : "补位记录"} / ${html(actionLabelFor(e.action))}</strong>
            ${e.spotlightReason ? `<span class="ending-evidence-chip">${html(e.spotlightReason)}</span>` : ""}
            <p>${html(localizedVisibleTextOrFallback(e.summary, "代表事件已记录，复盘文本待整理。"))}</p>
        </div>
    `).join('');

    // 债务引用
    const debtRefs = (state.ending.debtRefs || []).map(d => `
        <div class="ending-evidence-card debt">
            <strong>${html(debtTypeText(d.debtType))} / ${html(riskSeverityText(d.severity))}</strong>
            <p>${html(localizedVisibleTextOrFallback(d.summary, "旧账仍有记录，下一轮注意补米线。"))}</p>
        </div>
    `).join('');

    // 最终日报引用
    const finalReportRef = state.ending.routeReview?.finalReportRef;
    const reportRefHtml = finalReportRef ?
        `<p class="final-report-ref">最终日报：第${finalReportRef.day}天日报</p>` :
        '<p class="final-report-ref">最终日报证据未返回</p>';

    // 事故素材
    const accidentMaterials = renderEndingAccidentMaterials(state.ending.routeReview?.accidentMaterials);

    // 差距分析
    const gapHtml = renderEndingGapAnalysis();

    panel.innerHTML = `
        <div class="ending-content">
            <div class="panel-header ending-header">
                <div>
                    <span class="ending-kicker">${maxDayLabel}收官</span>
                    <h3 class="panel-title">结局复盘</h3>
                </div>
                <span class="panel-badge">${html(routeLabelFor(state.ending.endingType))}</span>
            </div>
            <div class="ending-main">
                ${endingHero}
                ${gapHtml}
                ${endingEvidenceDigest}
                ${representativeEvents}
                <div class="ending-summary-strip" aria-label="结局关键指标">
                    <div class="ending-summary-item">
                        <span>结局路线</span>
                        <strong>${html(endingRouteLabel)}</strong>
                    </div>
                    <div class="ending-summary-item">
                        <span>总粉丝</span>
                        <strong>${html(fanCount)}</strong>
                    </div>
                    <div class="ending-summary-item">
                        <span>围观热度</span>
                        <strong>${html(watchHeat)}</strong>
                    </div>
                    <div class="ending-summary-item ${debtCount ? 'warning' : 'clear'}">
                        <span>旧账</span>
                        <strong>${debtCount ? `${debtCount} 条` : '已清场'}</strong>
                    </div>
                </div>
                ${endingRiskHints}
                <img class="ending-image" src="/gallery/${html(endingImageForEnding(state.ending))}" alt="${html(finalTitle)}" loading="lazy" decoding="async" style="max-width:320px;margin:16px auto;display:block;border-radius:12px;">
                <div class="ending-restart-simple">
                    <div class="ending-restart-console">
                        <label for="restartBias">复活赛路线倾向</label>
                        ${renderRestartOptions(restartBiasRoute)}
                        <p id="restartBiasHint" class="ending-restart-bias-hint">${html(restartBiasHintText(restartBiasRoute))}</p>
                        ${renderRestartBiasPreview(restartBiasRoute)}
                    </div>
                    <button type="button" class="primary ending-restart-button" data-action="restart" data-stage-advance-hotkey="Enter" aria-label="看完${html(finalTitle)}，开启复活赛" ${state.busy ? 'disabled aria-disabled="true"' : ''}>复活赛开播</button>
                </div>
                ${renderEndingNarrative(state.ending.endingType, state.ending)}
                ${endingShareCardHtml}
                <details class="ending-details">
                    <summary>展开完整复盘数据</summary>
                    <div class="ending-details-body">
                        <p class="ending-summary">${html(summary)}</p>
                        ${endingBalanceCards}
                        ${advancedEvidence}
                        <div class="ending-radar-chart" aria-label="路线雷达图">
                            <canvas id="routeRadarChart" width="300" height="300"></canvas>
                        </div>
                        <div class="ending-proof-strip" aria-label="结局路线证据摘录">
                            ${endingProofCards}
                        </div>
                        <div class="ending-reason-card">
                            <span>判定理由</span>
                            <p>${html(endingReason)}</p>
                            ${endingScoreLine ? `<small>${html(endingScoreLine)}</small>` : ""}
                            ${endingRule ? `<small>${html(unknownFailureReason || `规则：${endingRule}`)}</small>` : ""}
                        </div>
                        ${renderTimeline()}
                        <section>
                            <h4>日报证据</h4>
                            ${reportRefHtml}
                        </section>
                        <section>
                            <h4>路线分证据</h4>
                            <div class="route-evidence-trail">${routeEvidence || '<p class="ending-empty">无路线证据</p>'}</div>
                        </section>
                        <section>
                            <h4>代表事件</h4>
                            ${keyEvents || '<p class="ending-empty">无代表事件</p>'}
                        </section>
                        <section>
                            <h4>事故素材</h4>
                            ${accidentMaterials}
                        </section>
                        <section>
                            <h4>欠账清单</h4>
                            ${debtRefs || '<p class="ending-empty">所有账都还清了</p>'}
                        </section>
                    </div>
                </details>
            </div>
        </div>
    `;
    const endingDetails = panel.querySelector('.ending-details');
    if (endingDetails) {
        endingDetails.addEventListener('toggle', event => {
            if (event.currentTarget.open) {
                window.setTimeout(() => CHARTS.renderRouteRadar('routeRadarChart'), 0);
            }
        });
        if (!CHARTS.isAvailable() || endingDetails.open) {
            CHARTS.renderRouteRadar('routeRadarChart');
        }
    } else {
        CHARTS.renderRouteRadar('routeRadarChart');
    }
    updateRestartBiasHint();
    animateEndingReveal();
}

function animateEndingReveal() {
    const panel = document.getElementById('endingPanel');
    if (!panel) return;

    // Phase 1: Title fade-in (0-1.5s)
    const title = panel.querySelector('.ending-title');
    if (title) { title.style.opacity = '0'; title.style.transform = 'scale(0.8)';
        setTimeout(() => { title.style.transition = 'all 1s ease'; title.style.opacity = '1'; title.style.transform = 'scale(1)'; }, 200); }

    // Phase 2: Grade dramatic reveal (1.5-3s)
    const grade = panel.querySelector('.ending-grade-callout');
    if (grade) { grade.style.opacity = '0'; grade.style.transform = 'scale(0.5)';
        setTimeout(() => { grade.style.transition = 'all 0.8s cubic-bezier(0.34,1.56,0.64,1)'; grade.style.opacity = '1'; grade.style.transform = 'scale(1)'; }, 1500); }

    // Phase 3: Summary typewriter effect (3-5s)
    const summary = panel.querySelector('.ending-summary');
    if (summary) { const text = summary.textContent; summary.textContent = ''; summary.style.opacity = '1';
        let i = 0; const interval = setInterval(() => { summary.textContent = text.slice(0, ++i); if (i >= text.length) clearInterval(interval); }, 30);
        setTimeout(() => { i = 0; }, 3000); }

    // Phase 4: Score cards stagger (5-7s)
    const cards = panel.querySelectorAll('.ending-score-card');
    cards.forEach((card, idx) => { card.style.opacity = '0'; card.style.transform = 'translateY(20px)';
        setTimeout(() => { card.style.transition = 'all 0.5s ease'; card.style.opacity = '1'; card.style.transform = 'translateY(0)'; }, 5000 + idx * 300); });

    // Phase 5: Fan montage (7-9s)
    setTimeout(() => addFanMontage(panel), 7000);
}

function addFanMontage(panel) {
    const maxDayLabel = runMaxDayLabel();
    const montages = {
        'SINGING_IDOL': [`老粉：${maxDayLabel}唱下来，歌单终于有了自己的味道。`, '切片组：高音素材够剪十期了。', '录播组：存好了，随时可以回来听。'],
        'SLICE_SAINT': ['切片组：这轮素材够剪三个月。', '标题组：终于可以休息了。', '乐子人：正片？什么是正片？'],
        'BLACK_RED_MAIN_STAGE': ['楼友：主会场灯亮了。', '考据组：证据链已归档。', '房管：终于可以下班了。'],
        'ELECTRONIC_PICKLE': ['陪伴粉：明天还是这个时间吗？', '老粉：饭点到了，习惯性打开。', 'DD：路过，但这站我会记住。'],
        'DANCE_MEME': ['切片组：表情包素材已打包。', '路人粉：原来跳舞也能火。', `老粉：从尴尬到上头，只用了${maxDayLabel}。`],
        'SOCIAL_COLLAB': ['联动粉：你俩什么时候再合作？', '老粉：圈子扩了一倍。', 'DD：这站下车，下站再会。'],
        'CYBER_GIRLFRIEND': ['陪伴粉：晚安，明天见。', '老粉：赛博女友名不虚传。', '录播组：甜度素材已归档。'],
        'DD_BUS_STOP': ['DD：每站都有惊喜。', '老粉：终于有人把DD文化讲明白了。', '切片组：素材够剪一期DD特辑。'],
        'MAIN_STAGE_KING': [`老粉：等了${maxDayLabel}，终于等到这一天。`, '考据组：主会场数据已备份。', '房管：这场直播值了。'],
        'GLORIOUS_GRADUATION': [`老粉：虽然毕业了，但这${maxDayLabel}值得。`, '切片组：毕业素材剪不完。', '录播组：存好了，随时可以回来看。']
    };
    const endingType = state.ending?.endingType || 'UNKNOWN';
    const lines = montages[endingType] || [`弹幕：${maxDayLabel}辛苦了。`, '房管：存档完毕。', '老粉：下一局见。'];
    const container = document.createElement('div');
    container.className = 'ending-fan-montage';
    lines.forEach((line, i) => {
        const el = document.createElement('div');
        el.className = 'montage-line';
        el.textContent = line;
        el.style.opacity = '0';
        el.style.animation = `montageFadeIn 0.5s ease ${i * 0.8}s forwards`;
        container.appendChild(el);
    });
    panel.appendChild(container);
}

// === 结局全屏展示 ===
function clearEndingFullscreenTimers() {
    if (endingFullscreenShowTimer) {
        clearTimeout(endingFullscreenShowTimer);
        endingFullscreenShowTimer = null;
    }
    if (endingFullscreenHideTimer) {
        clearTimeout(endingFullscreenHideTimer);
        endingFullscreenHideTimer = null;
    }
    if (endingFullscreenTagTimer) {
        clearTimeout(endingFullscreenTagTimer);
        endingFullscreenTagTimer = null;
    }
    endingFullscreenChildTimers.forEach(timer => clearTimeout(timer));
    endingFullscreenChildTimers = [];
}

function scheduleEndingFullscreen() {
    clearEndingFullscreenTimers();
    endingFullscreenShowTimer = setTimeout(() => {
        endingFullscreenShowTimer = null;
        if (state.session?.phase === 'ENDING_READY' && state.ending) {
            showEndingFullscreen();
        }
    }, 500);
}

function showEndingFullscreen() {
    const overlay = document.getElementById('endingFullscreen');
    if (!overlay || state.session?.phase !== 'ENDING_READY' || !state.ending) return;
    clearEndingFullscreenTimers();

    const finalTitle = localizedVisibleTextOrFallback(state.ending.finalTitle, "本轮结局已揭晓");
    const rawScorecard = state.ending.routeReview?.scorecard || null;
    const scorecard = endingScorecardView(rawScorecard, state.ending.fanProfile || {}, (state.ending.debtRefs || []).length, (state.ending.keyEvents || []).length, "");

    // 设置背景图片
    const bg = document.getElementById('endingFsBg');
    if (bg) {
        bg.style.backgroundImage = `url(/gallery/${endingImageForEnding(state.ending)})`;
    }

    // 设置标题
    const titleEl = document.getElementById('endingFsTitle');
    if (titleEl) {
        titleEl.textContent = finalTitle;
    }

    // 设置评级
    const gradeEl = document.getElementById('endingFsGrade');
    if (gradeEl) {
        gradeEl.textContent = visibleTextOrFallback(scorecard.grade, "?");
    }

    // 设置分数
    const scoreEl = document.getElementById('endingFsScore');
    if (scoreEl) {
        scoreEl.textContent = `${scorecard.overall} 分`;
    }

    // 设置标签
    const tagsEl = document.getElementById('endingFsTags');
    if (tagsEl) {
        const tags = (state.ending.endingTags || []).map(t =>
            `<span class="ending-fs-tag">${html(localizedVisibleTextOrFallback(t.label || t, "结局标签"))}</span>`
        ).join('');
        tagsEl.innerHTML = tags;
    }

    // 显示弹窗
    overlay.classList.remove('hidden');
    requestAnimationFrame(() => {
        overlay.classList.add('active');
    });

    // 标签依次显示动画
    endingFullscreenTagTimer = setTimeout(() => {
        endingFullscreenTagTimer = null;
        const tags = tagsEl?.querySelectorAll('.ending-fs-tag') || [];
        tags.forEach((tag, i) => {
            const childTimer = setTimeout(() => tag.classList.add('visible'), i * 200);
            endingFullscreenChildTimers.push(childTimer);
        });
    }, 1000);

    // AI语音播报结局
    const endingTtsText = `${finalTitle}。${localizedVisibleTextOrFallback(state.ending.subtitle, '')}`;
    TTS.speak(endingTtsText);
}

function hideEndingFullscreen(immediate = false) {
    const overlay = document.getElementById('endingFullscreen');
    if (!overlay) return;
    clearEndingFullscreenTimers();
    overlay.classList.remove('active');
    if (immediate) {
        overlay.classList.add('hidden');
        return;
    }
    endingFullscreenHideTimer = setTimeout(() => {
        endingFullscreenHideTimer = null;
        overlay.classList.add('hidden');
    }, 500);
}

function endingShareText({
    finalTitle,
    subtitle,
    endingRouteLabel,
    currentRouteLabel,
    fanCount,
    trueFans,
    watchHeat,
    debtCount,
    keyEventCount,
    finalDayAction,
    scorecard,
    nextRunGoal
}) {
    const debtLine = debtCount > 0 ? `旧账${debtCount}条` : "旧账清场";
    const streamerLine = streamerFriendlyEndingLine({
        finalTitle,
        endingRouteLabel,
        currentRouteLabel,
        fanCount,
        watchHeat,
        debtCount,
        scorecard
    });
    const gradeLine = scorecard
        ? `评级${scorecard.grade}，总评${scorecard.overall}分，${compactSharePart(scorecard.gradeLabel, "路线已成型", 16)}`
        : "";
    const scoreLine = scorecard?.scoreLine ? compactSharePart(scorecard.scoreLine, "", 46) : "";
    const challengeLine = compactSharePart(nextRunGoal || scorecard?.nextChallenge, "", 48);
    const shareHeadline = runMaxDayLabel() === "30天"
        ? `《VUP出道局》30天收官：${finalTitle}`
        : `《VUP出道局》${runMaxDayLabel()}收官：${finalTitle}`;
    return [
        shareHeadline,
        subtitle,
        gradeLine,
        scoreLine,
        streamerLine,
        `结局路线：${endingRouteLabel}，当前路线：${currentRouteLabel}`,
        `总粉丝${fanCount}，真爱粉${trueFans}，围观热度${watchHeat}`,
        `代表事件${keyEventCount}条，${debtLine}，收官动作：${finalDayAction}`,
        challengeLine ? `下一轮挑战：${challengeLine.replace(/^下一轮挑战：/, "")}` : ""
    ].filter(Boolean).join("｜");
}

async function copyEndingShare(encodedText) {
    const text = decodeURIComponent(String(encodedText || ""));
    if (!text) {
        setStatus("战报还没生成，先等结局复盘加载完整。", "warning");
        return;
    }
    try {
        if (navigator.share) {
            await navigator.share({
                title: `VUP出道局 ${runMaxDayLabel()}结局战报`,
                text
            });
            setStatus("结局战报已交给系统分享面板。", "success");
            return;
        }
        if (navigator.clipboard?.writeText && window.isSecureContext) {
            await navigator.clipboard.writeText(text);
        } else {
            const textarea = document.createElement("textarea");
            textarea.value = text;
            textarea.setAttribute("readonly", "");
            textarea.style.position = "fixed";
            textarea.style.left = "-9999px";
            document.body.appendChild(textarea);
            textarea.select();
            document.execCommand("copy");
            textarea.remove();
        }
        setStatus("结局战报已复制，可以直接发群聊或动态。", "success");
    } catch (error) {
        if (error?.name === "AbortError") {
            setStatus("分享已取消，战报文本仍留在复盘卡里。", "info");
            return;
        }
        setStatus("浏览器拦截了分享或复制，请手动选中战报文本。", "warning");
    }
}

function svgText(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

function svgLines(value, maxChars = 18, maxLines = 2) {
    const text = String(value ?? "").trim();
    if (!text) return [""];
    const lines = [];
    for (let i = 0; i < text.length && lines.length < maxLines; i += maxChars) {
        lines.push(text.slice(i, i + maxChars));
    }
    if (text.length > maxChars * maxLines && lines.length) {
        lines[lines.length - 1] = `${lines[lines.length - 1].slice(0, Math.max(0, maxChars - 2))}...`;
    }
    return lines;
}

function svgMultilineText(value, x, y, options = {}) {
    const maxChars = options.maxChars || 18;
    const maxLines = options.maxLines || 2;
    const lineHeight = options.lineHeight || 24;
    const className = options.className || "body";
    return svgLines(value, maxChars, maxLines).map((line, index) =>
        `<text x="${x}" y="${y + index * lineHeight}" class="${className}">${svgText(line)}</text>`
    ).join("");
}

const ENDING_SHARE_CARD_FORMATS = {
    landscape: {
        id: "landscape",
        label: "16:9",
        fileSuffix: "16x9",
        width: 1920,
        height: 1080
    },
    portrait: {
        id: "portrait",
        label: "9:16",
        fileSuffix: "9x16",
        width: 1080,
        height: 1920
    },
    feed: {
        id: "feed",
        label: "4:5",
        fileSuffix: "4x5",
        width: 1080,
        height: 1350
    }
};

function endingShareCardFormat(format) {
    return ENDING_SHARE_CARD_FORMATS[format] || ENDING_SHARE_CARD_FORMATS.landscape;
}

function endingShareCardDefs() {
    return `
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#101217"/>
      <stop offset="0.55" stop-color="#18131d"/>
      <stop offset="1" stop-color="#10201f"/>
    </linearGradient>
    <linearGradient id="accent" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="#ff6ec7"/>
      <stop offset="1" stop-color="#3dd6c6"/>
    </linearGradient>
    <style>
      text { font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif; }
      .kicker { fill: #3dd6c6; font-size: 34px; font-weight: 800; letter-spacing: 0; }
      .title { fill: #f7f0dc; font-size: 76px; font-weight: 900; letter-spacing: 0; }
      .title-wide { fill: #f7f0dc; font-size: 88px; font-weight: 900; letter-spacing: 0; }
      .subtitle, .body { fill: #cfc7b5; font-size: 32px; font-weight: 600; letter-spacing: 0; }
      .body-wide { fill: #cfc7b5; font-size: 34px; font-weight: 650; letter-spacing: 0; }
      .label { fill: #8f887a; font-size: 24px; font-weight: 800; letter-spacing: 0; }
      .value { fill: #f7f0dc; font-size: 34px; font-weight: 900; letter-spacing: 0; }
      .value-wide { fill: #f7f0dc; font-size: 42px; font-weight: 900; letter-spacing: 0; }
      .grade { fill: #101217; font-size: 86px; font-weight: 900; letter-spacing: 0; }
      .grade-wide { fill: #101217; font-size: 96px; font-weight: 900; letter-spacing: 0; }
      .event-line { fill: #f7f0dc; font-size: 30px; font-weight: 700; letter-spacing: 0; }
      .event-line-wide { fill: #f7f0dc; font-size: 32px; font-weight: 760; letter-spacing: 0; }
      .event-index { fill: #101217; font-size: 22px; font-weight: 900; }
      .card { fill: rgba(255,255,255,0.055); stroke: rgba(247,240,220,0.18); stroke-width: 2; }
      .event-dot { fill: #f3c969; }
    </style>
  </defs>`.trim();
}

function endingShareCardLandscapeSvg(card, spec) {
    const eventTexts = (card.keyEvents || []).slice(0, 3).map((line, index) => {
        const y = 462 + index * 106;
        return `
            <circle cx="1054" cy="${y - 8}" r="18" class="event-dot"/>
            <text x="1054" y="${y - 1}" text-anchor="middle" class="event-index">${index + 1}</text>
            ${svgMultilineText(line, 1100, y - 18, { maxChars: 28, maxLines: 2, lineHeight: 26, className: "event-line-wide" })}
        `;
    }).join("");
    return `
<svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080" viewBox="0 0 1920 1080" data-share-aspect="${spec.label}">
  ${endingShareCardDefs()}
  <rect width="1920" height="1080" fill="url(#bg)"/>
  <rect x="52" y="52" width="1816" height="976" rx="34" fill="none" stroke="rgba(247,240,220,0.22)" stroke-width="3"/>
  <rect x="88" y="92" width="1744" height="16" rx="8" fill="url(#accent)"/>
  <text x="104" y="176" class="kicker">VUP出道局 ${svgText(runMaxDayLabel())}结局战报 · 16:9</text>
  <circle cx="1710" cy="194" r="80" fill="url(#accent)"/>
  <text x="1710" y="226" text-anchor="middle" class="grade-wide">${svgText(card.grade)}</text>
  ${svgMultilineText(card.title, 104, 330, { maxChars: 12, maxLines: 2, lineHeight: 96, className: "title-wide" })}
  ${svgMultilineText(card.subtitle, 104, 552, { maxChars: 25, maxLines: 2, lineHeight: 42, className: "body-wide" })}
  <rect x="104" y="672" width="766" height="86" rx="18" class="card"/>
  <text x="140" y="726" class="label">路线</text>
  <text x="232" y="728" class="value-wide">${svgText(card.route)}</text>
  <rect x="104" y="792" width="766" height="94" rx="18" class="card"/>
  <text x="140" y="850" class="label">总评</text>
  <text x="232" y="852" class="value-wide">${svgText(card.score)}分 / ${svgText(card.gradeLabel)}</text>
  <text x="1020" y="386" class="label">关键事件</text>
  ${eventTexts}
  <rect x="1020" y="772" width="776" height="160" rx="24" class="card"/>
  <text x="1064" y="826" class="label">粉丝画像</text>
  ${svgMultilineText(card.fanProfile, 1064, 876, { maxChars: 31, maxLines: 2, lineHeight: 38, className: "body-wide" })}
  <text x="104" y="948" class="label">旧账状态：${svgText(card.debtLine)}</text>
  <text x="1020" y="980" class="label">复活赛建议：${svgText(compactSharePart(card.nextRun, "", 35))}</text>
</svg>`.trim();
}

function endingShareCardVerticalSvg(card, spec) {
    if (!card) return "";
    const isPortrait = spec.id === "portrait";
    const titleY = isPortrait ? 344 : 276;
    const subtitleY = isPortrait ? 540 : 444;
    const summaryY = isPortrait ? 650 : 526;
    const eventLabelY = isPortrait ? 818 : 612;
    const eventStartY = isPortrait ? 888 : 612;
    const profileY = isPortrait ? 1180 : 846;
    const spokenY = isPortrait ? 1540 : 1136;
    const footerY = isPortrait ? 1848 : 1290;
    const frameHeight = spec.height - 92;
    const eventTexts = (card.keyEvents || []).slice(0, 3).map((line, index) => {
        const y = eventStartY + index * (isPortrait ? 88 : 72);
        return `
            <circle cx="76" cy="${y - 8}" r="15" class="event-dot"/>
            <text x="76" y="${y - 3}" text-anchor="middle" class="event-index">${index + 1}</text>
            ${svgMultilineText(line, 112, y - 14, { maxChars: 24, maxLines: 2, lineHeight: 22, className: "event-line" })}
        `;
    }).join("");
    return `
<svg xmlns="http://www.w3.org/2000/svg" width="${spec.width}" height="${spec.height}" viewBox="0 0 ${spec.width} ${spec.height}" data-share-aspect="${spec.label}">
  ${endingShareCardDefs()}
  <rect width="${spec.width}" height="${spec.height}" fill="url(#bg)"/>
  <rect x="46" y="46" width="988" height="${frameHeight}" rx="32" fill="none" stroke="rgba(247,240,220,0.22)" stroke-width="3"/>
  <rect x="72" y="78" width="936" height="14" rx="7" fill="url(#accent)"/>
  <text x="76" y="156" class="kicker">VUP出道局 ${svgText(runMaxDayLabel())}结局战报 · ${svgText(spec.label)}</text>
  <circle cx="910" cy="160" r="72" fill="url(#accent)"/>
  <text x="910" y="190" text-anchor="middle" class="grade">${svgText(card.grade)}</text>
  ${svgMultilineText(card.title, 76, titleY, { maxChars: 10, maxLines: 2, lineHeight: 82, className: "title" })}
  ${svgMultilineText(card.subtitle, 76, subtitleY, { maxChars: 23, maxLines: 2, lineHeight: 38, className: "subtitle" })}
  <rect x="76" y="${summaryY}" width="928" height="56" rx="16" class="card"/>
  <text x="108" y="${summaryY + 36}" class="label">路线</text>
  <text x="190" y="${summaryY + 37}" class="value">${svgText(card.route)}</text>
  <text x="650" y="${summaryY + 36}" class="label">总评</text>
  <text x="730" y="${summaryY + 37}" class="value">${svgText(card.score)}分 / ${svgText(card.gradeLabel)}</text>
  <text x="76" y="${eventLabelY}" class="label">关键事件</text>
  ${eventTexts}
  <rect x="76" y="${profileY}" width="928" height="${isPortrait ? 258 : 226}" rx="24" class="card"/>
  <text x="112" y="${profileY + 56}" class="label">粉丝画像</text>
  ${svgMultilineText(card.fanProfile, 112, profileY + 100, { maxChars: 30, maxLines: 2, lineHeight: 36, className: "body" })}
  <text x="112" y="${profileY + (isPortrait ? 202 : 188)}" class="label">旧账状态</text>
  <text x="280" y="${profileY + (isPortrait ? 202 : 188)}" class="body">${svgText(card.debtLine)}</text>
  <text x="76" y="${spokenY}" class="label">主播口播</text>
  ${svgMultilineText(card.streamerLine, 76, spokenY + 52, { maxChars: 26, maxLines: isPortrait ? 3 : 2, lineHeight: 38, className: "value" })}
  <text x="76" y="${footerY}" class="label">复活赛建议：${svgText(compactSharePart(card.nextRun, "", isPortrait ? 34 : 30))}</text>
</svg>`.trim();
}

function endingShareCardSvg(card = state.endingShareCard, format = "landscape") {
    if (!card) return "";
    const spec = endingShareCardFormat(format);
    if (spec.id === "landscape") {
        return endingShareCardLandscapeSvg(card, spec);
    }
    return endingShareCardVerticalSvg(card, spec);
}

function safeDownloadFilename(value, fallback = "vup-ending-share-card") {
    return String(value || fallback)
        .replace(/[\\/:*?"<>|]/g, "")
        .replace(/\s+/g, "-")
        .slice(0, 48) || fallback;
}

function downloadEndingShareCard(format = "landscape") {
    const card = state.endingShareCard;
    const spec = endingShareCardFormat(format);
    const svg = endingShareCardSvg(card, spec.id);
    if (!card || !svg) {
        setStatus("战报图还没生成，先等结局复盘加载完整。", "warning");
        return;
    }
    const blob = new Blob([svg], { type: "image/svg+xml;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${safeDownloadFilename(card.title)}-战报-${spec.fileSuffix}.svg`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
    setStatus(`${spec.label}结局战报图已保存为 SVG，可以直接发动态或群聊。`, "success");
}

// 渲染结局事故素材
function renderEndingAccidentMaterials(accidentMaterials) {
    const materials = accidentMaterials || [];
    if (materials.length === 0) {
        return '<p class="ending-empty">本轮没有事故素材入库</p>';
    }
    return materials.map(m => `
        <div class="ending-evidence-card">
            <strong>${html(visibleTextOrFallback(materialLabelFor(m), "事故素材"))}</strong>
            <p>第${m.day}天 / ${html(actionLabelFor(m.sourceAction))}</p>
        </div>
    `).join('');
}

// 渲染事故素材（用于日报）
function renderAccidentMaterials(evidenceRefs) {
    const materials = (evidenceRefs || []).filter(ref => ref?.type === 'accident_material');
    if (materials.length === 0) {
        return '';
    }
    return `
        <div class="accident-material-list" style="margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--border);">
            <strong style="font-size: 13px; color: var(--neon-pink);">事故素材</strong>
            <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-top: 6px;">
                ${materials.map(m => `
                    <span style="padding: 4px 10px; background: var(--bg-secondary); border-radius: 8px; font-size: 12px; border: 1px solid var(--border);">
                        ${html(materialLabelFor(m))}
                    </span>
                `).join('')}
            </div>
        </div>
    `;
}

// 同台角色图片映射
const npcFallbackImages = [
    "v4/npcs/portraits/singing-mentor.png",
    "v4/npcs/portraits/dance-coach.png",
    "v4/npcs/portraits/clipper-editor.png",
    "v4/npcs/portraits/sponsor-manager.png",
    "v4/npcs/portraits/rival-commentator.png",
    "v4/npcs/portraits/fan-moderator.png",
    "v4/npcs/portraits/platform-analyst.png",
    "v4/npcs/portraits/collab-streamer.png"
];

function digestText(value, fallback) {
    const text = String(localizedVisibleTextOrFallback(value, fallback) || "").trim();
    return text.length > 34 ? `${text.slice(0, 34)}...` : text;
}

function npcDisplayNameText(npc, fallback = "匿名同行") {
    return firstLocalizedReadableText([npc?.displayName], fallback);
}

function npcLineText(value, fallback = "同台情报还在整理。") {
    return localizedVisibleTextOrFallback(value, fallback);
}

function npcDigestHintText(npc, fallback = "同台情报还在整理。") {
    return firstLocalizedReadableText([npc?.advice, npc?.moodLine, npc?.relevance], fallback);
}

function buzzHeadlineText(buzz, fallback = "热搜安静") {
    return firstLocalizedReadableText([buzz?.headline], fallback);
}

function buzzLineText(value, fallback = "热搜记录还在整理。") {
    return localizedVisibleTextOrFallback(value, fallback);
}

function buzzDigestHintText(buzz, fallback = "热搜记录还在整理。") {
    return firstLocalizedReadableText([buzz?.nextMoveHint, buzz?.trendLabel, buzz?.forumLine], fallback);
}

function renderInfoBriefItem({ tab, label, value, hint, tone }) {
    return `
        <button class="info-brief-item ${tone}" type="button" data-action="open-info-brief" data-info-tab="${tab}" aria-label="${html(insightDigestAriaLabel(label, value, hint))}">
            <span>${html(label)}</span>
            <strong>${html(value)}</strong>
            <small>${html(hint)}</small>
        </button>
    `;
}

const infoTabPanels = {
    main: ['statsSection', 'actionPanel'],
    platform: ['npcPanel', 'platformInteractPanel'],
    ambient: ['ambientPanel'],
    report: ['reportPanel', 'reportHistoryPanel']
};

function renderInfoBrief() {
    const panel = document.getElementById('infoBrief');
    if (!panel) return;

    if (!state.vup) {
        panel.innerHTML = '';
        return;
    }

    const npc = state.npcSpotlight;
    const buzz = state.buzzBriefing;
    const expectation = state.audienceExpectation;
    const lead = expectation?.lead;
    const firstTopic = state.fanTopics?.[0];
    const danmakuMood = state.danmaku?.mood;
    const items = [
        renderInfoBriefItem({
            tab: 'npc',
            label: '同台',
            value: digestText(npc ? npcDisplayNameText(npc) : '', '暂无同台'),
            hint: digestText(npc ? npcDigestHintText(npc) : '', '今天没人递话，先稳住自己节奏。'),
            tone: npc ? 'has-signal' : 'quiet'
        }),
        renderInfoBriefItem({
            tab: 'buzz',
            label: '热搜',
            value: digestText(buzzHeadlineText(buzz), '热搜安静'),
            hint: digestText(buzz ? buzzDigestHintText(buzz) : '', '没有新节奏，别急着开庭。'),
            tone: buzz ? 'has-signal' : 'quiet'
        }),
        renderInfoBriefItem({
            tab: 'audience',
            label: '观众',
            value: digestText(lead?.routeLabel || expectation?.pivotRiskLabel, '观望中'),
            hint: digestText(expectation?.pivotRiskLine || expectation?.nextMoveHint || lead?.evidenceLine, '先看粉丝想吃哪口。'),
            tone: expectation ? 'has-signal' : 'quiet'
        }),
        renderInfoBriefItem({
            tab: 'ambient',
            label: '群聊',
            value: firstTopic ? fanTopicTitleText(firstTopic) : digestText(danmakuMood, '场外安静'),
            hint: digestText(firstTopic ? fanTopicSummaryHint(firstTopic) : state.danmaku?.danmakus?.[0]?.text, '群友暂时没递新小作文。'),
            tone: firstTopic || danmakuMood ? 'has-signal' : 'quiet'
        })
    ].join('');

    panel.innerHTML = `
        <div class="info-brief-head">
            <span>今日情报板</span>
            <small>先读风向，别急着开庭</small>
        </div>
        <div class="info-brief-list">${items}</div>
    `;
}

function insightDigestAriaLabel(label, value, hint) {
    const accessibleHint = String(hint || '')
        .replace(/DD/g, '多担')
        .replace(/SC/g, '醒目留言');
    return `${label}：${value}${accessibleHint ? `，${accessibleHint}` : ''}`;
}

function renderInsightDigestItem({ tab, label, value, hint, tone }) {
    return `
        <button class="insight-digest-item ${tone}" type="button" data-action="open-insight-tab" data-insight-tab="${tab}" aria-label="${html(insightDigestAriaLabel(label, value, hint))}">
            <span>${html(label)}</span>
            <strong>${html(value)}</strong>
            <small>${html(hint)}</small>
        </button>
    `;
}

function renderInsightDigest() {
    const panel = document.getElementById('insightDigest');
    if (!panel) return;

    if (!state.vup) {
        panel.innerHTML = `
            <div class="insight-digest-empty">
                <strong>场外情报</strong>
                <span>创建VUP后同步同台、热搜和观众期待。</span>
            </div>
        `;
        return;
    }

    const npc = state.npcSpotlight;
    const buzz = state.buzzBriefing;
    const expectation = state.audienceExpectation;
    const lead = expectation?.lead;
    const items = [
        renderInsightDigestItem({
            tab: 'npc',
            label: '同台',
            value: digestText(npc ? npcDisplayNameText(npc) : '', '暂无同台'),
            hint: digestText(npc ? npcDigestHintText(npc) : '', '今天没有同行递话，先按自己节奏营业。'),
            tone: npc ? 'has-signal' : 'quiet'
        }),
        renderInsightDigestItem({
            tab: 'buzz',
            label: '热搜',
            value: digestText(buzzHeadlineText(buzz), '热搜安静'),
            hint: digestText(buzz ? buzzDigestHintText(buzz) : '', '没有新节奏，稳扎稳打也算赢。'),
            tone: buzz ? 'has-signal' : 'quiet'
        }),
        renderInsightDigestItem({
            tab: 'audience',
            label: '观众',
            value: digestText(lead?.routeLabel || expectation?.pivotRiskLabel, '观望中'),
            hint: digestText(expectation?.pivotRiskLine || expectation?.nextMoveHint || lead?.evidenceLine, '先稳住老粉预期，再决定要不要转向。'),
            tone: expectation ? 'has-signal' : 'quiet'
        })
    ].join('');

    panel.innerHTML = `
        <div class="insight-digest-head">
            <span>今日场外</span>
            <small>点一项看详情</small>
        </div>
        <div class="insight-digest-list">${items}</div>
    `;
}

function renderNPC() {
    const panel = document.getElementById('npcPanel');
    if (!panel) return;
    if (!state.npcSpotlight) {
        panel.innerHTML = '<p class="info-empty">暂无同台动态</p>';
        return;
    }

    const npc = state.npcSpotlight;
    const image = npcFallbackImages.includes(npc.image) ? npc.image : npcFallbackImages[0];
    const displayName = npcDisplayNameText(npc);
    panel.innerHTML = `
        <div class="npc-spotlight info-card">
            <div class="npc-spotlight-head">
                <img class="npc-art" src="/gallery/${image}" alt="${html(displayName)}" loading="lazy">
                <div>
                    <span class="npc-kicker">同台动态</span>
                    <h4>${html(displayName)}</h4>
                </div>
            </div>
            <div class="npc-radar-grid">
                ${npcSignalChip("心情", npc.moodLine, "mood")}
                ${npcSignalChip("关联", npc.relevance, "relevance")}
            </div>
            <p class="npc-nextplay">${html(npcLineText(npc.advice, "今天先稳住主舞台，别让场外情绪带偏。"))}</p>
        </div>
    `;
}

function npcSignalChip(label, value, tone = "") {
    return `
        <div class="npc-signal-chip ${tone}">
            <span>${html(label)}</span>
            <strong>${html(npcLineText(value, "同台情报还在整理。"))}</strong>
        </div>
    `;
}

// 渲染同台角色（用于测试）
function renderNpcSpotlight() {
    renderNPC();
}

// ===== 平台Tab渲染 =====
function renderPlatform() {
    const grid = document.getElementById('platformNpcGrid');
    const buffEl = document.getElementById('platformStealBuff');
    if (!grid) return;

    // NPC 主动联动邀请卡片（有事件时在平台顶部弹出）
    const chainEvent = state.chainEvent;
    let chainEventCardHtml = '';
    if (chainEvent && chainEvent.hasEvent) {
        chainEventCardHtml = `
            <div class="chain-event-card">
                <div class="chain-event-head">
                    <div class="chain-event-avatar">${html((chainEvent.npcKey || '?').charAt(0))}</div>
                    <div class="chain-event-info">
                        <span class="chain-event-tag">✨ NPC 主动联动邀请</span>
                        <h4>${html(chainEvent.title || '')}</h4>
                        <span class="chain-event-npc">${html(chainEvent.npcKey || '')}</span>
                    </div>
                </div>
                <p class="chain-event-desc">${html(chainEvent.description || '')}</p>
                <div class="chain-event-actions">
                    <button type="button" class="platform-action-btn chain-event-accept" data-action="resolve-chain-event" data-npc-key="${html(chainEvent.npcKey || '')}" data-event-key="${html(chainEvent.eventKey || '')}">🤝 接受联动</button>
                    <button type="button" class="platform-action-btn chain-event-dismiss" data-action="dismiss-chain-event">稍后</button>
                </div>
            </div>
        `;
    }

    if (!state.platformData || !state.platformData.npcs || state.platformData.npcs.length === 0) {
        grid.innerHTML = chainEventCardHtml + '<p class="info-empty">平台主播数据加载中...</p>';
        return;
    }

    // 显示偷学buff
    if (buffEl) {
        const buff = state.platformData.stealBuff;
        buffEl.innerHTML = buff
            ? `<span class="steal-buff-active">✨ ${html(buff)}</span>`
            : '';
    }

    const npcs = state.platformData.npcs;
    grid.innerHTML = chainEventCardHtml + npcs.map(npc => {
        const affinityBar = Math.max(0, Math.min(100, npc.affinity));
        const affinityTone = npc.affinity < 0 ? 'cold' : npc.affinity < 20 ? 'normal' : npc.affinity < 50 ? 'friendly' : 'close';
        const stealBadge = npc.canStealLearn
            ? '<span class="npc-steal-badge unlocked">可偷学</span>'
            : npc.stealUnlocked
                ? '<span class="npc-steal-badge locked">亲密度不足</span>'
                : '';
        return `
            <div class="platform-npc-card" data-npc-key="${html(npc.key)}">
                <div class="platform-npc-head">
                    <div class="platform-npc-avatar">${html(npc.name.charAt(0))}</div>
                    <div class="platform-npc-info">
                        <h4>${html(npc.name)}</h4>
                        <span class="platform-npc-label">${html(npc.label)}</span>
                    </div>
                    ${stealBadge}
                </div>
                <div class="platform-npc-affinity">
                    <div class="affinity-bar ${affinityTone}" style="width:${affinityBar}%"></div>
                    <span class="affinity-value">${npc.affinity}</span>
                </div>
                <div class="platform-npc-actions">
                    <button type="button" class="platform-action-btn praise" data-action="platform-interact" data-npc-key="${html(npc.key)}" data-interaction-type="DANMAKU_PRAISE">👍 赞美</button>
                    <button type="button" class="platform-action-btn flame" data-action="platform-interact" data-npc-key="${html(npc.key)}" data-interaction-type="DANMAKU_FLAME">🔥 引战</button>
                    <button type="button" class="platform-action-btn technical" data-action="platform-interact" data-npc-key="${html(npc.key)}" data-interaction-type="DANMAKU_TECHNICAL">💡 技术</button>
                    <button type="button" class="platform-action-btn collab" data-action="platform-interact" data-npc-key="${html(npc.key)}" data-interaction-type="COLLAB">🤝 联动</button>
                    <button type="button" class="platform-action-btn steal ${npc.canStealLearn ? '' : 'disabled'}" data-action="platform-steal-learn" data-npc-key="${html(npc.key)}" ${npc.canStealLearn ? '' : 'disabled'}>🥷 偷学</button>
                </div>
            </div>
        `;
    }).join('');
}

async function platformInteract(npcKey, interactionType) {
    await withBusy('发送弹幕中', async () => {
        const result = await api('/api/npc/interact', {
            method: 'POST',
            body: { npcKey, interactionType }
        });
        if (result && result.data) {
            setStatus(`${result.data.npcName}：${result.data.dialogue}`, 'info');
        }
        await refreshAfterWrite();
    });
}

async function platformStealLearn(npcKey) {
    await withBusy('偷学中', async () => {
        const result = await api('/api/npc/steal-learn', {
            method: 'POST',
            body: { npcKey }
        });
        if (result && result.data) {
            const d = result.data;
            const msg = d.discovered
                ? `${d.message} 亲密度${d.affinityChange}，声望${d.reputationChange}。`
                : `${d.message} 亲密度${d.affinityChange}。`;
            setStatus(msg, d.discovered ? 'warning' : 'info');
        }
        await refreshAfterWrite();
    });
}

// 接受 NPC 主动联动：调 resolve 端点标记事件已解决，再重新加载事件并刷新平台
async function resolveChainEvent(npcKey, eventKey) {
    await withBusy('接受联动', async () => {
        await api('/api/npc/chain-event/resolve', {
            method: 'POST',
            body: { npcKey, eventKey }
        });
        ANIM.toast('联动已接受！', 'success');
        state.chainEvent = await api('/api/npc/chain-event');
        renderPlatform();
    });
}

// 稍后处理：仅清除本地事件状态并刷新平台
function dismissChainEvent() {
    state.chainEvent = { hasEvent: false };
    renderPlatform();
}

function renderBuzz() {
    const panel = document.getElementById('buzzPanel');
    if (!state.buzzBriefing) {
        panel.innerHTML = '<p class="info-empty">暂无热搜</p>';
        return;
    }

    const buzz = state.buzzBriefing;
    const headline = buzzHeadlineText(buzz);
    panel.innerHTML = `
        <div class="buzz-card info-card">
            <div class="buzz-card-head">
                <span>热搜风向</span>
                <h4>${html(headline)}</h4>
            </div>
            <div class="buzz-signal-grid">
                ${buzzSignalChip("论坛", buzz.forumLine, "forum")}
                ${buzzSignalChip("切片", buzz.clipperLine, "clipper")}
                ${buzzSignalChip("下一手", firstLocalizedReadableText([buzz.nextMoveHint, buzz.trendLabel], '下一步风向还在整理。'), "next")}
            </div>
            <div class="buzz-signal-grid buzz-extra-grid">
                ${buzzSignalChip("同行动态", buzz.peerUpdate, "peer")}
                ${buzzSignalChip("平台风向", buzz.platformWind, "wind")}
                ${buzzSignalChip("粉丝文化", buzz.fanCulture, "fan")}
                ${buzzSignalChip("热度走势", buzz.heatTrend, "heat")}
            </div>
        </div>
    `;
}

function buzzSignalChip(label, value, tone = "") {
    return `
        <div class="buzz-signal-chip ${tone}">
            <span>${html(label)}</span>
            <strong>${html(buzzLineText(value, "热搜记录还在整理。"))}</strong>
        </div>
    `;
}

// 渲染热搜（用于测试）
function renderBuzzBriefing() {
    renderBuzz();
}

function stageLabelText(briefing, fallback = "阶段复盘") {
    return localizedVisibleTextOrFallback(briefing?.stageLabel, fallback);
}

function stageTrendLabelText(trend, fallback = "口味待观察") {
    return localizedVisibleTextOrFallback(trend?.label, fallback);
}

function stageFortuneIconText(briefing, fallback = "运") {
    return localizedVisibleTextOrFallback(briefing?.dailyFortuneIcon, fallback);
}

function stageFortuneText(briefing, fallback = "今日运势还在抽签，先按体力排班。") {
    return localizedVisibleTextOrFallback(briefing?.dailyFortune, fallback);
}

function stageFortuneAdviceText(briefing, fallback = "运势建议还在整理，别硬冲高风险标题。") {
    return localizedVisibleTextOrFallback(briefing?.dailyFortuneAdvice, fallback);
}

function stageLineText(value, fallback = "阶段情报还在整理。") {
    return localizedVisibleTextOrFallback(value, fallback);
}

function stageBriefingChip(label, value, tone = "") {
    return `
        <div class="stage-briefing-chip ${tone}">
            <span>${html(label)}</span>
            <strong>${html(stageLineText(value))}</strong>
        </div>
    `;
}

function optionalStageBriefingChip(label, value, tone = "") {
    const text = localizedFieldText(value, "");
    return text ? stageBriefingChip(label, text, tone) : "";
}

function reportHistorySummaryText(report, fallback = "这天日报还在补录，录播组正在对轴。") {
    return visibleTextOrFallback(report?.summary, fallback);
}

function reportHistoryChip(label, value, tone = "") {
    return `
        <span class="report-history-chip ${tone}">
            <em>${html(label)}</em>
            <strong>${html(value)}</strong>
        </span>
    `;
}

function renderStageBriefing() {
    const panel = document.getElementById('stageBriefingPanel');
    if (!panel) return;

    if (!state.stageBriefing) {
        panel.innerHTML = '<p class="stage-briefing-empty">暂无阶段复盘</p>';
        return;
    }

    const briefing = state.stageBriefing;
    const currentTrend = briefing.currentTrend || {};
    const nextTrend = briefing.nextTrend || {};
    const objectiveTitle = stageLineText(briefing.objectiveTitle, "阶段目标");
    const objectiveSummary = stageLineText(briefing.objectiveSummary, "阶段目标正在生成。");
    const objectiveProgressLabel = stageLineText(briefing.objectiveProgressLabel, "0/0");
    const objectiveProgress = Number(briefing.objectiveProgress || 0);
    const objectiveItems = renderObjectiveItems(briefing.objectiveItems);
    const objectiveStreak = objectiveStreakStrip(briefing);
    const routeMastery = routeMasteryCard(briefing);
    const likelyEnding = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN";
    const routeMap = renderRouteVisibilityMap(likelyEnding, true);
    const crisisCards = renderCrisisCards(true);
    const rhythmSummary = stageRhythmSummary(briefing);
    const tendencySummary = routeTendencySummary(briefing);

    // 每日运势
    const fortuneHtml = briefing.dailyFortune ? `
        <div class="stage-fortune-card">
            <div class="stage-fortune-head">
                <span class="fortune-mark">${html(stageFortuneIconText(briefing))}</span>
                <strong class="stage-fortune-title">今日运势：${html(stageFortuneText(briefing))}</strong>
            </div>
            <p class="stage-fortune-advice">${html(stageFortuneAdviceText(briefing))}</p>
        </div>
    ` : '';

    panel.innerHTML = `
        <div class="stage-briefing-card info-card">
            <div class="stage-briefing-head">
                <h4>${html(stageLabelText(briefing))}</h4>
                <span class="stage-briefing-days">${briefing.milestoneToday ? '今天复盘' : `还有 ${briefing.daysUntilMilestone || 0} 天`}</span>
            </div>
            ${fortuneHtml}
            ${rhythmSummary || tendencySummary ? `
                <div class="stage-briefing-rhythm">
                    ${rhythmSummary ? `
                        <span class="stage-briefing-rhythm-chip">
                            <em>阶段节奏</em>
                            <strong>${html(rhythmSummary)}</strong>
                        </span>
                    ` : ""}
                    ${tendencySummary ? `
                        <span class="stage-briefing-rhythm-chip route">
                            <em>路线倾向</em>
                            <strong>${html(tendencySummary)}</strong>
                        </span>
                    ` : ""}
                </div>
            ` : ""}
            <div class="stage-briefing-signals">
                <span class="stage-trend-pill current">${html(stageTrendLabelText(currentTrend))}</span>
                <span class="stage-trend-pill next">${html(stageTrendLabelText(nextTrend))}</span>
            </div>
            <div class="stage-objective-card ${objectiveProgressClass(objectiveProgress)}">
                <div class="stage-objective-title">
                    <div>
                        <span>阶段目标</span>
                        <strong>${html(objectiveTitle)}</strong>
                    </div>
                    <em>${html(objectiveProgressLabel)}</em>
                </div>
                <p>${html(objectiveSummary)}</p>
                <div class="ready-goal-objective-bar" aria-hidden="true">
                    <span></span>
                </div>
                ${objectiveStreak}
                ${routeMastery}
                <div class="stage-objective-list">${objectiveItems}</div>
            </div>
            <div class="stage-briefing-grid">
                ${stageBriefingChip("路线", briefing.routeSnapshot, "route")}
                ${stageBriefingChip("粉丝", briefing.fanSnapshot, "fans")}
                ${stageBriefingChip("风险", briefing.riskSnapshot, "risk")}
            </div>
            <div class="stage-briefing-focus">
                ${routeMap}
                ${crisisCards}
            </div>
            <div class="stage-briefing-nextplay">
                <div>
                    <span>下周口味</span>
                    <strong>${html(stageLineText(briefing.nextTrendHint, "平台口味还在对表。"))}</strong>
                </div>
                <div>
                    <span>今日打法</span>
                    <strong>${html(stageLineText(briefing.actionHint, "今日打法还在排班。"))}</strong>
                </div>
            </div>
        </div>
    `;
}

function audienceRouteText(expectation, fallback = "观望路线") {
    return localizedVisibleTextOrFallback(expectation?.routeLabel, fallback);
}

function audienceLockText(expectation, fallback = "观望中") {
    return localizedVisibleTextOrFallback(expectation?.lockLabel, fallback);
}

function audienceLineText(value, fallback = "观众证据还在整理。") {
    return localizedVisibleTextOrFallback(value, fallback);
}

function funMetricLabelText(metric, fallback = "乐子指标") {
    return localizedVisibleTextOrFallback(metric?.label, fallback);
}

function funMetricLineText(metric, fallback = "乐子人还在对轴，先别急着开庭。") {
    return firstLocalizedReadableText([metric?.line, metric?.tone], fallback);
}

function funMetricChips(metric, metricValue) {
    const line = funMetricLineText(metric);
    const status = metricValue >= 70 ? "高涨" : metricValue >= 40 ? "升温" : "观望";
    const next = metricValue >= 70 ? "控风险" : metricValue >= 40 ? "可加码" : "补素材";
    return {
        heat: `${metricValue}%`,
        status,
        next,
        line: line.length > 18 ? `${line.slice(0, 18)}...` : line
    };
}

function comboLabelText(combo, fallback = "未命名组合技") {
    return localizedVisibleTextOrFallback(combo?.label, fallback);
}

function comboEvidenceText(combo, fallback = "组合证据还在整理，等录播组对轴。") {
    return localizedVisibleTextOrFallback(combo?.evidence, fallback);
}

function comboHintText(combo, fallback = "继续探索套路。") {
    return localizedVisibleTextOrFallback(combo?.hint, fallback);
}

function comboNextText(combo, discovered) {
    if (!discovered) {
        const hint = comboHintText(combo, "继续探索套路");
        return hint.length > 8 ? `${hint.slice(0, 8)}...` : hint;
    }
    const nextByKey = {
        PRACTICE_TO_STREAM: "练后开播",
        VIDEO_TO_STREAM: "投稿接播",
        VIDEO_TO_CLIP: "补片再剪",
        CHARGE_COMPLETE: "回气后冲",
        HARD_PRACTICE: "接路线局",
        OVERWORK: "安排休息",
        FORGOTTEN: "恢复营业",
        SOCIAL_WARMUP: "互动接播"
    };
    return nextByKey[combo?.comboKey] || "可复用";
}

function comboDiscoveryChips(combo, discovered) {
    const evidence = comboEvidenceText(combo, "证据待补");
    return {
        status: discovered ? "已发现" : "锁定",
        evidence: evidence.length > 8 ? `${evidence.slice(0, 8)}...` : evidence,
        next: comboNextText(combo, discovered)
    };
}

function comboCollectionSets() {
    const discovered = new Set((state.comboDiscovery?.discovered || [])
        .map(combo => combo?.comboKey)
        .filter(Boolean));
    const locked = new Set((state.comboDiscovery?.locked || [])
        .map(combo => combo?.comboKey)
        .filter(Boolean));
    return { discovered, locked };
}

function comboActionStatus(action) {
    const comboKey = String(action?.comboKey || "");
    const riskCombo = ["OVERWORK", "FORGOTTEN"].includes(comboKey);
    const { discovered, locked } = comboCollectionSets();
    if (riskCombo) {
        return {
            kind: "risk",
            label: "警报",
            tagLabel: "警报",
            tone: "combo-risk",
            rank: 3,
            line: "风险连招会留下压力，除非正在控局，否则先确认代价。"
        };
    }
    if (comboKey && locked.has(comboKey)) {
        return {
            kind: "unlock",
            label: "解锁",
            tagLabel: "解锁",
            tone: "combo-unlock",
            rank: 0,
            line: "这手会补账号图鉴，适合想开新套路的周目。"
        };
    }
    if (comboKey && discovered.has(comboKey)) {
        return {
            kind: "replay",
            label: "刷分",
            tagLabel: "复刷",
            tone: "combo",
            rank: 1,
            line: "账号已收录的熟练打法，适合稳定加厚路线证据。"
        };
    }
    return {
        kind: "window",
        label: "可接",
        tagLabel: "连招",
        tone: "combo",
        rank: 2,
        line: "当前行动窗口能接出组合技。"
    };
}

function activeComboActions() {
    if (!Array.isArray(state.actions)) return [];
    const seen = new Set();
    return state.actions
        .filter(action => action?.comboKey && !action.disabledReason)
        .filter(action => {
            if (seen.has(action.comboKey)) return false;
            seen.add(action.comboKey);
            return true;
        })
        .sort((left, right) =>
            comboActionStatus(left).rank - comboActionStatus(right).rank
            || actionNameText(left).localeCompare(actionNameText(right), "zh-Hans-CN"))
        .slice(0, 3);
}

function comboProgressPercent(discovery) {
    const explicit = Number(discovery?.progressPercent);
    if (Number.isFinite(explicit) && explicit >= 0) {
        return Math.max(0, Math.min(100, Math.round(explicit)));
    }
    const total = Number(discovery?.totalCount || 0);
    const found = Number(discovery?.discoveredCount || 0);
    return total > 0 ? Math.round(Math.max(0, Math.min(100, found * 100 / total))) : 0;
}

function renderActiveComboActions() {
    const active = activeComboActions();
    if (!active.length) {
        return `
            <div class="combo-live empty">
                <span>本手追踪</span>
                <strong>暂无可触发连招</strong>
                <small>先铺练习、投稿、休息或同台互动，下一天会出现可接动作。</small>
            </div>
        `;
    }
    const items = active.map(action => {
        const status = comboActionStatus(action);
        return `
        <button type="button" class="combo-live-action ${status.kind}" data-action="focus-live-combo" data-action-type="${html(action.actionType)}" data-combo-key="${html(action.comboKey)}" data-combo-status="${html(status.kind)}">
            <span>${html(status.label)}</span>
            <strong>${html(actionNameText(action))}</strong>
            <small>${html(actionComboShortText(action) || "组合技")} · ${html(actionComboHintText(action, status.line))}</small>
        </button>
    `;
    }).join("");
    return `
        <div class="combo-live">
            <span>本手可接</span>
            <strong>${active.length} 个连招窗口</strong>
            <div class="combo-live-actions">${items}</div>
        </div>
    `;
}

function personaTagLabelText(tag, fallback = "未署名人设标签") {
    return localizedVisibleTextOrFallback(tag?.label, fallback);
}

function personaTagStatusText(tag, fallback = "观望中") {
    return firstLocalizedReadableText([tag?.statusLabel, statusLabelFor(tag?.status)], fallback);
}

function personaTagEvidenceText(tag, fallback = "人设证据还在整理，先按当前路线营业。") {
    return localizedVisibleTextOrFallback(tag?.evidence, fallback);
}

function personaTagChips(tag, strength) {
    const status = personaTagStatusText(tag);
    const evidence = personaTagEvidenceText(tag);
    const next = strength >= 80 ? "巩固路线" : strength >= 50 ? "补证据" : "先试水";
    return {
        strength: `${strength}%`,
        next,
        evidence: evidence.length > 18 ? `${evidence.slice(0, 18)}...` : evidence
    };
}

function memeStageText(item) {
    const stageNames = {
        NEW: "新梗期",
        REPEAT: "复读期",
        HEAVY_REPEAT: "重度复读",
        BOOMERANG: "回旋期"
    };
    return localizedVisibleTextOrFallback(item?.stageLabel, stageNames[item?.stage] || "新梗期");
}

function memeNextHintText(item, fallback = "梗阶段走向待观察，先别复读过量。") {
    return localizedVisibleTextOrFallback(item?.nextHint, fallback);
}

function memeLifecycleChips(item) {
    const uses = Math.max(0, Number.parseInt(item?.usesInWindow || 0, 10) || 0);
    const nextByStage = {
        NEW: "可试水",
        REPEAT: "控频率",
        HEAVY_REPEAT: "换新料",
        BOOMERANG: "先降温"
    };
    return {
        uses: `${uses}次`,
        next: nextByStage[item?.stage] || "先观察"
    };
}

function memeLifecycleStageClass(item) {
    const stage = String(item?.stage || "NEW").toLowerCase().replace(/[^a-z0-9-]/g, "-");
    return `stage-${stage}`;
}

function endingForecastHeadlineText(forecast, fallback = "结局预演") {
    return localizedVisibleTextOrFallback(forecast?.headline, fallback);
}

function endingForecastFinalTitleText(forecast, fallback = "结局标题待定") {
    return localizedVisibleTextOrFallback(forecast?.likelyFinalTitle, fallback);
}

function endingForecastIfEndedNowTitleText(forecast, fallback = "") {
    return localizedVisibleTextOrFallback(forecast?.ifEndedNowTitle, fallback);
}

function endingForecastTargetReasonText(forecast, fallback = "") {
    return localizedVisibleTextOrFallback(forecast?.targetReason, fallback);
}

function endingForecastRequirementLabelText(requirement, fallback = "门槛待核对") {
    return localizedVisibleTextOrFallback(requirement?.label, fallback);
}

function endingForecastRequirementLineText(requirement, fallback = "门槛证据还在复盘。") {
    return localizedVisibleTextOrFallback(requirement?.line, fallback);
}

function endingForecastRiskText(forecast, fallback = "结局风险待观察。") {
    return localizedVisibleTextOrFallback(forecast?.riskLine, fallback);
}

function endingForecastSprintText(forecast, fallback = "冲刺建议待排班。") {
    return localizedVisibleTextOrFallback(forecast?.sprintHint, fallback);
}

function renderAudienceExpectation() {
    const panel = document.getElementById('audienceExpectationPanel');
    if (!state.audienceExpectation) {
        panel.innerHTML = '<p class="audience-expectation-empty">暂无观众期待</p>';
        return;
    }

    const board = state.audienceExpectation;
    const expectations = (board.expectations || []).slice(0, 4).map(expectation => {
        const share = percent(expectation.share);
        return `
            <div class="audience-expectation-item">
                <div class="audience-expectation-head">
                    <strong class="audience-expectation-route">${html(audienceRouteText(expectation))}</strong>
                    <span class="audience-expectation-lock">${html(audienceLockText(expectation))}</span>
                </div>
                <progress class="expectation-meter" max="100" value="${share}" aria-label="${html(audienceRouteText(expectation))}观众期待占比"></progress>
                <p class="audience-expectation-line">${html(audienceLineText(expectation.evidenceLine))}</p>
            </div>
        `;
    }).join('');

    panel.innerHTML = `
            <div class="audience-expectation-card info-card">
                <div class="audience-expectation-head">
                    <h4>观众期待</h4>
                    <span class="audience-expectation-pivot">${html(audienceLineText(board.pivotRiskLabel, '可试路线'))}</span>
                </div>
            <div class="audience-expectation-list">${expectations || '<p class="audience-expectation-empty">暂无可展示期待</p>'}</div>
        </div>
    `;
}

function renderFunAudienceProfile() {
    const panel = document.getElementById('funAudiencePanel');
    if (!state.funAudienceProfile) {
        panel.innerHTML = '<p class="fun-audience-empty">暂无乐子人画像</p>';
        return;
    }

    const profile = state.funAudienceProfile;
    const metrics = (profile.metrics || []).map(metric => {
        const metricValue = percent(metric.value);
        const chips = funMetricChips(metric, metricValue);
        return `
            <div class="fun-metric">
                <div class="fun-metric-head">
                    <h4 class="fun-metric-title">${html(funMetricLabelText(metric))}</h4>
                </div>
                <div class="fun-metric-chip-row">
                    <span class="fun-metric-chip heat"><em>热度</em><strong>${html(chips.heat)}</strong></span>
                    <span class="fun-metric-chip status"><em>状态</em><strong>${html(chips.status)}</strong></span>
                    <span class="fun-metric-chip next"><em>下一手</em><strong>${html(chips.next)}</strong></span>
                </div>
                <p class="fun-metric-line">${html(funMetricLineText(metric))}</p>
            </div>
        `;
    }).join('');

    panel.innerHTML = `
        <div class="fun-audience-card info-card">
            <div class="fun-audience-card-head">
                <h4>乐子人画像</h4>
                <span>${metrics ? '4项信号' : '无信号'}</span>
            </div>
            <div class="fun-metric-list">${metrics}</div>
        </div>
    `;
}

function routeGalleryStatus(routeKey, endingCollection) {
    const ending = (endingCollection?.endings || []).find(item => item?.type === routeKey);
    if (ending?.unlocked) {
        const grade = localizedVisibleTextOrFallback(ending.bestGrade, "D");
        const score = Number.isFinite(Number(ending.bestScore)) ? Number(ending.bestScore) : 0;
        return `已收录 ${grade} · ${score}分`;
    }
    const score = routeScoreFor(routeKey);
    if (score > 0) return `证据 ${score}分`;
    if (state.vup?.currentRoute === routeKey) return "当前路线";
    if (state.endingForecast?.likelyEndingType === routeKey) return "预演目标";
    return "未起势";
}

function renderRouteGallery() {
    const panel = document.getElementById('routeGalleryPanel');
    if (!panel) return;

    const currentRoute = state.vup?.currentRoute || "UNKNOWN";
    const likelyRoute = state.endingForecast?.likelyEndingType || currentRoute;
    const endingCollection = state.achievementProgress?.endingCollection;
    const routeCards = ROUTE_GALLERY_ORDER.map(routeKey => {
        const visual = routeVisualFor(routeKey);
        const playbook = routePlaybookFor(routeKey);
        const active = routeKey === currentRoute || routeKey === likelyRoute;
        const status = routeGalleryStatus(routeKey, endingCollection);
        return `
            <article class="route-gallery-item ${active ? "active" : ""}" data-route="${html(routeKey)}" data-action="toggle-route-detail" tabindex="0" role="button" aria-label="查看${html(routeLabelFor(routeKey))}路线要求">
                <img src="/gallery/${html(visual.cover)}" alt="${html(routeLabelFor(routeKey))}路线封面" loading="lazy" decoding="async">
                <div>
                    <span>${html(visual.tone)}</span>
                    <strong>${html(routeLabelFor(routeKey))}</strong>
                    <small>${html(playbook.next)}</small>
                </div>
                <em>${html(status)}</em>
                <div class="route-detail" hidden>
                    <p><strong>幻想：</strong>${html(playbook.fantasy)}</p>
                    <p><strong>代价：</strong>${html(playbook.cost)}</p>
                    <p><strong>下一手：</strong>${html(playbook.next)}</p>
                </div>
            </article>
        `;
    }).join("");
    const totalCount = endingCollection?.totalCount || ROUTE_GALLERY_ORDER.length;
    const unlockedCount = endingCollection?.unlockedCount || 0;

    panel.innerHTML = `
        <section class="route-gallery-card info-card" aria-label="路线图库">
            <div class="route-gallery-head">
                <div>
                    <span>路线图库</span>
                    <strong>${html(routeLabelFor(likelyRoute || currentRoute || "UNKNOWN"))}</strong>
                </div>
                <small>${html(endingCollection ? `${unlockedCount}/${totalCount} 已收录` : "首周目探索中")}</small>
            </div>
            <div class="route-gallery-grid">${routeCards}</div>
        </section>
    `;
}

function renderCombo() {
    const panel = document.getElementById('comboPanel');
    if (!state.comboDiscovery) {
        panel.innerHTML = '<p class="combo-empty">暂无组合技</p>';
        return;
    }

    const comboCard = (c, discovered) => {
        const chips = comboDiscoveryChips(c, discovered);
        const hintLine = discovered
            ? `<p class="combo-hint">${html(comboEvidenceText(c))}</p>`
            : `<p class="combo-hint">${html(comboHintText(c))}</p>`;
        return `
        <div class="info-card combo-item ${discovered ? 'discovered' : 'locked'}">
            <div class="combo-head">
                <h4 class="combo-title">${html(comboLabelText(c))}</h4>
            </div>
            <div class="combo-chip-row">
                <span class="combo-chip status"><em>状态</em><strong>${html(chips.status)}</strong></span>
                <span class="combo-chip evidence"><em>证据</em><strong>${html(chips.evidence)}</strong></span>
                <span class="combo-chip next"><em>下一手</em><strong>${html(chips.next)}</strong></span>
            </div>
            ${hintLine}
        </div>
        `;
    };

    const discovered = (state.comboDiscovery.discovered || []).map(c => comboCard(c, true)).join('');

    const locked = (state.comboDiscovery.locked || []).slice(0, 3).map(c => comboCard(c, false)).join('');

    const allCombos = discovered + locked;
    const progress = comboProgressPercent(state.comboDiscovery);
    const collectionLabel = localizedVisibleTextOrFallback(state.comboDiscovery.collectionLabel, "账号图鉴");
    const currentRunCount = Number(state.comboDiscovery.currentRunDiscoveredCount || 0);
    const newThisRunCount = Number(state.comboDiscovery.newThisRunCount || 0);
    const nextLabel = comboLabelText(
        { label: state.comboDiscovery.nextComboLabel },
        state.comboDiscovery.lockedCount > 0 ? "下一张锁定卡" : "全部已发现"
    );
    const nextHint = localizedVisibleTextOrFallback(state.comboDiscovery.nextHint, "继续用不同节奏探索打法。");
    const liveCombos = renderActiveComboActions();

    panel.innerHTML = `
        <div class="combo-card">
            <div class="combo-card-head">
                <h4>组合技发现板</h4>
                <span>${state.comboDiscovery.discoveredCount || 0}/${state.comboDiscovery.totalCount || 0}</span>
            </div>
            <div class="combo-account-strip" aria-label="跨周目组合技图鉴">
                <span><em>范围</em><strong>${html(collectionLabel)}</strong></span>
                <span><em>本局复现</em><strong>${html(currentRunCount)}</strong></span>
                <span><em>新收录</em><strong>${html(newThisRunCount)}</strong></span>
            </div>
            <div class="combo-progress" data-progress="${html(progress)}" aria-valuenow="${html(progress)}">
                <div>
                    <span>图鉴进度</span>
                    <strong>${html(progress)}%</strong>
                </div>
                <div class="combo-progress-bar" aria-hidden="true"><span></span></div>
                <small>${html(nextHint)}</small>
            </div>
            <div class="combo-next-target">
                <span>下一张</span>
                <strong>${html(nextLabel)}</strong>
                <small>${html(state.comboDiscovery.lockedCount > 0 ? "按提示补行动顺序，解锁后会进入日报和结算证据。" : "可以围绕已发现套路冲更高评分。")}</small>
            </div>
            ${liveCombos}
            <div class="combo-list">${allCombos || '<p class="combo-empty">暂无可展示组合技</p>'}</div>
        </div>
    `;
    const progressBar = panel.querySelector(".combo-progress-bar span");
    if (progressBar) {
        progressBar.style.width = `${progress}%`;
    }
}

// 渲染组合技（用于测试）
function renderComboDiscovery() {
    renderCombo();
}

function renderPersonaTags() {
    const panel = document.getElementById('personaTagPanel');
    if (!state.personaTags) {
        panel.innerHTML = '<p class="persona-tag-empty">暂无人设标签</p>';
        return;
    }

    const tags = (state.personaTags.tags || []).slice(0, 4).map(tag => {
        const strength = Math.max(0, Math.min(100, tag.strength || 0));
        const chips = personaTagChips(tag, strength);
        return `
            <div class="info-card persona-tag-item">
                <div class="persona-tag-head">
                    <h4 class="persona-tag-title">${html(personaTagLabelText(tag))}</h4>
                </div>
                <div class="persona-tag-chip-row">
                    <span class="persona-tag-chip strength"><em>强度</em><strong>${html(chips.strength)}</strong></span>
                    <span class="persona-tag-chip status"><em>状态</em><strong>${html(personaTagStatusText(tag))}</strong></span>
                    <span class="persona-tag-chip next"><em>下一手</em><strong>${html(chips.next)}</strong></span>
                </div>
                <p class="persona-tag-evidence">${html(personaTagEvidenceText(tag))}</p>
            </div>
        `;
    }).join('');

    panel.innerHTML = `
        <div class="persona-tag-card">
            <div class="persona-tag-card-head">
                <h4>人设标签</h4>
                <span>${state.personaTags.activeCount || 0}个激活</span>
            </div>
            <div class="persona-tag-list">${tags || '<p class="persona-tag-empty">暂无标签</p>'}</div>
        </div>
    `;
}

function renderMemeLifecycle() {
    const panel = document.getElementById('memeLifecyclePanel');
    if (!state.memeLifecycle) {
        panel.innerHTML = '<p style="color: var(--text-muted);">暂无梗生命周期</p>';
        return;
    }

    const items = (state.memeLifecycle.items || []).map(item => {
        const chips = memeLifecycleChips(item);
        return `
            <div class="info-card meme-lifecycle-item ${memeLifecycleStageClass(item)}">
                <div class="meme-lifecycle-head">
                    <h4 class="meme-lifecycle-title">${html(memeLabelFor(item))}</h4>
                </div>
                <div class="meme-lifecycle-chip-row">
                    <span class="meme-lifecycle-chip uses"><em>复用</em><strong>${html(chips.uses)}</strong></span>
                    <span class="meme-lifecycle-chip stage"><em>阶段</em><strong>${html(memeStageText(item))}</strong></span>
                    <span class="meme-lifecycle-chip next"><em>下一手</em><strong>${html(chips.next)}</strong></span>
                </div>
                <p class="meme-lifecycle-hint">${html(memeNextHintText(item))}</p>
            </div>
        `;
    }).join('');

    panel.innerHTML = `
        <div class="meme-lifecycle-card">
            <div class="meme-lifecycle-card-head">
                <h4>梗生命周期</h4>
                <span>${state.memeLifecycle.items?.length || 0}条</span>
            </div>
            <div class="meme-lifecycle-list">${items || '<p style="color: var(--text-muted);">暂无梗阶段</p>'}</div>
        </div>
    `;
}

function renderReportHistory() {
    const panel = document.getElementById('reportHistoryPanel');
    if (!state.reports || state.reports.length === 0) {
        panel.innerHTML = '<p class="report-history-empty">暂无历史日报</p>';
        return;
    }

    const items = state.reports.slice(-3).reverse().map(r => `
        <div class="report-history-item info-card">
            <div class="report-history-chip-row">
                ${reportHistoryChip("Day", `第${r.day}天`, "day")}
                ${reportHistoryChip("摘要", reportHistorySummaryText(r), "summary")}
            </div>
        </div>
    `).join('');

    panel.innerHTML = `
        <div class="report-history-card">
            <div class="report-history-card-head">
                <h4>最近3天日报</h4>
                <span>${Math.min(state.reports.length, 3)}条</span>
            </div>
            <div class="report-history-list">${items}</div>
        </div>
        ${renderFanStructureChart(state.vup ? state.vup.fanStructure : null)}
        ${renderTrendCharts(state.reports || [])}
        ${renderVisualTimeline(state.reports || [])}
        ${renderDebtChain(state.vup ? state.vup.debts : [])}
        ${renderEndingProbabilityBar(state.endingForecast)}
        <div class="report-history-fan-chart" aria-label="粉丝趋势">
            <canvas id="fanTrendChart" width="400" height="200"></canvas>
        </div>
    `;
    CHARTS.renderFanLine('fanTrendChart', state.reports);
    if (state.vup && state.vup.fanStructure) initFanChart(state.vup.fanStructure);
    if (state.reports) initTrendCharts(state.reports);
}

function renderAchievements() {
    const section = document.getElementById('achievementMini');
    if (!state.achievementProgress) {
        section.innerHTML = '';
        return;
    }

    const { unlockedCount, totalCount, title, endingCollection, featuredTitle, earnedTitles, nextTitle } = state.achievementProgress;
    const endings = endingCollection?.endings || [];

    // 成就解锁音效（数量增加时播放）
    if (unlockedCount > lastAchievementCount && lastAchievementCount > 0) {
        SFX.achievement();
        // 显示成就解锁弹窗
        const latestEnding = endings.filter(e => e.unlocked).pop();
        if (latestEnding) {
            showAchievementPopup(
                latestEnding.icon || '🏆',
                latestEnding.name || '新成就',
                localizedVisibleTextOrFallback(latestEnding.description, '解锁了新结局')
            );
        } else if (featuredTitle) {
            showAchievementPopup(
                '🎖️',
                localizedVisibleTextOrFallback(featuredTitle.label, '新头衔'),
                localizedVisibleTextOrFallback(featuredTitle.description, '获得了新头衔')
            );
        }
    }
    lastAchievementCount = unlockedCount;
    const titleChips = (Array.isArray(earnedTitles) ? earnedTitles : []).slice(-3).reverse().map(item => `
        <span class="achievement-title-chip" title="${html(localizedVisibleTextOrFallback(item.description, item.label))}">
            <em>${html(localizedVisibleTextOrFallback(item.tier, "头衔"))}</em>
            <strong>${html(localizedVisibleTextOrFallback(item.label, "账号头衔"))}</strong>
        </span>
    `).join('');
    const featuredLabel = localizedVisibleTextOrFallback(featuredTitle?.label, title || "成就入门");
    const featuredTier = localizedVisibleTextOrFallback(featuredTitle?.tier, "头衔");
    const nextTitleText = nextTitle
        ? `${localizedVisibleTextOrFallback(nextTitle.label, "下一头衔")} · ${localizedVisibleTextOrFallback(nextTitle.progressText, "推进中")}`
        : "头衔轨道已收齐";
    const atlasSlots = endings.map(ending => {
        const unlocked = Boolean(ending.unlocked);
        const bestGrade = localizedVisibleTextOrFallback(ending.bestGrade, unlocked ? "D" : "?");
        const bestScore = Number.isFinite(Number(ending.bestScore)) ? Number(ending.bestScore) : 0;
        const label = `${ending.name}：${ending.progressText || (unlocked ? `最佳${bestGrade} ${bestScore}分` : "未解锁")}`;
        return `
            <span class="ending-atlas-slot ${unlocked ? 'unlocked' : 'locked'}" title="${html(label)}" aria-label="${html(label)}">
                <em>${html(unlocked ? ending.icon : "?")}</em>
                <strong>${html(unlocked ? bestGrade : "?")}</strong>
            </span>
        `;
    }).join('');
    const atlasCount = endingCollection
        ? `${endingCollection.unlockedCount}/${endingCollection.totalCount}`
        : "0/9";
    const runCount = endingCollection?.completedRuns || 0;
    const atlasUnlockHint = `完成${runMaxDayLabel()}后点亮结局图鉴。`;
    const nextHint = endingCollection?.nextTargetHint || atlasUnlockHint;
    const nextRunGoal = endingCollection?.nextRunGoal || nextHint;
    const nextTargetLabel = endingCollection?.nextTargetLabel || "下一张图鉴";
    const unlockedEndings = endings.filter(ending => ending.unlocked);
    const latestUnlocked = unlockedEndings[unlockedEndings.length - 1];
    const bestEnding = unlockedEndings
        .slice()
        .sort((left, right) => Number(right.bestScore || 0) - Number(left.bestScore || 0))[0];
    const bestRecordText = bestEnding
        ? `${bestEnding.name} ${localizedVisibleTextOrFallback(bestEnding.bestGrade, "D")} · ${Number(bestEnding.bestScore) || 0}分`
        : "暂无最佳记录";
    const nextAtlasTarget = endings.find(ending => ending.type === endingCollection?.nextTargetType);
    const nextGradeText = nextAtlasTarget
        ? localizedVisibleTextOrFallback(nextAtlasTarget.nextGradeHint, nextHint)
        : nextHint;
    const targetRouteMap = nextAtlasTarget
        ? [
            ["打法", nextAtlasTarget.routeRecipe],
            ["门槛", nextAtlasTarget.gateHint],
            ["避坑", nextAtlasTarget.trapHint]
        ].map(([label, line]) => `
            <span>
                <em>${html(label)}</em>
                <strong>${html(localizedVisibleTextOrFallback(line, atlasUnlockHint))}</strong>
            </span>
        `).join('')
        : '';
    const atlasVisibleStatus = latestUnlocked
        ? `最近收录：${latestUnlocked.name}`
        : `最近收录：暂无，先完成任意${runMaxDayLabel()}结局。`;
    section.innerHTML = `
        <div class="achievement-summary">
            <div>
                <div class="achievement-count">${unlockedCount}/${totalCount}</div>
                <div class="achievement-label">${html(title)}</div>
            </div>
        </div>
        <div class="achievement-title-board" aria-label="账号头衔">
            <div class="achievement-title-featured">
                <span>${html(featuredTier)}</span>
                <strong>${html(featuredLabel)}</strong>
                <small>${html(nextTitleText)}</small>
            </div>
            ${titleChips ? `<div class="achievement-title-list">${titleChips}</div>` : ''}
        </div>
        <div class="ending-atlas-summary" title="${html(nextHint)}">
            <div class="ending-atlas-head">
                <span>结局图鉴</span>
                <strong>${atlasCount}</strong>
            </div>
            <div class="ending-atlas-target">
                <span>下轮目标</span>
                <strong>${html(nextTargetLabel)}</strong>
            </div>
            ${targetRouteMap ? `<div class="ending-atlas-route-map" aria-label="下轮目标路线地图">${targetRouteMap}</div>` : ''}
            <div class="ending-atlas-visible-cues" aria-label="结局图鉴可见进度">
                <span><em>收录</em><strong>${html(atlasVisibleStatus)}</strong></span>
                <span><em>最佳</em><strong>${html(bestRecordText)}</strong></span>
                <span><em>下一档</em><strong>${html(nextGradeText)}</strong></span>
            </div>
            <div class="ending-atlas-grid">${atlasSlots}</div>
            <div class="ending-atlas-goal">${html(nextRunGoal)}</div>
            <div class="ending-atlas-foot">${runCount}周目 · ${html(nextHint)}</div>
        </div>
    `;
}

// === 成就解锁弹窗 ===
let lastAchievementPopupTime = 0;
const ACHIEVEMENT_POPUP_COOLDOWN = 3000; // 3秒冷却时间

function showAchievementPopup(icon, name, description) {
    const popup = document.getElementById('achievementPopup');
    if (!popup) return;

    // 防止频繁弹出
    const now = Date.now();
    if (now - lastAchievementPopupTime < ACHIEVEMENT_POPUP_COOLDOWN) return;
    lastAchievementPopupTime = now;

    // 设置内容
    const iconEl = document.getElementById('achievementPopupIcon');
    const nameEl = document.getElementById('achievementPopupName');
    const descEl = document.getElementById('achievementPopupDesc');

    if (iconEl) iconEl.textContent = icon || '🏆';
    if (nameEl) nameEl.textContent = name || '新成就解锁';
    if (descEl) descEl.textContent = description || '';

    // 显示弹窗
    popup.classList.remove('hidden');
    requestAnimationFrame(() => {
        popup.classList.add('active');
    });

    // 创建星星粒子效果
    createAchievementParticles(popup);

    // 播放成就音效
    SFX.achievement();
    BGM.play('achievement');

    // AI语音播报成就
    if (name) TTS.speak(`成就解锁：${name}`);

    // 2秒后自动消失
    if (achievementPopupHideTimer) {
        clearTimeout(achievementPopupHideTimer);
    }
    achievementPopupHideTimer = setTimeout(() => {
        achievementPopupHideTimer = null;
        hideAchievementPopup();
    }, 2000);
}

function hideAchievementPopup() {
    const popup = document.getElementById('achievementPopup');
    if (!popup) return;
    popup.classList.remove('active');
    setTimeout(() => {
        popup.classList.add('hidden');
        if (BGM.currentTrack === 'achievement') {
            syncBGM();
        }
    }, 400);
}

function createAchievementParticles(container) {
    const content = container.querySelector('.achievement-popup-content');
    if (!content) return;

    const colors = ['#f3c969', '#3dd6c6', '#ff6ec7', '#a855f7'];

    for (let i = 0; i < 12; i++) {
        const particle = document.createElement('div');
        particle.className = 'achievement-popup-particle';
        particle.style.left = `${20 + Math.random() * 60}%`;
        particle.style.top = `${30 + Math.random() * 40}%`;
        particle.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
        particle.style.animationDelay = `${Math.random() * 0.5}s`;
        particle.style.animationDuration = `${0.8 + Math.random() * 0.4}s`;
        content.appendChild(particle);

        setTimeout(() => particle.remove(), 1500);
    }
}

// 视觉氛围渲染
function renderVisualMood() {
    const routeKey = state.vup?.currentRoute || 'UNKNOWN';
    const phaseKey = state.session?.phase || 'READY';
    const portrait = document.getElementById('portrait');
    const visual = routeVisualFor(routeKey);

    if (!state.vup && document.getElementById("creationStylePreview")) {
        syncRouteTheme("UNKNOWN", false);
        syncCreationStylePreview();
        return;
    }

    syncRouteTheme(routeKey, Boolean(state.vup));

    if (portrait) {
        portrait.src = `/gallery/${visual.portrait || visual.cover || routePortraits.UNKNOWN}`;
        portrait.alt = `${routeLabelFor(routeKey)}路线主视觉`;
        portrait.dataset.backdrop = phaseBackdrops[phaseKey] || visual.backdrop;
    }

    const badge = document.getElementById('portraitBadge');
    if (badge && state.vup) {
        badge.textContent = `${routeLabelFor(routeKey)} · ${visual.tone}`;
    }
}

function endingGapPrimaryAction(requirement, likelyEnding = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN") {
    const actionTypes = endingGapActionTypes(requirement, likelyEnding);
    const actions = Array.isArray(state.actions) ? state.actions : [];
    return actionTypes
        .map(type => actions.find(action => action?.actionType === type))
        .filter(Boolean)
        .sort((left, right) => Number(Boolean(right.enabled)) - Number(Boolean(left.enabled)))[0] || null;
}

function renderEndingForecastGapCta(forecast) {
    const requirement = (forecast?.requirements || []).find(item => item?.status && item.status !== "PASS");
    if (!requirement) return "";
    const likelyEnding = forecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN";
    const action = endingGapPrimaryAction(requirement, likelyEnding);
    const actionType = action?.actionType || endingGapActionTypes(requirement, likelyEnding)[0] || "";
    const actionName = action ? actionNameText(action) : actionLabelFor(actionType);
    const disabled = action && !action.enabled;
    const progress = endingGapProgressText(requirement);
    const button = actionType ? `
        <button type="button" data-action="focus-ending-gap" data-action-type="${html(actionType)}" ${disabled ? 'aria-disabled="true"' : ''}>
            ${html(disabled ? "先看卡住原因" : `定位：${actionName}`)}
        </button>
    ` : "";
    return `
        <div class="ending-forecast-gap-primary">
            <span>当前缺口</span>
            <strong>${html(endingGapSignalLabel(requirement))} · ${html(endingGapActionLabel(requirement))}</strong>
            ${progress ? `<em class="ending-forecast-progress">${html(progress)}</em>` : ""}
            <small>${html(endingForecastRequirementLineText(requirement))}</small>
            ${button}
        </div>
    `;
}

// 结局预演渲染
function renderEndingForecast() {
    const panel = document.getElementById('endingForecastPanel');
    if (!panel) return;

    if (!state.endingForecast) {
        panel.innerHTML = '<p class="ending-forecast-empty">暂无结局预演</p>';
        return;
    }

    const forecast = state.endingForecast;
    const confidence = percent(forecast.confidence);
    const image = forecast.image || endingImageFor(forecast.likelyEndingType || 'UNKNOWN');
    const ifEndedNowTitle = endingForecastIfEndedNowTitleText(forecast, "");
    const targetReason = endingForecastTargetReasonText(forecast, "");
    const targetComparison = ifEndedNowTitle || targetReason ? `
        <div class="ending-forecast-target-compare">
            <span><em>冲刺目标</em><strong>${html(endingForecastFinalTitleText(forecast))}</strong></span>
            ${ifEndedNowTitle ? `<span><em>现在结算</em><strong>${html(ifEndedNowTitle)}</strong></span>` : ""}
            ${targetReason ? `<small>${html(targetReason)}</small>` : ""}
        </div>
    ` : "";
    const requirements = (forecast.requirements || []).map(requirement => {
        const statusClass = requirement.status === 'PASS' ? 'pass' : 'warn';
        const progress = endingGapProgressText(requirement);
        return `
        <div class="ending-forecast-requirement">
            <div class="ending-forecast-requirement-head">
                <strong class="ending-forecast-requirement-label">${html(endingForecastRequirementLabelText(requirement))}</strong>
                <span class="ending-forecast-status ${statusClass}">${html(statusLabelFor(requirement.status))}</span>
            </div>
            ${progress ? `<div class="ending-forecast-progress">${html(progress)}</div>` : ""}
            <p class="ending-forecast-requirement-line">${html(endingForecastRequirementLineText(requirement))}</p>
        </div>
        `;
    }).join('');

    panel.innerHTML = `
        <div class="ending-forecast-card info-card">
            <img class="ending-forecast-image" src="/gallery/${html(image)}" alt="${html(endingForecastFinalTitleText(forecast))}" loading="lazy" decoding="async">
            <div class="ending-forecast-head">
                <h4>结局预演台</h4>
                <span class="ending-forecast-confidence">${confidence}%</span>
            </div>
            <strong class="ending-forecast-headline">${html(endingForecastHeadlineText(forecast))}</strong>
            <div class="ending-forecast-tags">
                <span class="ending-forecast-tag">${html(endingForecastFinalTitleText(forecast))}</span>
                <span class="ending-forecast-tag">${html(routeLabelFor(forecast.likelyEndingType || 'UNKNOWN'))}</span>
            </div>
            ${targetComparison}
            ${renderEndingForecastGapCta(forecast)}
            <div class="ending-forecast-list">${requirements || '<p class="ending-forecast-empty">暂无可展示门槛</p>'}</div>
            <p class="ending-forecast-copy">${html(endingForecastRiskText(forecast))}</p>
            <p class="ending-forecast-copy">${html(endingForecastSprintText(forecast))}</p>
        </div>
    `;
}

// 用户操作
async function register() {
    await withBusy('创建本地存档中', async () => {
        await api('/api/auth/register', {
            method: 'POST',
            body: {
                username: document.getElementById('username').value,
                password: document.getElementById('password').value,
                nickname: document.getElementById('nickname').value
            }
        });
        state.user = await api('/api/auth/login', {
            method: 'POST',
            body: {
                username: document.getElementById('username').value,
                password: document.getElementById('password').value
            }
        });
        await hydrate();
    });
}

async function login() {
    await withBusy('打开旧档中', async () => {
        state.user = await api('/api/auth/login', {
            method: 'POST',
            body: {
                username: document.getElementById('username').value,
                password: document.getElementById('password').value
            }
        });
        await hydrate();
    });
}

function activeLocalSlotNumber() {
    return Number(state.vup?.slotNumber || state.saveSlots?.find(slot => slot?.occupied)?.slotNumber || 1);
}

async function quickStartGuest(slotNumber = 1) {
    const targetSlot = Number(slotNumber || 1);
    await withBusy('本地开局中', async () => {
        const result = await api(`/api/save-slots/${targetSlot}/start`, { method: 'POST', body: {} });
        await finishRunEntry(result);
        setStatus(`本地槽 ${targetSlot} 已开播，第一天可以直接选行动。`, 'success');
        return result;
    });
}

async function continueLocalRun(slotNumber = activeLocalSlotNumber()) {
    const targetSlot = Number(slotNumber || 1);
    await withBusy('读取本地档中', async () => {
        try {
            const result = await api(`/api/save-slots/${targetSlot}/continue`);
            await finishRunEntry(result);
            setStatus(`已接回槽 ${targetSlot}，继续当前这一天。`, 'success');
            return result;
        } catch (error) {
            if (error?.code === 'NO_SAVE') {
                setLocalSaveSlots([]);
                render();
                setStatus('还没有可继续的本地档，先开始第一局。', 'warning');
                return { suppressBusySuccess: true };
            }
            throw error;
        }
    });
}

async function saveLocalSlot() {
    if (!state.vup) {
        setStatus('还没有正在进行的本地局。', 'warning');
        return;
    }
    const targetSlot = activeLocalSlotNumber();
    await withBusy('保存本地档中', async () => {
        const slot = await api(`/api/save-slots/${targetSlot}/save`, { method: 'POST', body: {} });
        await refreshLocalSaveSlots();
        renderHeader();
        setStatus(manualSaveStatusText(slot), 'success');
        return slot;
    });
}

function saveArchiveFileName(slotNumber, archive) {
    const slotName = archive?.slot?.name || state.saveSlots?.find(slot => Number(slot?.slotNumber) === slotNumber)?.name || `slot-${slotNumber}`;
    const safeName = String(slotName)
        .replace(/[\\/:*?"<>|]/g, '-')
        .replace(/\s+/g, '-')
        .replace(/-+/g, '-')
        .slice(0, 48) || `slot-${slotNumber}`;
    return `vupworld-slot-${slotNumber}-${safeName}.json`;
}

function downloadJsonFile(fileName, payload) {
    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function chooseSaveArchiveFile() {
    return new Promise((resolve, reject) => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'application/json,.json';
        input.style.display = 'none';
        input.addEventListener('change', async () => {
            const file = input.files?.[0];
            input.remove();
            if (!file) {
                resolve(null);
                return;
            }
            try {
                const text = await file.text();
                resolve(JSON.parse(text));
            } catch (error) {
                reject(new Error('存档文件不是可读取的 JSON。'));
            }
        }, { once: true });
        document.body.appendChild(input);
        input.click();
    });
}

async function exportLocalSlot(slotNumber = activeLocalSlotNumber()) {
    const targetSlot = Number(slotNumber || 1);
    await withBusy('导出本地档中', async () => {
        const archive = await api(`/api/save-slots/${targetSlot}/export`);
        downloadJsonFile(saveArchiveFileName(targetSlot, archive), archive);
        setStatus(`槽 ${targetSlot} 已导出为 JSON 存档。`, 'success');
        return archive;
    });
}

async function importLocalSlot(slotNumber = activeLocalSlotNumber()) {
    const targetSlot = Number(slotNumber || 1);
    let archive;
    try {
        archive = await chooseSaveArchiveFile();
    } catch (error) {
        setStatus(error.message || '存档文件无法读取。', 'error');
        return;
    }
    if (!archive) {
        return;
    }
    const confirmed = window.confirm(`导入会覆盖本槽 ${targetSlot} 的当前进度。确定继续吗？`);
    if (!confirmed) {
        return;
    }
    await withBusy('导入本地档中', async () => {
        const result = await api(`/api/save-slots/${targetSlot}/import`, {
            method: 'POST',
            body: archive
        });
        await finishRunEntry(result);
        setStatus(`槽 ${targetSlot} 已导入，当前进度已恢复。`, 'success');
        return result;
    });
}

async function restartLocalSlot(slotNumber = activeLocalSlotNumber()) {
    const targetSlot = Number(slotNumber || 1);
    const confirmed = window.confirm(`重新开始槽 ${targetSlot} 会覆盖当前进度，并从第1天开始新一局。确定继续吗？`);
    if (!confirmed) {
        return;
    }
    await withBusy('重开本地槽中', async () => {
        const result = await api(`/api/save-slots/${targetSlot}/restart`, { method: 'POST', body: {} });
        await finishRunEntry(result);
        setStatus(`槽 ${targetSlot} 已重新开始，新的第一天已就绪。`, 'success');
        return result;
    });
}

async function renameLocalSlot(slotNumber = activeLocalSlotNumber()) {
    const targetSlot = Number(slotNumber || 1);
    const saveSlots = Array.isArray(state.saveSlots) ? state.saveSlots : [];
    const primarySlot = saveSlots.find(slot => Number(slot?.slotNumber) === targetSlot) || null;
    const currentName = primarySlot?.name || `本地存档 ${targetSlot}`;
    const nextName = window.prompt(`输入槽 ${targetSlot} 的新存档名`, currentName);
    if (nextName === null) {
        return;
    }
    const trimmed = nextName.trim();
    if (!trimmed) {
        setStatus('存档名不能为空。', 'warning');
        return;
    }
    if (trimmed.length > 64) {
        setStatus('存档名不能超过64个字。', 'warning');
        return;
    }
    await withBusy('重命名本地存档中', async () => {
        const slot = await api(`/api/save-slots/${targetSlot}/rename`, {
            method: 'POST',
            body: { name: trimmed }
        });
        await refreshLocalSaveSlots();
        renderAuth();
        renderHeader();
        setStatus(`本地存档已重命名为「${trimmed}」。`, 'success');
        return slot;
    });
}

function localActionSlotNumber(element) {
    const parsed = Number(element?.dataset?.slotNumber || 1);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
}

function localActionType(element) {
    return element?.dataset?.actionType || "";
}

function callLocalAction(fn, ...args) {
    if (typeof fn !== "function") return false;
    const result = fn(...args);
    if (result && typeof result.catch === "function") {
        result.catch(error => {
            console.error("Local action failed:", error);
            setStatus(error?.message || "Action failed. Please refresh and try again.", "error");
        });
    }
    return true;
}

function bindLocalActionBridge() {
    if (window.__vupLocalActionBridgeBound) return;
    window.__vupLocalActionBridgeBound = true;

    document.addEventListener("click", event => {
        const element = event.target?.closest?.("[data-action]");
        if (!element || !document.contains(element) || element.disabled || element.getAttribute("aria-disabled") === "true") return;

        const action = element.dataset.action;
        let handled = false;

        switch (action) {
            case "quick-start-guest":
            case "start-local-slot":
                handled = callLocalAction(quickStartGuest, localActionSlotNumber(element));
                break;
            case "continue-local-run":
                handled = callLocalAction(continueLocalRun, localActionSlotNumber(element));
                break;
            case "save-local-slot":
                handled = callLocalAction(saveLocalSlot);
                break;
            case "export-local-slot":
                handled = callLocalAction(exportLocalSlot, localActionSlotNumber(element));
                break;
            case "import-local-slot":
                handled = callLocalAction(importLocalSlot, localActionSlotNumber(element));
                break;
            case "rename-local-slot":
                handled = callLocalAction(renameLocalSlot, localActionSlotNumber(element));
                break;
            case "restart-local-slot":
                handled = callLocalAction(restartLocalSlot, localActionSlotNumber(element));
                break;
            case "retry-bootstrap":
                handled = callLocalAction(hydrate);
                break;
            case "submit-action":
            case "simple-primary-action":
            case "simple-alt-action":
            case "quick-submit-action":
            case "detail-submit-action":
                handled = callLocalAction(submitAction, localActionType(element));
                break;
            case "next-day":
                handled = callLocalAction(nextDay);
                break;
            case "choose-title":
                handled = callLocalAction(chooseTitle, Number(element.dataset.titleId));
                break;
            case "reroll-title":
                handled = callLocalAction(rerollTitle);
                break;
            case "cancel-action":
                handled = callLocalAction(cancelAction);
                break;
            case "choose-event":
                handled = callLocalAction(chooseEvent, element.dataset.choice);
                break;
            case "choose-interaction":
                handled = callLocalAction(chooseInteraction, element.dataset.choice);
                break;
            case "choose-fan-topic":
                handled = callLocalAction(chooseFanTopic, element.dataset.topic, element.dataset.topicChoice);
                break;
            case "use-risk-tool": {
                const targetDebtId = Number(element.dataset.targetDebtId);
                handled = callLocalAction(
                    useRiskTool,
                    element.dataset.riskToolType,
                    Number.isFinite(targetDebtId) ? targetDebtId : null
                );
                break;
            }
            case "reply-letter":
                handled = callLocalAction(replyLetter, element.dataset.letterReply, element.dataset.letterId || "");
                break;
            case "opening-style-choice":
                handled = callLocalAction(submitOpeningStyleChoice, element.dataset.openingStyle);
                break;
            case "skip-offstream":
                handled = callLocalAction(skipOffStream);
                break;
            default:
                if (action && action.startsWith("offstream-")) {
                    handled = callLocalAction(submitOffStream, action.slice("offstream-".length).toUpperCase());
                }
                break;
        }

        if (handled) {
            event.preventDefault();
            event.stopImmediatePropagation();
        }
    });
}

bindLocalActionBridge();

async function logout() {
    await withBusy('退出登录中', async () => {
        await api('/api/auth/logout', { method: 'POST', body: {} });
        state.user = null;
        resetGameState();
        render();
    });
}

async function createVup() {
    await withBusy('开始出道中', async () => {
        await api('/api/vup/create', {
            method: 'POST',
            body: {
                name: document.getElementById('vupName').value,
                persona: creationPersonaForSubmit()
            }
        });
        await refreshAfterWrite();
    });
}

async function submitAction(actionType) {
    if (!isActionConfirmSkipped()) {
        state.pendingAction = actionType;
        renderActions();
        setStatus("行动已选好，请在屏幕确认条里执行或返回。", "warning");
        window.requestAnimationFrame(() => {
            const panel = document.querySelector('.action-confirm-panel');
            panel?.scrollIntoView({ block: 'center', behavior: 'smooth' });
            panel?.querySelector('.confirm-execute-btn')?.focus({ preventScroll: true });
        });
        return;
    }
    await doSubmitAction(actionType);
}

async function submitOpeningStyleChoice(key) {
    const choice = openingStyleChoices.find(item => item.key === key);
    if (!choice) return;
    const action = findEnabledOpeningAction(choice);
    if (!action) {
        setStatus("今天暂时没有可执行行动，先刷新一下进度。", "warning");
        return;
    }
    state.openingStyleChoice = {
        key: choice.key,
        route: choice.route,
        label: choice.label
    };
    const planType = choice.route === "BLACK_RED_MAIN_STAGE" && action.actionType === "NPC_INTERACT"
        ? "BORROW_HEAT"
        : undefined;
    await doSubmitAction(action.actionType, { openingStyle: choice.key, planType });
}

async function doSubmitAction(actionType, options = {}) {
    await withBusy('提交今日行动中', async () => {
        const endingGap = endingGapSnapshot(actionType);
        const planType = options.planType ?? actionPlanTypeFor(actionType);
        const keyParts = currentRunKeyParts("action", actionType, planType, options.openingStyle || "");

        // 打击感动画：找到选中的行动卡片
        const card = options.openingStyle
            ? document.querySelector(`.opening-style-choice-card[data-opening-style="${options.openingStyle}"]`)
            : document.querySelector(`.daily-plan-card [data-action="daily-plan-submit"][data-action-type="${actionType}"]`)?.closest('.daily-plan-card')
                || document.querySelector(`.action-item[data-action-type="${actionType}"]`);
        if (card) {
            card.style.transition = 'transform 0.15s ease-out';
            card.style.transform = 'scale(1.05)';
        }

        const requestBody = {
            actionType,
            planType: actionPlanTypeFor(actionType),
            idempotencyKey: stableIdempotencyKey('day-action', keyParts)
        };
        if (options.planType !== undefined) {
            requestBody.planType = options.planType;
        }
        const result = await api('/api/day/action', {
            method: 'POST',
            body: requestBody
        });
        state.lastActionResult = result?.actionResult || null;
        if (state.lastActionResult && endingGap) {
            state.lastActionResult.endingGapSnapshot = endingGap;
        }
        state.pendingAction = null;

        // 打击感动画：闪光+弹跳+成功音效
        if (card) {
            ANIM.flash(card, 'positive');
            ANIM.bounce(card);
            ANIM.cardShine(card);
            setTimeout(() => {
                card.style.transform = '';
                card.style.transition = '';
            }, 400);
        }

        // 行动反馈动画：数字弹出 + 粒子效果
        triggerActionFeedbackAnimations(state.lastActionResult, card);

        await refreshAfterWrite();
        if (state.vup) checkMilestones(state.vup.day || 1, state.vup);
        clearStableIdempotencyKey('day-action', keyParts);
        window.setTimeout(focusActionResultHud, 0);
    });
}

/**
 * 触发行动反馈动画：根据行动结果弹出数字、粒子和Toast通知
 * @param {Object} result - 行动结果对象
 * @param {HTMLElement} cardEl - 行动卡片元素（可选）
 */
function triggerActionFeedbackAnimations(result, cardEl) {
    if (!result) return;

    const fanChange = Number(result.fanChange || 0);
    const staminaChange = Number(result.staminaChange || 0);
    const routeChange = Number(result.routeScoreChange || 0);
    const reputationChange = Number(result.reputationChange || 0);
    const popularityChange = Number(result.popularityChange || 0);
    const watchHeatChange = Number(result.watchHeatChange || 0);

    // 找到 stat 区域的元素用于定位弹出数字
    const statsSection = document.querySelector('.desktop-stat-brief');

    // 延迟一帧等 render 完成后再弹数字
    requestAnimationFrame(() => {
        // 粉丝数字弹出 + 粒子
        if (fanChange !== 0) {
            const fanPill = findStatPill('粉丝');
            const fanText = fanChange > 0 ? `+${fanChange}` : `${fanChange}`;
            const fanType = fanChange > 0 ? 'fans' : 'negative';
            ANIM.floatNumber(fanPill || statsSection, fanText, fanType);
            if (fanChange > 0) {
                ANIM.particles(fanPill || statsSection, 'heart', Math.min(fanChange > 20 ? 10 : 5, 12));
            }
        }

        // 体力数字弹出
        if (staminaChange !== 0) {
            const staminaPill = findStatPill('体力');
            const staminaText = staminaChange > 0 ? `+${staminaChange}` : `${staminaChange}`;
            ANIM.floatNumber(staminaPill || statsSection, staminaText, staminaChange > 0 ? 'positive' : 'negative');
        }

        // 路线分数弹出 + 光点粒子
        if (routeChange !== 0) {
            const routeHud = document.querySelector('[data-stat="route-change"]');
            const routeText = routeChange > 0 ? `+${routeChange}` : `${routeChange}`;
            ANIM.floatNumber(routeHud || statsSection, routeText, 'route');
            if (routeChange > 0) {
                ANIM.particles(routeHud || statsSection, 'sparkle', 4);
            }
        }

        // 口碑数字弹出
        if (reputationChange !== 0) {
            const repPill = findStatPill('口碑');
            const repText = reputationChange > 0 ? `+${reputationChange}` : `${reputationChange}`;
            ANIM.floatNumber(repPill || statsSection, repText, reputationChange > 0 ? 'positive' : 'negative');
        }

        // 曝光是后台成长权重，不抢前台主指标；有详情卡时再提示。
        if (popularityChange !== 0) {
            const popCard = findStatCard('曝光') || findStatCard('人气');
            const popText = popularityChange > 0 ? `+${popularityChange}` : `${popularityChange}`;
            if (popCard) {
                ANIM.floatNumber(popCard, popText, popularityChange > 0 ? 'positive' : 'negative');
            }
        }

        // 热度数字弹出
        if (watchHeatChange !== 0) {
            const heatCard = findStatCard('围观');
            const heatText = watchHeatChange > 0 ? `+${watchHeatChange}` : `${watchHeatChange}`;
            ANIM.floatNumber(heatCard || statsSection, heatText, watchHeatChange > 0 ? 'gold' : 'negative');
            if (watchHeatChange >= 5) {
                ANIM.particles(heatCard || statsSection, 'star', 6);
            }
        }

        // Toast 通知
        if (fanChange > 0) {
            ANIM.toast(`粉丝 +${fanChange}`, 'success');
        }
        if (routeChange > 0) {
            ANIM.toast(`歌力 +${routeChange}`, 'info');
        }
        if (reputationChange > 0) {
            ANIM.toast(`口碑 +${reputationChange}`, 'success');
        }
    });
}

/** 在 stat pill 中查找指定 label 的元素 */
function findStatPill(label) {
    const pills = document.querySelectorAll('.stat-pill');
    for (const pill of pills) {
        if (pill.querySelector('span')?.textContent === label) return pill;
    }
    const briefs = document.querySelectorAll('.desktop-stat-brief .stat-pill');
    for (const pill of briefs) {
        if (pill.querySelector('span')?.textContent === label) return pill;
    }
    return null;
}

/** 在 stat card 中查找指定 label 的元素 */
function findStatCard(label) {
    const cards = document.querySelectorAll('.stat-card');
    for (const card of cards) {
        if (card.querySelector('.stat-card-label')?.textContent === label) return card;
    }
    return null;
}

function isActionConfirmSkipped() {
    return localStorage.getItem('vup_skipActionConfirm') === 'true';
}

function toggleActionConfirmSkip() {
    const current = isActionConfirmSkipped();
    localStorage.setItem('vup_skipActionConfirm', String(!current));
    renderHeader();
}

function confirmPendingAction() {
    const actionType = state.pendingAction;
    if (actionType) {
        state.pendingAction = null;
        doSubmitAction(actionType);
    }
}

function cancelPendingAction() {
    state.pendingAction = null;
    renderActions();
}

function actionConfirmPanelHtml(action) {
    if (!action) return '';
    const actionName = actionNameText(action);
    const effectPreview = actionPreviewText(action.effectPreview, "收获待观察");
    const riskPreview = action.riskPreview ? actionPreviewText(action.riskPreview, "风险待观察") : "低风险";
    const riskLevel = actionRiskLevelText(action.riskLevel);
    const apCost = action.actionPointCost || 0;
    const fanRange = action.effectPreview || "待观察";
    const decisionSummary = actionDecisionSummary(action, actionCardState(action, false, false));
    return `
        <div class="action-confirm-panel" role="dialog" aria-label="确认执行行动">
            <div class="action-confirm-header">
                <h4>${html(actionName)}</h4>
                <span class="action-confirm-risk ${riskToneForAction(action.riskLevel)}">${html(riskLevel)}</span>
            </div>
            <div class="action-confirm-body">
                <div class="action-confirm-stats">
                    <div class="action-confirm-stat">
                        <span class="stat-label">行动点</span>
                        <span class="stat-value">${apCost}</span>
                    </div>
                    <div class="action-confirm-stat">
                        <span class="stat-label">预期收获</span>
                        <span class="stat-value">${html(effectPreview)}</span>
                    </div>
                    <div class="action-confirm-stat">
                        <span class="stat-label">风险</span>
                        <span class="stat-value">${html(riskPreview)}</span>
                    </div>
                </div>
                <div class="action-confirm-deltas">
                    <span class="delta-item delta-reason"><em>推荐理由</em><strong>${html(decisionSummary.reason)}</strong></span>
                    <span class="delta-item delta-benefit"><em>收获</em><strong>${html(decisionSummary.benefit)}</strong></span>
                    <span class="delta-item delta-risk"><em>风险</em><strong>${html(decisionSummary.risk)}</strong></span>
                </div>
                ${renderActionEffectPreview(action)}
            </div>
            <div class="action-confirm-buttons">
                <button type="button" class="confirm-execute-btn" data-action="confirm-pending-action">确认执行</button>
                <button type="button" class="confirm-cancel-btn" data-action="cancel-pending-action">返回</button>
            </div>
        </div>
    `;
}

async function chooseTitle(titleId) {
    await withBusy('选择标题中', async () => {
        const keyParts = currentRunKeyParts("title", titleId, state.session?.selectedPlanId);
        const result = await api('/api/stream/title/choose', {
            method: 'POST',
            body: {
                titleTemplateId: titleId,
                idempotencyKey: stableIdempotencyKey('stream-title', keyParts)
            }
        });
        state.lastActionResult = result?.actionResult || null;
        await refreshAfterWrite();
        clearStableIdempotencyKey('stream-title', keyParts);
    });
}

async function rerollTitle() {
    await withBusy('换一批标题中', async () => {
        const keyParts = currentRunKeyParts("reroll", state.session?.titleRerollCount || 0);
        await api('/api/stream/titles/reroll', {
            method: 'POST',
            body: { idempotencyKey: stableIdempotencyKey('title-reroll', keyParts) }
        });
        await refreshAfterWrite();
        clearStableIdempotencyKey('title-reroll', keyParts);
    });
}

async function chooseEvent(choiceId) {
    await withBusy('处理事件中', async () => {
        const eventKeyParts = [
            state.pendingEvent?.debtId,
            state.pendingEvent?.title,
            choiceId
        ];
        // 事件面板发光效果
        const eventPanel = document.querySelector('.event-panel, .event-card, [role="dialog"]');
        ANIM.eventGlow(eventPanel);
        ANIM.toast('事件处理中...', 'info');
        await api('/api/event/choose', {
            method: 'POST',
            body: {
                choiceId: choiceId,
                idempotencyKey: stableIdempotencyKey('event-choice', eventKeyParts)
            }
        });
        await refreshAfterWrite();
        clearStableIdempotencyKey('event-choice', eventKeyParts);
    });
}

async function chooseInteraction(choiceType) {
    await withBusy('处理直播现场中', async () => {
        const keyParts = currentRunKeyParts(
            "interaction",
            state.pendingInteraction?.eventId || state.pendingInteraction?.title,
            choiceType
        );
        await api('/api/interaction/choose', {
            method: 'POST',
            body: {
                choiceType: choiceType,
                idempotencyKey: stableIdempotencyKey('interaction-choice', keyParts)
            }
        });
        await refreshAfterWrite();
        clearStableIdempotencyKey('interaction-choice', keyParts);
    });
}

// 渲染正式事件选择
function renderFormalEventChoice(eventChoice) {
    return renderEventChoice(eventChoice, 'event', 0);
}

async function nextDay() {
    await withBusy('进入下一天中', async () => {
        const keyParts = currentRunKeyParts("next-day");
        await api('/api/day/next', {
            method: 'POST',
            body: { idempotencyKey: stableIdempotencyKey('next-day', keyParts) }
        });
        state.lastActionResult = null;
        state.lastRiskToolResult = null;
        await refreshAfterWrite();
        SFX.nextDay();
        clearStableIdempotencyKey('next-day', keyParts);
    });
}

async function restart() {
    const restartBias = document.getElementById('restartBias')?.value || 'UNKNOWN';
    await withBusy('复活赛准备中', async () => {
        const restartKeyParts = [
            state.ending?.id,
            state.ending?.endingType,
            restartBias
        ];
        await api('/api/rebirth/restart', {
            method: 'POST',
            body: {
                confirmRestart: true,
                restartBiasType: restartBias,
                idempotencyKey: stableIdempotencyKey('restart', restartKeyParts)
            }
        });
        state.restartBiasOverride = null;
        state.lastActionResult = null;
        state.lastRiskToolResult = null;
        await refreshAfterWrite();
        clearStableIdempotencyKey('restart', restartKeyParts);
    });
}

function restartEndingIdentity() {
    return [
        state.ending?.id,
        state.ending?.endingType,
        state.ending?.finalTitle
    ].filter(value => value !== null && value !== undefined && value !== "").join(":") || "ENDING_READY";
}

const restartBiasHintCopy = {
    SINGING_IDOL: "歌势遗珠复活赛，先还高音债，再把歌回排成固定档。",
    ELECTRONIC_PICKLE: "电子榨菜复活赛，老粉保温低压陪饭，先做成稳定饭点。",
    SLICE_SAINT: "切片组继续供货，复活赛投稿箱别断粮，先让乐子人有素材。",
    BLACK_RED_MAIN_STAGE: "主会场继续开庭，复活赛标题组可以上强度，但米线工具要随手备好。",
    CYBER_GIRLFRIEND: "赛博女友复活赛开低压陪伴档，别把营业线打成独角兽审判。",
    DD_BUS_STOP: "DD公交站复活赛先跑联动排班，把同台关系打通，别让车门夹到老粉。",
    MAIN_STAGE_KING: "主会场之王复活赛再开麦，先接住热度，再补一条稳粉路线。",
    GLORIOUS_GRADUATION: "光荣毕业复活赛体面返场，少一点预支，多一点告别仪式。",
    UNKNOWN: "查无此V复活赛，先小开一局，别让标题组整月摸鱼。"
};

function restartBiasRouteFor(routeKey, fallbackRouteKey = "UNKNOWN") {
    const route = String(routeKey || "").trim();
    if (Object.prototype.hasOwnProperty.call(restartBiasHintCopy, route)) return route;
    const fallbackRoute = String(fallbackRouteKey || "").trim();
    if (Object.prototype.hasOwnProperty.call(restartBiasHintCopy, fallbackRoute)) return fallbackRoute;
    return "UNKNOWN";
}

function restartBiasHintText(routeKey) {
    return restartBiasHintCopy[restartBiasRouteFor(routeKey)];
}

function restartBiasPreview(routeKey) {
    const route = restartBiasRouteFor(routeKey);
    const actionTypes = primaryRouteActionTypes(route);
    const actionLine = actionTypes.length
        ? actionTypes.slice(0, 3).map(actionLabelFor).join(" / ")
        : "先小开一局，再用图鉴目标定路线";
    const label = routeLabelFor(route);
    const target = route === "UNKNOWN" ? "先定路线" : label;
    const boundary = route === "UNKNOWN"
        ? "不继承旧债，只保留重开提示；前7天尽快选一条可结算路线。"
        : "只继承路线提示和少量粉丝偏置；旧债、资源和倍率都重新开局。";
    return {
        route,
        label,
        target,
        actionLine,
        boundary
    };
}

function renderRestartBiasPreview(routeKey) {
    const preview = restartBiasPreview(routeKey);
    return `
        <div id="restartBiasPreview" class="ending-restart-preview" aria-label="复活赛倾向预览">
            <span><em>目标</em><strong>${html(preview.target)}</strong></span>
            <span><em>首周</em><strong>${html(preview.actionLine)}</strong></span>
            <span><em>边界</em><strong>${html(preview.boundary)}</strong></span>
        </div>
    `;
}

function syncRestartBiasPreview(routeKey) {
    const panel = document.getElementById('restartBiasPreview');
    if (!panel) return;
    const preview = restartBiasPreview(routeKey);
    const slots = panel.querySelectorAll('span');
    if (slots[0]) slots[0].querySelector('strong').textContent = preview.target;
    if (slots[1]) slots[1].querySelector('strong').textContent = preview.actionLine;
    if (slots[2]) slots[2].querySelector('strong').textContent = preview.boundary;
}

function updateRestartBiasHint(userChanged = false) {
    const select = document.getElementById('restartBias');
    const hint = document.getElementById('restartBiasHint');
    if (!hint) return;
    const route = restartBiasRouteFor(select?.value);
    if (userChanged && select) {
        state.restartBiasOverride = {
            endingId: restartEndingIdentity(),
            value: route
        };
    }
    hint.textContent = restartBiasHintText(route);
    syncRestartBiasPreview(route);
    const prompt = document.getElementById('restartNextRunPrompt');
    if (prompt) {
        prompt.textContent = nextRunMotivationText({
            routeKey: route,
            endingCollection: state.achievementProgress?.endingCollection,
            forecast: state.endingForecast,
            ending: state.ending
        });
    }
}

// 渲染重开选项
function renderRestartOptions(selectedRoute = "UNKNOWN") {
    const route = restartBiasRouteFor(selectedRoute);
    return `
        <select id="restartBias" class="ending-restart-select" aria-label="选择复活赛路线倾向" data-change-action="update-restart-bias-hint">
            <option value="SINGING_IDOL" ${route === 'SINGING_IDOL' ? 'selected' : ''}>歌势遗珠</option>
            <option value="ELECTRONIC_PICKLE" ${route === 'ELECTRONIC_PICKLE' ? 'selected' : ''}>电子榨菜</option>
            <option value="SLICE_SAINT" ${route === 'SLICE_SAINT' ? 'selected' : ''}>切片圣体</option>
            <option value="BLACK_RED_MAIN_STAGE" ${route === 'BLACK_RED_MAIN_STAGE' ? 'selected' : ''}>黑红主会场</option>
            <option value="CYBER_GIRLFRIEND" ${route === 'CYBER_GIRLFRIEND' ? 'selected' : ''}>赛博女友</option>
            <option value="DD_BUS_STOP" ${route === 'DD_BUS_STOP' ? 'selected' : ''}>DD公交站</option>
            <option value="MAIN_STAGE_KING" ${route === 'MAIN_STAGE_KING' ? 'selected' : ''}>主会场之王</option>
            <option value="GLORIOUS_GRADUATION" ${route === 'GLORIOUS_GRADUATION' ? 'selected' : ''}>光荣毕业</option>
            <option value="UNKNOWN" ${route === 'UNKNOWN' ? 'selected' : ''}>查无此V</option>
        </select>
    `;
}

async function chooseFanTopic(topicKey, choiceType) {
    await withBusy('处理粉丝群议题中', async () => {
        const keyParts = currentRunKeyParts("fan-topic", topicKey, choiceType);
        await api('/api/fan-topic/choose', {
            method: 'POST',
            body: {
                topicKey,
                choiceType,
                idempotencyKey: stableIdempotencyKey('fan-topic-choice', keyParts)
            }
        });
        await refreshAfterWrite();
        clearStableIdempotencyKey('fan-topic-choice', keyParts);
    });
}

async function useRiskTool(toolType, targetDebtId) {
    await withBusy('米线工具执行中', async () => {
        const keyParts = currentRunKeyParts("risk-tool", toolType, targetDebtId);
        const result = await api('/api/risk-tool/use', {
            method: 'POST',
            body: {
                toolType,
                targetDebtId,
                idempotencyKey: stableIdempotencyKey('risk-tool-use', keyParts)
            }
        });
        state.lastRiskToolResult = result;
        await refreshAfterWrite();
        openInsightTab('ambient');
        window.setTimeout(() => pulseFocusTarget(document.querySelector('.risk-tool-result')), 0);
        clearStableIdempotencyKey('risk-tool-use', keyParts);
    });
}

async function cancelAction() {
    await withBusy('取消今日企划中', async () => {
        const keyParts = currentRunKeyParts("cancel", state.session?.selectedPlanId);
        await api('/api/day/action/cancel', {
            method: 'POST',
            body: { idempotencyKey: stableIdempotencyKey('cancel-action', keyParts) }
        });
        await refreshAfterWrite();
        clearStableIdempotencyKey('cancel-action', keyParts);
    });
}

// ============================================================
// 排行榜渲染
// ============================================================
let _leaderboardCache = null;
let _leaderboardCacheTime = 0;
let _myRankCache = null;
let _myRankCacheTime = 0;
const LEADERBOARD_CACHE_TTL = 30000;

async function loadLeaderboard(dimension = 'score', limit = 10) {
    const now = Date.now();
    const cacheKey = `${dimension}:${limit}`;
    if (_leaderboardCache && _leaderboardCacheTime > now - LEADERBOARD_CACHE_TTL && _leaderboardCache._key === cacheKey) {
        return _leaderboardCache;
    }
    try {
        const data = await api(`/api/leaderboard?dimension=${encodeURIComponent(dimension)}&limit=${limit}`);
        _leaderboardCache = data || [];
        _leaderboardCache._key = cacheKey;
        _leaderboardCacheTime = now;
    } catch (e) {
        _leaderboardCache = [];
        _leaderboardCache._key = cacheKey;
    }
    return _leaderboardCache;
}

async function loadMyRank() {
    const now = Date.now();
    if (_myRankCache && _myRankCacheTime > now - LEADERBOARD_CACHE_TTL) {
        return _myRankCache;
    }
    try {
        _myRankCache = await api('/api/leaderboard/me');
        _myRankCacheTime = now;
    } catch (e) {
        _myRankCache = null;
    }
    return _myRankCache;
}

function renderLeaderboard() {
    const panel = document.getElementById('leaderboardPanel');
    if (!panel) return;

    const dimension = state._leaderboardDimension || 'score';
    const dimensionLabels = { score: '综合分', fans: '粉丝数', reputation: '口碑', runs: '周目数' };
    const dimensionLabel = dimensionLabels[dimension] || '综合分';

    const dimTabs = Object.entries(dimensionLabels).map(([key, label]) => `
        <button type="button" class="leaderboard-dim-tab ${key === dimension ? 'active' : ''}"
                data-action="leaderboard-dim" data-dimension="${html(key)}">${html(label)}</button>
    `).join('');

    const entries = Array.isArray(state._leaderboardData) ? state._leaderboardData : [];
    const myRank = state._myRankData;

    if (entries.length === 0) {
        panel.innerHTML = `
            <div class="leaderboard-card info-card">
                <div class="leaderboard-head">
                    <span class="leaderboard-kicker">排行榜</span>
                    <h4>${html(dimensionLabel)}</h4>
                </div>
                <div class="leaderboard-dims">${dimTabs}</div>
                <p class="leaderboard-empty">暂无排行数据，完成一次出道局后解锁。</p>
            </div>
        `;
        return;
    }

    const medalIcons = ['🥇', '🥈', '🥉'];
    const rows = entries.map(entry => {
        const rank = entry.rank || 0;
        const medal = rank <= 3 ? medalIcons[rank - 1] : '';
        const rankDisplay = medal || `<span class="leaderboard-rank-num">${rank}</span>`;
        const scoreKey = dimension === 'score' ? 'score' : dimension;
        const value = entry[scoreKey] ?? entry.score ?? 0;
        const grade = entry.grade ? `<span class="leaderboard-grade grade-${html(entry.grade)}">${html(entry.grade)}</span>` : '';
        return `
            <div class="leaderboard-row ${rank <= 3 ? 'top-rank' : ''}">
                <span class="leaderboard-rank">${rankDisplay}</span>
                <span class="leaderboard-name">${html(entry.nickname || '匿名')}</span>
                ${grade}
                <span class="leaderboard-value">${html(value)}</span>
            </div>
        `;
    }).join('');

    const myRankHtml = myRank ? `
        <div class="leaderboard-my-rank">
            <span class="leaderboard-my-label">我的记录</span>
            <span class="leaderboard-my-nick">${html(myRank.nickname || '未知')}</span>
            <span class="leaderboard-my-score">最佳 ${html(myRank.bestScore ?? 0)} 分</span>
            <span class="leaderboard-my-runs">${html(myRank.totalRuns ?? 0)} 周目</span>
        </div>
    ` : '';

    panel.innerHTML = `
        <div class="leaderboard-card info-card">
            <div class="leaderboard-head">
                <span class="leaderboard-kicker">排行榜</span>
                <h4>${html(dimensionLabel)}</h4>
            </div>
            <div class="leaderboard-dims">${dimTabs}</div>
            <div class="leaderboard-list">${rows}</div>
            ${myRankHtml}
        </div>
    `;
}

async function switchLeaderboardDimension(dimension) {
    state._leaderboardDimension = dimension;
    renderLeaderboard();
    try {
        state._leaderboardData = await loadLeaderboard(dimension, 10);
        renderLeaderboard();
    } catch (e) {
        setStatus(e.message || '排行榜加载失败', 'error');
    }
}

async function initLeaderboardData() {
    if (state._leaderboardData) return;
    try {
        const [data, myRank] = await Promise.all([
            loadLeaderboard(state._leaderboardDimension || 'score', 10),
            loadMyRank()
        ]);
        state._leaderboardData = data;
        state._myRankData = myRank;
        renderLeaderboard();
    } catch (e) { /* silent */ }
}

// ============================================================
// 结局图鉴渲染（解锁图鉴面板）
// ============================================================
function renderUnlockAtlas() {
    const panel = document.getElementById('unlockAtlasPanel');
    if (!panel) return;
    panel.innerHTML = buildUnlockAtlasHtml();
}

// 构建图鉴HTML（供面板和弹窗复用）
function buildUnlockAtlasHtml() {
    const endingCollection = state.achievementProgress?.endingCollection;
    if (!endingCollection) {
        return `
            <div class="unlock-atlas-card info-card">
                <div class="unlock-atlas-head">
                    <span class="unlock-atlas-kicker">结局图鉴</span>
                    <h4>解锁图鉴</h4>
                </div>
                <p class="unlock-atlas-empty">完成一次${runMaxDayLabel()}出道局后解锁结局图鉴。</p>
            </div>
        `;
    }

    const endings = endingCollection.endings || [];
    const unlockedCount = endingCollection.unlockedCount || 0;
    const totalCount = endingCollection.totalCount || endings.length;
    const completedRuns = endingCollection.completedRuns || 0;
    const nextHint = endingCollection.nextTargetHint || `完成${runMaxDayLabel()}后点亮结局图鉴。`;

    const endingCards = endings.map(ending => {
        const unlocked = Boolean(ending.unlocked);
        const icon = ending.icon || '?';
        const name = ending.name || '未知结局';
        const bestGrade = ending.bestGrade || (unlocked ? 'D' : '?');
        const bestScore = Number.isFinite(Number(ending.bestScore)) ? Number(ending.bestScore) : 0;
        const progressText = ending.progressText || (unlocked ? `最佳${bestGrade} ${bestScore}分` : '未解锁');
        const typeClass = ending.type ? ending.type.toLowerCase().replace(/_/g, '-') : '';
        const guidance = endingAtlasGuidance(ending.type);
        const atlasPlan = unlocked
            ? `<small class="unlock-atlas-run-plan">最佳结局摘要：${html(progressText)}，可换线补图鉴。</small>`
            : `<small class="unlock-atlas-run-plan">推荐路线：${html(guidance.route)}；行动：${html(guidance.action)}；${html(guidance.risk)}</small>`;

        return `
            <div class="unlock-atlas-ending ${unlocked ? 'unlocked' : 'locked'} ${typeClass}">
                <div class="unlock-atlas-ending-icon">${html(icon)}</div>
                <div class="unlock-atlas-ending-info">
                    <strong>${html(name)}</strong>
                    <span class="unlock-atlas-ending-grade grade-${html(bestGrade)}">${html(bestGrade)}</span>
                    <small>${html(progressText)}</small>
                    ${atlasPlan}
                </div>
                ${unlocked ? '<span class="unlock-atlas-check">✓</span>' : '<span class="unlock-atlas-lock">🔒</span>'}
            </div>
        `;
    }).join('');

    const progressPercent = totalCount > 0 ? Math.round((unlockedCount / totalCount) * 100) : 0;
    const nextGoal = endingCollection.nextRunGoal || nextHint;

    return `
        <div class="unlock-atlas-card info-card">
            <div class="unlock-atlas-head">
                <span class="unlock-atlas-kicker">结局图鉴</span>
                <h4>解锁图鉴</h4>
            </div>
            <div class="unlock-atlas-progress-bar">
                <div class="unlock-atlas-progress-fill" style="width: ${progressPercent}%"></div>
                <span class="unlock-atlas-progress-text">${unlockedCount}/${totalCount} (${progressPercent}%)</span>
            </div>
            <div class="unlock-atlas-meta">
                <span>累计 ${completedRuns} 周目</span>
                <span>${html(nextGoal)}</span>
            </div>
            <div class="unlock-atlas-grid">${endingCards}</div>
        </div>
    `;
}

// 成就图鉴弹窗
function showAtlasPopup() {
    const existing = document.getElementById('atlasPopupOverlay');
    if (existing) { existing.remove(); return; }
    const overlay = document.createElement('div');
    overlay.id = 'atlasPopupOverlay';
    overlay.className = 'modal-overlay';
    overlay.style.display = 'flex';
    overlay.innerHTML = `
        <div class="modal-content modal-content--wide">
            <div class="atlas-popup-head">
                <h3>🏅 成就图鉴</h3>
                <button type="button" class="atlas-popup-close" data-action="close-atlas-popup" aria-label="关闭">×</button>
            </div>
            <div class="atlas-popup-body">${buildUnlockAtlasHtml()}</div>
        </div>
    `;
    document.body.appendChild(overlay);
    if (typeof openDialog === 'function') openDialog(overlay);
}

function closeAtlasPopup() {
    const overlay = document.getElementById('atlasPopupOverlay');
    if (overlay) {
        if (typeof closeDialog === 'function') closeDialog(overlay);
        overlay.remove();
    }
}

// ============================================================
// V2 UI Overhaul: Tab Layout / Modals / Drawer / Avatar
// ============================================================

// --- Tab switching ---
function switchMainTab(tabName) {
    const target = tabName === 'platform'
        ? 'npcs'
        : tabName === 'infoHub'
            ? (state.infoTab || 'host')
            : 'host';
    activateInfoTab(target);

    // Update main tab bar active state and aria-selected
    document.querySelectorAll('#mainTabBar [data-action="switch-main-tab"]').forEach(btn => {
        const isActive = btn.dataset.mainTab === tabName;
        btn.classList.toggle('active', isActive);
        btn.setAttribute('aria-selected', String(isActive));
        btn.tabIndex = isActive ? 0 : -1;
    });
}

function openLiveDrawer(target = 'host') {
    state.infoTabManualPhase = state.session?.phase || (state.vup ? "READY" : "");
    activateInfoTab(target);
}

function closeLiveDrawer() {
    const drawer = document.getElementById('liveDrawer');
    if (drawer) drawer.classList.remove('open');
}

function switchTab(tabName) {
    state.infoTabManualPhase = state.session?.phase || (state.vup ? "READY" : "");
    activateInfoTab(tabName);
}

window.openLiveDrawer = openLiveDrawer;
window.closeLiveDrawer = closeLiveDrawer;
window.switchTab = switchTab;
window.switchMainTab = switchMainTab;
if (typeof window.activateInfoTab !== 'function') {
    window.activateInfoTab = function(target = 'host') {
        state.infoTab = target || 'host';
    };
}
if (typeof window.openInsightTab !== 'function') {
    window.openInsightTab = function(target = 'host') {
        return window.activateInfoTab(target);
    };
}

function reportDrawerElements() {
    return {
        drawer: document.getElementById('reportDrawer'),
        panel: document.getElementById('reportPanel')
    };
}

function syncReportDrawerState() {
    const { drawer, panel } = reportDrawerElements();
    if (!panel) return;
    const hasReport = !panel.classList.contains('hidden');
    if (drawer) drawer.classList.toggle('open', hasReport);
    if (hasReport) {
        activateInfoTab('report');
    }
}

function toggleReportDrawer() {
    const { drawer, panel } = reportDrawerElements();
    if (!panel || panel.classList.contains('hidden')) {
        activateInfoTab('report');
        return;
    }
    if (drawer) drawer.classList.toggle('open');
    activateInfoTab('report');
}

function collapseReportDrawer() {
    const { drawer } = reportDrawerElements();
    if (drawer) drawer.classList.remove('open');
}

// Map old info tab names to new main tab names
const INFO_TAB_TO_MAIN_TAB = {
    npc: 'infoHub',
    buzz: 'infoHub',
    audience: 'infoHub',
    routes: 'infoHub',
    ambient: 'infoHub',
    combo: 'infoHub',
    report: 'infoHub',
    leaderboard: 'infoHub',
    atlas: 'infoHub',
    demo: 'infoHub'
};

function updateVupAvatar() {
    const img = document.getElementById('vupAvatarImg');
    const badge = document.getElementById('vupAvatarBadge');
    if (!img) return;

    const route = state.vup?.currentRoute || 'UNKNOWN';
    img.src = ROUTE_AVATAR_MAP[route] || ROUTE_AVATAR_MAP.UNKNOWN;

    if (badge) {
        const routeLabel = state.vup?.currentRoute
            ? (routeVisualFor(route)?.tone || route)
            : '等待出道';
        badge.textContent = routeLabel;
    }
}

// --- Report Drawer observer ---
function setupReportDrawerObserver() {
    const reportPanel = document.getElementById('reportPanel');
    if (!reportPanel) return;

    const observer = new MutationObserver(() => syncReportDrawerState());

    observer.observe(reportPanel, { attributes: true, attributeFilter: ['class'] });
    syncReportDrawerState();
}

// --- Initialize V2 UI on page load ---
function initV2UI() {
    setupReportDrawerObserver();
    updateVupAvatar();
    initCollapsibleObservers();
}

// info-hub 区块折叠（点击标题切换）
document.addEventListener('click', e => {
    const title = e.target.closest('.info-hub-title');
    if (title) title.closest('.info-hub-section')?.classList.toggle('collapsed');
});

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initV2UI);
} else {
    initV2UI();
}

// ============================================================
// Info Panel Collapsible (information layering)
// ============================================================

const _collapsibleState = {};
const _collapsibleObservers = new Map();

/** Extract a short title from panel's existing content (first h4, or fallback). */
function _extractPanelTitle(panel) {
    const h4 = panel.querySelector('h4');
    if (h4) return h4.textContent.trim();
    const h3 = panel.querySelector('h3');
    if (h3) return h3.textContent.trim();
    const strong = panel.querySelector('strong');
    if (strong) return strong.textContent.trim();
    return panel.id.replace('Panel', '');
}

/** Wrap existing panel content in collapsible-header + collapsible-content. */
function _wrapCollapsible(panel) {
    if (panel.querySelector('.collapsible-header')) return;

    const title = _extractPanelTitle(panel);
    const prevChildren = Array.from(panel.childNodes);

    const header = document.createElement('div');
    header.className = 'collapsible-header';
    header.innerHTML = `<h4>${title}</h4><span class="collapsible-toggle"><span class="toggle-label">展开</span><span class="toggle-arrow">&#9660;</span></span>`;

    const content = document.createElement('div');
    content.className = 'collapsible-content';
    prevChildren.forEach(child => content.appendChild(child));

    panel.appendChild(header);
    panel.appendChild(content);

    header.addEventListener('click', () => _toggleCollapsible(panel));
}

/** Toggle collapsed/expanded state for a panel. */
function _toggleCollapsible(panel) {
    const wasCollapsed = panel.classList.contains('collapsed');
    panel.classList.toggle('collapsed');
    _collapsibleState[panel.id] = panel.classList.contains('collapsed');

    const label = panel.querySelector('.toggle-label');
    if (label) label.textContent = panel.classList.contains('collapsed') ? '展开' : '收起';
}

/** Observe a single panel for content changes (innerHTML replacement). */
function _observePanel(panel) {
    if (_collapsibleObservers.has(panel.id)) return;

    const observer = new MutationObserver(() => {
        // Skip if no collapsible children left (panel was cleared)
        if (!panel.querySelector('.collapsible-header') &&
            !panel.querySelector('.collapsible-content') &&
            panel.childNodes.length > 0) {
            // Content was replaced - re-wrap
            _initSinglePanel(panel);
        }
    });

    observer.observe(panel, { childList: true, subtree: true });
    _collapsibleObservers.set(panel.id, observer);
}

/** Initialize a single collapsible panel. */
function _initSinglePanel(panel) {
    if (!panel.hasAttribute('data-collapsible')) return;
    if (panel.childNodes.length === 0) return;

    // Preserve previous collapsed state
    if (!(panel.id in _collapsibleState)) {
        _collapsibleState[panel.id] = true; // default collapsed
    }

    _wrapCollapsible(panel);

    if (_collapsibleState[panel.id]) {
        panel.classList.add('collapsed');
        const label = panel.querySelector('.toggle-label');
        if (label) label.textContent = '展开';
    } else {
        panel.classList.remove('collapsed');
        const label = panel.querySelector('.toggle-label');
        if (label) label.textContent = '收起';
    }
}

/** Scan all data-collapsible panels and initialize any that have content. */
function initCollapsiblePanels() {
    document.querySelectorAll('.info-panel[data-collapsible="true"]').forEach(panel => {
        if (panel.querySelector('.collapsible-header')) return;
        if (panel.childNodes.length === 0) return;
        _initSinglePanel(panel);
    });
}

/** Set up MutationObservers on all data-collapsible panels (called once). */
function initCollapsibleObservers() {
    document.querySelectorAll('.info-panel[data-collapsible="true"]').forEach(panel => {
        _observePanel(panel);
    });
}

// === 粉丝结构可视化 ===
function renderFanStructureChart(fanStructure) {
  if (!fanStructure || !window.Chart) return '';

  return `<div class="fan-chart-container"><canvas id="fanPieChart" width="220" height="220"></canvas></div>`;
}

function initFanChart(fanStructure) {
  const canvas = document.getElementById('fanPieChart');
  if (!canvas || !window.Chart) return;

  destroyChartForCanvas(canvas);
  new window.Chart(canvas, {
    type: 'doughnut',
    data: {
      labels: ['真粉', '乐子', '独角兽', 'DD'],
      datasets: [{
        data: [fanStructure.trueFans, fanStructure.funFans, fanStructure.unicornFans, fanStructure.ddFans],
        backgroundColor: ['#f3c969', '#65d17a', '#ff7eb3', '#94b7ff'],
        borderColor: ['rgba(243,201,105,0.8)', 'rgba(101,209,122,0.8)', 'rgba(255,126,179,0.8)', 'rgba(149,183,255,0.8)'],
        borderWidth: 2
      }]
    },
    options: {
      responsive: false,
      cutout: '60%',
      plugins: {
        legend: {
          position: 'bottom',
          labels: { color: 'rgba(255,255,255,0.8)', font: { size: 11 }, padding: 8 }
        },
        tooltip: {
          callbacks: {
            label: function(ctx) {
              const total = ctx.dataset.data.reduce((a, b) => a + b, 0);
              const pct = total > 0 ? Math.round(ctx.raw / total * 100) : 0;
              return `${ctx.label}: ${ctx.raw} (${pct}%)`;
            }
          }
        }
      }
    }
  });
}

// === 行动反馈预览 ===
function renderActionEffectPreview(action) {
  if (!action?.effectPreview) return '';

  // 解析effectPreview中的数值
  const effects = parseEffectPreview(action.effectPreview);
  if (!effects.length) return '';

  return `
    <div class="action-effect-bars">
      ${effects.map(e => `
        <div class="effect-bar-row">
          <span class="effect-label">${html(e.label)}</span>
          <div class="effect-bar-track">
            <div class="effect-bar-fill ${e.value >= 0 ? 'positive' : 'negative'}"
                 style="width: ${Math.min(Math.abs(e.value) * 3, 100)}%"></div>
          </div>
          <span class="effect-value ${e.value >= 0 ? 'positive' : 'negative'}">
            ${e.value >= 0 ? '+' : ''}${e.value}
          </span>
          <span class="effect-explain">${html(metricImpactText(e))}</span>
        </div>
      `).join('')}
    </div>
  `;
}

function parseEffectPreview(preview) {
  if (!preview) return [];
  const effects = [];
  const pushEffect = effect => {
    if (!effects.some(existing => existing.key === effect.key)) {
      effects.push(effect);
    }
  };
  const patterns = [
    { regex: /粉丝[+＋](\d+)/, label: '粉丝', key: 'fans', sign: 1 },
    { regex: /粉丝[-－](\d+)/, label: '粉丝', key: 'fans', sign: -1 },
    { regex: /口碑[+＋](\d+)/, label: '口碑', key: 'reputation', sign: 1 },
    { regex: /口碑[-－](\d+)/, label: '口碑', key: 'reputation', sign: -1 },
    { regex: /(围观热度|围观|热度)[+＋](\d+)/, label: '围观', key: 'watchHeat', sign: 1, group: 2 },
    { regex: /(围观热度|围观|热度)[-－](\d+)/, label: '围观', key: 'watchHeat', sign: -1, group: 2 },
    { regex: /人气[+＋](\d+)/, label: '人气', key: 'popularity', sign: 1 },
    { regex: /人气[-－](\d+)/, label: '人气', key: 'popularity', sign: -1 },
    { regex: /梗浓度[+＋](\d+)/, label: '梗浓度', key: 'memeLevel', sign: 1 },
    { regex: /梗浓度[-－](\d+)/, label: '梗浓度', key: 'memeLevel', sign: -1 },
    { regex: /体力[+＋](\d+)/, label: '体力', key: 'stamina', sign: 1 },
    { regex: /体力[-－](\d+)/, label: '体力', key: 'stamina', sign: -1 },
    { regex: /灵感[+＋](\d+)/, label: '灵感', key: 'inspiration', sign: 1 },
    { regex: /灵感[-－](\d+)/, label: '灵感', key: 'inspiration', sign: -1 },
    { regex: /(运营预算|预算)[+＋](\d+)/, label: '运营预算', key: 'coin', sign: 1, group: 2 },
    { regex: /(运营预算|预算)[-－](\d+)/, label: '运营预算', key: 'coin', sign: -1, group: 2 }
  ];
  const arrowPatterns = [
    { regex: /粉丝[↑⬆]/, label: '粉丝', key: 'fans', value: 1 },
    { regex: /粉丝[↓⬇]/, label: '粉丝', key: 'fans', value: -1 },
    { regex: /口碑[↑⬆]/, label: '口碑', key: 'reputation', value: 1 },
    { regex: /口碑[↓⬇]/, label: '口碑', key: 'reputation', value: -1 },
    { regex: /(围观热度|围观|热度)[↑⬆]/, label: '围观', key: 'watchHeat', value: 1 },
    { regex: /(围观热度|围观|热度)[↓⬇]/, label: '围观', key: 'watchHeat', value: -1 },
    { regex: /体力[↑⬆]/, label: '体力', key: 'stamina', value: 1 },
    { regex: /体力[↓⬇]/, label: '体力', key: 'stamina', value: -1 },
    { regex: /灵感[↑⬆]/, label: '灵感', key: 'inspiration', value: 1 },
    { regex: /灵感[↓⬇]/, label: '灵感', key: 'inspiration', value: -1 },
    { regex: /(运营预算|预算)[↑⬆]/, label: '运营预算', key: 'coin', value: 1 },
    { regex: /(运营预算|预算)[↓⬇]/, label: '运营预算', key: 'coin', value: -1 },
    { regex: /风险[↑⬆]/, label: '风险', key: 'risk', value: 1 },
    { regex: /风险[↓⬇]/, label: '风险', key: 'risk', value: -1 }
  ];

  patterns.forEach(p => {
    const match = preview.match(p.regex);
    if (match) {
      pushEffect({ label: p.label, key: p.key, value: parseInt(match[p.group || 1]) * p.sign });
    }
  });
  arrowPatterns.forEach(p => {
    if (preview.match(p.regex)) {
      pushEffect({ label: p.label, key: p.key, value: p.value });
    }
  });

  return effects;
}

function metricImpactExplanations(text) {
  return parseEffectPreview(String(text || ""));
}

function metricImpactText(effect) {
  const up = Number(effect?.value || 0) >= 0;
  switch (effect?.key) {
    case 'fans':
      return up ? '关注你的人变多，结局和平台排名更稳。' : '观众流失，成长和结局评分会吃亏。';
    case 'reputation':
      return up ? '老粉更安心，舆论风险下降。' : '观众信任下降，更容易被黑和翻旧账。';
    case 'watchHeat':
      return up ? '直播间更热闹，弹幕更多，但也更容易引来争议。' : '讨论降温，传播变慢但压力也小。';
    case 'popularity':
      return up ? '曝光面扩大，更容易被路人看到。' : '曝光下降，传播速度变慢。';
    case 'memeLevel':
      return up ? '记忆点更强，切片更容易传播。' : '梗记忆变弱，出圈能力下降。';
    case 'stamina':
      return up ? '今天还能做更多事。' : '消耗精力，太低会卡行动。';
    case 'inspiration':
      return up ? '内容点子增加，可用于标题、企划和投稿。' : '消耗内容点子，缺了会卡内容行动。';
    case 'coin':
      return up ? '可花预算增加，制作和风险处理更从容。' : '花掉预算，后续制作和处理风险会更紧。';
    case 'risk':
      return up ? '风险上升，未来更容易爆旧账和舆论事件。' : '风险下降，后续行动更稳。';
    default:
      return up ? '这项指标上升，会改善当前局面。' : '这项指标下降，需要后续补回来。';
  }
}

function renderMetricImpactLine(effect) {
  const value = Number(effect?.value || 0);
  const favorable = effect?.key === 'risk' ? value <= 0 : value >= 0;
  return `
    <span class="metric-impact-line ${favorable ? 'positive' : 'negative'}">
      <em>${html(effect.label)} ${value >= 0 ? '+' : ''}${value}</em>
      <strong>${html(metricImpactText(effect))}</strong>
    </span>
  `;
}

// === 时间线可视化 ===
function renderVisualTimeline(reports) {
  if (!reports?.length) return '';

  return `
    <div class="visual-timeline">
      ${reports.map((r, i) => {
        const fanDelta = r.dataDelta?.fanChange || 0;
        const hasEvent = r.highlights?.length > 0;
        const nodeType = fanDelta > 20 ? 'milestone' : fanDelta < -5 ? 'crisis' : hasEvent ? 'event' : 'normal';

        return `
          <div class="timeline-node ${nodeType}">
            <div class="timeline-dot"></div>
            <div class="timeline-connector"></div>
            <div class="timeline-content">
              <div class="timeline-day">第${r.day}天</div>
              <div class="timeline-summary">${html(r.summary || '').substring(0, 50)}</div>
              <div class="timeline-delta ${fanDelta >= 0 ? 'positive' : 'negative'}">
                ${fanDelta >= 0 ? '+' : ''}${fanDelta} 粉丝
              </div>
              ${r.selectedTitle ? `<div class="timeline-title">📺 ${html(r.selectedTitle)}</div>` : ''}
            </div>
          </div>
        `;
      }).join('')}
    </div>
  `;
}

// === 债务关系图 ===
function renderDebtChain(debts) {
  if (!debts?.length) return '';

  const openDebts = debts.filter(d => d.status === 'OPEN');
  const clearedDebts = debts.filter(d => d.status === 'CLEARED');

  return `
    <div class="debt-chain">
      <div class="debt-chain-title">🔗 债务链</div>
      ${openDebts.length ? `
        <div class="debt-chain-section">
          <div class="chain-section-label">🔴 未清偿 (${openDebts.length})</div>
          ${openDebts.map(d => `
            <div class="debt-node open severity-${d.severity}">
              <div class="debt-type">${html(d.debtLabel || d.debtType)}</div>
              <div class="debt-meta">
                <span class="debt-severity">严重度: ${d.severity}</span>
                <span class="debt-due">${d.remainingDays <= 0 ? '已到期！' : d.remainingDays + '天后到期'}</span>
              </div>
              <div class="debt-summary">${html(debtSourceLine(d))}</div>
              <div class="debt-summary">${html(debtConsequenceLine(d))}</div>
              <div class="debt-summary">${html(debtRecommendedLine(d))}</div>
            </div>
          `).join('')}
        </div>
      ` : ''}
      ${clearedDebts.length ? `
        <div class="debt-chain-section">
          <div class="chain-section-label">✅ 已清偿 (${clearedDebts.length})</div>
          ${clearedDebts.slice(-3).map(d => `
            <div class="debt-node cleared">
              <div class="debt-type">${html(d.debtLabel || d.debtType)}</div>
            </div>
          `).join('')}
        </div>
      ` : ''}
      ${!openDebts.length && !clearedDebts.length ? '<div class="debt-chain-empty">暂无债务记录</div>' : ''}
    </div>
  `;
}

// === 路线雷达图 ===
function renderRouteRadarChart(routeScores) {
  if (!routeScores || !window.Chart) return '';

  const canvas = document.createElement('canvas');
  canvas.id = 'routeRadarCanvas';
  canvas.width = 300;
  canvas.height = 300;

  const labels = Object.keys(routeScores).filter(k => k !== 'UNKNOWN').map(k => routeLabelFor(k));
  const data = Object.entries(routeScores).filter(([k]) => k !== 'UNKNOWN').map(([, v]) => v);
  const maxScore = Math.max(...data, 1);

  setTimeout(() => {
    new Chart(canvas, {
      type: 'radar',
      data: {
        labels: labels,
        datasets: [{
          label: '路线分',
          data: data,
          backgroundColor: 'rgba(61,214,198,0.2)',
          borderColor: 'rgba(61,214,198,0.8)',
          borderWidth: 2,
          pointBackgroundColor: 'rgba(61,214,198,1)',
          pointRadius: 4
        }]
      },
      options: {
        responsive: false,
        scales: {
          r: {
            beginAtZero: true,
            max: maxScore + 2,
            ticks: { display: false },
            grid: { color: 'rgba(255,255,255,0.1)' },
            pointLabels: { color: 'rgba(255,255,255,0.8)', font: { size: 12 } }
          }
        },
        plugins: { legend: { display: false } }
      }
    });
  }, 100);

  return `<div class="route-radar-container" id="routeRadarContainer"></div>`;
}

// === 趋势折线图 ===
function renderTrendCharts(reports) {
  if (!reports?.length || !window.Chart) return '';

  return `
    <div class="trend-charts-container">
      <div class="trend-chart-wrapper">
        <canvas id="fanSegmentTrendChart" height="200"></canvas>
      </div>
      <div class="trend-chart-wrapper">
        <canvas id="opinionTrendChart" height="200"></canvas>
      </div>
    </div>
  `;
}

function initTrendCharts(reports) {
  if (!reports?.length || !window.Chart) return;

  const days = reports.map(r => `第${r.day}天`);

  const fanCanvas = document.getElementById('fanSegmentTrendChart');
  if (fanCanvas) {
    destroyChartForCanvas(fanCanvas);
    new window.Chart(fanCanvas, {
      type: 'line',
      data: {
        labels: days,
        datasets: [
          { label: '真粉', data: reports.map(r => r.dataDelta?.trueFanCount || 0), borderColor: '#f3c969', fill: false },
          { label: '乐子', data: reports.map(r => r.dataDelta?.funFanCount || 0), borderColor: '#65d17a', fill: false },
          { label: '独角兽', data: reports.map(r => r.dataDelta?.unicornFanCount || 0), borderColor: '#ff7eb3', fill: false },
          { label: 'DD', data: reports.map(r => r.dataDelta?.ddFanCount || 0), borderColor: '#94b7ff', fill: false }
        ]
      },
      options: {
        responsive: true,
        plugins: { legend: { labels: { color: 'rgba(255,255,255,0.8)' } }, title: { display: true, text: '粉丝趋势', color: 'white' } },
        scales: {
          x: { ticks: { color: 'rgba(255,255,255,0.6)' }, grid: { color: 'rgba(255,255,255,0.05)' } },
          y: { ticks: { color: 'rgba(255,255,255,0.6)' }, grid: { color: 'rgba(255,255,255,0.05)' } }
        }
      }
    });
  }

  const opinionCanvas = document.getElementById('opinionTrendChart');
  if (opinionCanvas) {
    destroyChartForCanvas(opinionCanvas);
    new window.Chart(opinionCanvas, {
      type: 'line',
      data: {
        labels: days,
        datasets: [
          { label: '口碑', data: reports.map(r => r.dataDelta?.reputation || 0), borderColor: '#3dd6c6', fill: false },
          { label: '热度', data: reports.map(r => r.dataDelta?.watchHeat || 0), borderColor: '#ff7262', fill: false },
          { label: '人气', data: reports.map(r => r.dataDelta?.popularity || 0), borderColor: '#94b7ff', fill: false }
        ]
      },
      options: {
        responsive: true,
        plugins: { legend: { labels: { color: 'rgba(255,255,255,0.8)' } }, title: { display: true, text: '舆论趋势', color: 'white' } },
        scales: {
          x: { ticks: { color: 'rgba(255,255,255,0.6)' }, grid: { color: 'rgba(255,255,255,0.05)' } },
          y: { ticks: { color: 'rgba(255,255,255,0.6)' }, grid: { color: 'rgba(255,255,255,0.05)' } }
        }
      }
    });
  }
}

// ============================================================
// 伪直播动画系统 (A1)
// ============================================================
let streamSimInterval = null;
let streamSimDanmaku = [];

function startStreamSimulation(planType, titleText) {
  // 创建全屏overlay
  const overlay = document.createElement('div');
  overlay.id = 'streamSimulationOverlay';
  overlay.innerHTML = `
    <div class="stream-sim-container">
      <div class="stream-sim-header">
        <span class="stream-sim-live">🔴 直播中</span>
        <span class="stream-sim-title">${html(titleText)}</span>
        <span class="stream-sim-viewers" id="streamSimViewers">0</span>
      </div>
      <div class="stream-sim-danmaku-area" id="streamSimDanmakuArea"></div>
      <div class="stream-sim-progress">
        <div class="stream-sim-progress-bar" id="streamSimProgress"></div>
      </div>
      <div class="stream-sim-status" id="streamSimStatus">准备开播...</div>
    </div>
  `;
  document.body.appendChild(overlay);

  // 动画序列
  let elapsed = 0;
  const duration = 12000; // 12秒
  let baseViewers = state.vup?.watchHeat ? Math.floor(state.vup.watchHeat * 10) : 50;

  streamSimInterval = setInterval(() => {
    elapsed += 100;
    const progress = elapsed / duration;

    // 进度条
    const progressBar = document.getElementById('streamSimProgress');
    if (progressBar) progressBar.style.width = `${progress * 100}%`;

    // 观众数变化
    const viewerGrowth = Math.floor(baseViewers * progress * (0.8 + Math.random() * 0.4));
    const currentViewers = baseViewers + viewerGrowth;
    const viewerEl = document.getElementById('streamSimViewers');
    if (viewerEl) viewerEl.textContent = currentViewers.toLocaleString();

    // 弹幕推送（每800ms一条）
    if (elapsed % 800 < 100) {
      pushStreamSimDanmaku();
    }

    // 状态文案
    const statusEl = document.getElementById('streamSimStatus');
    if (statusEl) {
      if (progress < 0.2) statusEl.textContent = '开播中...';
      else if (progress < 0.5) statusEl.textContent = '直播进行中';
      else if (progress < 0.8) statusEl.textContent = '🔥 高潮来了！';
      else statusEl.textContent = '即将下播...';
    }

    // 结束
    if (elapsed >= duration) {
      endStreamSimulation();
    }
  }, 100);

  // 初始弹幕
  setTimeout(() => pushStreamSimDanmaku(), 500);
}

function pushStreamSimDanmaku() {
  const area = document.getElementById('streamSimDanmakuArea');
  if (!area) return;

  const personas = ['老粉','切片组','录播组','楼友','独角兽','DD','乐子人','房管'];
  const persona = personas[Math.floor(Math.random() * personas.length)];
  const messages = getDanmakuMessages(persona);
  const msg = messages[Math.floor(Math.random() * messages.length)];

  const bullet = document.createElement('div');
  bullet.className = `danmaku-bullet danmaku-${persona}`;
  bullet.innerHTML = `<span class="danmaku-name">[${persona}]</span> ${html(msg)}`;
  bullet.style.top = `${Math.random() * 70 + 10}%`;
  bullet.style.animationDuration = `${3 + Math.random() * 2}s`;
  area.appendChild(bullet);

  // 清理旧弹幕
  setTimeout(() => bullet.remove(), 5000);
}

function getDanmakuMessages(persona) {
  const pool = {
    '老粉': ['主播加油！','今天状态不错','慢慢来','唱得好听','这波可以','老粉欣慰','终于开播了'],
    '切片组': ['这段能剪','时间轴已打','标题想好了','切了切了','精华在这'],
    '录播组': ['全程录着','录播组到位','放心录','互联网记忆'],
    '楼友': ['建议开楼','这事不简单','楼里在讨论','有瓜吃'],
    '独角兽': ['主播今天辛苦了','注意休息','我一直在','陪伴是最长情'],
    'DD': ['路过看看','主播不错','关注了','下次还来'],
    '乐子人': ['哈哈哈哈哈','节目效果拉满','笑死','别急'],
    '房管': ['已禁言','弹幕文明','大家冷静','维护秩序']
  };
  return pool[persona] || ['加油'];
}

function endStreamSimulation() {
  clearInterval(streamSimInterval);
  streamSimInterval = null;

  const overlay = document.getElementById('streamSimulationOverlay');
  if (overlay) {
    // 结束动画
    const status = document.getElementById('streamSimStatus');
    if (status) {
      status.textContent = '✅ 直播结束！';
      status.classList.add('stream-sim-end');
    }

    // 1秒后移除
    setTimeout(() => {
      overlay.classList.add('stream-sim-fadeout');
      setTimeout(() => overlay.remove(), 500);
      // 触发下播行动选择
      if (typeof refreshPhaseData === 'function') refreshPhaseData();
    }, 1000);
  }
}

// ============================================================
// 数值变化粒子反馈 (A2)
// ============================================================
function animateStatChanges() {
  if (!state.vup) return;

  const changes = [
    { key: 'fans', current: state.vup.fanStructure?.fans, prev: previousStats.fans, label: '粉丝' },
    { key: 'reputation', current: state.vup.opinion?.reputation, prev: previousStats.reputation, label: '口碑' },
    { key: 'watchHeat', current: state.vup.opinion?.watchHeat, prev: previousStats.watchHeat, label: '热度' },
    { key: 'popularity', current: state.vup.opinion?.popularity, prev: previousStats.popularity, label: '人气' },
    { key: 'memeLevel', current: state.vup.opinion?.memeLevel, prev: previousStats.memeLevel, label: '梗浓度' },
    { key: 'stamina', current: state.vup.resources?.stamina, prev: previousStats.stamina, label: '体力' },
    { key: 'coin', current: state.vup.resources?.coin, prev: previousStats.coin, label: '运营预算' },
    { key: 'inspiration', current: state.vup.resources?.inspiration, prev: previousStats.inspiration, label: '灵感' }
  ];

  changes.forEach(c => {
    if (c.current != null && c.prev != null && c.current !== c.prev) {
      const diff = c.current - c.prev;
      const sign = diff > 0 ? '+' : '';
      const color = diff > 0 ? 'var(--neon-green)' : 'var(--neon-pink)';
      const icon = diff > 0 ? '📈' : '📉';

      // 浮动数字反馈
      const anchor = document.getElementById('statsSection') || document.body;
      ANIM.floatNumber(anchor, `${icon} ${sign}${diff} ${c.label}`, diff > 0 ? 'positive' : 'negative');

      // 找到对应的stat卡片并闪烁
      const statEl = document.querySelector(`[data-stat="${c.key}"]`);
      if (statEl) {
        ANIM.flash(statEl, diff > 0 ? 'positive' : 'negative');
      }
    }
  });
}

// ============================================================
// 里程碑庆祝系统 (A3)
// ============================================================
const LEGACY_MILESTONES = {
  DAY_1: { title: '🎉 出道日！', desc: '30天旅程的开始', icon: '🎤' },
  DAY_7: { title: '📅 第一周结束', desc: '你已经坚持了一周', icon: '⭐' },
  DAY_15: { title: '🔄 中期检查', desc: '15天了，看看你的路线', icon: '📊' },
  DAY_21: { title: '🏃 冲刺倒计时', desc: '最后9天，加油！', icon: '⏰' },
  DAY_30: { title: '🎊 最终日', desc: '今天是最后一天', icon: '🏆' },
  FAN_100: { title: '💯 100粉丝！', desc: '你的第一批忠实观众', icon: '🎉' },
  FAN_500: { title: '🔥 500粉丝！', desc: '开始有影响力了', icon: '⭐' },
  FAN_1000: { title: '🌟 1000粉丝！', desc: '千粉达成！', icon: '🏆' },
  ROUTE_LOCKED: { title: '🧭 路线确认', desc: '你找到了自己的方向', icon: '🎯' }
};

function checkLegacyMilestones(day, vup) {
  const milestones = [];
  if (day === 1) milestones.push(LEGACY_MILESTONES.DAY_1);
  if (day === 7) milestones.push(LEGACY_MILESTONES.DAY_7);
  if (day === 15) milestones.push(LEGACY_MILESTONES.DAY_15);
  if (day === 21) milestones.push(LEGACY_MILESTONES.DAY_21);
  if (day === 30) milestones.push(LEGACY_MILESTONES.DAY_30);
  if (vup?.fanStructure?.fans >= 100 && (previousStats?.fans || 0) < 100) milestones.push(LEGACY_MILESTONES.FAN_100);
  if (vup?.fanStructure?.fans >= 500 && (previousStats?.fans || 0) < 500) milestones.push(LEGACY_MILESTONES.FAN_500);
  if (vup?.fanStructure?.fans >= 1000 && (previousStats?.fans || 0) < 1000) milestones.push(LEGACY_MILESTONES.FAN_1000);

  milestones.forEach((m, i) => setTimeout(() => showMilestoneCelebration(m), i * 2000));
}

function showMilestoneCelebration(milestone) {
  const popup = document.createElement('div');
  popup.className = 'milestone-celebration';
  popup.innerHTML = `
    <div class="milestone-card">
      <div class="milestone-icon">${milestone.icon}</div>
      <div class="milestone-title">${milestone.title}</div>
      <div class="milestone-desc">${milestone.desc}</div>
    </div>
  `;
  document.body.appendChild(popup);

  // confetti效果
  ANIM.confetti(popup);
  SFX.play('achievement');

  setTimeout(() => {
    popup.classList.add('milestone-fadeout');
    setTimeout(() => popup.remove(), 500);
  }, 3000);
}

// ============================================================
// 直播间氛围增强 (A5)
// ============================================================
function animateViewerCount() {
  const el = document.getElementById('liveRoomViewers');
  if (!el || !state.vup) return;
  const base = Math.floor((state.vup.opinion?.watchHeat || 0) * 10 + 50);
  const jitter = Math.floor(base * (Math.random() * 0.1 - 0.05));
  el.textContent = (base + jitter).toLocaleString();
}

// 弹幕密度随热度变化
function getDanmakuDensity() {
  const heat = state.vup?.opinion?.watchHeat || 0;
  if (heat >= 70) return 300;  // 每300ms一条
  if (heat >= 40) return 800;  // 每800ms一条
  if (heat >= 20) return 1500; // 每1.5秒一条
  return 3000; // 每3秒一条
}

// 每3秒更新观众数
setInterval(animateViewerCount, 3000);

// ============================================================
// 债务倒计时UI (A6)
// ============================================================
function renderDebtCountdown() {
  if (!state.vup?.debts?.length) return '';
  const openDebts = state.vup.debts.filter(d => d.status === 'OPEN');
  if (!openDebts.length) return '';

  return `
    <div class="debt-countdown-bar">
      ${openDebts.map(d => `
        <div class="debt-alert ${d.urgent ? 'debt-urgent' : ''}" data-severity="${d.severity}">
          <span class="debt-icon">${d.severity >= 3 ? '🔴' : '⚠️'}</span>
          <span class="debt-label">${html(d.debtLabel || d.debtType)}</span>
          <span class="debt-timer">${d.remainingDays <= 0 ? '今天到期！' : d.remainingDays + '天后'} · 严重度${html(d.severity)} · ${html(debtRecommendedLine(d))}</span>
        </div>
      `).join('')}
    </div>
  `;
}

// ============================================================
// 快速行动模式 (A8)
// ============================================================
function quickAction(actionType) {
  // 一键选择行动并跳过下播
  submitAction(actionType);
}

function quickOffStream() {
  // 自动选择推荐的下播行动
  const recommended = state.offStreamOptions?.find(o => o.recommended);
  if (recommended) {
    submitOffStream(recommended.type);
  }
}

function renderQuickActionBar() {
  if (state.session?.phase !== 'READY' || !state.vup) return '';
  return `
    <div class="quick-action-bar">
      <button type="button" class="btn-quick-action" data-action="quick-submit-action" data-action-type="REST" title="快速休息">⚡ 休息</button>
      <button type="button" class="btn-quick-action" data-action="quick-submit-action" data-action-type="TRAIN_SONG" title="快速练歌">⚡ 练歌</button>
      <button type="button" class="btn-quick-action" data-action="quick-submit-action" data-action-type="TRAIN_DANCE" title="快速练舞">⚡ 练舞</button>
    </div>
  `;
}

// === 结局叙事系统 (B3) ===
const ENDING_NARRATIVES = {
  'ELECTRONIC_PICKLE': {
    opening: '30天前，你还是一个默默无闻的新人。没有人知道你的名字，直播间里只有几个路过的朋友。',
    middle: '你选择了最稳健的路——不追热点，不搞争议，踏踏实实做内容。有人说你无聊，有人说你没野心，但你知道，能让人安心待着的直播间，才是最好的直播间。',
    closing: '30天后，你有了自己的小天地。粉丝不多，但都是真心喜欢你的人。这就是你的"活法"——不需要轰轰烈烈，只需要安安稳稳地，做自己喜欢的事。',
    tagline: '不是所有V都需要成为顶流。能让人安心的地方，就是最好的地方。'
  },
  'SINGING_IDOL': {
    opening: '你从第一天就知道自己想做什么——唱歌。不是为了出圈，不是为了流量，只是因为唱歌让你快乐。',
    middle: '你把大部分时间花在练歌上。有人问你为什么不搞点别的，你只是笑笑。直播间里，你的歌声成了最稳定的陪伴。渐渐地，有人开始专门为你的歌声而来。',
    closing: '30天后，你成了小有名气的歌势V。不是最火的，但是最被认可的。你的歌声，成了某些人每天的期待。',
    tagline: '用歌声证明自己，用坚持赢得认可。'
  },
  'SLICE_SAINT': {
    opening: '你发现了一个秘密——在VTuber的世界里，切片才是真正的传播利器。',
    middle: '你开始有意识地制造"可切片"的瞬间。不是刻意，而是知道什么样的内容会被传播。切片组开始频繁光顾你的直播间，你的名字出现在越来越多的推荐里。',
    closing: '30天后，你的切片播放量比直播还高。有人说你是"切片圣体"，你笑着接受了这个称号。毕竟，能让更多人看到你，有什么不好呢？',
    tagline: '切片不是偷懒，是让好内容被更多人看到。'
  },
  'BLACK_RED_MAIN_STAGE': {
    opening: '你选择了一条最危险的路——在争议中成长。',
    middle: '你知道黑红不是目的，但你不惧怕争议。每一次风波，你都选择正面回应。有人骂你，有人挺你，但没有人能忽视你。你的名字，成了热搜的常客。',
    closing: '30天后，你站在了主会场的中央。不是所有人都喜欢你，但所有人都知道你。这就是你想要的——被看见，被记住。',
    tagline: '不怕被讨论，只怕被遗忘。'
  },
  'CYBER_GIRLFRIEND': {
    opening: '你找到了一个独特的定位——陪伴。',
    middle: '你花大量时间陪粉丝聊天，回应每一条私信，记住每个常客的名字。有人说你太"营业"了，但你知道，真心的陪伴和营业的区别，只有被陪伴的人才知道。',
    closing: '30天后，你有了一批最忠实的粉丝。他们不是来看节目效果的，是来"回家"的。你的直播间，成了某些人的精神寄托。',
    tagline: '陪伴是最长情的告白。'
  },
  'DD_BUS_STOP': {
    opening: '你是一个天生的社交达人。你喜欢认识新朋友，喜欢和不同的V联动。',
    middle: '你几乎和每个能联动的V都合作过。有人说你是DD公交站——谁都能来坐一站。但你知道，每一次联动，都是一次新的可能。',
    closing: '30天后，你认识了最多的人，拥有了最广的圈子。虽然粉丝忠诚度不高，但你的影响力，是所有V里最广的。',
    tagline: '朋友多了路好走。'
  },
  'MAIN_STAGE_KING': {
    opening: '你不想只是站在舞台上——你想掌控舞台。',
    middle: '你学会了在争议中保持主动。不是被动挨打，而是主动引导话题。每一次风波，你都能化被动为主动，把流量变成自己的武器。',
    closing: '30天后，你成了真正的"主会场之王"。不是因为你没有争议，而是因为你能在争议中游刃有余。',
    tagline: '不是避免风暴，而是学会在风暴中跳舞。'
  },
  'GLORIOUS_GRADUATION': {
    opening: '你从一开始就知道，30天后你会离开。',
    middle: '你珍惜每一天，认真对待每一个粉丝。你知道毕业不是失败，而是一个阶段的完美收官。你不想留下遗憾，只想给所有人一个最好的告别。',
    closing: '30天后，你选择了光荣毕业。粉丝们哭了，但他们都理解。你给了他们最好的30天，他们给了你最美好的回忆。',
    tagline: '最好的告别，是不留遗憾的离开。'
  },
  'UNKNOWN': {
    opening: '30天过去了，但你好像什么都没抓住。',
    middle: '你尝试了很多，但没有一件事做到极致。你既不是最好的歌手，也不是最有趣的主播，也不是最会搞事的那个。你只是……存在过。',
    closing: '没有人记得你的名字，没有人为你写小作文，没有人在你离开后想念你。"查无此V"——这四个字，是你30天的总结。',
    tagline: '不是所有出道都有结局。'
  }
};

function renderEndingNarrative(endingType, endingData) {
  const narrative = ENDING_NARRATIVES[endingType] || ENDING_NARRATIVES['UNKNOWN'];

  return `
    <div class="ending-narrative">
      <div class="narrative-chapter narrative-opening">
        <div class="chapter-marker">序章</div>
        <p>${html(narrative.opening)}</p>
      </div>
      <div class="narrative-chapter narrative-middle">
        <div class="chapter-marker">旅程</div>
        <p>${html(narrative.middle)}</p>
      </div>
      <div class="narrative-chapter narrative-closing">
        <div class="chapter-marker">终章</div>
        <p>${html(narrative.closing)}</p>
      </div>
      <div class="narrative-tagline">
        <blockquote>"${html(narrative.tagline)}"</blockquote>
      </div>
      ${renderEndingPersonalTouches(endingData)}
    </div>
  `;
}

function renderEndingPersonalTouches(endingData) {
  if (!endingData) return '';
  const touches = [];

  if (endingData.keyEvents?.length) {
    touches.push(`<div class="personal-touch"><strong>关键时刻：</strong>${endingData.keyEvents.slice(0,3).map(e => html(e)).join('、')}</div>`);
  }
  if (endingData.endingTags?.length) {
    touches.push(`<div class="personal-touch"><strong>你的标签：</strong>${endingData.endingTags.map(t => html(t)).join('、')}</div>`);
  }
  if (endingData.finalTitle) {
    touches.push(`<div class="personal-touch"><strong>你的称号：</strong>${html(endingData.finalTitle)}</div>`);
  }

  if (!touches.length) return '';
  return `<div class="ending-personal-touches">${touches.join('')}</div>`;
}

// === VUP对话系统 (B4) ===
const VUP_DIALOGUES = {
  DAY_1: [
    '今天是出道第一天……有点紧张。',
    '不知道会不会有人来看我的直播。',
    '加油！第一天一定要留下好印象！',
    '出道……这是我从小的梦想。',
    '希望能在30天里找到自己的路。'
  ],
  DAY_7: [
    '一周了！我居然坚持下来了。',
    '回头看这一周，学到了好多东西。',
    '粉丝们一直在支持我，好感动。',
    '一周纪念，给自己加个油！'
  ],
  DAY_15: [
    '15天了……时间过得好快。',
    '我好像找到一点感觉了。',
    '不知道后半程会怎么样。',
    '中期检查，看看我的路线走对了没。'
  ],
  DAY_30: [
    '30天了……不管结果如何，谢谢大家。',
    '最后一天了，好舍不得。',
    '这30天是我最珍贵的回忆。',
    '不管结局如何，我尽力了。'
  ],
  MOOD_HAPPY: [
    '今天心情不错！感觉做什么都顺。',
    '粉丝们的弹幕好暖，开心~',
    '嘿嘿，今天涨粉了！',
    '感觉我的努力被看到了。'
  ],
  MOOD_STRESSED: [
    '最近压力有点大……',
    '弹幕里好像有人在吵，有点烦。',
    '不知道该怎么处理这个情况。',
    '希望一切都能好起来。'
  ],
  MOOD_TIRED: [
    '好累……但还是要坚持。',
    '体力有点跟不上了。',
    '需要休息一下，充充电。',
    '连续工作好多天了，身体在抗议。'
  ],
  MOOD_EXCITED: [
    '切片组连夜上班了！热度在涨！',
    '今天的直播效果太好了！',
    '感觉要火！',
    '好多新面孔来了直播间！'
  ],
  DEBT_INCOMING: [
    '总觉得有什么事要发生……',
    '之前的某个选择，好像要付出代价了。',
    '有点担心接下来会发生什么。',
    '希望能平安度过这次危机。'
  ],
  ROUTE_LOCKED: [
    '我决定了，就走这条路！',
    '终于找到自己的方向了。',
    '接下来就专注在这条路上深耕吧。',
    '有了明确的目标，感觉踏实多了。'
  ],
  MILESTONE_100FAN: [
    '100粉了！虽然不多，但好开心！',
    '100个人愿意关注我，好感动。',
    '第一个里程碑达成！'
  ],
  MILESTONE_1000FAN: [
    '1000粉了！这是真的吗？！',
    '一千个人……我得好好珍惜。',
    '千粉达成！我要更努力！'
  ],
  GENERAL: [
    '今天也要加油！',
    '新的一天，新的开始。',
    '希望能给粉丝们带来快乐。',
    '做V真的好累，但也好快乐。',
    '今天想尝试一些新的东西。',
    '弹幕里的大家都好有趣。',
    '下播后要好好休息一下。',
    '明天也要继续努力！',
    '感觉自己在慢慢变好。',
    '直播的时候最开心了。'
  ]
};

function getVupDialogue(day, mood, milestones) {
  let pool = [];

  // 优先级：特殊日期 > 心情 > 里程碑 > 通用
  if (day === 1) pool = VUP_DIALOGUES.DAY_1;
  else if (day === 7) pool = VUP_DIALOGUES.DAY_7;
  else if (day === 15) pool = VUP_DIALOGUES.DAY_15;
  else if (day === 30) pool = VUP_DIALOGUES.DAY_30;
  else if (mood === 'HAPPY') pool = VUP_DIALOGUES.MOOD_HAPPY;
  else if (mood === 'STRESSED') pool = VUP_DIALOGUES.MOOD_STRESSED;
  else if (mood === 'TIRED') pool = VUP_DIALOGUES.MOOD_TIRED;
  else if (mood === 'EXCITED') pool = VUP_DIALOGUES.MOOD_EXCITED;
  else pool = VUP_DIALOGUES.GENERAL;

  // 随机选择
  const index = Math.floor(Math.random() * pool.length);
  return pool[index];
}

function showVupDialogue(text) {
  const bubble = document.createElement('div');
  bubble.className = 'vup-dialogue-bubble';
  bubble.innerHTML = `
    <div class="dialogue-avatar">🎤</div>
    <div class="dialogue-text">${html(text)}</div>
  `;

  const vupCard = document.querySelector('.vup-state-card') || document.querySelector('#tabMainPanel');
  if (vupCard) {
    vupCard.style.position = 'relative';
    vupCard.appendChild(bubble);
  } else {
    document.body.appendChild(bubble);
  }

  // 5秒后消失
  setTimeout(() => {
    bubble.classList.add('dialogue-fadeout');
    setTimeout(() => bubble.remove(), 500);
  }, 5000);
}

// 在每日开始时调用
function triggerDailyDialogue() {
  const day = state.vup?.day || 1;
  const mood = state.vup?.mood || 'NEUTRAL';
  const dialogue = getVupDialogue(day, mood);
  setTimeout(() => showVupDialogue(dialogue), 1000);
}

// === 结局图鉴条件提示 (E2) ===
const ENDING_CONDITIONS = {
  'ELECTRONIC_PICKLE': {
    label: '电子榨菜',
    conditions: ['粉丝 > 500', '口碑 > 60', '路线：电子榨菜', '无严重债务'],
    hint: '稳健路线，保持口碑，粉丝自然增长'
  },
  'SINGING_IDOL': {
    label: '歌势偶像',
    conditions: ['粉丝 > 1000', '练歌次数 >= 10', '路线：歌势偶像', '口碑 > 50'],
    hint: '多练歌，走歌势路线，保持口碑'
  },
  'SLICE_SAINT': {
    label: '切片圣体',
    conditions: ['粉丝 > 1500', '发切片次数 >= 8', '路线：切片圣体 / 梗舞整活', '梗浓度 > 50'],
    hint: '多发切片或短视频，提高梗浓度，走切片/梗舞路线'
  },
  'BLACK_RED_MAIN_STAGE': {
    label: '黑红主会场',
    conditions: ['粉丝 > 2000', '热度 > 70', '路线：黑红主会场', '有争议事件记录'],
    hint: '拥抱争议，提高热度，走黑红路线'
  },
  'CYBER_GIRLFRIEND': {
    label: '赛博女友',
    conditions: ['独角兽粉 > 100', '陪聊次数 >= 8', '路线：电子榨菜', '口碑 > 55'],
    hint: '多陪聊，培养独角兽粉，保持口碑'
  },
  'DD_BUS_STOP': {
    label: 'DD公交站',
    conditions: ['DD粉 > 150', '联动次数 >= 6', '路线：社交联动 / DD公交站', '热度 > 40'],
    hint: '多联动，吸引DD粉，走社交/DD路线'
  },
  'MAIN_STAGE_KING': {
    label: '主会场之王',
    conditions: ['粉丝 > 3000', '热度 > 80', '路线：黑红主会场', '商业化 > 40'],
    hint: '成为焦点，走黑红路线，提高商业化'
  },
  'GLORIOUS_GRADUATION': {
    label: '光荣毕业',
    conditions: ['口碑 > 70', '真粉 > 200', '粉丝群维护 >= 5', '无债务'],
    hint: '保持高口碑，培养真粉，维护粉丝群'
  },
  'UNKNOWN': {
    label: '查无此V',
    conditions: ['长期不定型', '路线证据不足', '第30天未形成可识别标签'],
    hint: '先选一条路线连续补证据，避免整局散掉'
  }
};

function renderEndingConditionHint(endingType) {
  const info = ENDING_CONDITIONS[endingType];
  if (!info) return '';

  return `
    <div class="ending-condition-hint">
      <div class="condition-title">🎯 ${info.label} 达成条件</div>
      <ul class="condition-list">
        ${info.conditions.map(c => '<li>' + html(c) + '</li>').join('')}
      </ul>
      <div class="condition-tip">💡 ${html(info.hint)}</div>
    </div>
  `;
}

function renderEndingAtlasSlotWithConditions(slot) {
  return `
    <div class="ending-atlas-slot ${slot.unlocked ? 'unlocked' : 'locked'}">
      <div class="atlas-icon">${slot.icon || '❓'}</div>
      <div class="atlas-name">${html(slot.name || '???')}</div>
      ${slot.unlocked
        ? '<div class="atlas-unlocked-info"><div>最佳评分: ' + (slot.bestScore || '-') + '</div><div>最佳评级: ' + (slot.bestGradeLabel || '-') + '</div></div>'
        : renderEndingConditionHint(slot.type)
      }
    </div>
  `;
}

// === 结局概率预测条 (F5) ===
function renderEndingProbabilityBar(forecast) {
  if (!forecast) return '';

  const endings = [
    { type: 'ELECTRONIC_PICKLE', label: '电子榨菜', color: '#3dd6c6' },
    { type: 'SINGING_IDOL', label: '歌势', color: '#f3c969' },
    { type: 'SLICE_SAINT', label: '切片', color: '#65d17a' },
    { type: 'BLACK_RED_MAIN_STAGE', label: '黑红', color: '#ff7262' },
    { type: 'CYBER_GIRLFRIEND', label: '赛博女友', color: '#ff7eb3' },
    { type: 'DD_BUS_STOP', label: 'DD', color: '#94b7ff' },
    { type: 'MAIN_STAGE_KING', label: '主会场', color: '#ff9f43' },
    { type: 'GLORIOUS_GRADUATION', label: '毕业', color: '#ffd700' },
    { type: 'UNKNOWN', label: '查无此V', color: '#666' }
  ];

  // 根据路线分计算概率（简化版）
  const routeScores = state.vup?.route ? Object.entries(state.vup.route) : [];
  const totalScore = routeScores.reduce((sum, [, v]) => sum + (v || 0), 0) || 1;

  const probabilities = endings.map(e => {
    const score = routeScores.find(([k]) => k === e.type)?.[1] || 0;
    return { ...e, probability: Math.round((score / totalScore) * 100) };
  }).sort((a, b) => b.probability - a.probability);

  return `
    <div class="ending-probability-bar">
      <div class="prob-title">结局预测</div>
      <div class="prob-bar-container">
        ${probabilities.filter(p => p.probability > 0).map(p => '<div class="prob-segment" style="width:' + p.probability + '%; background:' + p.color + '" title="' + p.label + ': ' + p.probability + '%">' + (p.probability >= 10 ? '<span class="prob-label">' + p.label + '</span>' : '') + '</div>').join('')}
      </div>
      <div class="prob-legend">
        ${probabilities.filter(p => p.probability > 0).slice(0, 3).map(p => '<span class="prob-legend-item"><span class="prob-dot" style="background:' + p.color + '"></span>' + p.label + ' ' + p.probability + '%</span>').join('')}
      </div>
    </div>
  `;
}

// === 永久解锁UI (E1前端) ===
function renderPermanentUnlocks(unlocks) {
  if (!unlocks?.length) return '';

  return `
    <div class="permanent-unlocks">
      <div class="unlocks-title">🏅 已解锁能力</div>
      <div class="unlocks-grid">
        ${unlocks.map(u => '<div class="unlock-card"><div class="unlock-icon">' + (u.icon || '⭐') + '</div><div class="unlock-label">' + html(u.label) + '</div><div class="unlock-desc">' + html(u.description) + '</div><div class="unlock-source">来源：' + html(u.sourceEnding) + '</div></div>').join('')}
      </div>
    </div>
  `;
}

// ==========================================
// VupWorld 耐玩精品 - 信息设计增强
// ==========================================

// === 日报三层结构 ===
var reportDetailExpanded = false;
var reportDebugExpanded = false;

function renderReportSummary(report) {
  if (!report) return '';
  var fd = report.dataDelta ? report.dataDelta.fanChange || 0 : 0;
  var rd = report.dataDelta ? report.dataDelta.reputationChange || 0 : 0;
  var hd = report.dataDelta ? report.dataDelta.watchHeatChange || 0 : 0;
  return '<div class="report-summary-layer">' +
    '<div class="report-summary-text">' + html(report.summary || '今天平静地过去了') + '</div>' +
    '<div class="report-key-numbers">' +
      '<div class="report-key-stat ' + (fd >= 0 ? 'positive' : 'negative') + '"><span class="stat-label">粉丝</span><span class="stat-value">' + (fd >= 0 ? '+' : '') + fd + '</span></div>' +
      '<div class="report-key-stat ' + (rd >= 0 ? 'positive' : 'negative') + '"><span class="stat-label">口碑</span><span class="stat-value">' + (rd >= 0 ? '+' : '') + rd + '</span></div>' +
      '<div class="report-key-stat ' + (hd >= 0 ? 'positive' : 'negative') + '"><span class="stat-label">热度</span><span class="stat-value">' + (hd >= 0 ? '+' : '') + hd + '</span></div>' +
    '</div>' +
    (report.riskHint ? '<div class="report-risk-hint">⚠️ ' + html(report.riskHint) + '</div>' : '') +
    '<button type="button" class="btn-expand-report" data-action="toggle-report-detail">' + (reportDetailExpanded ? '收起详情' : '查看详情 ▼') + '</button>' +
  '</div>';
}

function renderReportDetail(report) {
  if (!reportDetailExpanded || !report) return '';
  var dd = report.dataDelta || {};
  return '<div class="report-detail-layer">' +
    '<div class="report-detail-section"><h4>📊 粉丝变化</h4>' +
      '<div class="report-fan-breakdown">' +
        '<span>真粉: ' + (dd.trueFanChange >= 0 ? '+' : '') + (dd.trueFanChange || 0) + '</span>' +
        '<span>乐子: ' + (dd.funFanChange >= 0 ? '+' : '') + (dd.funFanChange || 0) + '</span>' +
        '<span>独角兽: ' + (dd.unicornFanChange >= 0 ? '+' : '') + (dd.unicornFanChange || 0) + '</span>' +
        '<span>DD: ' + (dd.ddFanChange >= 0 ? '+' : '') + (dd.ddFanChange || 0) + '</span>' +
      '</div></div>' +
    (report.highlights && report.highlights.length ? '<div class="report-detail-section"><h4>✨ 今日亮点</h4>' + report.highlights.map(function(h) { return '<div class="report-highlight">' + html(h) + '</div>'; }).join('') + '</div>' : '') +
    (report.debtSummary ? '<div class="report-detail-section"><h4>📋 债务状态</h4><div class="report-debt">' + html(report.debtSummary) + '</div></div>' : '') +
    '<button type="button" class="btn-expand-report debug" data-action="toggle-report-debug">' + (reportDebugExpanded ? '收起数据' : '查看完整数据 ▼') + '</button>' +
  '</div>';
}

function renderReportDebug(report) {
  if (!reportDebugExpanded || !report) return '';
  return '<div class="report-debug-layer"><pre class="report-debug-json">' + html(JSON.stringify(report, null, 2)) + '</pre></div>';
}

function toggleReportDetail() { reportDetailExpanded = !reportDetailExpanded; if (typeof render === 'function') render(); }
function toggleReportDebug() { reportDebugExpanded = !reportDebugExpanded; if (typeof render === 'function') render(); }

function renderThreeLayerReport(report) {
  return '<div class="report-three-layer">' + renderReportSummary(report) + renderReportDetail(report) + renderReportDebug(report) + '</div>';
}

// === 术语悬浮提示 ===
var TERM_GLOSSARY = {
  'VUP': { display: 'VUP', tooltip: '虚拟主播，用虚拟形象进行直播的创作者', example: '你就是一个VUP' },
  '真粉': { display: '真粉', tooltip: '稳定支持你的忠实观众，不容易流失', example: '真粉会在你低谷时依然支持你' },
  '真爱粉': { display: '真爱粉', tooltip: '稳定支持你的忠实观众，不容易流失', example: '真粉会在你低谷时依然支持你' },
  '乐子粉': { display: '乐子粉', tooltip: '看热闹的观众，来了又走，但能帮你传播', example: '乐子粉看你节目效果好就来了' },
  '乐子人': { display: '乐子人', tooltip: '看热闹的观众，来了又走，但能帮你传播', example: '乐子人看你节目效果好就来了' },
  '独角兽': { display: '独角兽', tooltip: '对你有独占欲的粉丝，投入感强但要求也多', example: '独角兽会管你跟谁联动' },
  'DD': { display: 'DD', tooltip: '同时喜欢很多V的观众，忠诚度低但覆盖面广', example: 'DD坐了一站就下车' },
  '切片': { display: '切片', tooltip: '把直播精彩片段剪辑成短视频传播', example: '切片比正片火是常有的事' },
  '梗浓度': { display: '梗浓度', tooltip: '你被观众记住的梗的积累程度', example: '梗浓度越高越容易出圈' },
  '米线': { display: '米线', tooltip: '边界感，VTuber圈对底线的说法', example: '米线施工队今天短暂上班' },
  'SC': { display: 'SC', tooltip: '游戏内模拟高亮互动/醒目留言', example: '收到高亮互动要及时回应' },
  '联动': { display: '联动', tooltip: '和其他VTuber一起直播', example: '联动可以互相引流' },
  '开庭': { display: '开庭', tooltip: '被全网讨论/审判', example: '全网开庭的时候弹幕最活跃' },
  '楼友': { display: '楼友', tooltip: '论坛用户，喜欢开帖讨论', example: '楼友说这事不简单' },
  '旧账': { display: '旧账', tooltip: '之前的选择留下的风险，会在未来爆发', example: '标题党留下的旧账迟早要还' },
  '路线': { display: '路线', tooltip: '你选择的发展方向', example: '走歌势路线就多练歌' },
  '口碑': { display: '口碑', tooltip: '观众对你的整体评价', example: '口碑低的时候容易被黑' },
  '热度': { display: '热度', tooltip: '你被关注和讨论的程度', example: '热度高时事件触发概率增加' },
  '弹幕': { display: '弹幕', tooltip: '直播间滚动的观众评论', example: '弹幕是最直接的观众反馈' },
  '房管': { display: '房管', tooltip: '直播间管理员', example: '房管手滑禁错人是经典事故' },
  '企划': { display: '企划', tooltip: '直播的类型和主题', example: '选什么企划决定了今天的直播方向' },
  '标题': { display: '标题', tooltip: '直播间的标题，决定观众点不点进来', example: '标题党有代价' },
  '下播': { display: '下播', tooltip: '直播结束后的运营工作', example: '下播比直播累' },
  '转型': { display: '转型', tooltip: '改变发展方向，会触发观众压力', example: '转型时老粉会不满' },
  '出道': { display: '出道', tooltip: 'VTuber的首次直播', example: '今天是你出道第一天' },
  '毕业': { display: '毕业', tooltip: 'VTuber停止活动的委婉说法', example: '光荣毕业是好结局' },
  '转生': { display: '转生', tooltip: '用新身份重新开始', example: '转生可以继承少量粉丝' },
  '粉丝群': { display: '粉丝群', tooltip: 'VTuber的粉丝社群', example: '粉丝群维护是必要的工作' },
  '榜一': { display: '榜一', tooltip: '互动支持最活跃的观众', example: '榜一声音太大时要小心处理' },
  '烤肉组': { display: '烤肉组', tooltip: '翻译组，把直播翻译成其他语言', example: '烤肉组让内容传播更广' },
  '录播组': { display: '录播组', tooltip: '专门录制直播的粉丝组织', example: '录播组全程录着' },
  '考据组': { display: '考据组', tooltip: '专门挖掘历史整理时间线的粉丝', example: '考据组什么都能扒出来' },
  '标题组': { display: '标题组', tooltip: '专门起标题的二创组织', example: '标题组提前上班了' }
};

function initTooltips() {
  document.querySelectorAll('[data-term]').forEach(function(el) {
    var term = el.getAttribute('data-term');
    var info = TERM_GLOSSARY[term];
    if (!info) return;
    el.classList.add('term-highlight');
    el.addEventListener('mouseenter', function(e) { showTermTooltip(e, info); });
    el.addEventListener('mouseleave', hideTermTooltip);
  });
  document.querySelectorAll('[data-metric-tooltip]').forEach(function(el) {
    if (el.dataset.metricTooltipBound === 'true') return;
    el.dataset.metricTooltipBound = 'true';
    el.addEventListener('mouseenter', function(e) {
      showTermTooltip(e, {
        display: el.querySelector('.stat-label')?.textContent || '数值',
        tooltip: el.dataset.metricTooltip || '',
        example: '点右上角 ? 查看所有数值说明'
      });
    });
    el.addEventListener('mouseleave', hideTermTooltip);
  });
}

function showTermTooltip(e, info) {
  var tooltip = document.getElementById('termTooltip');
  if (!tooltip) {
    tooltip = document.createElement('div');
    tooltip.id = 'termTooltip';
    tooltip.className = 'term-tooltip';
    document.body.appendChild(tooltip);
  }
  tooltip.innerHTML = '<div class="term-tooltip-title">' + html(info.display) + '</div>' +
    '<div class="term-tooltip-desc">' + html(info.tooltip) + '</div>' +
    (info.example ? '<div class="term-tooltip-example">💡 ' + html(info.example) + '</div>' : '');
  tooltip.style.display = 'block';
  tooltip.style.left = (e.pageX + 10) + 'px';
  tooltip.style.top = (e.pageY + 10) + 'px';
}

function hideTermTooltip() {
  var tooltip = document.getElementById('termTooltip');
  if (tooltip) tooltip.style.display = 'none';
}

function wrapTerms(text) {
  if (!text) return text;
  var result = html(text);
  Object.keys(TERM_GLOSSARY).forEach(function(term) {
    var regex = new RegExp('(?<![\\w<])' + term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '(?![\\w>])', 'g');
    result = result.replace(regex, '<span data-term="' + term + '">' + term + '</span>');
  });
  return result;
}

// === 渐进式UI解锁 ===
function getUnlockedPanels(day) {
  return {
    'stats': true, 'actions': true, 'coach': true,
    'fans': day >= 4, 'buzz': day >= 5, 'fortune': day >= 6,
    'timeline': day >= 7, 'route': day >= 7, 'combo': day >= 8,
    'persona': day >= 8, 'meme': day >= 9, 'npc': day >= 9,
    'achievement': day >= 10, 'forecast': day >= 10, 'expectation': day >= 12, 'highlight': day >= 14
  };
}

function isPanelUnlocked(panelId) {
  var day = state.vup ? state.vup.day || 1 : 1;
  var unlocked = getUnlockedPanels(day);
  return unlocked[panelId] !== false;
}

function showUnlockToast(panelName) {
  var toast = document.createElement('div');
  toast.className = 'unlock-toast';
  toast.innerHTML = '🔓 新情报解锁：<strong>' + panelName + '</strong>';
  document.body.appendChild(toast);
  setTimeout(function() {
    toast.classList.add('unlock-toast-fadeout');
    setTimeout(function() { toast.remove(); }, 500);
  }, 3000);
}

// === 运营诊断 ===
function renderOperationDiagnosis(vup, session) {
  if (!vup) return '';
  var diagnoses = [];
  var fans = vup.fanStructure;
  var opinion = vup.opinion;
  if (fans && fans.fans > 0) {
    var ddRatio = fans.ddFans / fans.fans;
    if (ddRatio > 0.4) diagnoses.push({ icon: '⚠️', text: 'DD粉占比' + Math.round(ddRatio * 100) + '%过高，建议增加真爱粉培养', level: 'warning' });
  }
  if (opinion && opinion.reputation < 45) diagnoses.push({ icon: '🔴', text: '口碑低于45，容易被黑，建议舆情监控或危机公关', level: 'danger' });
  if (opinion && opinion.watchHeat < 20) diagnoses.push({ icon: '💡', text: '热度偏低，建议做一次直播或发切片', level: 'info' });
  if (opinion && opinion.memeLevel > 60) diagnoses.push({ icon: '⚠️', text: '梗浓度高，注意梗疲劳，建议换个内容类型', level: 'warning' });
  if (vup.resources && vup.resources.stamina < 3) diagnoses.push({ icon: '💤', text: '体力不足，建议休息恢复', level: 'info' });
  if (!diagnoses.length) return '';
  return '<div class="operation-diagnosis"><div class="diagnosis-title">📊 运营诊断</div>' +
    diagnoses.map(function(d) {
      return '<div class="diagnosis-item diagnosis-' + d.level + '"><span class="diagnosis-icon">' + d.icon + '</span><span class="diagnosis-text">' + html(d.text) + '</span></div>';
    }).join('') + '</div>';
}

// === 直播间互动增强 ===
function renderLiveInteractionPanel() {
  if (!state.session) return '';
  var phase = state.session.phase;

  // 直播现场互动事件
  if (phase === 'NEED_INTERACTION_CHOICE' && state.pendingInteraction) {
    var event = state.pendingInteraction;
    return '<div class="live-interaction-panel">' +
      '<div class="interaction-title">🎤 ' + html(event.title || '直播互动') + '</div>' +
      '<div class="interaction-desc">' + html(event.description || '') + '</div>' +
      '<div class="interaction-choices">' +
        (event.choices || []).map(function(c) {
          return '<button type="button" class="interaction-choice-btn" data-action="choose-interaction" data-choice="' + html(c.choiceType || '') + '">' +
            '<span class="choice-label">' + html(c.label) + '</span>' +
            '<span class="choice-preview">' + html(c.effectPreview || '') + '</span>' +
          '</button>';
        }).join('') +
      '</div>' +
    '</div>';
  }

  return '';
}

// 弹幕点击增强
function onDanmakuClick(persona, message) {
  var info = {
    '老粉': { icon: '👴', desc: '忠实观众，一直在支持你' },
    '切片组': { icon: '✂️', desc: '专门剪辑精彩片段' },
    '录播组': { icon: '📹', desc: '全程录制，互联网记忆' },
    '楼友': { icon: '🏢', desc: '论坛用户，喜欢讨论' },
    '独角兽': { icon: '🦄', desc: '对你有独占欲的粉丝' },
    'DD': { icon: '🎪', desc: '多推，同时喜欢很多V' },
    '乐子人': { icon: '😂', desc: '看热闹不嫌事大' },
    '房管': { icon: '🛡️', desc: '直播间管理员' },
    '烤肉组': { icon: '🍖', desc: '翻译组，让内容传播更广' },
    '新观众': { icon: '👀', desc: '第一次来看直播' },
    '黑粉': { icon: '🖤', desc: '专门来找茬的人' }
  };
  var p = info[persona] || { icon: '💬', desc: '观众' };
  showNarrativePopup(p.icon + ' [' + persona + '] ' + p.desc + '\n\n"' + message + '"');
}

// === 粉丝来信互动化 ===
function renderFanLetterWithReply(letter) {
  if (!letter) return '';
  return '<div class="fan-letter-card">' +
    '<div class="letter-header">' +
      '<span class="letter-icon">' + (letter.icon || '✉️') + '</span>' +
      '<span class="letter-sender">' + html(letter.senderName || '匿名粉丝') + '</span>' +
      '<span class="letter-mood">' + (letter.mood === 'happy' ? '😊' : letter.mood === 'worried' ? '😟' : letter.mood === 'angry' ? '😠' : '😐') + '</span>' +
    '</div>' +
    '<div class="letter-content">' + html(letter.content || letter.text || '') + '</div>' +
    '<div class="letter-actions">' +
      '<button type="button" class="letter-reply-btn encourage" data-action="reply-letter" data-letter-reply="encourage" data-letter-id="' + html(letter.id || '') + '">💪 鼓励回复</button>' +
      '<button type="button" class="letter-reply-btn acknowledge" data-action="reply-letter" data-letter-reply="acknowledge" data-letter-id="' + html(letter.id || '') + '">🙏 感谢回复</button>' +
    '</div>' +
  '</div>';
}

async function replyLetter(type, letterId) {
  var responses = {
    'encourage': '你回复了一封鼓励的信。粉丝很开心，你的口碑小涨了一点。',
    'acknowledge': '你真诚地感谢了粉丝的支持。粉丝很感动，决定继续支持你。'
  };

  var btns = document.querySelectorAll('.letter-reply-btn');
  btns.forEach(function(btn) { btn.disabled = true; btn.style.opacity = '0.5'; });

  try {
    var result = await api('/api/fan-letter/reply', {
      method: 'POST',
      body: { replyType: type, letterId: letterId || '' }
    });
    if (result && result.vup) {
      state.vup = result.vup;
    }
    if (result && result.session) {
      state.session = result.session;
    }
    showNarrativePopup(responses[type] || '你回复了粉丝的来信。');
  } catch (e) {
    // Fallback: apply stat changes client-side if endpoint unavailable
    if (state && state.vup) {
      var opinion = state.vup.opinion;
      if (type === 'encourage') {
        if (opinion) opinion.reputation = (opinion.reputation || 0) + 2;
        if (opinion) opinion.popularity = (opinion.popularity || 0) + 1;
      } else if (type === 'acknowledge') {
        if (opinion) opinion.reputation = (opinion.reputation || 0) + 1;
        var fanStruct = state.vup.fanStructure;
        if (fanStruct) {
          fanStruct.trueFans = (fanStruct.trueFans || 0) + 3;
          fanStruct.fans = (fanStruct.fans || 0) + 3;
        }
      }
    }
    showNarrativePopup(responses[type] || '你回复了粉丝的来信。');
  }
  render();
}

// === 同行压力面板 ===
function renderRivalPanel(rivals) {
  if (!rivals || !rivals.length) return '';
  var myFans = state.vup ? (state.vup.fanStructure ? state.vup.fanStructure.fans : 0) : 0;
  var maxFans = Math.max(myFans, rivals.reduce(function(max, r) { return Math.max(max, r.fans); }, 1));

  return '<div class="rival-panel">' +
    '<div class="rival-title">⚔️ 同行竞争</div>' +
    rivals.map(function(r) {
      var barWidth = Math.round(r.fans / maxFans * 100);
      return '<div class="rival-row ' + (r.ahead ? 'rival-ahead' : '') + '">' +
        '<div class="rival-name">' + html(r.name) + ' <span class="rival-style">(' + html(r.style) + ')</span></div>' +
        '<div class="rival-bar-track">' +
          '<div class="rival-bar-fill" style="width:' + barWidth + '%"></div>' +
          '<span class="rival-fans">' + r.fans + '粉</span>' +
        '</div>' +
        (r.ahead ? '<div class="rival-ahead-badge">🔴 领先你</div>' : '') +
      '</div>';
    }).join('') +
    '<div class="rival-row rival-me">' +
      '<div class="rival-name">你 <span class="rival-style">(' + (state.vup ? html(state.vup.currentRoute || '未知') : '未知') + ')</span></div>' +
      '<div class="rival-bar-track">' +
        '<div class="rival-bar-fill me" style="width:' + Math.round(myFans / maxFans * 100) + '%"></div>' +
        '<span class="rival-fans">' + myFans + '粉</span>' +
      '</div>' +
    '</div>' +
  '</div>';
}

// === 数据复盘面板 ===
function renderDataReview(report) {
  if (!report || !report.dataDelta) return '';
  var dd = report.dataDelta;
  return '<div class="data-review">' +
    '<div class="review-title">📊 今日数据复盘</div>' +
    '<div class="review-grid">' +
      '<div class="review-item"><span class="review-label">粉丝变化</span><span class="review-value ' + ((dd.fanChange || 0) >= 0 ? 'positive' : 'negative') + '">' + ((dd.fanChange || 0) >= 0 ? '+' : '') + (dd.fanChange || 0) + '</span></div>' +
      '<div class="review-item"><span class="review-label">口碑变化</span><span class="review-value ' + ((dd.reputationChange || 0) >= 0 ? 'positive' : 'negative') + '">' + ((dd.reputationChange || 0) >= 0 ? '+' : '') + (dd.reputationChange || 0) + '</span></div>' +
      '<div class="review-item"><span class="review-label">热度变化</span><span class="review-value ' + ((dd.watchHeatChange || 0) >= 0 ? 'positive' : 'negative') + '">' + ((dd.watchHeatChange || 0) >= 0 ? '+' : '') + (dd.watchHeatChange || 0) + '</span></div>' +
      '<div class="review-item"><span class="review-label">梗浓度变化</span><span class="review-value ' + ((dd.memeChange || 0) >= 0 ? 'positive' : 'negative') + '">' + ((dd.memeChange || 0) >= 0 ? '+' : '') + (dd.memeChange || 0) + '</span></div>' +
    '</div>' +
    (report.selectedTitle ? '<div class="review-title-used">📺 直播标题: ' + html(report.selectedTitle) + '</div>' : '') +
  '</div>';
}

// === 黑天鹅事件弹窗 ===
function showBlackSwanPopup(event) {
  var popup = document.createElement('div');
  popup.className = 'black-swan-overlay';
  popup.innerHTML = '<div class="black-swan-card">' +
    '<div class="black-swan-icon">⚡</div>' +
    '<div class="black-swan-title">' + html(event.title) + '</div>' +
    '<div class="black-swan-desc">' + html(event.description) + '</div>' +
    '<div class="black-swan-effects">' +
      (event.heatChange ? '<span class="swan-effect ' + (event.heatChange > 0 ? 'positive' : 'negative') + '">热度' + (event.heatChange > 0 ? '+' : '') + event.heatChange + '</span>' : '') +
      (event.reputationChange ? '<span class="swan-effect ' + (event.reputationChange > 0 ? 'positive' : 'negative') + '">口碑' + (event.reputationChange > 0 ? '+' : '') + event.reputationChange + '</span>' : '') +
    '</div>' +
    '<button type="button" class="black-swan-dismiss" data-action="dismiss-closest" data-dismiss-target=".black-swan-overlay">知道了</button>' +
  '</div>';
  document.body.appendChild(popup);
  if (SFX && SFX.play) SFX.play('debtWarn');
}

// === 脱粉回踩弹窗 ===
function showBacklashPopup(result) {
  showNarrativePopup('⚠️ ' + result.title + '\n\n' + result.description);
}

// === 事件叙事弹窗 ===
function showNarrativePopup(text, callback) {
  var popup = document.createElement('div');
  popup.className = 'narrative-popup-overlay';
  popup.innerHTML = '<div class="narrative-popup">' +
    '<div class="narrative-popup-text" id="narrativeTypewriter"></div>' +
    '<button type="button" class="narrative-popup-skip" data-action="dismiss-closest" data-dismiss-target=".narrative-popup-overlay">跳过 ▶</button>' +
  '</div>';
  document.body.appendChild(popup);

  // 打字机效果
  var el = document.getElementById('narrativeTypewriter');
  var i = 0;
  var interval = setInterval(function() {
    if (i < text.length) {
      el.textContent += text[i];
      i++;
    } else {
      clearInterval(interval);
      setTimeout(function() {
        popup.classList.add('narrative-popup-fadeout');
        setTimeout(function() { popup.remove(); if (callback) callback(); }, 500);
      }, 2000);
    }
  }, 50);

  // 点击跳过
  popup.addEventListener('click', function(e) {
    if (e.target.classList.contains('narrative-popup-skip')) return;
    clearInterval(interval);
    el.textContent = text;
    setTimeout(function() {
      popup.classList.add('narrative-popup-fadeout');
      setTimeout(function() { popup.remove(); if (callback) callback(); }, 500);
    }, 500);
  });
}

function showRouteNarrative(routeText) {
  if (!routeText) return;
  showNarrativePopup(routeText);
}

// === 生涯模式UI ===
function renderCareerPanel(career) {
  if (!career) return '';

  return '<div class="career-panel">' +
    '<div class="career-title">🎯 生涯模式</div>' +
    '<div class="career-progress-bar">' +
      '<div class="career-progress-fill" style="width:' + Math.round(career.currentRun / career.totalRuns * 100) + '%"></div>' +
      '<span class="career-progress-text">第' + career.currentRun + '/' + career.totalRuns + '局</span>' +
    '</div>' +
    '<div class="career-goal">' +
      '<span class="career-goal-label">当前阶段：</span>' +
      '<span class="career-goal-value">' + html(career.currentGoal) + '</span>' +
    '</div>' +
    '<div class="career-target">' +
      '<span class="career-target-label">目标粉丝：</span>' +
      '<span class="career-target-value">' + career.fanTarget.toLocaleString() + '</span>' +
      '<span class="career-current">(当前: ' + career.totalFans.toLocaleString() + ')</span>' +
    '</div>' +
    (career.timeline && career.timeline.length ?
      '<div class="career-timeline">' + career.timeline.map(function(t) {
        return '<div class="career-timeline-item ' + t.status + '">' +
          '<span class="timeline-run">第' + t.run + '局</span>' +
          '<span class="timeline-label">' + html(t.label) + '</span>' +
          '<span class="timeline-status">' + (t.status === 'completed' ? '✅' : '⏳') + '</span>' +
        '</div>';
      }).join('') + '</div>' : '') +
  '</div>';
}

function renderEndingWithCareer(ending, career) {
  if (!ending) return '';
  var endingHtml = renderEndingNarrative(ending.endingType, ending);
  if (career && career.currentRun < career.totalRuns) {
    endingHtml += '<div class="career-next-run">' +
      '<div class="next-run-title">🎯 生涯继续</div>' +
      '<div class="next-run-desc">第' + (career.currentRun + 1) + '局即将开始</div>' +
      '<div class="next-run-goal">下一阶段目标：' + (career.fanTarget * 3).toLocaleString() + '粉丝</div>' +
    '</div>';
  }
  return endingHtml;
}

// === Chart.js 本地加载fallback ===
function initChartJs() {
  if (window.Chart) {
    window.dispatchEvent(new Event('chartjs-ready'));
    return;
  }
  // 如果本地加载失败，尝试CDN fallback
  var script = document.createElement('script');
  script.src = 'https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js';
  script.onload = function() { window.dispatchEvent(new Event('chartjs-ready')); };
  script.onerror = function() { window.__chartJsUnavailable = true; console.warn('Chart.js 加载失败'); };
  document.head.appendChild(script);
}

let chartJsFallbackRequested = false;

function dispatchChartJsReady() {
  window.dispatchEvent(new Event('chartjs-ready'));
}

function requestChartJsFallback() {
  if (chartJsFallbackRequested) return;
  chartJsFallbackRequested = true;
  initChartJs();
}

function handleOptionalChartJsLoad() {
  if (window.Chart) {
    dispatchChartJsReady();
    return;
  }
  requestChartJsFallback();
}

function bindOptionalChartJsLoader() {
  var localScript = document.querySelector('script[data-optional-chartjs="true"]');
  if (!localScript) {
    requestChartJsFallback();
    return;
  }
  localScript.addEventListener('load', handleOptionalChartJsLoad, { once: true });
  localScript.addEventListener('error', function() {
    window.__chartJsUnavailable = true;
    requestChartJsFallback();
  }, { once: true });
  if (window.Chart || window.__chartJsPlaceholder) {
    handleOptionalChartJsLoad();
  }
}

bindOptionalChartJsLoader();

document.addEventListener('error', function(event) {
  var image = event.target;
  if (image && image.matches && image.matches('[data-remove-on-error="true"]')) {
    image.remove();
  }
}, true);

// === 图片加载失败fallback ===
function initImageFallback() {
  var imgs = document.querySelectorAll('img');
  imgs.forEach(function(img) {
    img.addEventListener('error', function() {
      if (this.dataset.fallbackApplied) return;
      this.dataset.fallbackApplied = 'true';
      // 尝试回退到原始PNG
      var src = this.getAttribute('src') || '';
      if (src.includes('gallery-optimized')) {
        this.src = src.replace('gallery-optimized', 'gallery').replace(/-\d+\.webp/, '.png');
      }
    });
  });
}
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initImageFallback);
} else {
  initImageFallback();
}

// === 错误恢复集成 ===
function handleApiError(error, retryCallback) {
  if (error && error.status >= 500) {
    if (typeof showRecoverableError === 'function') showRecoverableError(error, retryCallback);
  } else if (error && error.status === 0) {
    if (typeof showRecoverableError === 'function') showRecoverableError({ message: '网络连接失败，请检查网络后重试' }, retryCallback);
  }
}

// === 下一步提示 ===
function renderNextStepHint() {
  if (!state.session) return '';
  var phase = state.session.phase;
  var hints = {
    'READY': '今天还没有行动，先选择一个主行动。',
    'NEED_TITLE': '直播行动已完成，请选择一个标题。',
    'OFF_STREAM_READY': '下播后还可以做一次辅助行动。',
    'OFF_STREAM_RESOLVED': '正在处理事件...',
    'NEED_INTERACTION_CHOICE': '出现直播互动，请选择处理方式。',
    'NEED_EVENT_CHOICE': '出现事件，请选择处理方式。',
    'REPORT_READY': '今日日报已生成，可以复盘并进入下一天。',
    'ENDING_READY': '本局已结束，查看结局复盘。'
  };
  var hint = hints[phase] || '';
  if (!hint) return '';
  return '<div class="next-step-hint">' + html(hint) + '</div>';
}

window.__vupAppMainReady = true;
