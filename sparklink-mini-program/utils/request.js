// utils/request.js
// 注意：禁止在模块顶层调用 getApp()。require 顺序可能导致 App() 尚未执行，getApp() 为 undefined。
const envConfig = require('../config/env.js');

function getAppSafe() {
  try {
    return getApp();
  } catch (e) {
    return undefined;
  }
}

function hydrateAuthFromStorage() {
  try {
    const a = getAppSafe();
    if (!a || !a.globalData) {
      return;
    }
    if (!a.globalData.token) {
      const t = wx.getStorageSync('token');
      if (t) {
        a.globalData.token = t;
      }
    }
    if (a.globalData.userId == null || a.globalData.userId === '') {
      const u = wx.getStorageSync('userId');
      if (u != null && u !== '') {
        a.globalData.userId = u;
      }
    }
  } catch (e) {
    console.warn('hydrateAuthFromStorage', e);
  }
}

/** 当前请求用的鉴权与 baseUrl（不依赖模块加载时刻的 getApp 结果） */
function resolveRequestContext() {
  hydrateAuthFromStorage();
  const appInst = getAppSafe();
  const gd = appInst && appInst.globalData ? appInst.globalData : {};
  const token =
    (gd.token != null && gd.token !== '' ? gd.token : null) ||
    wx.getStorageSync('token') ||
    '';
  const rawUid =
    gd.userId != null && gd.userId !== ''
      ? gd.userId
      : wx.getStorageSync('userId');
  const userId = rawUid != null && rawUid !== '' ? rawUid : '';
  const baseUrl =
    (gd.baseUrl && String(gd.baseUrl).trim()) || envConfig.baseUrl || '';
  return { token, userId, baseUrl };
}

// 请求封装
const request = (options) => {
  return new Promise((resolve, reject) => {
    const ctx = resolveRequestContext();
    const header = options.header || {};

    // 添加 token
    if (ctx.token) {
      header['Authorization'] = `Bearer ${ctx.token}`;
    }

    // 添加 X-User-Id
    if (ctx.userId !== '') {
      header['X-User-Id'] = String(ctx.userId);
    }

    // 添加 Content-Type
    if (!header['Content-Type']) {
      header['Content-Type'] = 'application/json';
    }

    const method = (options.method || 'GET').toUpperCase();
    let payload = options.data;
    if (payload == null || payload === undefined) {
      payload = {};
    }
    const ct = String(header['Content-Type'] || '').toLowerCase();
    if (
      method !== 'GET' &&
      ct.includes('application/json') &&
      typeof payload === 'object' &&
      !(payload instanceof ArrayBuffer)
    ) {
      try {
        payload = JSON.stringify(payload);
      } catch (e) {
        console.warn('request JSON.stringify', e);
      }
    }

    const root = options.baseUrl || ctx.baseUrl || envConfig.baseUrl;

    wx.request({
      url: root + options.url,
      method: options.method || 'GET',
      data: payload,
      header: header,
      success: (res) => {
        if (res.data.code === 0) {
          resolve(res.data);
        } else if (res.data.code === 401) {
          // 未登录，跳转登录页
          console.warn('未登录，请先到登录页登录');
          // wx.navigateTo({
          //   url: '/pages/login/login'
          // });
          reject(res.data);
        } else {
          const errText = res.data.message || res.data.msg || '请求失败';
          if (!options.suppressErrorToast) {
            wx.showToast({
              title: errText,
              icon: 'none'
            });
          }
          reject(res.data);
        }
      },
      fail: (err) => {
        wx.showToast({
          title: '网络请求失败',
          icon: 'none'
        });
        reject(err);
      }
    });
  });
};

// GET 请求
const get = (url, data, options = {}) => {
  return request({
    url,
    data,
    method: 'GET',
    ...options
  });
};

// POST 请求
const post = (url, data, options = {}) => {
  return request({
    url,
    data,
    method: 'POST',
    ...options
  });
};

// PUT 请求
const put = (url, data, options = {}) => {
  return request({
    url,
    data,
    method: 'PUT',
    ...options
  });
};

// DELETE 请求
const del = (url, data, options = {}) => {
  return request({
    url,
    data,
    method: 'DELETE',
    ...options
  });
};

/** 从 request reject 的 err 对象取可读文案（与后端 Result message/msg 一致） */
function getApiErrorMessage(err, fallback) {
  const fb = fallback != null ? fallback : '操作失败';
  if (err == null) {
    return fb;
  }
  if (typeof err === 'string') {
    return err || fb;
  }
  const m = err.message != null && String(err.message).trim() !== '' ? err.message : err.msg;
  return m != null && String(m).trim() !== '' ? String(m).trim() : fb;
}

module.exports = {
  request,
  get,
  post,
  put,
  delete: del,
  getApiErrorMessage
};