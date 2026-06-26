// main.js - 应用入口
// 职责: 注册事件、初始化模块、拉取数据、渲染首屏
// 禁止: 不直接拼HTML、不直接fetch、不保存复杂状态

import { escapeHtml, html } from './utils/escape-html.js';
import { apiGet, apiPost, ApiError } from './api-client.js';
import { getState, patchState, resetState, subscribe, syncToWindow } from './state.js';
import { byId, setHtml, setText, delegate } from './dom.js';
import { runAction, registerAllActions } from './action-registry.js';
import { openDialog, closeDialog } from './features/accessibility.js';
import { showRecoverableError } from './features/error-recovery.js';
import { setBusy, clearBusy, setStatus as setAppStatus, clearStatus as clearAppStatus } from './features/app-status.js';

// 导出给旧代码兼容
window.html = html;
window.escapeHtml = escapeHtml;
window.apiGet = apiGet;
window.apiPost = apiPost;

// 功能模块暴露给 app.js (IIFE 通过 typeof 检查)
window.openDialog = openDialog;
window.closeDialog = closeDialog;
window.showRecoverableError = showRecoverableError;
window.setApiBusy = setBusy;
window.clearApiBusy = clearBusy;
window.setAppStatus = setAppStatus;
window.clearAppStatus = clearAppStatus;

// 初始化
export function bootApp() {
  syncToWindow();

  // 注册所有动作
  registerAllActions();

  // 注册全局事件委托
  delegate(document, 'click', '[data-action]', (event, element) => {
    // Legacy app.js still owns buttons with inline onclick. Let those finish first
    // so migrating actions into this registry can happen one journey at a time.
    if (element.hasAttribute('onclick')) return;
    event.preventDefault();
    const action = element.dataset.action;
    runAction(action, { event, element });
  });

  delegate(document, 'change', '[data-change-action]', (event, element) => {
    const action = element.dataset.changeAction;
    runAction(action, { event, element });
  });

  delegate(document, 'input', '[data-input-action]', (event, element) => {
    const action = element.dataset.inputAction;
    runAction(action, { event, element });
  });

  console.log('VupWorld module bootstrapped');
}

// 自动启动
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', bootApp);
} else {
  bootApp();
}
