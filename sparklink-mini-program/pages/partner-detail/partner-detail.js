// pages/partner-detail/partner-detail.js
const app = getApp();
const { get, post, del, getApiErrorMessage } = require('../../utils/request');
const { navigateToChatWithPeer } = require('../../utils/navigateToChat.js');

/**
 * 统一从 Result 中取出 DTO，并兼容未部署新后端时缺少 activityTagScore/publisherTagScore 的字段
 */
function normalizeMatchScoreDto(res) {
  if (!res) {
    return null;
  }
  const dto = res.data != null ? res.data : res;
  if (!dto || typeof dto !== 'object') {
    return null;
  }
  if (dto.activityTagScore == null && dto.interestScore != null) {
    dto.activityTagScore = dto.interestScore;
  }
  if (dto.publisherTagScore == null && dto.interestScore != null) {
    dto.publisherTagScore = dto.interestScore;
  }
  return dto;
}

function pickPlanField(obj, keys) {
  for (let i = 0; i < keys.length; i += 1) {
    const k = keys[i];
    if (obj != null && Object.prototype.hasOwnProperty.call(obj, k) && obj[k] != null && obj[k] !== '') {
      return obj[k];
    }
  }
  return undefined;
}

/**
 * 搭子计划时间：ISO 字符串、时间戳、Jackson 数组 [y,m,d,h,mi,s] 或对象 { year, monthValue, dayOfMonth, hour, minute }
 */
