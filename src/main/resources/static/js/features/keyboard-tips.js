// keyboard-tips.js - 键盘快捷键提示系统
// Interface: registerShortcut(key, callback, description), showShortcutsList()
// 规则: 快捷键以 ? 唤起帮助面板

const shortcuts = new Map();
let helpPanel = null;

export function registerShortcut(key, callback, description) {
  if (!key || typeof callback !== 'function') return;
  shortcuts.set(key.toLowerCase(), { callback, description: description || '' });
}

export function unregisterShortcut(key) {
  shortcuts.delete(key.toLowerCase());
}

function handleKeyDown(event) {
  // 焦点在input/textarea时跳过（避免影响文字输入）
  const tag = (event.target.tagName || '').toLowerCase();
  if (tag === 'input' || tag === 'textarea' || event.target.isContentEditable) {
    return;
  }

  // 帮助快捷键
  if (event.key === '?' || (event.shiftKey && event.key === '/')) {
    event.preventDefault();
    showShortcutsList();
    return;
  }

  // 关闭帮助面板
  if (event.key === 'Escape' && helpPanel) {
    closeHelpPanel();
    return;
  }

  const key = event.key.toLowerCase();
  const shortcut = shortcuts.get(key);
  if (shortcut) {
    event.preventDefault();
    try {
      shortcut.callback(event);
    } catch (err) {
      console.error('快捷键回调错误:', err);
    }
  }
}

export function showShortcutsList() {
  if (helpPanel) {
    closeHelpPanel();
    return;
  }

  helpPanel = document.createElement('div');
  helpPanel.className = 'shortcuts-panel';
  helpPanel.setAttribute('role', 'dialog');
  helpPanel.setAttribute('aria-label', '键盘快捷键');

  const items = Array.from(shortcuts.entries())
    .filter(([_, v]) => v.description)
    .map(([k, v]) => `<div class="shortcut-item">
        <kbd>${escapeHtml(k.toUpperCase())}</kbd>
        <span>${escapeHtml(v.description)}</span>
      </div>`)
    .join('');

  helpPanel.innerHTML = `
    <div class="shortcuts-panel-head">
      <strong>键盘快捷键</strong>
      <button type="button" data-shortcuts-close aria-label="关闭">×</button>
    </div>
    <div class="shortcuts-panel-body">
      ${items || '<div class="shortcuts-empty">暂无可用快捷键</div>'}
      <div class="shortcut-item">
        <kbd>?</kbd>
        <span>显示/隐藏此帮助</span>
      </div>
      <div class="shortcut-item">
        <kbd>ESC</kbd>
        <span>关闭弹窗</span>
      </div>
    </div>
  `;

  document.body.appendChild(helpPanel);

  // 触发动画
  requestAnimationFrame(() => helpPanel.classList.add('visible'));

  // 关闭按钮
  helpPanel.querySelector('[data-shortcuts-close]').addEventListener('click', closeHelpPanel);
  // 点击外部关闭
  setTimeout(() => {
    document.addEventListener('click', outsideClickHandler, { once: true });
  }, 0);
}

function outsideClickHandler(e) {
  if (helpPanel && !helpPanel.contains(e.target)) {
    closeHelpPanel();
  }
}

function closeHelpPanel() {
  if (!helpPanel) return;
  helpPanel.classList.remove('visible');
  setTimeout(() => {
    if (helpPanel && helpPanel.parentNode) {
      helpPanel.parentNode.removeChild(helpPanel);
    }
    helpPanel = null;
  }, 200);
}

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, c => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[c]);
}

// 自动注册
let installed = false;
export function installShortcuts() {
  if (installed) return;
  installed = true;
  document.addEventListener('keydown', handleKeyDown);
}
