const PROD_BASE = import.meta.env.VITE_API_BASE_URL || 'https://www.aipick.cloud';

/** 开发环境走 Vite 代理（空字符串 = 同源 /api） */
export function getApiBaseUrl() {
  if (import.meta.env.DEV) {
    const env = import.meta.env.VITE_API_BASE_URL;
    if (env !== undefined && env !== '') {
      return String(env).replace(/\/$/, '');
    }
    return '';
  }
  return String(PROD_BASE).replace(/\/$/, '');
}

export default {
  getApiBaseUrl,
  appName: 'Spark Link'
};
