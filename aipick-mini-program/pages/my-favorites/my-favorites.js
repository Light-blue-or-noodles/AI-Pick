// pages/my-favorites/my-favorites.js
const app = getApp();
const { get, post } = require('../../utils/request');

function toCoverUrl(coverImage) {
  if (!coverImage) return '/images/activity-banner.jpg';
  if (coverImage.startsWith('http')) return coverImage;
  const base = app.globalData.baseUrl || 'http://localhost:8080';
  return base + '/api' + (coverImage.startsWith('/') ? coverImage : '/' + coverImage);
}

function mapActivityCover(list) {
  return (list || []).map(a => ({ ...a, cover: toCoverUrl(a.coverImage) }));
}

Page({
  data: {
    favoriteActivities: [],
    isLoading: true
  },

  onLoad() {
    this.fetchFavoriteActivities();
  },

  onShow() {
    this.fetchFavoriteActivities();
  },

  // Fetch favorite activities - GET /api/activity/favorites
  fetchFavoriteActivities() {
    this.setData({ isLoading: true });
    
    get('/api/activity/favorites', {})
      .then((res) => {
        const list = res.data != null ? (Array.isArray(res.data) ? res.data : []) : [];
        this.setData({ 
          favoriteActivities: mapActivityCover(list), 
          isLoading: false 
        });
      })
      .catch((err) => {
        this.setData({ favoriteActivities: [], isLoading: false });
        console.error('获取收藏活动失败', err);
      });
  },

  // View activity detail
  onViewActivityDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/activity-detail/activity-detail?id=${id}`
    });
  },

  // Cancel favorite activity
  onCancelActivityFavorite(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '取消收藏',
      content: '确定取消收藏该活动吗？',
      success: (res) => {
        if (res.confirm) {
          post(`/api/activity/${id}/favorite`, { action: 'cancel' })
            .then(() => {
              wx.showToast({ title: '已取消收藏', icon: 'success' });
              this.fetchFavoriteActivities();
            })
            .catch(() => {
              wx.showToast({ title: '操作失败', icon: 'none' });
            });
        }
      }
    });
  }
});