function formatPlanDateTime(v) {
  if (v == null || v === '') {
    return '';
  }
  if (typeof v === 'number' && !Number.isNaN(v)) {
    const ms = v < 1e12 ? v * 1000 : v;
    const d = new Date(ms);
    if (Number.isNaN(d.getTime())) {
      return '';
    }
    const pad = (n) => (n < 10 ? `0${n}` : String(n));
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(
      d.getMinutes()
    )}`;
  }
  if (v && typeof v === 'object' && !Array.isArray(v)) {
    if (
      typeof v.year === 'number' &&
      typeof v.monthValue === 'number' &&
      typeof v.dayOfMonth === 'number'
    ) {
      const pad = (n) => (n < 10 ? `0${n}` : String(n));
      const h = v.hour != null ? Number(v.hour) : 0;
      const mi = v.minute != null ? Number(v.minute) : 0;
      if (Number.isNaN(h) || Number.isNaN(mi)) {
        return '';
      }
      return `${v.year}-${pad(v.monthValue)}-${pad(v.dayOfMonth)} ${pad(h)}:${pad(mi)}`;
    }
  }
  if (Array.isArray(v)) {
    const a = v.map((x) => Number(x));
    if (a.length >= 5 && !a.some((n) => Number.isNaN(n))) {
      const pad = (n) => (n < 10 ? `0${n}` : String(n));
      return `${a[0]}-${pad(a[1])}-${pad(a[2])} ${pad(a[3])}:${pad(a[4])}`;
    }
  }
  const s = String(v)
    .replace('T', ' ')
    .trim();
  if (!s || s === 'null' || s === 'undefined') {
    return '';
  }
  const norm = s.match(
    /^(\d{4})-(\d{1,2})-(\d{1,2})[ T](\d{1,2}):(\d{1,2})(?::\d{1,2})?/
  );
  if (norm) {
    const pad2 = (x) => String(Number(x)).padStart(2, '0');
    return `${norm[1]}-${pad2(norm[2])}-${pad2(norm[3])} ${pad2(norm[4])}:${pad2(norm[5])}`;
  }
  return s.length > 32 ? s.substring(0, 32) : s;
}

/**
 * 有结束时间才拼区间；历史数据仅 planTime、无 planEndTime 时只显示开始时间
 */
function buildPlanTimeRangeDisplay(planStart, planEnd) {
  const start = formatPlanDateTime(planStart);
  const end = formatPlanDateTime(planEnd);
  if (!start && !end) {
    return '';
  }
  if (start && !end) {
    return start;
  }
  if (!start && end) {
    return `截止 ${end}`;
  }
  if (start === end) {
    return start;
  }
  return `${start} ～ ${end}`;
}

/**
 * 无计划起止时间时，用创建时间作补充说明（发布于）
 */
function buildTimeRangeWithFallback(raw, planRange) {
  if (planRange) {
    return planRange;
  }
  const createRaw = pickPlanField(raw, ['createTime', 'create_time']);
  const c = createRaw != null && createRaw !== '' ? formatPlanDateTime(createRaw) : '';
  if (c) {
    return `计划时间未设置 · 发布于 ${c}`;
  }
  return '';
}

function resolvePlanTimeMeta(raw) {
  const planRaw = pickPlanField(raw, [
    'planTime',
    'plan_time',
    'planStartTime',
    'plan_start_time'
  ]);
  const planEndRaw = pickPlanField(raw, ['planEndTime', 'plan_end_time', 'planEnd', 'plan_end']);
  const range = buildPlanTimeRangeDisplay(planRaw, planEndRaw);
  const createRaw = pickPlanField(raw, ['createTime', 'create_time']);
  const publishTime = createRaw != null && createRaw !== '' ? formatPlanDateTime(createRaw) : '';
  if (range) {
    return {
      hasPlanTime: true,
      primary: range,
      secondary: ''
    };
  }
  return {
    hasPlanTime: false,
    primary: '待定',
    secondary: publishTime ? `发布于 ${publishTime}` : ''
  };
}

function parseLocationMeta(raw) {
  if (raw == null || String(raw).trim() === '') {
    return null;
  }
  const text = String(raw).trim();
  const parts = text.split(/\s*[·•|｜]\s*/).map((p) => p.trim()).filter(Boolean);
  if (parts.length >= 2) {
    return {
      primary: parts[0],
      secondary: parts.slice(1).join(' ')
    };
  }
  return {
    primary: text,
    secondary: ''
  };
}

Page({
  data: {
    partnerId: null,
    partnerInfo: null,
    isLoading: true,
    isFollowing: false,
    hasApplied: false,
    isOwner: false,
    isFull: false,
    remainSpots: 0,
    joining: false,
    showMoreMenu: false,
    showMatchPanel: false,
    matchResult: null
  },

  onLoad(options) {
    const id = options.id || options.partnerId || '';
    const validId = id && String(id).trim() && String(id) !== 'undefined';
    if (validId) {
      this.setData({ partnerId: id });
      this.fetchPartnerDetail(id);
    } else {
      this.setData({ isLoading: false });
      wx.showToast({ title: '参数错误', icon: 'none' });
    }
  },

  // 后端：GET /api/user/follow/check/{userId}
  checkFollowStatus(userId) {
    if (!userId) return;
    get(`/api/user/follow/check/${userId}`, {})
      .then((result) => {
        const d = result && result.data;
        const isFollowing = !!(d && d.isFollowing);
        this.setData({ isFollowing });
      })
      .catch(() => {});
  },

  mapPartnerDetail(raw) {
    const baseUrl = app.globalData.baseUrl || 'https://www.aipick.cloud';
    const toFullUrl = (path) => {
      if (!path || typeof path !== 'string') return '';
      if (path.startsWith('http')) return app.normalizeImageUrl ? app.normalizeImageUrl(path, baseUrl) : path;
      const p = path.startsWith('/') ? path : '/' + path;
      if (p.indexOf('/api/') === 0) return baseUrl + p;
      return baseUrl + '/api' + p;
    };
    const prefRaw = raw.preference || '';
    const preferenceTags = String(prefRaw)
      .split(/[,，、\s]+/)
      .map((s) => s.trim())
      .filter((s) => s.length > 0);
    const planRaw = pickPlanField(raw, [
      'planTime',
      'plan_time',
      'planStartTime',
      'plan_start_time'
    ]);
    const planEndRaw = pickPlanField(raw, ['planEndTime', 'plan_end_time', 'planEnd', 'plan_end']);
    const planTimeDisplay = planRaw != null && planRaw !== '' ? formatPlanDateTime(planRaw) : '';
    const planTimeMeta = resolvePlanTimeMeta(raw);
    const locationRaw = raw.address || raw.location || '';
    const locationMeta = parseLocationMeta(locationRaw);
    const targetCount = raw.maxParticipants != null ? raw.maxParticipants : raw.targetCount;
    const currentCount = raw.currentParticipants != null ? raw.currentParticipants : raw.currentCount;
    const maxP = targetCount != null ? Number(targetCount) : 0;
    const curP = currentCount != null ? Number(currentCount) : 0;
    const remainSpots = maxP > 0 ? Math.max(0, maxP - curP) : 0;
    const isFull = maxP > 0 && curP >= maxP;
    return {
      partnerInfo: {
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
        targetCount,
        currentCount,
        location: locationRaw,
        locationMeta,
        planTime: planRaw,
        planEndTime: planEndRaw,
        planTimeDisplay,
        planTimeMeta,
        matchScore: raw.matchScore,
        status: raw.status
      },
      isFollowing: !!raw.isFollowed || !!raw.isFollowing,
      hasApplied: !!raw.hasApplied,
      isOwner: !!raw.isOwner,
      isFull,
      remainSpots
    };
  },

  fetchPartnerDetail(id) {
    if (id == null || id === '' || String(id) === 'undefined') {
      this.setData({ isLoading: false });
      return;
    }
    this.setData({ isLoading: true });
    get(`/api/partner/${id}`, {}, { suppressErrorToast: true })
      .then((res) => {
        const raw = res && res.data != null ? res.data : res;
        if (!raw || typeof raw !== 'object') {
          this.setData({ partnerInfo: null, isLoading: false });
          wx.showToast({ title: '加载失败', icon: 'none' });
          return;
        }
        const mapped = this.mapPartnerDetail(raw);
        this.setData({
          ...mapped,
          isLoading: false
        });
        if (mapped.partnerInfo.userId) {
          this.checkFollowStatus(mapped.partnerInfo.userId);
        }
      })
      .catch((err) => {
        this.setData({ partnerInfo: null, isLoading: false });
        wx.showToast({ title: getApiErrorMessage(err, '加载失败'), icon: 'none' });
      });
  },

  ensureLoginForJoin() {
    const token = wx.getStorageSync('token') || (app.globalData && app.globalData.token);
    if (token) {
      return true;
    }
    wx.showModal({
      title: '提示',
      content: '登录后方可报名搭子',
      confirmText: '去登录',
      success: (r) => {
        if (r.confirm) {
          wx.navigateTo({ url: '/pages/login/login' });
        }
      }
    });
    return false;
  },

  onJoinPartner() {
    const partner = this.data.partnerInfo;
    const partnerId = this.data.partnerId;
    if (!partner || !partnerId) {
      return;
    }
    if (this.data.isOwner) {
      return;
    }
    if (!this.ensureLoginForJoin()) {
      return;
    }

    if (this.data.hasApplied) {
      wx.showModal({
        title: '取消报名',
        content: `确定要取消「${partner.title}」的报名吗？`,
        success: (res) => {
          if (!res.confirm) {
            return;
          }
          this.setData({ joining: true });
          del(`/api/partner/${partnerId}/apply`, {}, { suppressErrorToast: true })
            .then(() => {
              wx.showToast({ title: '已取消报名', icon: 'success' });
              this.fetchPartnerDetail(partnerId);
            })
            .catch((err) => {
              wx.showToast({ title: getApiErrorMessage(err, '取消失败'), icon: 'none' });
            })
            .finally(() => {
              this.setData({ joining: false });
            });
        }
      });
      return;
    }

    if (this.data.isFull) {
      wx.showToast({ title: '名额已满', icon: 'none' });
      return;
    }

    wx.showModal({
      title: '报名参加',
      content: `确定要报名「${partner.title}」吗？`,
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        this.setData({ joining: true });
        post(`/api/partner/${partnerId}/apply`, {}, { suppressErrorToast: true })
          .then(() => {
            wx.showToast({ title: '报名成功', icon: 'success' });
            this.fetchPartnerDetail(partnerId);
          })
          .catch((err) => {
            wx.showToast({ title: getApiErrorMessage(err, '报名失败'), icon: 'none' });
          })
          .finally(() => {
            this.setData({ joining: false });
          });
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
    const userId = this.data.partnerInfo && this.data.partnerInfo.userId;
    if (!userId) {
      wx.showToast({ title: '无法关注', icon: 'none' });
      return;
    }
    const willFollow = !this.data.isFollowing;
    const action = willFollow ? 'follow' : 'cancel';
    post(
      `/api/user/${userId}/follow`,
      { action },
      { suppressErrorToast: true }
    )
      .then(() => {
        this.setData({ isFollowing: willFollow });
        wx.showToast({
          title: willFollow ? '已关注' : '已取消关注',
          icon: 'success'
        });
      })
      .catch((err) => {
        wx.showToast({
          title: getApiErrorMessage(err),
          icon: 'none'
        });
      });
  },

  onStartChat() {
    const partnerInfo = this.data.partnerInfo || {};
    const { userId, nickname, avatar } = partnerInfo;

    if (!userId) {
      wx.showToast({ title: '无法发起聊天', icon: 'none' });
      return;
    }

    navigateToChatWithPeer({ userId, nickname, avatar });
  },

  onShowMatch() {
    const token = wx.getStorageSync('token');
    if (!token) {
      wx.showModal({
        title: '提示',
        content: '登录后可查看你与发布者之间的 AI 匹配度',
        confirmText: '去登录',
        success: (r) => {
          if (r.confirm) {
            wx.navigateTo({ url: '/pages/login/login' });
          }
        }
      });
      return;
    }
    const partner = this.data.partnerInfo;
    if (!partner || !partner.userId) {
      wx.showToast({ title: '无法计算匹配度', icon: 'none' });
      return;
    }
    const rawMe =
      (app.globalData && app.globalData.userId != null && app.globalData.userId !== ''
        ? app.globalData.userId
        : null) || wx.getStorageSync('userId');
    const me = rawMe != null && rawMe !== '' ? String(rawMe).trim() : '';
    const target = String(partner.userId).trim();
    if (!me) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    if (me === target) {
      wx.showToast({ title: '不能与自己计算匹配度', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '分析中...', mask: true });
    post(
      '/api/ai/match-score',
      {
        userId: Number(me),
        targetId: Number(target),
        partnerId: partner.id != null ? Number(partner.id) : undefined
      },
      { suppressErrorToast: true }
    )
      .then((res) => {
        wx.hideLoading();
        const dto = normalizeMatchScoreDto(res);
        this.setData({ matchResult: dto || null, showMatchPanel: true });
      })
      .catch((err) => {
        wx.hideLoading();
        wx.showToast({ title: getApiErrorMessage(err), icon: 'none' });
      });
  },

  onCloseMatchPanel() {
    this.setData({ showMatchPanel: false, matchResult: null });
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
