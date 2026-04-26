// pages/my-following/my-following.js
const app = getApp();
const { get, post, getApiErrorMessage } = require('../../utils/request');
const { navigateToChatWithPeer } = require('../../utils/navigateToChat.js');

Page({
  data: {
    followingList: [],
    isLoading: true
  },

  onLoad() {
    this.fetchFollowingList();
  },

  onShow() {
    this.fetchFollowingList();
  },

  onPullDownRefresh() {
    this.fetchFollowingList().finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  // Fetch following list - GET /api/user/following
  fetchFollowingList() {
    this.setData({ isLoading: true });
    
    return get('/api/user/following', {})
      .then((result) => {
        // 后端为 MyBatis-Plus IPage：数据在 records，不是数组根
        const page = result && result.data;
        const list = page && Array.isArray(page.records) ? page.records : [];
        this.setData({
          followingList: list,
          isLoading: false
        });
      })
      .catch((err) => {
        this.setData({ followingList: [], isLoading: false });
        console.error('获取关注列表失败', err);
      });
  },

  // View user profile
  onViewProfile(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/user-profile/user-profile?userId=${id}`
    });
  },

  // Toggle follow (unfollow)
  onToggleFollow(e) {
    const id = e.currentTarget.dataset.id;
    const user = this.data.followingList.find(u => u.id === id);
    if (!user) return;

    wx.showModal({
      title: '取消关注',
      content: `确定取消关注 ${user.nickname || '该用户'} 吗？`,
      success: (res) => {
        if (res.confirm) {
          post(`/api/user/${id}/follow`, { action: 'cancel' }, { suppressErrorToast: true })
            .then(() => {
              wx.showToast({ title: '已取消关注', icon: 'success' });
              this.fetchFollowingList();
            })
            .catch((err) => {
              wx.showToast({ title: getApiErrorMessage(err), icon: 'none' });
            });
        }
      }
    });
  },

  // Start chat
  onStartChat(e) {
    const id = e.currentTarget.dataset.id;
    const nickname = e.currentTarget.dataset.nickname || '';
    const avatar = e.currentTarget.dataset.avatar || '';
    if (!id) return;
    navigateToChatWithPeer({ userId: id, nickname, avatar });
  }
});
