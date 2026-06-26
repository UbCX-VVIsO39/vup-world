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
