// main.js - 应用入口
// 职责: 注册事件、初始化模块、拉取数据、渲染首屏
// 禁止: 不直接拼HTML、不直接fetch、不保存复杂状态

import { escapeHtml, html } from './utils/escape-html.js';
import { apiGet, apiPost, ApiError } from './api-client.js';
import { getState, patchState, resetState, subscribe, syncToWindow } from './state.js';
import { byId, setHtml, setText, delegate, flashHint } from './dom.js';
import { runAction, registerAllActions } from './action-registry.js';
import { openDialog, closeDialog } from './features/accessibility.js';
import { showRecoverableError } from './features/error-recovery.js';
import { setBusy, clearBusy, setStatus as setAppStatus, clearStatus as clearAppStatus } from './features/app-status.js';
import { installShortcuts, registerShortcut } from './features/keyboard-tips.js';
import { showToast, toast } from './features/toast.js';

// 导出给旧代码兼容
window.html = html;
window.escapeHtml = escapeHtml;
window.apiGet = apiGet;
window.apiPost = apiPost;
window.flashHint = flashHint;
window.showToast = showToast;
window.toast = toast;

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

  // 初始化全局快捷键系统
  installShortcuts();
  registerDefaultShortcuts();

  console.log('VupWorld module bootstrapped');
}

// 注册默认的全局快捷键
function registerDefaultShortcuts() {
  // 1-4 数字键选择行动
  for (let i = 1; i <= 4; i++) {
    registerShortcut(String(i), () => {
      const btn = document.querySelector(`[data-action-hotkey="${i}"]`);
      if (btn && !btn.matches(':disabled, [aria-disabled="true"]')) {
        btn.click();
        flashHint('已选择行动 ' + i);
      }
    }, '选择第' + i + '个行动');
  }

  // N 键下一天
  registerShortcut('n', () => {
    const btn = document.querySelector('[data-action="next-day"]');
    if (btn && !btn.matches(':disabled, [aria-disabled="true"]')) {
      btn.click();
      flashHint('下一天 →');
    }
  }, '进入下一天');

  // M 键切换主播模式
  registerShortcut('m', () => {
    const btn = document.getElementById('streamerModeToggle');
    if (btn) btn.click();
  }, '切换主播展示模式');

  // 数字键 1-3 切换Tab
  registerShortcut('!', () => switchToTab('mainPanel'), '切换到「今日」');  // Shift+1
  registerShortcut('@', () => switchToTab('platform'), '切换到「平台」');   // Shift+2
  registerShortcut('#', () => switchToTab('infoHub'), '切换到「总览」');    // Shift+3
}

function switchToTab(tab) {
  const btn = document.querySelector(`[data-main-tab="${tab}"]`);
  if (btn) btn.click();
}

// 自动启动
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', bootApp);
} else {
  bootApp();
}
