// pages/my-partner/my-partner.js
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
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    if (!token || userId == null || userId === '') {
      this.setData({ partners: [], isLoading: false });
      return Promise.resolve();
    }
    this.setData({ isLoading: true });
    return new Promise((resolve) => {
      wx.request({
        url: `${baseUrl}/api/partner/my`,
        method: 'GET',
        header: {
          Authorization: 'Bearer ' + token,
          'X-User-Id': String(userId)
        },
        data: { type: 'created' },
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
  },

  noop() {},

  onDeletePartner(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.showModal({
      title: '删除搭子',
      content: '确定删除该搭子？删除后无法恢复。',
      success: (modalRes) => {
        if (!modalRes.confirm) return;
        const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
        const token = app.globalData.token || wx.getStorageSync('token');
        const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
        wx.request({
          url: `${baseUrl}/api/partner/${id}`,
          method: 'DELETE',
          header: {
            Authorization: 'Bearer ' + token,
            'X-User-Id': String(userId)
          },
          success: (res) => {
            const body = res.data;
            if (res.statusCode >= 200 && res.statusCode < 300 && body && (body.code === 0 || body.code === 200)) {
              wx.showToast({ title: '已删除', icon: 'success' });
              this.fetchList();
            } else {
              wx.showToast({ title: (body && body.message) || '删除失败', icon: 'none' });
            }
          },
          fail: () => {
            wx.showToast({ title: '网络错误', icon: 'none' });
          }
        });
      }
    });
  }
});
