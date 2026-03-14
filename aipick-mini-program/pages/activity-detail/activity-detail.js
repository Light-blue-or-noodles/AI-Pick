// pages/activity-detail/activity-detail.js
const app = getApp();
const { get } = require('../../utils/request');

Page({
  data: {
    activityId: null,
    activityInfo: null,
    isLoading: true,
    isCollected: false,
    currentImage: 0,
    showMoreMenu: false
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ activityId: options.id });
      this.fetchActivityDetail(options.id);
    }
  },

  // Fetch activity detail
  fetchActivityDetail(id) {
    this.setData({ isLoading: true });

    get(`/api/activity/${id}`)
      .then((res) => {
        const activity = res.data;
        this.setData({
          activityInfo: activity,
          isLoading: false
        });
      })
      .catch((err) => {
        console.warn('获取活动详情失败，检查 /api/activity/:id 是否存在', err);
        this.setData({ isLoading: false });
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

  // Image preview
  onPreviewImage(e) {
    const index = e.currentTarget.dataset.index;
    wx.previewImage({
      current: this.data.activityInfo.images[index],
      urls: this.data.activityInfo.images
    });
  },

  // Image change
  onImageChange(e) {
    this.setData({
      currentImage: e.detail.current
    });
  },

  // View organizer profile
  onViewOrganizer() {
    wx.navigateTo({
      url: `/pages/partner-detail/partner-detail?id=${this.data.activityInfo.organizer.id}`
    });
  },

  // View participant
  onViewParticipant(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/partner-detail/partner-detail?id=${id}`
    });
  },

  // Toggle collect
  onToggleCollect() {
    const newStatus = !this.data.isCollected;
    this.setData({ isCollected: newStatus });
    
    wx.showToast({
      title: newStatus ? '已收藏' : '取消收藏',
      icon: 'success'
    });
  },

  // Share
  onShare() {
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    });
  },

  // Join activity
  onJoin() {
    const activity = this.data.activityInfo;
    
    if (activity.currentParticipants >= activity.maxParticipants) {
      wx.showToast({ title: '名额已满', icon: 'none' });
      return;
    }

    wx.showModal({
      title: '报名参加',
      content: `确定要参加「${activity.title}」吗？`,
      success: (res) => {
        if (res.confirm) {
          wx.showToast({ title: '报名成功', icon: 'success' });
        }
      }
    });
  },

  // Report
  onReport() {
    wx.showActionSheet({
      itemList: ['举报活动', '举报主办方'],
      success: (res) => {
        wx.showToast({ title: '举报成功', icon: 'success' });
      },
      complete: () => {
        this.onHideMore();
      }
    });
  }
});