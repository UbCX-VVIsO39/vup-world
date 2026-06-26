// escape-html.js - HTML转义工具
// Interface: escapeHtml(value) -> string
// 规则: 所有后端文本默认escape，trustedHtml()只用于前端模板

export function escapeHtml(value) {
  if (value == null) return '';
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// 兼容旧代码
export const html = escapeHtml;
