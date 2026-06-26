// api-client.js - 统一API客户端
// Interface: apiGet(path, options?) -> Promise<data>
//           apiPost(path, body?, options?) -> Promise<data>
// 规则: 所有fetch()只在此文件出现

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.status = status;
    this.data = data;
  }
}

async function request(path, options = {}) {
  const { method = 'GET', body, timeout = 30000 } = options;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeout);

  try {
    const fetchOptions = {
      method,
      headers: { 'Content-Type': 'application/json' },
      signal: controller.signal
    };
    if (body && method !== 'GET') {
      fetchOptions.body = JSON.stringify(body);
    }

    const response = await fetch(path, fetchOptions);
    clearTimeout(timer);

    if (!response.ok) {
      let data = null;
      try { data = await response.json(); } catch (e) {}
      throw new ApiError(data?.message || `HTTP ${response.status}`, response.status, data);
    }

    const data = await response.json();
    if (data && data.success === false) {
      throw new ApiError(data.message || '请求失败', response.status, data);
    }
    return data;
  } catch (e) {
    clearTimeout(timer);
    if (e.name === 'AbortError') throw new ApiError('请求超时', 0, null);
    if (e instanceof ApiError) throw e;
    throw new ApiError('网络错误: ' + e.message, 0, null);
  }
}

export function apiGet(path, options = {}) {
  return request(path, { ...options, method: 'GET' });
}

export function apiPost(path, body = {}, options = {}) {
  return request(path, { ...options, method: 'POST', body });
}
