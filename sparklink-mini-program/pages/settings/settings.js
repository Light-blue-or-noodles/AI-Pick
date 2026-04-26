// pages/settings/settings.js
const app = getApp();
const IMService = require('../../utils/im');

/** 清缓存时保留的本地键（登录态、资料、IM、隐私设置页持久化） */
const STORAGE_KEYS_KEEP_ON_CLEAR_CACHE = new Set([
  'token',
  'userId',
  'userInfo',
  'userNickname',
  'userAvatar',
  'isLoggedIn',
  'imUserSig',
  'imUserID',
  'imSdkAppId',
  'privacySettings'
]);

Page({
  data: {
    // Account settings
    phone: '',
    wechatBound: true,
    
    // Notification settings
    messageNotification: true,
    newFollowerNotification: true,
    
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

  // Clear cache（保留登录与账号相关 storage，仅删临时数据如 AI session、筛选条件等）
  onClearCache() {
    wx.showModal({
      title: '提示',
      content:
        '将清除筛选条件、AI/聊天会话缓存等临时数据，登录状态会保留。确定继续吗？',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        try {
          const info = wx.getStorageInfoSync();
          const keys = info.keys || [];
          keys.forEach((key) => {
            if (!STORAGE_KEYS_KEEP_ON_CLEAR_CACHE.has(key)) {
              wx.removeStorageSync(key);
            }
          });
          try {
            const g = getApp();
            if (g && g.globalData && Array.isArray(g.globalData._networkTrace)) {
              g.globalData._networkTrace = [];
            }
          } catch (e) {
            /* ignore */
          }
          wx.showToast({ title: '缓存已清理', icon: 'success' });
        } catch (e) {
          console.warn('onClearCache', e);
          wx.showToast({ title: '清理失败', icon: 'none' });
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
          IMService.logout().catch(() => {});
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