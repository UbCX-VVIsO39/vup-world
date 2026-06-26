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

export function patchState(partial) {
  previousState = { ...currentState };
  currentState = { ...currentState, ...partial };
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

// 兼容旧代码：同步window.state
export function syncToWindow() {
  Object.defineProperty(window, 'state', {
    get: () => currentState,
    set: (v) => patchState(v),
    configurable: true
  });
  Object.defineProperty(window, 'previousStats', {
    get: () => previousState,
    configurable: true
  });
}
