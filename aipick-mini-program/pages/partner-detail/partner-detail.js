// pages/partner-detail/partner-detail.js
const app = getApp();

Page({
  data: {
    partnerId: null,
    partnerInfo: null,
    isLoading: true,
    isFollowing: false,
    showMoreMenu: false
  },

  onLoad(options) {
    const id = options.id || options.partnerId || '';
    const validId = id && String(id).trim() && String(id) !== 'undefined';
    if (validId) {
      this.setData({ partnerId: id });
      this.fetchPartnerDetail(id);
    } else {
      this.setData({ isLoading: false });
      wx.showToast({ title: '参数错误', icon: 'none' });
    }
  },

  onUnload() {
    // Clear timer if any
  },

  // Fetch partner detail - GET /api/partner/:id
  fetchPartnerDetail(id) {
    if (id == null || id === '' || String(id) === 'undefined') {
      this.setData({ isLoading: false });
      return;
    }
    this.setData({ isLoading: true });
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    const header = {};
    if (token) header['Authorization'] = 'Bearer ' + token;
    if (userId) header['X-User-Id'] = String(userId);
    wx.request({
      url: `${baseUrl}/api/partner/${id}`,
      method: 'GET',
      header,
      success: (res) => {
        const data = res.data;
        if (res.statusCode === 200 && data && (data.code === 0 || data.code === 200)) {
          const info = data.data != null ? data.data : data;
          this.setData({
            partnerInfo: info,
            isFollowing: !!info.isFollowing,
            isLoading: false
          });
        } else {
          this.setData({ partnerInfo: null, isLoading: false });
          wx.showToast({ title: (data && data.message) || '加载失败', icon: 'none' });
        }
      },
      fail: (err) => {
        this.setData({ partnerInfo: null, isLoading: false });
        wx.showToast({
          title: err.errMsg && err.errMsg.indexOf('url') !== -1 ? '网络错误' : '加载失败',
          icon: 'none'
        });
      }
    });
  },

  // Go back
  onGoBack() {
    wx.navigateBack();
  },

  // Show more menu
  onShowMore() {
    this.setData({ showMoreMenu: true });
  },

  // Hide more menu
  onHideMore() {
    this.setData({ showMoreMenu: false });
  },

  // Toggle follow
  onToggleFollow() {
    const newStatus = !this.data.isFollowing;
    this.setData({ isFollowing: newStatus });
    
    wx.showToast({
      title: newStatus ? '关注成功' : '取消关注',
      icon: 'success'
    });
  },

  // Start chat
  onStartChat() {
    if (!this.data.partnerInfo) return;
    const nickname = this.data.partnerInfo.nickname || '';
    wx.navigateTo({
      url: `/pages/chat/chat?userId=${this.data.partnerId}&nickname=${encodeURIComponent(nickname)}`
    });
  },

  // Show match score
  onShowMatch() {
    if (!this.data.partnerInfo) return;
    const name = this.data.partnerInfo.nickname || 'TA';
    const score = this.data.partnerInfo.matchScore != null ? this.data.partnerInfo.matchScore : 0;
    wx.showModal({
      title: '匹配度',
      content: `您与 ${name} 的匹配度为 ${score}%`,
      showCancel: false
    });
  },

  // View published partner
  onViewPartner(e) {
    const partnerId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/partner-detail/partner-detail?id=${partnerId}`
    });
  },

  // Report user
  onReport() {
    wx.showActionSheet({
      itemList: ['举报用户', '拉黑用户'],
      success: (res) => {
        if (res.tapIndex === 0) {
          wx.showToast({ title: '举报成功', icon: 'success' });
        } else {
          wx.showToast({ title: '已拉黑', icon: 'success' });
        }
      },
      complete: () => {
        this.onHideMore();
      }
    });
  },

  // Share
  onShareAppMessage() {
    const name = (this.data.partnerInfo && this.data.partnerInfo.nickname) || '搭子';
    return {
      title: `来看看 ${name} 的主页`,
      path: `/pages/partner-detail/partner-detail?id=${this.data.partnerId}`
    };
  }
});