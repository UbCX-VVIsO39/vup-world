// accessibility.js - 可访问性焦点管理 (with focus trap)
let previousFocusElement = null;
let focusTrapCleanup = null;

export function openDialog(dialogElement) {
  previousFocusElement = document.activeElement;
  dialogElement.style.display = 'flex';
  dialogElement.setAttribute('role', 'dialog');
  dialogElement.setAttribute('aria-modal', 'true');
  var focusable = getFocusable(dialogElement);
  if (focusable.length) setTimeout(function() { focusable[0].focus(); }, 50);
  // Install focus trap
  installFocusTrap(dialogElement);
}

export function closeDialog(dialogElement) {
  if (focusTrapCleanup) { focusTrapCleanup(); focusTrapCleanup = null; }
  dialogElement.style.display = 'none';
  if (previousFocusElement && previousFocusElement.focus) { previousFocusElement.focus(); previousFocusElement = null; }
}

function installFocusTrap(container) {
  if (focusTrapCleanup) { focusTrapCleanup(); }
  function handler(e) {
    if (e.key !== 'Tab') return;
    var focusable = getFocusable(container);
    if (focusable.length === 0) return;
    var first = focusable[0];
    var last = focusable[focusable.length - 1];
    if (e.shiftKey) {
      if (document.activeElement === first) { e.preventDefault(); last.focus(); }
    } else {
      if (document.activeElement === last) { e.preventDefault(); first.focus(); }
    }
  }
  document.addEventListener('keydown', handler);
  focusTrapCleanup = function() { document.removeEventListener('keydown', handler); };
}

function getFocusable(container) {
  return Array.from(container.querySelectorAll('button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'));
}
