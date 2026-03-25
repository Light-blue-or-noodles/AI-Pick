// pages/about-us/about-us.js
const app = getApp();

Page({
  data: {
    version: '1.0.0',
    buildDate: '2026-03-25'
  },

  onLoad() {
    // Get version info from app
    const accountInfo = wx.getAccountInfoSync();
    this.setData({
      version: accountInfo.miniProgram?.version || '1.0.0'
    });
  },

  // Copy contact info
  onCopyContact() {
    wx.setClipboardData({
      data: 'contact@aipick.com',
      success: () => {
        wx.showToast({ title: '已复制', icon: 'success' });
      }
    });
  },

  // Navigate to user agreement
  onViewAgreement() {
    wx.navigateTo({
      url: '/pages/agreement/user-agreement/user-agreement'
    });
  },

  // Navigate to privacy policy
  onViewPrivacy() {
    wx.navigateTo({
      url: '/pages/agreement/privacy-policy/privacy-policy'
    });
  },

  // Check for updates
  onCheckUpdate() {
    wx.showLoading({ title: '检查中...' });
    
    const updateManager = wx.getUpdateManager();
    
    updateManager.onCheckForUpdate((res) => {
      wx.hideLoading();
      if (res.hasUpdate) {
        wx.showModal({
          title: '发现新版本',
          content: '新版本已准备好，是否重启应用？',
          success: (modalRes) => {
            if (modalRes.confirm) {
              updateManager.applyUpdate();
            }
          }
        });
      } else {
        wx.showToast({ title: '当前已是最新版本', icon: 'none' });
      }
    });
  }
});
