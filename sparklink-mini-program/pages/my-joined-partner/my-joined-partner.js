// pages/my-joined-partner/my-joined-partner.js
const app = getApp();
const { mapPartnerForList } = require('../../utils/partnerListMap.js');

Page({
  data: {
    partners: [],
    isLoading: true
  },

  onLoad() {
    this.fetchList();
  },

  onShow() {
    this.fetchList();
  },

  onPullDownRefresh() {
    this.fetchList().finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  fetchList() {
    const baseUrl = app.globalData.baseUrl || 'https://www.aipick.cloud';
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    if (!token || userId == null || userId === '') {
      this.setData({ partners: [], isLoading: false });
      return Promise.resolve();
    }
    this.setData({ isLoading: true });
    return new Promise((resolve) => {
      wx.request({
        url: `${baseUrl}/api/partner/my/joined`,
        method: 'GET',
        header: {
          Authorization: 'Bearer ' + token,
          'X-User-Id': String(userId)
        },
        success: (res) => {
          const data = res.data;
          if (res.statusCode >= 200 && res.statusCode < 300 && data && (data.code === 0 || data.code === 200)) {
            const raw = Array.isArray(data.data) ? data.data : [];
            const list = raw.map((p) => mapPartnerForList(p, baseUrl));
            this.setData({ partners: list, isLoading: false });
          } else {
            this.setData({ partners: [], isLoading: false });
            wx.showToast({ title: (data && data.message) || '加载失败', icon: 'none' });
          }
          resolve();
        },
        fail: () => {
          this.setData({ partners: [], isLoading: false });
          wx.showToast({ title: '网络错误', icon: 'none' });
          resolve();
        }
      });
    });
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/partner-detail/partner-detail?id=${id}`
    });
  }
});
