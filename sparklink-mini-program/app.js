// app.js
const envConfig = require('./config/env.js');
const IMService = require('./utils/im');

// 局域网真机调试：仅开发版生效；体验版/正式版始终用 config/env.js 的 production（云上 HTTPS）
// USE_LAN=true 并填本机 IPv4；ERR_CONNECTION_REFUSED 时检查本机 8080 与同一 Wi‑Fi
const USE_LAN = true;
const LAN_IP = '192.168.1.173';

function getBaseUrl() {
  const isDevelop =
    typeof __wxConfig !== 'undefined' && __wxConfig.envVersion === 'develop';
  if (isDevelop && USE_LAN && LAN_IP) {
    return `http://${LAN_IP}:8080`;
  }
  return envConfig.baseUrl;
}

const mediaUrl = require('./utils/mediaUrl.js');

/**
 * 将图片 URL 转为当前环境可访问的地址（与 utils/mediaUrl.resolveMediaUrl 一致，供旧代码调用）
 */
function normalizeImageUrl(url, baseUrl) {
  if (!url || typeof url !== 'string') return url;
  return mediaUrl.resolveMediaUrl(url, { baseUrl: baseUrl || getBaseUrl(), kind: 'general' });
}

function safeUpdateTabBarUnread(count) {
  const n = Math.max(0, Math.floor(Number(count) || 0));
  try {
    const pages = getCurrentPages();
    for (let i = pages.length - 1; i >= 0; i--) {
      const p = pages[i];
      if (p && typeof p.getTabBar === 'function') {
        const bar = p.getTabBar();
        if (bar && typeof bar.applyImUnreadBadge === 'function') {
          bar.applyImUnreadBadge(n);
          break;
        }
      }
    }
  } catch (e) {
    console.warn('updateTabBarUnread', e);
  }
}

App({
  globalData: {
    userInfo: null,
    baseUrl: getBaseUrl(),
    token: null,
    imUnreadCount: 0
  },

  normalizeImageUrl,

  /**
   * 同步腾讯云 IM 未读总数到全局与自定义 TabBar（消息 Tab）
   */
  updateImUnreadBadge(count) {
    const n = Math.max(0, Math.floor(Number(count) || 0));
    this.globalData.imUnreadCount = n;
    safeUpdateTabBarUnread(n);
  },

  onLaunch() {
    // 检查登录状态
    this.checkLoginStatus();
  },

  /**
   * 从后台回到前台时校验会话（节流）：库重置后本地仍为旧 userId 时，尽早走 /api/user/info 触发 clearLoginState。
   */
  onShow() {
    const token = wx.getStorageSync('token');
    const userId = wx.getStorageSync('userId');
    if (!token || userId == null || userId === '') {
      return;
    }
    const now = Date.now();
    if (this._lastSessionCheckAt && now - this._lastSessionCheckAt < 20000) {
      return;
    }
    this._lastSessionCheckAt = now;
    this.globalData.token = token;
    this.globalData.userId = userId;
    this.getUserInfo();
  },

  /**
   * 会话失效或用户已在服务端不存在（如库重置后本地仍为旧 userId）时统一清理，避免 IM/接口继续用幽灵账号。
   */
  clearLoginState() {
    IMService.logout().catch(() => {});
    this.globalData.token = null;
    this.globalData.userId = null;
    this.globalData.userInfo = null;
    this.globalData.imUnreadCount = 0;
    try {
      wx.removeStorageSync('token');
      wx.removeStorageSync('userId');
      wx.removeStorageSync('userInfo');
      wx.removeStorageSync('userNickname');
      wx.removeStorageSync('userAvatar');
      wx.removeStorageSync('isLoggedIn');
      wx.removeStorageSync('imUserSig');
      wx.removeStorageSync('imUserID');
      wx.removeStorageSync('imSdkAppId');
    } catch (e) {
      console.warn('clearLoginState', e);
    }
  },

  checkLoginStatus() {
    const token = wx.getStorageSync('token');
    const userId = wx.getStorageSync('userId');
    if (token && userId) {
      this.globalData.token = token;
      this.globalData.userId = userId;
    } else {
      this.globalData.imUnreadCount = 0;
    }
    // 不在此请求 /api/user/info：首屏为登录页时，用户未点击登录即拉取会污染展示；登录成功后再拉取（见 login 页 persistSession 后）。
  },

  /**
   * 拉取当前用户信息并写入 globalData / storage。
   * @returns {Promise<boolean>} 是否拉取成功（token 无效、用户不存在、网络失败均为 false）
   */
  getUserInfo() {
    if (!this.globalData.token || !this.globalData.userId) {
      return Promise.resolve(false);
    }
    return new Promise((resolve) => {
      wx.request({
        url: `${this.globalData.baseUrl}/api/user/info`,
        method: 'GET',
        header: {
          'Authorization': `Bearer ${this.globalData.token}`,
          'X-User-Id': String(this.globalData.userId)
        },
        success: (res) => {
          const body = res.data || {};
          const msg = body.message || body.msg || '';
          if (res.statusCode === 401 || body.code === 401) {
            this.clearLoginState();
            resolve(false);
            return;
          }
          if (
            res.statusCode === 400 ||
            body.code === 400
          ) {
            if (msg.indexOf('用户不存在') !== -1 && msg.indexOf('对方') === -1) {
              this.clearLoginState();
              wx.showToast({
                title: '账号已失效，请重新登录',
                icon: 'none',
                duration: 2500
              });
              resolve(false);
              return;
            }
          }
          if (body.code === 0) {
            const d = body.data || {};
            this.globalData.userInfo = d;
            try {
              wx.setStorageSync('userInfo', { ...d, isLogin: true });
            } catch (e) {
              console.warn('setStorageSync userInfo', e);
            }
            resolve(true);
            return;
          }
          resolve(false);
        },
        fail: () => {
          console.log('获取用户信息失败');
          resolve(false);
        }
      });
    });
  },
  
  // 登录
  login(code) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${this.globalData.baseUrl}/api/auth/login`,
        method: 'POST',
        data: { code },
        success: (res) => {
          if (res.data.code === 0) {
            const data = res.data.data;
            this.globalData.token = data.token;
            wx.setStorageSync('token', data.token);
            if (data.userId != null) {
              this.globalData.userId = data.userId;
              wx.setStorageSync('userId', data.userId);
            }
            resolve(res.data);
          } else {
            reject(res.data);
          }
        },
        fail: reject
      });
    });
  },
  
  // 检查权限
  checkAuth(callback) {
    if (!this.globalData.token) {
      wx.navigateTo({
        url: '/pages/login/login'
      });
      return false;
    }
    return true;
  }
});