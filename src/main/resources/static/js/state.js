// state.js - 全局状态管理
// Interface: getState(), patchState(partial), resetState(next), subscribe(listener)
// 规则: 只有此模块持有全局状态，其他模块通过getState()读取

// If app.js already created window.state (via `var state = {...}`), adopt it
// as the backing store so both code paths share the same object.
let currentState = (typeof window !== 'undefined' && window.state != null && typeof window.state === 'object')
  ? window.state
  : {};
let previousState = {};
const listeners = [];

export function getState() { return currentState; }
export function getPreviousState() { return previousState; }

// Keys known to contain nested objects that need deep merging
const DEEP_MERGE_KEYS = new Set(['vup']);

function deepClone(obj) {
  if (obj === null || typeof obj !== 'object') return obj;
  if (Array.isArray(obj)) return obj.map(deepClone);
  const clone = {};
  for (const key of Object.keys(obj)) {
    clone[key] = deepClone(obj[key]);
  }
  return clone;
}

function deepMerge(target, source) {
  for (const key of Object.keys(source)) {
    if (source[key] && typeof source[key] === 'object' && !Array.isArray(source[key])
        && target[key] && typeof target[key] === 'object' && !Array.isArray(target[key])) {
      target[key] = deepMerge(target[key], source[key]);
    } else {
      target[key] = source[key] !== undefined ? deepClone(source[key]) : target[key];
    }
  }
  return target;
}

export function patchState(partial) {
  previousState = { ...currentState };
  if (partial && typeof partial === 'object') {
    for (const key of Object.keys(partial)) {
      if (DEEP_MERGE_KEYS.has(key) && partial[key] && typeof partial[key] === 'object'
          && currentState[key] && typeof currentState[key] === 'object') {
        currentState[key] = deepMerge(currentState[key], partial[key]);
      } else {
        currentState[key] = partial[key] !== undefined ? (typeof partial[key] === 'object' ? deepClone(partial[key]) : partial[key]) : currentState[key];
      }
    }
  } else {
    currentState = partial;
  }
  listeners.forEach(fn => { try { fn(currentState, previousState); } catch (e) { console.error(e); } });
}

export function resetState(next) {
  previousState = { ...currentState };
  currentState = next || {};
  listeners.forEach(fn => { try { fn(currentState, previousState); } catch (e) { console.error(e); } });
}

export function subscribe(listener) {
  listeners.push(listener);
  return () => { const i = listeners.indexOf(listener); if (i >= 0) listeners.splice(i, 1); };
}

// 5.2 状态diff：按 key 订阅，只在指定字段变化时触发
export function subscribeKey(key, listener) {
  const wrapped = (curr, prev) => {
    if (curr[key] !== prev[key]) {
      try { listener(curr[key], prev[key]); } catch (e) { console.error(e); }
    }
  };
  return subscribe(wrapped);
}

// 计算两次状态之间变化的 key 列表
export function getChangedKeys(curr, prev) {
  if (!prev || typeof prev !== 'object') return Object.keys(curr || {});
  const keys = new Set([...Object.keys(curr || {}), ...Object.keys(prev)]);
  return [...keys].filter(k => curr[k] !== prev[k]);
}

// 兼容旧代码：同步window.state
function defineWindowBridge(name, descriptor) {
  const existing = Object.getOwnPropertyDescriptor(window, name);
  if (existing && !existing.configurable) {
    return false;
  }
  try {
    Object.defineProperty(window, name, { ...descriptor, configurable: true });
    return true;
  } catch (error) {
    console.warn(`Could not bind window.${name}:`, error);
    return false;
  }
}

function assignWindowValue(name, value) {
  try {
    window[name] = value;
  } catch (error) {
    console.warn(`Could not assign window.${name}:`, error);
  }
}

export function syncToWindow() {
  const stateDefined = defineWindowBridge('state', {
    get: () => currentState,
    set: (v) => patchState(v)
  });
  if (!stateDefined && window.state !== currentState) {
    assignWindowValue('state', currentState);
  }

  const previousStatsDefined = defineWindowBridge('previousStats', {
    get: () => previousState
  });
  if (!previousStatsDefined && window.previousStats !== previousState) {
    assignWindowValue('previousStats', previousState);
  }
}
