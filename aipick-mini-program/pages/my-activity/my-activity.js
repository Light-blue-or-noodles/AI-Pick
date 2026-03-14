// pages/my-activity/my-activity.js
const app = getApp();

Page({
  data: {
    activeTab: 0,
    tabs: ['全部', '我参加的', '我发起的'],
    joinedActivities: [],
    publishedActivities: [],
    allActivities: [],
    isLoading: true
  },

  onLoad() {
    this.fetchAllActivities();
  },

  onShow() {
    if (this.data.activeTab === 1) {
      this.fetchJoinedActivities();
    } else if (this.data.activeTab === 2) {
      this.fetchPublishedActivities();
    } else {
      this.fetchAllActivities();
    }
  },

  // Switch tab
  onSwitchTab(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({ activeTab: index, isLoading: true });
    
    if (index === 0) {
      this.fetchAllActivities();
    } else if (index === 1) {
      this.fetchJoinedActivities();
    } else {
      this.fetchPublishedActivities();
    }
  },

  // Fetch all activities - GET /api/user/activities
  fetchAllActivities() {
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    wx.request({
      url: `${baseUrl}/api/user/activities`,
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + (wx.getStorageSync('token') || '')
      },
      data: { type: 'all' },
      success: (res) => {
        const data = res.data;
        if (res.statusCode === 200 && data && (data.code === 0 || data.code === 200)) {
          const list = data.data != null ? (Array.isArray(data.data) ? data.data : []) : [];
          this.setData({ allActivities: list, isLoading: false });
        } else {
          this.setData({ allActivities: [], isLoading: false });
          wx.showToast({ title: (data && data.message) || '加载失败', icon: 'none' });
        }
      },
      fail: (err) => {
        this.setData({ allActivities: [], isLoading: false });
        wx.showToast({
          title: err.errMsg && err.errMsg.indexOf('url') !== -1 ? '网络错误' : '加载失败',
          icon: 'none'
        });
      }
    });
  },

  // Fetch joined activities
  fetchJoinedActivities() {
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    wx.request({
      url: `${baseUrl}/api/user/activities`,
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + (wx.getStorageSync('token') || '')
      },
      data: { type: 'joined' },
      success: (res) => {
        const data = res.data;
        if (res.statusCode === 200 && data && (data.code === 0 || data.code === 200)) {
          const list = data.data != null ? (Array.isArray(data.data) ? data.data : []) : [];
          this.setData({ joinedActivities: list, isLoading: false });
        } else {
          this.setData({ joinedActivities: [], isLoading: false });
          wx.showToast({ title: (data && data.message) || '加载失败', icon: 'none' });
        }
      },
      fail: (err) => {
        this.setData({ joinedActivities: [], isLoading: false });
        wx.showToast({
          title: err.errMsg && err.errMsg.indexOf('url') !== -1 ? '网络错误' : '加载失败',
          icon: 'none'
        });
      }
    });
  },

  // Fetch published activities
  fetchPublishedActivities() {
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    wx.request({
      url: `${baseUrl}/api/user/activities`,
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + (wx.getStorageSync('token') || '')
      },
      data: { type: 'published' },
      success: (res) => {
        const data = res.data;
        if (res.statusCode === 200 && data && (data.code === 0 || data.code === 200)) {
          const list = data.data != null ? (Array.isArray(data.data) ? data.data : []) : [];
          this.setData({ publishedActivities: list, isLoading: false });
        } else {
          this.setData({ publishedActivities: [], isLoading: false });
          wx.showToast({ title: (data && data.message) || '加载失败', icon: 'none' });
        }
      },
      fail: (err) => {
        this.setData({ publishedActivities: [], isLoading: false });
        wx.showToast({
          title: err.errMsg && err.errMsg.indexOf('url') !== -1 ? '网络错误' : '加载失败',
          icon: 'none'
        });
      }
    });
  },

  // Go to create activity
  onGoToCreate() {
    wx.navigateTo({
      url: '/pages/activity-create/activity-create'
    });
  },

  // View activity detail
  onViewDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/activity-detail/activity-detail?id=${id}`
    });
  },

  // Go back
  onGoBack() {
    wx.navigateBack();
  }
});