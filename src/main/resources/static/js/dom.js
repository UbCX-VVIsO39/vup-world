// dom.js - DOM操作收口
// Interface: byId(id), setHtml(id, html), setText(id, text), delegate(root, event, selector, handler)
// 规则: innerHTML只能在此模块出现

export function byId(id) { return document.getElementById(id); }

export function setHtml(id, html) {
  const el = document.getElementById(id);
  if (el) el.innerHTML = html;
}

export function setText(id, text) {
  const el = document.getElementById(id);
  if (el) el.textContent = text;
}

// 5.2 状态diff：仅在文本变化时更新 DOM，避免无谓重绘
export function patchText(id, newText) {
  const el = document.getElementById(id);
  if (!el) return false;
  const text = String(newText ?? '');
  if (el.textContent !== text) {
    el.textContent = text;
    return true;
  }
  return false;
}

export function toggleClass(id, className, enabled) {
  const el = document.getElementById(id);
  if (el) el.classList.toggle(className, enabled);
}

export function delegate(root, eventName, selector, handler) {
  root.addEventListener(eventName, (event) => {
    const target = event.target.closest(selector);
    if (target && root.contains(target)) {
      handler(event, target);
    }
  });
}

// 创建一个短暂提示的浮层
export function flashHint(message, duration = 1800) {
  let hint = document.getElementById('__vup_flash_hint');
  if (!hint) {
    hint = document.createElement('div');
    hint.id = '__vup_flash_hint';
    hint.className = 'vup-flash-hint';
    document.body.appendChild(hint);
  }
  hint.textContent = message;
  hint.classList.add('visible');
  clearTimeout(hint._timer);
  hint._timer = setTimeout(() => hint.classList.remove('visible'), duration);
}
