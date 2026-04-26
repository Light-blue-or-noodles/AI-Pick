/**
 * 媒体 URL 统一解析：后端约定返回相对路径 /static/... 或第三方 https；
 * 本站资源在小程序中用「当前 baseUrl + /api + 路径」访问，避免本机 IP 变化导致图片失效。
 */

function getBaseUrl(explicit) {
  if (explicit) {
    return String(explicit).replace(/\/$/, '');
  }
  try {
    const app = typeof getApp === 'function' ? getApp() : null;
    return (app && app.globalData && app.globalData.baseUrl
      ? String(app.globalData.baseUrl)
      : 'https://www.aipick.cloud').replace(/\/$/, '');
  } catch (e) {
    return 'https://www.aipick.cloud';
  }
}

/**
 * 将任意后端返回的图片引用解析为可请求地址（一般为 http(s)）
 * @param {string} input 相对路径、完整 URL、或历史带 IP 的 URL
 * @param {{ baseUrl?: string, kind?: 'avatar'|'cover'|'general' }} options
 */
function resolveMediaUrl(input, options) {
  const opts = options || {};
  const baseUrl = getBaseUrl(opts.baseUrl);
  const kind = opts.kind || 'general';

  if (input == null || String(input).trim() === '') {
    return defaultSrcForKind(kind);
  }
  let s = String(input).trim();

  if (s.startsWith('/images/') || s.startsWith('data:')) {
    return s;
  }

  if (s.startsWith('http://') || s.startsWith('https://')) {
    try {
      const u = new URL(s);
      const path = u.pathname || '';
      if (path.indexOf('/static/') >= 0 || path.indexOf('/api/static/') >= 0) {
        let p = path;
        if (p.indexOf('/api/') === 0) {
          p = p.slice(4);
        }
        if (!p.startsWith('/')) {
          p = '/' + p;
        }
        return baseUrl + '/api' + p;
      }
    } catch (e) {
      // ignore
    }
    if (/localhost|127\.0\.0\.1/.test(s)) {
      return s.replace(/https?:\/\/[^/]+/, baseUrl);
    }
    return s;
  }

  let path = s.startsWith('/') ? s : '/' + s;
  if (path.indexOf('/api/') === 0) {
    return baseUrl + path;
  }
  return baseUrl + '/api' + path;
}

function defaultSrcForKind(kind) {
  if (kind === 'avatar') {
    return '/images/default-avatar.png';
  }
  if (kind === 'cover') {
    return '/images/partner-banner.jpg';
  }
  return '/images/default-avatar.png';
}

module.exports = {
  getBaseUrl,
  resolveMediaUrl,
  defaultSrcForKind
};
