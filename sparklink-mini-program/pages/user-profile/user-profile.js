// pages/user-profile/user-profile.js
const app = getApp();
const { get, post, getApiErrorMessage } = require('../../utils/request');
const { mapPartnerForList } = require('../../utils/partnerListMap.js');
const { navigateToChatWithPeer } = require('../../utils/navigateToChat.js');

// URL 处理工具函数
const toFullUrl = (path) => {
  if (!path) return '';
  if (path.startsWith('http')) return path;
  const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
  return baseUrl + '/api' + (path.startsWith('/') ? path : '/' + path);
};

Page({
  data: {
    userId: null,
    userInfo: {},
    /** 原始接口数据，在资料刷新后重算与搭子列表页一致的展示 */
    partnersRaw: [],
    partners: [],
    isLoading: true
  },

  onLoad(options) {
    const userId = options.userId;
    if (!userId) {
      wx.showToast({ title: '用户ID不能为空', icon: 'none' });
      wx.navigateBack();
      return;
    }
    this.setData({ userId });
    this.fetchUserProfile();
    this.fetchUserPartners();
  },

  onShow() {
    if (this.data.userId) {
      this.fetchUserProfile();
      this.fetchUserPartners();
    }
  },

  onPullDownRefresh() {
    Promise.all([
      this.fetchUserProfile(),
      this.fetchUserPartners()
    ]).finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  /** 与搭子列表页 partner 使用同一套 mapPartnerForList（需资料里的昵称/头像作 author） */
  applyPartnerListMapping() {
    const baseUrl = app.globalData.baseUrl || '';
    const raw = this.data.partnersRaw || [];
    const ui = this.data.userInfo || {};
    const nick = (ui.nickname && String(ui.nickname).trim()) || '用户';
    let av = (ui.avatar && String(ui.avatar).trim()) || '';
    if (av && !av.startsWith('http') && !av.startsWith('/images')) {
      av = toFullUrl(av);
    }
    const partners = raw.map((item) => {
      const merged = {
        ...item,
        currentParticipants: item.currentCount,
        maxParticipants: item.maxCount,
        targetCount: item.maxCount,
        nickname: nick,
        avatar: av
      };
      return mapPartnerForList(merged, baseUrl);
    });
    this.setData({ partners });
  },

  // 获取用户信息
  fetchUserProfile() {
    return get(`/api/user/${this.data.userId}/profile`, {})
      .then((res) => {
        if (res.data) {
          // 处理头像 URL 和字段映射
          let userInfo = { ...res.data };
          if (userInfo.avatar && !userInfo.avatar.startsWith('http') && !userInfo.avatar.startsWith('/images')) {
            userInfo.avatar = toFullUrl(userInfo.avatar);
          }
          // 字段名映射：后端 isFollowed -> 前端 isFollowing
          userInfo.isFollowing = res.data.isFollowed;
          // 字段名映射：后端 followerCount -> 前端 fansCount
          userInfo.fansCount = res.data.followerCount;
          this.setData({ 
            userInfo: userInfo,
            isLoading: false
          });
          this.applyPartnerListMapping();
        }
      })
      .catch((err) => {
        console.error('获取用户信息失败', err);
        this.setData({ isLoading: false });
        wx.showToast({ title: '获取用户信息失败', icon: 'none' });
      });
  },

  // 获取用户发布的搭子列表（后端已按与列表页相同规则做可见性过滤，见 getUserVisiblePartners）
  fetchUserPartners() {
    return get(`/api/user/${this.data.userId}/partners`, {})
      .then((res) => {
        const list = res.data != null ? (Array.isArray(res.data) ? res.data : []) : [];
        this.setData({ partnersRaw: list });
        this.applyPartnerListMapping();
      })
      .catch((err) => {
        console.error('获取用户搭子列表失败', err);
        this.setData({ partnersRaw: [], partners: [] });
      });
  },

  // 切换关注状态
  onToggleFollow() {
    const { userId, userInfo } = this.data;
    const action = userInfo.isFollowing ? 'cancel' : 'follow';
    
    post(
      `/api/user/${userId}/follow`,
      { action },
      { suppressErrorToast: true }
    )
      .then(() => {
        const newIsFollowing = !userInfo.isFollowing;
        wx.showToast({
          title: newIsFollowing ? '关注成功' : '已取消关注',
          icon: 'success'
        });
        this.fetchUserProfile();
      })
      .catch((err) => {
        wx.showToast({
          title: getApiErrorMessage(err),
          icon: 'none'
        });
      });
  },

  // 开始私信
  onStartChat() {
    const { userId, userInfo } = this.data;
    if (!userId) return;
    const nickname = (userInfo && userInfo.nickname) || '';
    const avatar = (userInfo && userInfo.avatar) || '';
    navigateToChatWithPeer({ userId, nickname, avatar });
  },

  // 跳转到搭子详情
  goToPartnerDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/partner-detail/partner-detail?id=${id}`
    });
  }
});