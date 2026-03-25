// pages/account-security/account-security.js
const app = getApp();

Page({
  data: {
    phone: '',
    email: '',
    wechatBound: true,
    lastLoginTime: '',
    lastLoginDevice: ''
  },

  onLoad() {
    this.loadSecurityInfo();
  },

  onShow() {
    this.loadSecurityInfo();
  },

  // Load security info
  loadSecurityInfo() {
    const userInfo = wx.getStorageSync('userInfo') || {};
    
    this.setData({
      phone: userInfo.phone || '',
      email: userInfo.email || '',
      wechatBound: true,
      lastLoginTime: userInfo.lastLoginTime || '刚刚',
      lastLoginDevice: userInfo.lastLoginDevice || '微信客户端'
    });
  },

  // Change phone
  onChangePhone() {
    wx.navigateTo({
      url: '/pages/profile-edit/profile-edit'
    });
  },

  // Change email
  onChangeEmail() {
    wx.showToast({ title: '功能开发中', icon: 'none' });
  },

  // Change password
  onChangePassword() {
    wx.showModal({
      title: '修改密码',
      content: '修改密码功能需要验证身份，是否继续？',
      success: (res) => {
        if (res.confirm) {
          wx.showToast({ title: '功能开发中', icon: 'none' });
        }
      }
    });
  },

  // Bind/Unbind WeChat
  onToggleWeChat() {
    if (this.data.wechatBound) {
      wx.showModal({
        title: '解绑微信',
        content: '解绑后将无法使用微信快捷登录，确定解绑吗？',
        success: (res) => {
          if (res.confirm) {
            wx.showToast({ title: '功能开发中', icon: 'none' });
          }
        }
      });
    } else {
      wx.showToast({ title: '功能开发中', icon: 'none' });
    }
  },

  // View login devices
  onViewDevices() {
    wx.showToast({ title: '功能开发中', icon: 'none' });
  },

  // Account logout (delete account)
  onDeleteAccount() {
    wx.showModal({
      title: '注销账号',
      content: '注销后所有数据将被删除且无法恢复，确定注销吗？',
      confirmText: '确定注销',
      confirmColor: '#FF3B30',
      success: (res) => {
        if (res.confirm) {
          wx.showModal({
            title: '再次确认',
            content: '请再次确认是否要注销账号，此操作不可撤销！',
            confirmText: '确认注销',
            confirmColor: '#FF3B30',
            success: (confirmRes) => {
              if (confirmRes.confirm) {
                wx.showToast({ title: '功能开发中', icon: 'none' });
              }
            }
          });
        }
      }
    });
  }
});
