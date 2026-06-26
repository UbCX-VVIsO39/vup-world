// action-registry.js - 动作注册表
// Interface: registerAction(name, handler), runAction(name, context)
// 规则: 替代内联onclick，所有按钮通过data-action触发

const actions = {};
const noopAction = () => {};

function callGlobal(functionName, ...args) {
  const fn = globalThis[functionName];
  if (typeof fn !== 'function') {
    console.warn('Action handler is not available:', functionName);
    return undefined;
  }
  return fn(...args);
}

function legacyAction(functionName, argsFactory = () => []) {
  return context => callGlobal(functionName, ...argsFactory(context));
}

function dataset(context) {
  return context?.element?.dataset || {};
}

function dataNumber(context, key) {
  const value = dataset(context)[key];
  if (value == null || value === '') return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function optionalNumberArg(context, key) {
  const value = dataNumber(context, key);
  return value == null ? [] : [value];
}

function submitActionArgs(context) {
  const actionType = dataset(context).actionType;
  return actionType ? [actionType] : [];
}

function schedulePlanArgs(context) {
  const planKey = dataset(context).planKey;
  return planKey ? [planKey] : [];
}

function scheduleSlotActionArgs(context) {
  const data = dataset(context);
  return [data.slotKey, context?.element?.value].filter(value => value != null && value !== '');
}

function scheduleSlotIntensityArgs(context) {
  const data = dataset(context);
  return [data.slotKey, context?.element?.value].filter(value => value != null && value !== '');
}

function offstreamArgs(context) {
  const action = dataset(context).action || '';
  const type = action.startsWith('offstream-')
    ? action.slice('offstream-'.length).toUpperCase()
    : dataset(context).offstreamType;
  return type ? [type] : [];
}

function titleArgs(context) {
  return optionalNumberArg(context, 'titleId');
}

function choiceArgs(context) {
  const choice = dataset(context).choice;
  return choice ? [choice] : [];
}

function fanTopicArgs(context) {
  const data = dataset(context);
  return [data.topic, data.topicChoice].filter(value => value != null && value !== '');
}

function replyLetterArgs(context) {
  const data = dataset(context);
  return data.letterReply ? [data.letterReply, data.letterId || ''] : [];
}

function riskToolArgs(context) {
  const data = dataset(context);
  const targetDebtId = dataNumber(context, 'targetDebtId');
  return targetDebtId == null ? [data.riskToolType, null] : [data.riskToolType, targetDebtId];
}

function endingReviewAction() {
  callGlobal('hideEndingFullscreen');
  callGlobal('switchMainTab', 'mainPanel');
  globalThis.setTimeout(() => {
    const target = globalThis.document?.getElementById('endingPanel');
    callGlobal('pulseFocusTarget', target);
  }, 0);
}

function endingRestartAction() {
  callGlobal('hideEndingFullscreen');
  callGlobal('switchMainTab', 'mainPanel');
  globalThis.setTimeout(() => {
    const restartTarget = globalThis.document?.getElementById('restartBias');
    if (restartTarget) {
      callGlobal('pulseFocusTarget', restartTarget.closest('.ending-restart-simple') || restartTarget);
      restartTarget.focus({ preventScroll: false });
      return;
    }
    callGlobal('pulseFocusTarget', globalThis.document?.getElementById('endingPanel'));
  }, 0);
}

function switchMainTabAction(context) {
  const tab = dataset(context).mainTab;
  if (tab) callGlobal('switchMainTab', tab);
}

function switchTabAction(context) {
  const tab = dataset(context).tab;
  if (tab) callGlobal('switchTab', tab);
}

function openLiveDrawerAction(context) {
  const drawer = dataset(context).drawer || dataset(context).liveDrawer || dataset(context).tab;
  if (drawer) callGlobal('openLiveDrawer', drawer);
}

function sidebarTabAction(context) {
  const tab = dataset(context).sidebarTab;
  if (tab) callGlobal('switchSidebarTab', context.element, tab);
}

function selectGiftQtyAction(context) {
  const qty = dataNumber(context, 'qty');
  if (qty != null) callGlobal('selectGiftQty', qty);
}

function focusActionFromDataset(context, key) {
  const actionType = dataset(context)[key];
  if (actionType) callGlobal('focusActionCard', actionType);
}

function focusReadyQuickActionFromDataset(context, key) {
  const actionType = dataset(context)[key];
  if (actionType) callGlobal('focusReadyQuickAction', actionType);
}

function dismissClosestAction(context) {
  const selector = dataset(context).dismissTarget;
  const target = selector && context?.element?.closest(selector);
  if (target) target.remove();
}

function creationStyleKey(context) {
  return dataset(context).creationStyle || context?.element?.value;
}

export function registerAction(name, handler) {
  actions[name] = handler;
}

export function runAction(name, context) {
  const handler = actions[name];
  if (!handler) return;
  try {
    const result = handler(context);
    if (result && typeof result.catch === 'function') {
      result.catch(error => console.error('Action error:', name, error));
    }
  } catch (error) {
    console.error('Action error:', name, error);
  }
}

// 注册所有现有动作
export function registerAllActions() {
  // 认证和单机存档
  registerAction('login', legacyAction('login'));
  registerAction('register', legacyAction('register'));
  registerAction('logout', legacyAction('logout'));
  registerAction('create-vup', legacyAction('createVup'));
  registerAction('quick-start-guest', legacyAction('quickStartGuest', context => optionalNumberArg(context, 'slotNumber')));
  registerAction('continue-local-run', legacyAction('continueLocalRun', context => optionalNumberArg(context, 'slotNumber')));
  registerAction('save-local-slot', legacyAction('saveLocalSlot'));
  registerAction('export-local-slot', legacyAction('exportLocalSlot', context => optionalNumberArg(context, 'slotNumber')));
  registerAction('import-local-slot', legacyAction('importLocalSlot', context => optionalNumberArg(context, 'slotNumber')));
  registerAction('rename-local-slot', legacyAction('renameLocalSlot', context => optionalNumberArg(context, 'slotNumber')));
  registerAction('restart-local-slot', legacyAction('restartLocalSlot', context => optionalNumberArg(context, 'slotNumber')));
  registerAction('start-local-slot', legacyAction('quickStartGuest', context => optionalNumberArg(context, 'slotNumber')));

  // 日程相关
  registerAction('next-day', legacyAction('nextDay'));
  registerAction('submit-action', legacyAction('submitAction', submitActionArgs));
  registerAction('simple-primary-action', legacyAction('submitAction', submitActionArgs));
  registerAction('simple-alt-action', legacyAction('submitAction', submitActionArgs));
  registerAction('daily-plan-submit', legacyAction('submitSchedule', schedulePlanArgs));
  registerAction('daily-plan-next-step', legacyAction('submitSchedule', schedulePlanArgs));
  registerAction('daily-schedule-submit', legacyAction('submitSchedule', schedulePlanArgs));
  registerAction('daily-schedule-preset', legacyAction('selectDailySchedulePreset', schedulePlanArgs));
  registerAction('daily-schedule-action-change', legacyAction('updateDailyScheduleSlotAction', scheduleSlotActionArgs));
  registerAction('daily-schedule-intensity-change', legacyAction('updateDailyScheduleSlotIntensity', scheduleSlotIntensityArgs));
  registerAction('quick-submit-action', legacyAction('submitAction', submitActionArgs));
  registerAction('detail-submit-action', context => {
    context?.event?.stopPropagation();
    return callGlobal('submitAction', ...submitActionArgs(context));
  });
  registerAction('toggle-action-detail', legacyAction('toggleActionDetail', submitActionArgs));
  registerAction('sync-action-options-summary', legacyAction('syncActionOptionsSummary'));
  registerAction('confirm-pending-action', legacyAction('confirmPendingAction'));
  registerAction('cancel-pending-action', legacyAction('cancelPendingAction'));
  registerAction('opening-style-choice', legacyAction('submitOpeningStyleChoice', context => {
    const key = dataset(context).openingStyle;
    return key ? [key] : [];
  }));
  registerAction('skip-offstream', legacyAction('skipOffStream'));
  registerAction('offstream-browse_social', legacyAction('submitOffStream', offstreamArgs));
  registerAction('offstream-chat_room', legacyAction('submitOffStream', offstreamArgs));
  registerAction('offstream-clip_scouting', legacyAction('submitOffStream', offstreamArgs));
  registerAction('offstream-collab_plan', legacyAction('submitOffStream', offstreamArgs));
  registerAction('offstream-crisis_pr', legacyAction('submitOffStream', offstreamArgs));
  registerAction('offstream-dm_maintain', legacyAction('submitOffStream', offstreamArgs));
  registerAction('offstream-meme_research', legacyAction('submitOffStream', offstreamArgs));
  registerAction('offstream-organize_materials', legacyAction('submitOffStream', offstreamArgs));
  registerAction('offstream-read_letters', legacyAction('submitOffStream', offstreamArgs));
  registerAction('offstream-short_video_idea', legacyAction('submitOffStream', offstreamArgs));
  registerAction('offstream-song_selection', legacyAction('submitOffStream', offstreamArgs));
  registerAction('offstream-thumbnail_design', legacyAction('submitOffStream', offstreamArgs));
  registerAction('offstream-trend_watch', legacyAction('submitOffStream', offstreamArgs));
  registerAction('offstream-vocal_practice', legacyAction('submitOffStream', offstreamArgs));

  // 标题、事件、风险工具
  registerAction('choose-title', legacyAction('chooseTitle', titleArgs));
  registerAction('reroll-title', legacyAction('rerollTitle'));
  registerAction('cancel-action', legacyAction('cancelAction'));
  registerAction('choose-event', legacyAction('chooseEvent', choiceArgs));
  registerAction('choose-interaction', legacyAction('chooseInteraction', choiceArgs));
  registerAction('choose-fan-topic', legacyAction('chooseFanTopic', fanTopicArgs));
  registerAction('use-risk-tool', legacyAction('useRiskTool', riskToolArgs));
  registerAction('reply-letter', legacyAction('replyLetter', replyLetterArgs));

  // Tab切换
  registerAction('switch-main-tab', switchMainTabAction);
  registerAction('switch-tab', switchTabAction);
  registerAction('open-live-drawer', openLiveDrawerAction);
  registerAction('close-live-drawer', legacyAction('closeLiveDrawer'));
  registerAction('open-daily-feedback', legacyAction('showDailyFeedback', () => [{ force: true }]));
  registerAction('close-daily-feedback', legacyAction('closeDailyFeedback'));
  registerAction('open-feedback-report', legacyAction('openFeedbackReport'));

  // 直播相关
  registerAction('toggle-gift-panel', legacyAction('toggleGiftPanel'));
  registerAction('toggle-right-sidebar', legacyAction('toggleRightSidebar'));
  registerAction('toggle-streamer-mode', legacyAction('toggleStreamerMode'));
  registerAction('send-danmaku', legacyAction('sendDanmaku'));
  registerAction('send-gift', legacyAction('sendGift'));
  registerAction('select-gift-qty', selectGiftQtyAction);
  registerAction('switch-sidebar-tab', sidebarTabAction);

  // BGM
  registerAction('toggle-bgm', () => {
    if (globalThis.BGM) globalThis.BGM.toggle();
    if (globalThis.syncAudioSettingsPanel) globalThis.syncAudioSettingsPanel();
  });
  registerAction('set-bgm-volume', context => {
    const value = Number(context?.element?.value);
    if (globalThis.BGM && Number.isFinite(value)) globalThis.BGM.setVolume(value / 100);
    if (globalThis.syncAudioSettingsPanel) globalThis.syncAudioSettingsPanel();
  });

  // 设置
  registerAction('toggle-settings-panel', legacyAction('toggleSettingsPanel'));
  registerAction('close-settings-panel', legacyAction('closeSettingsPanel'));

  // 教练卡收起/展开
  registerAction('toggle-coach-panel', () => {
    const panel = document.getElementById('coachPanel');
    if (!panel) return;
    panel.classList.toggle('collapsed');
  });

  // 路线卡片点击展开/收起详情
  registerAction('toggle-route-detail', context => {
    const item = context?.element?.closest('.route-gallery-item');
    if (!item) return;
    const detail = item.querySelector('.route-detail');
    if (detail) detail.hidden = !detail.hidden;
    item.classList.toggle('expanded', detail && !detail.hidden);
  });

  // 成就图鉴弹窗
  registerAction('open-unlock-atlas', legacyAction('showAtlasPopup'));
  registerAction('close-atlas-popup', legacyAction('closeAtlasPopup'));

  // 平台互动：发弹幕/联动/偷学
  registerAction('platform-interact', context => {
    const data = dataset(context);
    const npcKey = data.npcKey;
    const interactionType = data.interactionType;
    if (npcKey && interactionType) callGlobal('platformInteract', npcKey, interactionType);
  });
  registerAction('platform-steal-learn', context => {
    const npcKey = dataset(context).npcKey;
    if (npcKey) callGlobal('platformStealLearn', npcKey);
  });

  // NPC 主动联动：接受 / 稍后
  registerAction('resolve-chain-event', context => {
    const data = dataset(context);
    if (data.npcKey && data.eventKey) callGlobal('resolveChainEvent', data.npcKey, data.eventKey);
  });
  registerAction('dismiss-chain-event', () => {
    callGlobal('dismissChainEvent');
  });

  // 教程相关
  registerAction('close-tutorial', legacyAction('hideTutorial'));
  registerAction('skip-tutorial', legacyAction('hideTutorial'));
  registerAction('open-metric-help', legacyAction('showMetricHelp'));
  registerAction('close-metric-help', legacyAction('hideMetricHelp'));

  // 音频
  registerAction('toggle-sfx', context => {
    if (globalThis.SFX) globalThis.SFX.toggle();
    const btn = context.element;
    if (btn && globalThis.SFX && btn.tagName === 'BUTTON') {
      btn.classList.toggle('muted');
      btn.textContent = globalThis.SFX.muted ? '🔇' : '🔊';
    }
    if (globalThis.syncAudioSettingsPanel) globalThis.syncAudioSettingsPanel();
  });
  registerAction('toggle-tts', context => {
    if (globalThis.TTS) globalThis.TTS.toggle();
    const btn = context.element;
    if (btn && globalThis.TTS && btn.tagName === 'BUTTON') {
      btn.classList.toggle('muted');
      btn.textContent = globalThis.TTS.enabled ? '🗣️' : '🤫';
    }
    if (globalThis.syncAudioSettingsPanel) globalThis.syncAudioSettingsPanel();
  });

  // 结局相关
  registerAction('ending-review', endingReviewAction);
  registerAction('ending-restart', endingRestartAction);
  registerAction('restart', legacyAction('restart'));
  registerAction('restart-from-share-card', legacyAction('restart'));
  registerAction('update-restart-bias-hint', legacyAction('updateRestartBiasHint', () => [true]));
  registerAction('copy-ending-share', legacyAction('copyEndingShare', context => {
    const text = dataset(context).shareText;
    return text ? [text] : [];
  }));
  registerAction('copy-stage-milestone', legacyAction('copyStageMilestoneShare', context => {
    const text = dataset(context).shareText;
    return text ? [text] : [];
  }));
  registerAction('download-ending-share-card', legacyAction('downloadEndingShareCard', context => {
    const format = dataset(context).shareFormat;
    return format ? [format] : [];
  }));
  registerAction('toggle-report-detail', legacyAction('toggleReportDetail'));
  registerAction('toggle-report-debug', legacyAction('toggleReportDebug'));

  // 开发验证入口。生产玩家主流程不会展示这些入口。
  registerAction('demo-reset', legacyAction('demoReset'));
  registerAction('demo-run', legacyAction('demoRun'));
  registerAction('demo-reset-run', legacyAction('demoResetAndRun'));
  registerAction('demo-fast-forward', legacyAction('demoFastForward'));
  registerAction('sync-demo-strategy-selects', legacyAction('syncDemoStrategySelects', context => {
    const value = context?.element?.value;
    return value == null ? [] : [value];
  }));

  // 只改变前端焦点或展开状态的旧动作。
  registerAction('focus-ending-gap', context => focusActionFromDataset(context, 'actionType'));
  registerAction('focus-live-combo', context => focusActionFromDataset(context, 'actionType'));
  registerAction('focus-ready-first-run', context => focusReadyQuickActionFromDataset(context, 'primerActionType'));
  registerAction('focus-route-mastery', context => focusReadyQuickActionFromDataset(context, 'masteryActionType'));
  registerAction('leaderboard-dim', context => {
    const key = dataset(context).dimension;
    if (key) callGlobal('switchLeaderboardDimension', key);
  });
  registerAction('open-cockpit-goal', legacyAction('openInsightTab', () => ['buzz']));
  registerAction('open-cockpit-stage', legacyAction('openInsightTab', () => ['npc']));
  registerAction('open-daily-plan-risk', legacyAction('openDebtControl'));
  registerAction('open-debt-control', legacyAction('openDebtControl'));
  registerAction('open-defense-demo', legacyAction('openInsightTab', () => ['demo']));
  registerAction('open-info-brief', context => {
    const tab = dataset(context).infoTab;
    if (tab) callGlobal('openInsightTab', tab);
  });
  registerAction('open-insight-tab', context => {
    const tab = dataset(context).insightTab;
    if (tab) callGlobal('openInsightTab', tab);
  });
  registerAction('open-objective-item', context => focusActionFromDataset(context, 'objectiveActionType'));
  registerAction('open-objective-today', context => focusActionFromDataset(context, 'objectiveActionType'));
  registerAction('open-operation-risk', legacyAction('openDebtControl'));
  registerAction('open-ready-atlas-goal', legacyAction('openAtlasGoalTarget'));
  registerAction('open-ready-goal', context => {
    const target = dataset(context).goalTarget;
    if (target === 'risk') callGlobal('openInsightTab', 'ambient');
    else if (target) callGlobal('openInsightTab', target);
  });
  registerAction('open-report-next-risk', legacyAction('openDebtControl'));
  registerAction('open-report-risk-recovery', legacyAction('openDebtControl'));
  registerAction('open-score-guide-evidence', legacyAction('openScoreGuideTarget', () => ['evidence']));
  registerAction('open-score-guide-risk', legacyAction('openScoreGuideTarget', () => ['risk']));
  registerAction('open-score-guide-route', legacyAction('openScoreGuideTarget', () => ['route']));
  registerAction('retry-actions', legacyAction('hydrate'));
  registerAction('retry-bootstrap', legacyAction('hydrate'));
  registerAction('dismiss-closest', dismissClosestAction);
  registerAction('toggle-action-confirm-skip', legacyAction('toggleActionConfirmSkip'));
  registerAction('sync-creation-style-preview', legacyAction('syncCreationStylePreview'));
  registerAction('apply-creation-style-preset', legacyAction('applyCreationStylePreset', context => {
    const key = creationStyleKey(context);
    return key ? [key] : [];
  }));
  registerAction('select-creation-style', legacyAction('applyCreationStylePreset', context => {
    const key = creationStyleKey(context);
    return key ? [key] : [];
  }));
  registerAction('create-loadout-summary', noopAction);
}
