// pages/partner-detail/partner-detail.js
const app = getApp();
const { get, post } = require('../../utils/request');

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
      this.checkFollowStatus(id);
    } else {
      this.setData({ isLoading: false });
      wx.showToast({ title: '参数错误', icon: 'none' });
    }
  },

  // Check follow status
  checkFollowStatus(partnerId) {
    // 获取搭子发布者ID后检查关注状态
    const userId = this.data.partnerInfo && this.data.partnerInfo.userId;
    if (!userId) return;
    
    get(`/api/user/${userId}/follow/status`, {})
      .then((res) => {
        const isFollowing = res.data === true || res.data === 1;
        this.setData({ isFollowing });
      })
      .catch(() => {
        // Silent fail
      });
  },

  // Toggle follow
  onToggleFollow() {
    const userId = this.data.partnerInfo && this.data.partnerInfo.userId;
    if (!userId) {
      wx.showToast({ title: '无法关注', icon: 'none' });
      return;
    }
    
    const newStatus = !this.data.isFollowing;
    const action = newStatus ? 'add' : 'cancel';
    
    post(`/api/user/${userId}/follow`, { action })
      .then(() => {
        this.setData({ isFollowing: newStatus });
        wx.showToast({
          title: newStatus ? '已关注' : '取消关注',
          icon: 'success'
        });
      })
      .catch(() => {
        wx.showToast({
          title: '操作失败',
          icon: 'none'
        });
      });
  },

  fetchPartnerDetail(id) {
    if (id == null || id === '' || String(id) === 'undefined') {
      this.setData({ isLoading: false });
      return;
    }
    this.setData({ isLoading: true });
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    const header = { 'content-type': 'application/json' };
    if (token) header['Authorization'] = 'Bearer ' + token;
    if (userId) header['X-User-Id'] = String(userId);
    wx.request({
      url: `${baseUrl}/api/partner/${id}`,
      method: 'GET',
      header,
      success: (res) => {
        const data = res.data;
        if (res.statusCode === 200 && data && (data.code === 0 || data.code === 200)) {
          const raw = data.data != null ? data.data : data;
          const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
          const toFullUrl = (path) => {
            if (!path || typeof path !== 'string') return '';
            if (path.startsWith('http')) return (app.normalizeImageUrl ? app.normalizeImageUrl(path, baseUrl) : path);
            const p = path.startsWith('/') ? path : '/' + path;
            if (p.indexOf('/api/') === 0) return baseUrl + p;
            return baseUrl + '/api' + p;
          };
          const prefRaw = raw.preference || '';
          const preferenceTags = String(prefRaw)
            .split(/[,，、\s]+/)
            .map((s) => s.trim())
            .filter((s) => s.length > 0);
          const planRaw = raw.planTime;
          const planTimeDisplay = planRaw
            ? String(planRaw).replace('T', ' ').substring(0, 16)
            : '';
          const partnerInfo = {
            id: raw.id,
            userId: raw.userId,
            title: raw.title || '搭子',
            nickname: raw.nickname || '用户',
            avatar: toFullUrl(raw.avatar) || '/images/default-avatar.png',
            coverImage: toFullUrl(raw.coverImage) || '',
            typeName: raw.typeName || '',
            preference: prefRaw,
            preferenceTags,
            scopeName: raw.scopeName || '公开',
            description: raw.description || raw.content || '',
            bio: raw.description || raw.content || '暂无详情',
            targetCount: raw.maxParticipants != null ? raw.maxParticipants : raw.targetCount,
            currentCount: raw.currentParticipants != null ? raw.currentParticipants : raw.currentCount,
            location: raw.address || raw.location || '',
            planTime: planRaw,
            planTimeDisplay,
            matchScore: raw.matchScore
          };
          this.setData({
            partnerInfo,
            isFollowing: !!raw.isFollowing,
            isLoading: false
          });
        } else {
          this.setData({ partnerInfo: null, isLoading: false });
          wx.showToast({ title: (data && data.message) || '加载失败', icon: 'none' });
        }
      },
      fail: () => {
        this.setData({ partnerInfo: null, isLoading: false });
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  onShowMore() {
    this.setData({ showMoreMenu: true });
  },

  onHideMore() {
    this.setData({ showMoreMenu: false });
  },

  onToggleFollow() {
    const newStatus = !this.data.isFollowing;
    this.setData({ isFollowing: newStatus });
    wx.showToast({ title: newStatus ? '关注成功' : '取消关注', icon: 'success' });
  },

  onStartChat() {
    wx.showToast({ title: '敬请期待', icon: 'none' });
  },

  onShowMatch() {
    wx.showToast({ title: '敬请期待', icon: 'none' });
  },

  onNotInterested() {
    this.setData({ showMoreMenu: false });
    wx.showToast({ title: '已记录', icon: 'success' });
  },

  onReport() {
    this.setData({ showMoreMenu: false });
    wx.showToast({ title: '已收到', icon: 'none' });
  }
});
