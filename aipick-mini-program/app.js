// app.js
App({
  globalData: {
    userInfo: null,
    baseUrl: 'http://localhost:8080',
    token: null
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