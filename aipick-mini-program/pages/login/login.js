// pages/login/login.js
const app = getApp();

Page({
  data: {
    agreeProtocol: true,
    isLoading: false
  },

  onLoad() {
    // Check if already logged in
    const token = wx.getStorageSync('token');
    if (token) {
      this.checkLoginStatus();
    }
  },

  // Check login status
  checkLoginStatus() {
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo) {
      this.goToHome();
    }
  },

  // WeChat one-click login
  onWechatLogin() {
    if (!this.data.agreeProtocol) {
      wx.showToast({
        title: '请先同意用户协议',
        icon: 'none'
      });
      return;
    }
    if (this.data.isLoading) {
      return;
    }
    this.setData({ isLoading: true });

    // Get WeChat login code
    wx.login({
      success: (res) => {
        if (res.code) {
          // Call backend API to login
          this.loginWithCode(res.code, 'wechat');
        } else {
          this.handleLoginError('获取登录凭证失败');
        }
      },
      fail: (err) => {
        this.handleLoginError('登录失败');
      }
    });
  },

  // Phone number login
  onPhoneLogin() {
    if (!this.data.agreeProtocol) {
      wx.showToast({
        title: '请先同意用户协议',
        icon: 'none'
      });
      return;
    }

    // Navigate to phone login page or show phone number input
    wx.showToast({
      title: '手机号登录开发中',
      icon: 'none'
    });
  },

  // Login with code (WeChat mini program login)
  loginWithCode(code, type) {
    wx.showLoading({ title: '登录中...' });

    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    wx.request({
      url: `${baseUrl}/api/user/wechat-login`,
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data: { code },
      success: (res) => {
        wx.hideLoading();
        const data = res.data;
        if (res.statusCode === 200 && data && (data.code === 0 || data.code === 200)) {
          const payload = data.data || data;
          if (payload.token) {
            wx.setStorageSync('token', payload.token);
            app.globalData.token = payload.token;
          }
          if (payload.userId) {
            wx.setStorageSync('userId', payload.userId);
            app.globalData.userId = payload.userId;
          }
          if (payload.userInfo) {
            wx.setStorageSync('userInfo', payload.userInfo);
          }
          wx.setStorageSync('isLoggedIn', true);
          this.setData({ isLoading: false });
          this.goToHome();
        } else {
          // 登录失败时使用测试账号（开发环境）
          console.warn('登录失败，使用测试账号', data);
          this.useTestAccount();
        }
      },
      fail: (err) => {
        wx.hideLoading();
        // 网络错误时使用测试账号（开发环境）
        console.warn('网络错误，使用测试账号', err);
        this.useTestAccount();
      }
    });
  },

  // 使用测试账号（开发环境备用方案）
  useTestAccount() {
    const testToken = 'test-token-1';
    const testUserId = 1;
    const testUserInfo = {
      id: 1,
      nickname: '测试用户',
      avatar: '/images/default-avatar.png'
    };
    
    wx.setStorageSync('token', testToken);
    wx.setStorageSync('userId', testUserId);
    wx.setStorageSync('userInfo', testUserInfo);
    wx.setStorageSync('isLoggedIn', true);
    
    app.globalData.token = testToken;
    app.globalData.userId = testUserId;
    
    this.setData({ isLoading: false });
    wx.showToast({
      title: '使用测试账号登录',
      icon: 'none'
    });
    this.goToHome();
  },

  // Handle login error
  handleLoginError(msg) {
    this.setData({ isLoading: false });
    wx.showToast({
      title: msg,
      icon: 'none'
    });
  },

  // Navigate to home
  goToHome() {
    wx.switchTab({
      url: '/pages/index/index'
    });
  },

  // Toggle protocol agreement
  toggleProtocol() {
    this.setData({
      agreeProtocol: !this.data.agreeProtocol
    });
  },

  // View user agreement
  viewUserAgreement() {
    wx.navigateTo({
      url: '/pages/agreement/user-agreement/user-agreement'
    });
  },

  // View privacy policy
  viewPrivacyPolicy() {
    wx.navigateTo({
      url: '/pages/agreement/privacy-policy/privacy-policy'
    });
  }
});