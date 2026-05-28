import axios from 'axios';
import { showToast } from 'vant';
import { getApiBaseUrl } from '@/config/env';
import { KEYS, getItem, removeItem } from './storage';

let sessionClearToastLock = false;

function shouldClearSession(status, body) {
  if (!body && status !== 401) {
    return false;
  }
  const msg = (body && (body.message || body.msg)) || '';
  if (status === 401 || body?.code === 401) {
    return true;
  }
  if ((status === 400 || body?.code === 400) && msg.indexOf('用户不存在') !== -1) {
    if (msg.indexOf('对方') !== -1) {
      return false;
    }
    return true;
  }
  return false;
}

function clearLocalSession() {
  removeItem(KEYS.token);
  removeItem(KEYS.userId);
  removeItem(KEYS.userInfo);
  removeItem(KEYS.userAvatar);
  removeItem(KEYS.userNickname);
  removeItem(KEYS.isLoggedIn);
  removeItem(KEYS.imUserSig);
  removeItem(KEYS.imUserID);
  removeItem(KEYS.imSdkAppId);
}

const http = axios.create({
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

http.interceptors.request.use((config) => {
  const token = getItem(KEYS.token);
  const userId = getItem(KEYS.userId);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  if (userId) {
    config.headers['X-User-Id'] = String(userId);
  }
  const base = config.baseURL != null ? config.baseURL : getApiBaseUrl();
  if (base !== undefined) {
    config.baseURL = base;
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body && body.code === 0) {
      return body;
    }
    const status = response.status;
    if (shouldClearSession(status, body) && !response.config.skipSessionClear) {
      clearLocalSession();
      if (!response.config.suppressSessionToast && !sessionClearToastLock) {
        sessionClearToastLock = true;
        showToast('登录已失效，请重新登录');
        setTimeout(() => {
          sessionClearToastLock = false;
          if (window.location.pathname !== '/login') {
            window.location.href = '/login';
          }
        }, 500);
      }
    }
    const errText = (body && (body.message || body.msg)) || '请求失败';
    if (!response.config.suppressErrorToast) {
      showToast(errText);
    }
    return Promise.reject(body || { message: errText });
  },
  (error) => {
    const status = error.response?.status;
    const body = error.response?.data;
    if (shouldClearSession(status, body) && !error.config?.skipSessionClear) {
      clearLocalSession();
      if (!error.config?.suppressSessionToast) {
        showToast('登录已失效，请重新登录');
      }
    }
    if (!error.config?.suppressErrorToast) {
      showToast('网络请求失败');
    }
    return Promise.reject(error);
  }
);

export function get(url, params, options = {}) {
  return http.get(url, { params, ...options });
}

export function post(url, data, options = {}) {
  return http.post(url, data, options);
}

export function put(url, data, options = {}) {
  return http.put(url, data, options);
}

export function del(url, data, options = {}) {
  return http.delete(url, { data, ...options });
}

export function uploadFile(url, file, fieldName = 'file', options = {}) {
  const form = new FormData();
  form.append(fieldName, file);
  return http.post(url, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    ...options
  });
}

export function getApiErrorMessage(err, fallback = '操作失败') {
  if (err == null) {
    return fallback;
  }
  if (typeof err === 'string') {
    return err || fallback;
  }
  const m = err.message || err.msg;
  return m && String(m).trim() ? String(m).trim() : fallback;
}

export { http, clearLocalSession };
