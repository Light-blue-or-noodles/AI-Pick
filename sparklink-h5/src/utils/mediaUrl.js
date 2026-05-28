import { getApiBaseUrl } from '@/config/env';

/**
 * 将后端相对路径转为可访问的完整 URL
 */
export function resolveMediaUrl(url, options = {}) {
  if (!url || typeof url !== 'string') {
    return options.fallback || '';
  }
  const s = url.trim();
  if (!s || s.startsWith('/images') || s === 'null') {
    return options.fallback || '';
  }
  if (s.startsWith('http://') || s.startsWith('https://')) {
    return s;
  }
  const base = (options.baseUrl || getApiBaseUrl()).replace(/\/$/, '');
  const path = s.startsWith('/') ? s : `/${s}`;
  if (path.startsWith('/api/')) {
    return `${base}${path}`;
  }
  return `${base}/api${path}`;
}

export function normalizeImageUrl(url, baseUrl) {
  return resolveMediaUrl(url, { baseUrl, fallback: '' });
}
