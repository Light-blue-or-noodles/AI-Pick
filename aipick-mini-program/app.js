// app.js
// 局域网真机调试：改为 true 并填写电脑的局域网 IP
const USE_LAN = true;
const LAN_IP = '192.168.1.173';

function getBaseUrl() {
  if (USE_LAN && LAN_IP) return `http://${LAN_IP}:8080`;
  return 'http://localhost:8080';
}

const mediaUrl = require('./utils/mediaUrl.js');

/**
 * 将图片 URL 转为当前环境可访问的地址（与 utils/mediaUrl.resolveMediaUrl 一致，供旧代码调用）
 */
function normalizeImageUrl(url, baseUrl) {
  if (!url || typeof url !== 'string') return url;
  return mediaUrl.resolveMediaUrl(url, { baseUrl: baseUrl || getBaseUrl(), kind: 'general' });
}

App({
  globalData: {
    userInfo: null,
    baseUrl: getBaseUrl(),
    token: null
  },

  normalizeImageUrl,

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
      this.getUserInfo();
    }
    // 未登录时不做处理，由页面自行判断
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
          this.globalData.token = null;
          this.globalData.userId = null;
          this.globalData.userInfo = null;
          wx.removeStorageSync('token');
          wx.removeStorageSync('userId');
          return;
        }
        if (res.data && res.data.code === 0) {
          this.globalData.userInfo = res.data.data;
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