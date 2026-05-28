/**
 * 页面级 session 缓存：二次进入先展示缓存，后台静默刷新，避免 0/空列表 跳变
 */

const PREFIX = 'sparklink:page:';

export function getPageCache(key, ttlMs = 5 * 60 * 1000) {
  if (!key || typeof sessionStorage === 'undefined') {
    return null;
  }
  try {
    const raw = sessionStorage.getItem(PREFIX + key);
    if (!raw) {
      return null;
    }
    const { t, data } = JSON.parse(raw);
    if (ttlMs > 0 && Date.now() - t > ttlMs) {
      sessionStorage.removeItem(PREFIX + key);
      return null;
    }
    return data;
  } catch {
    return null;
  }
}

export function setPageCache(key, data) {
  if (!key || typeof sessionStorage === 'undefined') {
    return;
  }
  try {
    sessionStorage.setItem(PREFIX + key, JSON.stringify({ t: Date.now(), data }));
  } catch {
    /* quota */
  }
}

export function removePageCache(key) {
  if (!key || typeof sessionStorage === 'undefined') {
    return;
  }
  try {
    sessionStorage.removeItem(PREFIX + key);
  } catch {
    /* ignore */
  }
}
