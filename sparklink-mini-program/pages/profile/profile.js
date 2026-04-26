// pages/profile/profile.js
const app = getApp();
const { resolveMediaUrl } = require('../../utils/mediaUrl.js');

// 带认证头的请求（与 index 一致，避免 401）
function request(options) {
  const header = options.header || {};
  const token = app.globalData.token || wx.getStorageSync('token');
  const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
  if (token) header['Authorization'] = 'Bearer ' + token;
  if (userId) header['X-User-Id'] = String(userId);
  return new Promise((resolve, reject) => {
    wx.request({
      ...options,
      header: { ...header, ...options.header },
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res);
        } else {
          reject(new Error(`请求失败，状态码：${res.statusCode}`));
        }
      },
      fail: (err) => {
        reject(err);
      }
    });
  });
}

Page({
  data: {
    // 避免首屏 userInfo 为 null 时 network-image 未拿到 url；与 profile-edit 用同一套头像 URL 解析
    userInfo: {
      avatar: '/images/default-avatar.png',
      nickname: '',
      bio: '',
      isLogin: false
    },
    stats: {
      partners: 0,
      followers: 0,
      following: 0
    },
    loadingUser: false,
    loadingStats: false,
    menuItems: [
      { id: 'join-company', icon: '/images/company.png', title: '我的公司', arrow: true },
      { id: 'join-school', icon: '/images/school.png', title: '我的学校', arrow: true },
      { id: 'my-partners', icon: '/images/my-partner.png', title: '我发布的搭子', arrow: true },
      { id: 'my-following', title: '我的关注', arrow: true },
      { id: 'my-followers', title: '我的粉丝', arrow: true },
      { id: 'settings', icon: '/images/settings.png', title: '设置', arrow: true }
    ]
  },

  onShow() {
    const that = this;
    setTimeout(function () {
      if (typeof that.getTabBar === 'function') {
        const bar = that.getTabBar();
        if (bar) bar.setData({ selected: 3 });
      }
    }, 0);
    // 只在此处拉用户资料：避免与 onLoad 各调一次 /api/user/info 导致并发、首屏头像竞态
    this.loadUserInfo();
  },

  // 加载用户信息（调用真实 API）
  async loadUserInfo() {
    this.setData({ loadingUser: true });
    try {
      const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
      const res = await request({
        url: `${baseUrl}/api/user/info`,
        method: 'GET'
      });

      if (res.data && res.data.code === 0) {
        const data = res.data.data || {};
        let avatar = data.avatar;
        if (avatar && typeof avatar === 'string' && (avatar.indexOf('__tmp__') !== -1 || avatar.indexOf('://tmp/') !== -1 || (avatar.indexOf('127.0.0.1') !== -1 && avatar.indexOf(':8080') === -1))) {
          avatar = '/images/default-avatar.png';
        }
        const raw = avatar || '/images/default-avatar.png';
        let finalAvatar = raw;
        if (!raw.startsWith('/images/')) {
          finalAvatar = resolveMediaUrl(raw, { baseUrl, kind: 'avatar' });
        }
        if (finalAvatar && finalAvatar.startsWith('http') && /localhost|127\.0\.0\.1/.test(finalAvatar) && app.normalizeImageUrl) {
          finalAvatar = app.normalizeImageUrl(finalAvatar, baseUrl);
        }
        const userInfo = { ...data, avatar: finalAvatar, isLogin: true };
        this.setData({ userInfo });
        wx.setStorageSync('userInfo', userInfo);
      } else {
        // 接口返回非成功码，视为未登录状态
        this.setData({
          userInfo: {
            avatar: '/images/default-avatar.png',
            nickname: '点击登录',
            bio: '设置你的个性签名',
            isLogin: false
          }
        });
        wx.showToast({
          title: '获取用户信息失败',
          icon: 'none'
        });
      }
    } catch (e) {
      // 请求异常，视为未登录状态
      this.setData({
        userInfo: {
          avatar: '/images/default-avatar.png',
          nickname: '点击登录',
          bio: '设置你的个性签名',
          isLogin: false
        }
      });
      wx.showToast({
        title: '网络异常，稍后重试',
        icon: 'none'
      });
    } finally {
      this.setData({ loadingUser: false });
      this.loadStats();
    }
  },

  // 加载统计数据（调用真实 API）
  async loadStats() {
    this.setData({ loadingStats: true });
    try {
      const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
      const res = await request({
        url: `${baseUrl}/api/user/stats`,
        method: 'GET'
      });

      if (res.data && res.data.code === 0) {
        const d = res.data.data || {};
        this.setData({
          stats: {
            partners: d.partners != null ? d.partners : 0,
            followers: d.followers != null ? d.followers : 0,
            following: d.following != null ? d.following : 0
          }
        });
      } else {
        wx.showToast({
          title: '获取统计信息失败',
          icon: 'none'
        });
      }
    } catch (e) {
      wx.showToast({
        title: '统计信息加载失败',
        icon: 'none'
      });
    } finally {
      this.setData({ loadingStats: false });
    }
  },

  // 头像加载失败时回退默认图（如真机网络图片被拦截时）
  onAvatarError() {
    const u = this.data.userInfo || {};
    if (u.avatar && u.avatar !== '/images/default-avatar.png') {
      this.setData({ 'userInfo.avatar': '/images/default-avatar.png' });
    }
  },

  // 点击头像登录
  onAvatarClick() {
    if (!this.data.userInfo || !this.data.userInfo.isLogin) {
      wx.navigateTo({
        url: '/pages/login/login'
      });
    } else {
      wx.navigateTo({
        url: '/pages/profile-edit/profile-edit'
      });
    }
  },

  // 编辑资料
  editProfile() {
    wx.navigateTo({
      url: '/pages/profile-edit/profile-edit'
    });
  },

  // 顶部统计：与下方菜单「我发布的搭子」「我的关注」同路径
  goStatMyPartners() {
    if (!this.data.userInfo || !this.data.userInfo.isLogin) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    wx.navigateTo({ url: '/pages/my-partner/my-partner' });
  },

  goStatFollowing() {
    if (!this.data.userInfo || !this.data.userInfo.isLogin) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    wx.navigateTo({ url: '/pages/my-following/my-following' });
  },

  goStatFollowers() {
    if (!this.data.userInfo || !this.data.userInfo.isLogin) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    wx.navigateTo({ url: '/pages/my-followers/my-followers' });
  },

  // 菜单点击
  onMenuItemClick(e) {
    const id = e.currentTarget.dataset.id;
    
    // 检查登录
    if (!this.data.userInfo.isLogin) {
      wx.navigateTo({
        url: '/pages/login/login'
      });
      return;
    }

    switch (id) {
      case 'join-company':
        wx.navigateTo({
          url: '/pages/company/join/company-join'
        });
        break;
      case 'join-school':
        wx.navigateTo({
          url: '/pages/school/join/school-join'
        });
        break;
      case 'my-partners':
        wx.navigateTo({
          url: '/pages/my-partner/my-partner'
        });
        break;
      case 'my-following':
        wx.navigateTo({
          url: '/pages/my-following/my-following'
        });
        break;
      case 'my-followers':
        wx.navigateTo({
          url: '/pages/my-followers/my-followers'
        });
        break;
      case 'settings':
        wx.navigateTo({
          url: '/pages/settings/settings'
        });
        break;
    }
  },

  // 分享
  onShareAppMessage() {
    return {
      title: 'Spark Link - 智能社交搭子平台',
      path: '/pages/index/index'
    };
  }
});