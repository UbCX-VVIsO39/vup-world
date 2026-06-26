// error-recovery.js - 错误恢复
export function showRecoverableError(error, retryCallback) {
  var overlay = document.createElement('div');
  overlay.id = 'errorOverlay';
  overlay.className = 'error-overlay';
  var msg = error && error.message ? error.message : '未知错误';
  overlay.innerHTML = '<div class="error-card"><div class="error-icon">⚠️</div><div class="error-title">出现问题</div><div class="error-message">' + escapeHtml(msg) + '</div><div class="error-actions">' + (retryCallback ? '<button class="error-btn retry" id="errorRetry">重试</button>' : '') + '<button class="error-btn dismiss" id="errorDismiss">关闭</button></div></div>';
  document.body.appendChild(overlay);
  var retryBtn = document.getElementById('errorRetry');
  var dismissBtn = document.getElementById('errorDismiss');
  if (retryBtn) retryBtn.addEventListener('click', function() { overlay.remove(); retryCallback(); });
  if (dismissBtn) dismissBtn.addEventListener('click', function() { overlay.remove(); });
}

function escapeHtml(s) { return String(s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }
