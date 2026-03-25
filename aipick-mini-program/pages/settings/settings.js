// pages/settings/settings.js
const app = getApp();

Page({
  data: {
    // Account settings
    phone: '',
    wechatBound: true,
    
    // Notification settings
    messageNotification: true,
    newFollowerNotification: true,
    activityNotification: true,
    
    // Privacy settings
    chatPermission: 'all',  // all, friends, none
    profileVisibility: 'friends'  // all, friends, none
  },

  onLoad() {
    this.loadSettings();
  },

  // Load settings
  loadSettings() {
    const userInfo = wx.getStorageSync('userInfo') || {};
    
    this.setData({
      phone: userInfo.phone || '',
      messageNotification: true,
      newFollowerNotification: true,
      activityNotification: true,
      chatPermission: 'all',
      profileVisibility: 'friends'
    });
  },

  // Go back
  // Edit profile (avatar + nickname 等)
  onEditProfile() {
    wx.navigateTo({
      url: '/pages/profile-edit/profile-edit'
    });
  },

  // Change phone
  onChangePhone() {
    wx.showToast({ title: '功能开发中', icon: 'none' });
  },

  // Change password
  onChangePassword() {
    wx.showToast({ title: '功能开发中', icon: 'none' });
  },

  // Navigate to account security
  onAccountSecurity() {
    wx.navigateTo({
      url: '/pages/account-security/account-security'
    });
  },

  // Navigate to privacy settings
  onPrivacySettings() {
    wx.navigateTo({
      url: '/pages/privacy-settings/privacy-settings'
    });
  },

  // Navigate to about us
  onAboutUs() {
    wx.navigateTo({
      url: '/pages/about-us/about-us'
    });
  },

  // Navigate to my favorites
  onMyFavorites() {
    wx.navigateTo({
      url: '/pages/my-favorites/my-favorites'
    });
  },

  // Navigate to my following
  onMyFollowing() {
    wx.navigateTo({
      url: '/pages/my-following/my-following'
    });
  },

  // Bind WeChat
  onBindWeChat() {
    wx.showToast({ title: '已绑定微信', icon: 'success' });
  },

  // Toggle notification
  onToggleNotification(e) {
    const type = e.currentTarget.dataset.type;
    const value = !this.data[type];
    
    this.setData({
      [type]: value
    });
    
    wx.showToast({
      title: value ? '已开启' : '已关闭',
      icon: 'success'
    });
  },

  // Set chat permission
  onSetChatPermission(e) {
    const value = e.currentTarget.dataset.value;
    this.setData({ chatPermission: value });
  },

  // Set profile visibility
  onSetProfileVisibility(e) {
    const value = e.currentTarget.dataset.value;
    this.setData({ profileVisibility: value });
  },

  // Clear cache
  onClearCache() {
    wx.showModal({
      title: '提示',
      content: '确定要清空缓存吗？',
      success: (res) => {
        if (res.confirm) {
          // Clear cache
          const tempFiles = wx.getStorageInfoSync();
          wx.clearStorageSync();
          
          wx.showToast({ title: '缓存已清空', icon: 'success' });
        }
      }
    });
  },

  // Logout
  onLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          // Clear storage
          wx.clearStorageSync();
          
          wx.showToast({ title: '已退出', icon: 'success' });
          
          setTimeout(() => {
            wx.reLaunch({
              url: '/pages/login/login'
            });
          }, 1500);
        }
      }
    });
  }
});