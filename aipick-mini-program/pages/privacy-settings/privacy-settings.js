// pages/privacy-settings/privacy-settings.js
Page({
  data: {
    chatPermission: 'all',
    chatPermissionOptions: ['全部', '好友', '不允许'],
    chatPermissionValues: ['all', 'friends', 'none'],

    profileVisibility: 'friends',
    profileVisibilityOptions: ['全部', '好友', '仅自己'],
    profileVisibilityValues: ['all', 'friends', 'none'],

    searchable: true,
    showOnlineStatus: true,
    showLastSeen: true
  },

  onLoad() {
    this.loadPrivacySettings();
  },

  loadPrivacySettings() {
    const settings = wx.getStorageSync('privacySettings') || {};

    this.setData({
      chatPermission: settings.chatPermission || 'all',
      profileVisibility: settings.profileVisibility || 'friends',
      searchable: settings.searchable !== false,
      showOnlineStatus: settings.showOnlineStatus !== false,
      showLastSeen: settings.showLastSeen !== false
    });
  },

  savePrivacySettings() {
    const settings = {
      chatPermission: this.data.chatPermission,
      profileVisibility: this.data.profileVisibility,
      searchable: this.data.searchable,
      showOnlineStatus: this.data.showOnlineStatus,
      showLastSeen: this.data.showLastSeen
    };
    wx.setStorageSync('privacySettings', settings);
  },

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

  onToggleSearchable(e) {
    const value = e.detail.value;
    this.setData({ searchable: value });
    this.savePrivacySettings();

    wx.showToast({
      title: value ? '已开启' : '已关闭',
      icon: 'success'
    });
  },

  onToggleOnlineStatus(e) {
    const value = e.detail.value;
    this.setData({ showOnlineStatus: value });
    this.savePrivacySettings();

    wx.showToast({
      title: value ? '已开启' : '已关闭',
      icon: 'success'
    });
  },

  onToggleLastSeen(e) {
    const value = e.detail.value;
    this.setData({ showLastSeen: value });
    this.savePrivacySettings();

    wx.showToast({
      title: value ? '已开启' : '已关闭',
      icon: 'success'
    });
  },

  onViewBlockedUsers() {
    wx.showToast({ title: '功能开发中', icon: 'none' });
  },

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
