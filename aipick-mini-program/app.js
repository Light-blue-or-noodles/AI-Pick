// app.js
const envConfig = require('./config/env.js');
const IMService = require('./utils/im');

// 局域网真机调试：改为 true 并填写电脑的局域网 IP
const USE_LAN = true;
const LAN_IP = '192.168.1.173';

function getBaseUrl() {
  // 优先使用局域网配置（真机调试时）
  if (USE_LAN && LAN_IP) return `http://${LAN_IP}:8080`;
  // 使用环境配置
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
  
  getUserInfo() {
    if (!this.globalData.token || !this.globalData.userId) {
      return;
    }
    wx.request({
      url: `${this.globalData.baseUrl}/api/user/info`,
      method: 'GET',
      header: {
        'Authorization': `Bearer ${this.globalData.token}`,
        'X-User-Id': String(this.globalData.userId)
      },
      success: (res) => {
        if (res.statusCode === 401) {
          // token 过期或无效，清除登录状态
          IMService.logout().catch(() => {});
          this.globalData.token = null;
          this.globalData.userId = null;
          this.globalData.userInfo = null;
          wx.removeStorageSync('token');
          wx.removeStorageSync('userId');
          return;
        }
        if (res.data && res.data.code === 0) {
          const d = res.data.data || {};
          this.globalData.userInfo = d;
          try {
            wx.setStorageSync('userInfo', { ...d, isLogin: true });
          } catch (e) {
            console.warn('setStorageSync userInfo', e);
          }
        }
      },
      fail: () => {
        console.log('获取用户信息失败');
      }
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