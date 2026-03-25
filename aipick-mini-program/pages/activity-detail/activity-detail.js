// pages/activity-detail/activity-detail.js
const app = getApp();
const { get, post } = require('../../utils/request');

Page({
  data: {
    activityId: null,
    activityInfo: null,
    isLoading: true,
    isCollected: false,
    hasRegistered: false,
    currentImage: 0,
    showMoreMenu: false
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ activityId: options.id });
      this.fetchActivityDetail(options.id);
      this.checkFavoriteStatus(options.id);
    }
  },

  // Check favorite status
  checkFavoriteStatus(activityId) {
    get(`/api/activity/${activityId}/favorite/status`, {})
      .then((res) => {
        const isCollected = res.data === true || res.data === 1;
        this.setData({ isCollected });
      })
      .catch(() => {
        // Silent fail
      });
  },

  // Fetch activity detail
  fetchActivityDetail(id) {
    this.setData({ isLoading: true });
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    const norm = app.normalizeImageUrl
      ? (u) => app.normalizeImageUrl(u, baseUrl)
      : (u) => {
          if (!u || typeof u !== 'string') return '';
          const t = u.trim();
          if (t.startsWith('http')) return t;
          return baseUrl + '/api' + (t.startsWith('/') ? t : '/' + t);
        };

    const header = {};
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    if (token) header['Authorization'] = 'Bearer ' + token;
    if (userId) header['X-User-Id'] = String(userId);

    wx.request({
      url: `${baseUrl}/api/activity/${id}`,
      method: 'GET',
      header,
      success: (res) => {
        if (res.statusCode !== 200 || !res.data || res.data.code !== 0) {
          this.setData({ activityInfo: null, isLoading: false });
          return;
        }
        const raw = res.data.data || {};
        const activity = raw.activity || raw;
        if (!activity || !activity.id) {
          this.setData({ activityInfo: null, isLoading: false });
          return;
        }
        const startTime = activity.startTime;
        let timeStr = '';
        if (startTime) {
          const s = typeof startTime === 'string' ? startTime : (startTime + '');
          timeStr = s.replace('T', ' ').substring(0, 16);
        }
        const maxP = activity.maxParticipants != null ? activity.maxParticipants : 0;
        const curP = activity.currentParticipants != null ? activity.currentParticipants : 0;
        const remain = Math.max(0, maxP - curP);
        const fee = activity.fee != null ? Number(activity.fee) : 0;
        const priceText = fee === 0 ? '免费' : `¥${fee}`;
        const defaultCover = baseUrl + '/api/static/covers/activity-default.png';
        const cover = activity.coverImage ? norm(activity.coverImage) : defaultCover;

        // 多图：优先使用接口返回的 images（首张为封面），否则仅封面
        let images = [cover];
        if (activity.images) {
          try {
            const list = typeof activity.images === 'string' ? JSON.parse(activity.images) : activity.images;
            if (Array.isArray(list) && list.length > 0) {
              const fullList = list.map((p) => (p ? norm(p) : '')).filter(Boolean);
              if (fullList.length > 0) {
                images = fullList;
              }
            }
          } catch (e) {
            // 解析失败用封面
          }
        }
        const defaultAvatarUrl = baseUrl + '/api/static/covers/activity-default.png';
        const rawOrganizer = raw.organizer;
        const organizer = rawOrganizer
          ? {
              id: rawOrganizer.id || activity.userId,
              avatar: (rawOrganizer.avatar && rawOrganizer.avatar) || defaultAvatarUrl,
              nickname: rawOrganizer.nickname || '发起人',
              rating: rawOrganizer.rating || ''
            }
          : {
              id: activity.userId,
              avatar: defaultAvatarUrl,
              nickname: '发起人',
              rating: ''
            };
        const activityInfo = {
          id: activity.id,
          title: activity.title || '活动',
          description: activity.description || '暂无详情',
          time: timeStr,
          location: activity.location || '暂无地点',
          currentParticipants: curP,
          maxParticipants: maxP,
          remainSpots: remain,
          priceText,
          category: activity.category || '活动',
          images,
          organizer,
          participants: []
        };
        const hasRegistered =
          activity.hasRegistered === true ||
          activity.hasRegistered === 1 ||
          activity.hasRegistered === '1' ||
          raw.hasRegistered === true ||
          raw.hasRegistered === 1 ||
          raw.hasRegistered === '1';
        this.setData({
          activityInfo,
          hasRegistered,
          isLoading: false
        });
      },
      fail: (err) => {
        console.warn('获取活动详情失败', err);
        this.setData({ activityInfo: null, isLoading: false });
      }
    });
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
    const index = e.currentTarget.dataset.index || 0;
    const { images } = this.data.activityInfo || {};
    const all = (images || []).filter(Boolean);
    wx.previewImage({
      current: all[index] || all[0],
      urls: all
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
    const activityId = this.data.activityId;
    const newStatus = !this.data.isCollected;
    
    const action = newStatus ? 'add' : 'cancel';
    
    post(`/api/activity/${activityId}/favorite`, { action })
      .then(() => {
        this.setData({ isCollected: newStatus });
        wx.showToast({
          title: newStatus ? '已收藏' : '取消收藏',
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

  // Share
  onShare() {
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    });
  },

  // Join activity（对接后端报名接口并刷新人数）
  onJoin() {
    const activity = this.data.activityInfo;
    const activityId = this.data.activityId;
    if (!activity || !activityId) return;
    // 已报名 -> 取消报名
    if (this.data.hasRegistered) {
      wx.showModal({
        title: '取消报名',
        content: `确定要取消「${activity.title}」的报名吗？`,
        success: (res) => {
          if (!res.confirm) return;
          post('/api/activity/' + activityId + '/cancel', {})
            .then(() => {
              wx.showToast({ title: '已取消报名', icon: 'success' });
              this.fetchActivityDetail(activityId);
            })
            .catch(() => {
              wx.showToast({ title: '取消失败', icon: 'none' });
            });
        }
      });
      return;
    }

    // 未报名 -> 正常报名
    const maxP = activity.maxParticipants != null ? activity.maxParticipants : 0;
    const curP = activity.currentParticipants != null ? activity.currentParticipants : 0;
    if (maxP > 0 && curP >= maxP) {
      wx.showToast({ title: '名额已满', icon: 'none' });
      return;
    }

    wx.showModal({
      title: '报名参加',
      content: `确定要参加「${activity.title}」吗？`,
      success: (res) => {
        if (!res.confirm) return;
        post('/api/activity/' + activityId + '/join', {})
          .then(() => {
            wx.showToast({ title: '报名成功', icon: 'success' });
            this.fetchActivityDetail(activityId);
          })
          .catch(() => {
            wx.showToast({ title: '报名失败', icon: 'none' });
          });
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