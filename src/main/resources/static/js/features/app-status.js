// app-status.js - 加载状态管理
var busyCount = 0;
var statusTimer = null;

export function setBusy(label) {
  busyCount++;
  var el = document.getElementById('appStatus');
  if (el) { el.textContent = label || '处理中...'; el.className = 'app-status busy'; el.style.display = 'block'; }
  document.querySelectorAll('button[type="submit"], .action-btn').forEach(function(btn) { btn.disabled = true; });
}

export function clearBusy() {
  busyCount = Math.max(0, busyCount - 1);
  if (busyCount === 0) {
    var el = document.getElementById('appStatus');
    if (el) el.style.display = 'none';
    document.querySelectorAll('button[type="submit"], .action-btn').forEach(function(btn) { btn.disabled = false; });
  }
}

export function setStatus(message, type) {
  var el = document.getElementById('appStatus');
  if (el) { el.textContent = message; el.className = 'app-status ' + (type||'info'); el.style.display = 'block'; }
  if (statusTimer) clearTimeout(statusTimer);
  if (type !== 'error') statusTimer = setTimeout(function() { clearStatus(); }, 3000);
}

export function clearStatus() {
  if (statusTimer) { clearTimeout(statusTimer); statusTimer = null; }
  var el = document.getElementById('appStatus');
  if (el) el.style.display = 'none';
}
