// pages/profile/profile.js
const app = getApp();

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
    userInfo: null,
    stats: {
      partners: 0,
      activities: 0,
      messages: 0
    },
    loadingUser: false,
    loadingStats: false,
    menuItems: [
      { id: 'join-company', icon: '/images/company.png', title: '加入公司', arrow: true },
      { id: 'join-school', icon: '/images/school.png', title: '加入学校', arrow: true },
      { id: 'my-partners', icon: '/images/my-partner.png', title: '我的搭子', arrow: true },
      { id: 'my-activities', icon: '/images/my-activity.png', title: '我的活动', arrow: true },
      { id: 'my-favorites', icon: '/images/favorite.png', title: '我的收藏', arrow: true },
      { id: 'my-posts', icon: '/images/post.png', title: '我的发布', arrow: true },
      { id: 'settings', icon: '/images/settings.png', title: '设置', arrow: true },
      { id: 'about', icon: '/images/about.png', title: '关于我们', arrow: true }
    ]
  },

  onLoad() {
    this.loadUserInfo();
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({
        selected: 4
      });
    }
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
        this.setData({
          userInfo: {
            ...data,
            isLogin: true
          }
        });
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
        this.setData({
          stats: res.data.data || {
            partners: 0,
            activities: 0,
            messages: 0
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
          url: '/pages/my-partners/my-partners'
        });
        break;
      case 'my-activities':
        wx.navigateTo({
          url: '/pages/my-activities/my-activities'
        });
        break;
      case 'my-favorites':
        wx.navigateTo({
          url: '/pages/my-favorites/my-favorites'
        });
        break;
      case 'my-posts':
        wx.navigateTo({
          url: '/pages/my-posts/my-posts'
        });
        break;
      case 'settings':
        wx.navigateTo({
          url: '/pages/settings/settings'
        });
        break;
      case 'about':
        wx.navigateTo({
          url: '/pages/about/about'
        });
        break;
    }
  },

  // 分享
  onShareAppMessage() {
    return {
      title: 'AI-Pick AI - 智能社交搭子平台',
      path: '/pages/index/index'
    };
  }
});