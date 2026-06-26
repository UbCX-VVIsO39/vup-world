// accessibility.js - 可访问性焦点管理
let previousFocusElement = null;

export function openDialog(dialogElement) {
  previousFocusElement = document.activeElement;
  dialogElement.style.display = 'flex';
  dialogElement.setAttribute('role', 'dialog');
  dialogElement.setAttribute('aria-modal', 'true');
  var focusable = getFocusable(dialogElement);
  if (focusable.length) setTimeout(function() { focusable[0].focus(); }, 50);
}

export function closeDialog(dialogElement) {
  dialogElement.style.display = 'none';
  if (previousFocusElement && previousFocusElement.focus) { previousFocusElement.focus(); previousFocusElement = null; }
}

function getFocusable(container) {
  return Array.from(container.querySelectorAll('button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'));
}
