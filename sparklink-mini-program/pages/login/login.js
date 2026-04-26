// pages/login/login.js
const app = getApp();
const envConfig = require('../../config/env.js');

Page({
  data: {
    agreeProtocol: true,
    isLoading: false
  },

  onLoad() {
    // 不再在 onLoad 时根据 token 自动跳转首页，避免用户点击「登录」进入本页时闪屏后立刻被切走；
    // 用户进入登录页后始终展示登录表单，登录成功后再跳转。
  },

  /**
   * 本机已有有效会话时进入首页并拉资料；不在 app.onLaunch 里请求 /api/user/info，避免未点登录就拉错资料。
   */
  onShow() {
    const token = wx.getStorageSync('token');
    const userId = wx.getStorageSync('userId');
    if (!token || userId == null || userId === '' || !wx.getStorageSync('isLoggedIn')) {
      return;
    }
    app.globalData.token = token;
    app.globalData.userId = userId;
    // 须等 /api/user/info 成功再进首页：库重置后本地仍为旧 userId 时，应先清会话并留在登录页
    app.getUserInfo().then((ok) => {
      if (ok) {
        wx.switchTab({ url: '/pages/index/index' });
      }
    });
  },

  // WeChat one-click login（同一次点击内拉头像昵称，便于写入库并 account_import 到腾讯云 IM）
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

    const afterProfile = (userInfo) => {
      wx.login({
        success: (res) => {
          if (res.code) {
            this.loginWithCode(res.code, userInfo || null);
          } else {
            this.handleLoginError('获取登录凭证失败');
          }
        },
        fail: () => {
          this.handleLoginError('登录失败');
        }
      });
    };

    wx.getUserProfile({
      desc: '用于完善资料与聊天头像展示',
      success: (res) => afterProfile(res.userInfo),
      fail: () => afterProfile(null)
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

  persistSession(payload, baseUrl) {
    if (payload.token) {
      wx.setStorageSync('token', payload.token);
      app.globalData.token = payload.token;
    }
    if (payload.userId != null && payload.userId !== '') {
      wx.setStorageSync('userId', payload.userId);
      app.globalData.userId = payload.userId;
    }
    if (payload.nickname) {
      wx.setStorageSync('userNickname', payload.nickname);
    }
    if (payload.avatar) {
      const av = app.normalizeImageUrl
        ? app.normalizeImageUrl(payload.avatar, baseUrl)
        : payload.avatar;
      wx.setStorageSync('userAvatar', av || payload.avatar);
    }
    if (payload.userInfo) {
      wx.setStorageSync('userInfo', payload.userInfo);
    }
    wx.setStorageSync('isLoggedIn', true);
  },

  // Login with code (WeChat mini program login)；wxUserInfo 为 getUserProfile 的 userInfo，可选
  loginWithCode(code, wxUserInfo) {
    wx.showLoading({ title: '登录中...' });

    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    const data = { code };
    /* 不传微信占位昵称「微信用户」，避免服务端把库里已改好的昵称覆盖掉；头像仍可单独同步 */
    if (wxUserInfo && (wxUserInfo.nickName || wxUserInfo.avatarUrl)) {
      const nick = (wxUserInfo.nickName && String(wxUserInfo.nickName).trim()) || '';
      const isPlaceholderNick = nick === '微信用户';
      data.userInfo = {};
      if (nick && !isPlaceholderNick) {
        data.userInfo.nickname = nick;
      }
      if (wxUserInfo.avatarUrl) {
        data.userInfo.avatar = wxUserInfo.avatarUrl;
      }
      if (typeof wxUserInfo.gender === 'number') {
        data.userInfo.gender = wxUserInfo.gender;
      }
      if (Object.keys(data.userInfo).length === 0) {
        delete data.userInfo;
      }
    }
    wx.request({
      url: `${baseUrl}/api/user/wechat-login`,
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data,
      success: (res) => {
        wx.hideLoading();
        const data = res.data;
        if (res.statusCode === 200 && data && (data.code === 0 || data.code === 200)) {
          const payload = data.data || data;
          this.persistSession(payload, baseUrl);
          app.getUserInfo();
          this.setData({ isLoading: false });
          this.goToHome();
        } else {
          const msg = (data && (data.message || data.msg)) || '登录失败';
          if (envConfig.allowWechatLoginTestFallback) {
            console.warn('登录失败，使用测试账号', data);
            this.useTestAccount();
          } else {
            this.handleLoginError(msg);
          }
        }
      },
      fail: (err) => {
        wx.hideLoading();
        if (envConfig.allowWechatLoginTestFallback) {
          console.warn('网络错误，使用测试账号', err);
          this.useTestAccount();
        } else {
          this.handleLoginError('网络异常，请检查后端与合法域名');
        }
      }
    });
  },

  /**
   * 开发环境：优先请求服务端签发真实 JWT（否则 Invalid JWT 会导致 /api/im/usersig 401，无法拿 userSig）
   */
  useTestAccount() {
    wx.showLoading({ title: '测试登录中...' });
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    wx.request({
      url: `${baseUrl}/api/user/test-login`,
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data: {},
      success: (res) => {
        wx.hideLoading();
        const data = res.data;
        if (res.statusCode === 200 && data && data.code === 0 && data.data) {
          this.persistSession(data.data, baseUrl);
          app.getUserInfo();
          this.setData({ isLoading: false });
          wx.showToast({
            title: '测试账号已登录',
            icon: 'none'
          });
          this.goToHome();
        } else {
          this.fallbackOfflineTestAccount(baseUrl);
        }
      },
      fail: () => {
        wx.hideLoading();
        this.fallbackOfflineTestAccount(baseUrl);
      }
    });
  },

  fallbackOfflineTestAccount(baseUrl) {
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
      title: '离线测试账号：IM 无法拉取凭证',
      icon: 'none',
      duration: 2800
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