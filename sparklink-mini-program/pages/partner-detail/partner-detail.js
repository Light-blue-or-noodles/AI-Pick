// pages/partner-detail/partner-detail.js
const app = getApp();
const { get, post, getApiErrorMessage } = require('../../utils/request');
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

Page({
  data: {
    partnerId: null,
    partnerInfo: null,
    isLoading: true,
    isFollowing: false,
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

  fetchPartnerDetail(id) {
    if (id == null || id === '' || String(id) === 'undefined') {
      this.setData({ isLoading: false });
      return;
    }
    this.setData({ isLoading: true });
    const baseUrl = app.globalData.baseUrl || 'https://www.aipick.cloud';
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
          const baseUrl = app.globalData.baseUrl || 'https://www.aipick.cloud';
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
          const planRaw = pickPlanField(raw, [
            'planTime',
            'plan_time',
            'planStartTime',
            'plan_start_time'
          ]);
          const planEndRaw = pickPlanField(raw, ['planEndTime', 'plan_end_time', 'planEnd', 'plan_end']);
          const planTimeDisplay = planRaw != null && planRaw !== '' ? formatPlanDateTime(planRaw) : '';
          const planTimeRangeDisplay = buildTimeRangeWithFallback(
            raw,
            buildPlanTimeRangeDisplay(planRaw, planEndRaw)
          );
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
            planEndTime: planEndRaw,
            planTimeDisplay,
            planTimeRangeDisplay,
            matchScore: raw.matchScore
          };
          this.setData({
            partnerInfo,
            isFollowing: !!raw.isFollowing,
            isLoading: false
          });
          if (partnerInfo.userId) {
            this.checkFollowStatus(partnerInfo.userId);
          }
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
