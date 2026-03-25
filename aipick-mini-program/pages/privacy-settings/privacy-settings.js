// pages/privacy-settings/privacy-settings.js
const app = getApp();

Page({
  data: {
    // Chat permission
    chatPermission: 'all',  // all, friends, none
    chatPermissionOptions: ['全部', '好友', '不允许'],
    chatPermissionValues: ['all', 'friends', 'none'],
    
    // Profile visibility
    profileVisibility: 'friends',  // all, friends, none
    profileVisibilityOptions: ['全部', '好友', '仅自己'],
    profileVisibilityValues: ['all', 'friends', 'none'],
    
    // Activity visibility
    activityVisibility: 'all',  // all, friends, none
    activityVisibilityOptions: ['全部', '好友', '仅自己'],
    activityVisibilityValues: ['all', 'friends', 'none'],
    
    // Searchable
    searchable: true,
    
    // Show online status
    showOnlineStatus: true,
    
    // Show last seen
    showLastSeen: true
  },

  onLoad() {
    this.loadPrivacySettings();
  },

  // Load privacy settings from storage
  loadPrivacySettings() {
    const settings = wx.getStorageSync('privacySettings') || {};
    
    this.setData({
      chatPermission: settings.chatPermission || 'all',
      profileVisibility: settings.profileVisibility || 'friends',
      activityVisibility: settings.activityVisibility || 'all',
      searchable: settings.searchable !== false,
      showOnlineStatus: settings.showOnlineStatus !== false,
      showLastSeen: settings.showLastSeen !== false
    });
  },

  // Save privacy settings
  savePrivacySettings() {
    const settings = {
      chatPermission: this.data.chatPermission,
      profileVisibility: this.data.profileVisibility,
      activityVisibility: this.data.activityVisibility,
      searchable: this.data.searchable,
      showOnlineStatus: this.data.showOnlineStatus,
      showLastSeen: this.data.showLastSeen
    };
    wx.setStorageSync('privacySettings', settings);
  },

  // Set chat permission
  onSetChatPermission(e) {
    const index = e.detail.value;
    const value = this.data.chatPermissionValues[index];
    this.setData({ chatPermission: value });
    this.savePrivacySettings();
    
    wx.showToast({
      title: `已设置为：${this.data.chatPermissionOptions[index]}`,
      icon: 'none'
    });
  },

  // Set profile visibility
  onSetProfileVisibility(e) {
    const index = e.detail.value;
    const value = this.data.profileVisibilityValues[index];
    this.setData({ profileVisibility: value });
    this.savePrivacySettings();
    
    wx.showToast({
      title: `已设置为：${this.data.profileVisibilityOptions[index]}`,
      icon: 'none'
    });
  },

  // Set activity visibility
  onSetActivityVisibility(e) {
    const index = e.detail.value;
    const value = this.data.activityVisibilityValues[index];
    this.setData({ activityVisibility: value });
    this.savePrivacySettings();
    
    wx.showToast({
      title: `已设置为：${this.data.activityVisibilityOptions[index]}`,
      icon: 'none'
    });
  },

  // Toggle searchable
  onToggleSearchable(e) {
    const value = e.detail.value;
    this.setData({ searchable: value });
    this.savePrivacySettings();
    
    wx.showToast({
      title: value ? '已开启' : '已关闭',
      icon: 'success'
    });
  },

  // Toggle online status
  onToggleOnlineStatus(e) {
    const value = e.detail.value;
    this.setData({ showOnlineStatus: value });
    this.savePrivacySettings();
    
    wx.showToast({
      title: value ? '已开启' : '已关闭',
      icon: 'success'
    });
  },

  // Toggle last seen
  onToggleLastSeen(e) {
    const value = e.detail.value;
    this.setData({ showLastSeen: value });
    this.savePrivacySettings();
    
    wx.showToast({
      title: value ? '已开启' : '已关闭',
      icon: 'success'
    });
  },

  // View blocked users
  onViewBlockedUsers() {
    wx.showToast({ title: '功能开发中', icon: 'none' });
  },

  // Clear footprints
  onClearFootprints() {
    wx.showModal({
      title: '清除足迹',
      content: '确定清除所有浏览足迹吗？',
      success: (res) => {
        if (res.confirm) {
          wx.showToast({ title: '已清除', icon: 'success' });
        }
      }
    });
  }
});
