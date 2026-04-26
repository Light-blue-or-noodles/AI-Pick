// pages/my-followers/my-followers.js
const app = getApp();
const { get, getApiErrorMessage } = require('../../utils/request');
const { navigateToChatWithPeer } = require('../../utils/navigateToChat.js');

Page({
  data: {
    followersList: [],
    isLoading: true
  },

  onLoad() {
    this.fetchFollowersList();
  },

  onShow() {
    this.fetchFollowersList();
  },

  onPullDownRefresh() {
    this.fetchFollowersList().finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  fetchFollowersList() {
    this.setData({ isLoading: true });
    // 使用与 /api/user/follow/stats 同前缀的路径，避免部分环境将 /user/followers 误路由到「关注列表」
    return get('/api/user/follow/followers', {})
      .then((result) => {
        const page = result && result.data;
        const list = page && Array.isArray(page.records) ? page.records : [];
        this.setData({
          followersList: list,
          isLoading: false
        });
      })
      .catch((err) => {
        this.setData({ followersList: [], isLoading: false });
        console.error('获取粉丝列表失败', err);
        wx.showToast({ title: getApiErrorMessage(err) || '加载失败', icon: 'none' });
      });
  },

  onViewProfile(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/user-profile/user-profile?userId=${id}`
    });
  },

  onStartChat(e) {
    const id = e.currentTarget.dataset.id;
    const nickname = e.currentTarget.dataset.nickname || '';
    const avatar = e.currentTarget.dataset.avatar || '';
    if (!id) return;
    navigateToChatWithPeer({ userId: id, nickname, avatar });
  }
});
