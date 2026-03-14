// pages/my-partner/my-partner.js
const app = getApp();

Page({
  data: {
    activeTab: 0,
    tabs: ['进行中', '已结束', '我发出的'],
    partners: [],
    myPublished: [],
    isLoading: true
  },

  onLoad() {
    this.fetchMyPartners();
  },

  onShow() {
    // Refresh data when page shows
    if (this.data.activeTab === 2) {
      this.fetchMyPublished();
    }
  },

  // Switch tab
  onSwitchTab(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({ activeTab: index, isLoading: true });
    
    if (index === 0) {
      this.fetchMyPartners();
    } else if (index === 1) {
      this.fetchEndedPartners();
    } else {
      this.fetchMyPublished();
    }
  },

  // Fetch my partners (active) - GET /api/user/partners
  fetchMyPartners() {
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    wx.request({
      url: `${baseUrl}/api/user/partners`,
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + (wx.getStorageSync('token') || '')
      },
      data: { status: 'active' },
      success: (res) => {
        const data = res.data;
        if (res.statusCode === 200 && data && (data.code === 0 || data.code === 200)) {
          const list = data.data != null ? (Array.isArray(data.data) ? data.data : []) : [];
          this.setData({ partners: list, isLoading: false });
        } else {
          this.setData({ partners: [], isLoading: false });
          wx.showToast({ title: (data && data.message) || '加载失败', icon: 'none' });
        }
      },
      fail: (err) => {
        this.setData({ partners: [], isLoading: false });
        wx.showToast({
          title: err.errMsg && err.errMsg.indexOf('url') !== -1 ? '网络错误' : '加载失败',
          icon: 'none'
        });
      }
    });
  },

  // Fetch ended partners
  fetchEndedPartners() {
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    wx.request({
      url: `${baseUrl}/api/user/partners`,
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + (wx.getStorageSync('token') || '')
      },
      data: { status: 'ended' },
      success: (res) => {
        const data = res.data;
        if (res.statusCode === 200 && data && (data.code === 0 || data.code === 200)) {
          const list = data.data != null ? (Array.isArray(data.data) ? data.data : []) : [];
          this.setData({ partners: list, isLoading: false });
        } else {
          this.setData({ partners: [], isLoading: false });
          wx.showToast({ title: (data && data.message) || '加载失败', icon: 'none' });
        }
      },
      fail: (err) => {
        this.setData({ partners: [], isLoading: false });
        wx.showToast({
          title: err.errMsg && err.errMsg.indexOf('url') !== -1 ? '网络错误' : '加载失败',
          icon: 'none'
        });
      }
    });
  },

  // Fetch my published partners
  fetchMyPublished() {
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    wx.request({
      url: `${baseUrl}/api/user/partners`,
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + (wx.getStorageSync('token') || '')
      },
      data: { status: 'published' },
      success: (res) => {
        const data = res.data;
        if (res.statusCode === 200 && data && (data.code === 0 || data.code === 200)) {
          const list = data.data != null ? (Array.isArray(data.data) ? data.data : []) : [];
          this.setData({ myPublished: list, isLoading: false });
        } else {
          this.setData({ myPublished: [], isLoading: false });
          wx.showToast({ title: (data && data.message) || '加载失败', icon: 'none' });
        }
      },
      fail: (err) => {
        this.setData({ myPublished: [], isLoading: false });
        wx.showToast({
          title: err.errMsg && err.errMsg.indexOf('url') !== -1 ? '网络错误' : '加载失败',
          icon: 'none'
        });
      }
    });
  },

  // Go to publish
  onGoToPublish() {
    wx.navigateTo({
      url: '/pages/partner-publish/partner-publish'
    });
  },

  // View partner detail
  onViewDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/partner-detail/partner-detail?id=${id}`
    });
  },

  // Quit partner
  onQuit(e) {
    const id = e.currentTarget.dataset.id;
    
    wx.showModal({
      title: '提示',
      content: '确定要退出该搭子吗？',
      success: (res) => {
        if (res.confirm) {
          wx.showToast({ title: '已退出', icon: 'success' });
          
          // Remove from list
          const partners = this.data.partners.filter(item => item.id !== id);
          this.setData({ partners });
        }
      }
    });
  },

  // Go back
  onGoBack() {
    wx.navigateBack();
  }
});