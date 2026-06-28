// toast.js - 轻量Toast通知系统
// Interface: showToast(message, options), toast.success/error/info/warning
// 规则: 不依赖任何UI框架，最多同时显示3个，自动消失

const TOAST_DURATION_DEFAULT = 3500;
const TOAST_MAX = 3;

function ensureContainer() {
  let container = document.getElementById('__vup_toast_container');
  if (container) return container;
  container = document.createElement('div');
  container.id = '__vup_toast_container';
  container.className = 'toast-container';
  document.body.appendChild(container);
  return container;
}

function createToast(message, options = {}) {
  const container = ensureContainer();
  const toast = document.createElement('div');
  const type = options.type || 'info';
  const icon = options.icon || ({
    success: '✓',
    error: '✕',
    warning: '⚠',
    info: 'ℹ'
  }[type] || 'ℹ');

  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <span class="toast-icon">${icon}</span>
    <span class="toast-message">${message}</span>
  `;

  container.appendChild(toast);

  // 限制最多显示
  while (container.children.length > TOAST_MAX) {
    container.removeChild(container.firstChild);
  }

  // 自动消失
  const duration = options.duration || TOAST_DURATION_DEFAULT;
  setTimeout(() => {
    toast.classList.add('toast-leaving');
    setTimeout(() => toast.remove(), 300);
  }, duration);

  return toast;
}

export function showToast(message, options = {}) {
  return createToast(message, options);
}

export const toast = {
  success: (msg, opts = {}) => createToast(msg, { ...opts, type: 'success' }),
  error:   (msg, opts = {}) => createToast(msg, { ...opts, type: 'error' }),
  warning: (msg, opts = {}) => createToast(msg, { ...opts, type: 'warning' }),
  info:    (msg, opts = {}) => createToast(msg, { ...opts, type: 'info' })
};
