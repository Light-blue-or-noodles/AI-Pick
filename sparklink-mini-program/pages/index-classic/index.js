// pages/index-classic/index.js — 历史首页；已不在 app.json 注册且 packOptions 忽略本目录以减小主包。若需恢复：写回 pages 并从 project.config packOptions.ignore 去掉本文件夹。
const app = getApp();
const recommendFeedback = require('../../utils/recommendFeedback.js');
const { mapPartnerForList } = require('../../utils/partnerListMap.js');

function formatPreferenceDisplay(pref) {
  if (!pref) return '';
  return String(pref)
    .split(/[,，、]+/)
    .map((s) => s.trim())
    .filter(Boolean)
    .join(' · ');
}

/** 带认证头的请求（登录/测试账号后携带 token 与 X-User-Id，避免 401） */
const request = (options) => {
  const header = options.header || {};
  const token = app.globalData.token || wx.getStorageSync('token');
  const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
  if (token) header['Authorization'] = 'Bearer ' + token;
  if (userId) header['X-User-Id'] = String(userId);
  return new Promise((resolve, reject) => {
    wx.request({
      ...options,
      header: { ...header, ...options.header },
      success(res) {
        const statusCode = res.statusCode;
        if (statusCode >= 200 && statusCode < 300) {
          resolve(res);
        } else {
          reject(res);
        }
      },
      fail(err) {
        reject(err);
      }
    });
  });
};

Page({
  data: {
    userInfo: {},
    recommendations: [],
    nearbyDynamics: [],
    // fortune: null,  // 今日运势 - 暂时注释，后期可能恢复使用
    isLoading: true
  },

  onLoad() {
    // 获取用户信息
    const userInfo = wx.getStorageSync('userInfo') || {};
    this.setData({ userInfo });
    
    // this.loadFortune();  // 今日运势 - 暂时注释，后期可能恢复使用
    this.loadNearbyDynamics();
    this.loadRecommendations();
  },

  onShow() {
    const that = this;
    setTimeout(function () {
      if (typeof that.getTabBar === 'function') {
        const bar = that.getTabBar();
        if (bar) bar.setData({ selected: 0 });
      }
    }, 0);
  },

  // 底部「首页」tab 被重复点击时刷新首页数据
  onTabReselect() {
    this.loadNearbyDynamics();
    this.loadRecommendations();
  },

  // 加载今日运势 - 暂时注释，后期可能恢复使用
  // loadFortune() {
  //   const defaultFortunes = [
  //     { type: '社交运势', desc: '今天容易认识新朋友', icon: '🌟', score: 92 },
  //     { type: '桃花运', desc: '适合参加社交活动', icon: '💖', score: 85 },
  //     { type: '游戏运', desc: '开黑容易连胜', icon: '🎮', score: 88 },
  //     { type: '运动运', desc: '适合锻炼身体', icon: '🏃', score: 90 }
  //   ];
  //   const randomDefault = defaultFortunes[Math.floor(Math.random() * defaultFortunes.length)];

  //   request({
  //     url: 'https://www.aipick.cloud/api/fortune/today',
  //     method: 'GET'
  //   })
  //     .then((res) => {
  //       let fortune = res && res.data;
  //       if (Array.isArray(fortune)) {
  //         fortune = fortune[0];
  //       }
  //       if (!fortune) {
  //         fortune = randomDefault;
  //       }
  //       this.setData({ fortune });
  //     })
  //     .catch((err) => {
  //       console.warn('加载今日运势失败，使用本地默认数据', err);
  //       this.setData({ fortune: randomDefault });
  //     });
  // },

  // 附近动态：搭子列表（Pick 搭，取最新几条）
  loadNearbyDynamics() {
    const baseUrl = app.globalData.baseUrl || 'https://www.aipick.cloud';
    request({
      url: `${baseUrl}/api/partner`,
      method: 'GET',
      data: {
        pageNum: 1,
        pageSize: 3,
        scopeType: 'platform'
      }
    })
      .then((res) => {
        const body = res && res.data;
        const records = (body && body.data && Array.isArray(body.data.records)) ? body.data.records : [];
        const base = app.globalData.baseUrl || 'https://www.aipick.cloud';
        const list = records.slice(0, 3).map((raw) => {
          const m = mapPartnerForList(raw, base);
          return {
            id: m.id,
            type: 'partner',
            name: m.title,
            content: m.preference || m.typeName || '',
            time: m.createTime,
            location: m.distance || '',
            avatar: m.cover || '/images/default-avatar.png',
            images: [],
            likeCount: raw.viewCount != null ? raw.viewCount : 0,
            commentCount: 0
          };
        });
        this.setData({ nearbyDynamics: list });
      })
      .catch((err) => {
        console.warn('加载附近动态失败', err);
        this.setData({ nearbyDynamics: [] });
      });
  },

  noop() {},

  /**
   * 提交推荐反馈（搭子帖），复用 utils/recommendFeedback
   */
  submitRecommendFeedback(feedbackType, targetId, matchScore, onOk) {
    recommendFeedback.postRecommendFeedback({
      feedbackType,
      targetType: recommendFeedback.TARGET_PARTNER,
      targetId,
      matchScore,
      baseUrl: app.globalData.baseUrl || 'https://www.aipick.cloud',
      onNeedLogin: () => {
        wx.showToast({ title: '请先登录后再反馈', icon: 'none' });
      },
      onSuccess: () => {
        if (typeof onOk === 'function') onOk();
      },
      showToastOnFail: true
    });
  },

  onRecommendSkip(e) {
    const id = e.currentTarget.dataset.id;
    const match = e.currentTarget.dataset.match;
    this.submitRecommendFeedback(recommendFeedback.FEEDBACK_SKIP, id, match, () => {
      const recommendations = (this.data.recommendations || []).filter((x) => x.id !== id);
      this.setData({ recommendations });
      wx.showToast({ title: '已跳过', icon: 'success' });
    });
  },

  onRecommendNotCompat(e) {
    const id = e.currentTarget.dataset.id;
    const match = e.currentTarget.dataset.match;
    this.submitRecommendFeedback(recommendFeedback.FEEDBACK_NOT_COMPATIBLE, id, match, () => {
      const recommendations = (this.data.recommendations || []).filter((x) => x.id !== id);
      this.setData({ recommendations });
      wx.showToast({ title: '已记录', icon: 'success' });
    });
  },

  onRecommendChat(e) {
    const id = e.currentTarget.dataset.id;
    const match = e.currentTarget.dataset.match;
    this.submitRecommendFeedback(recommendFeedback.FEEDBACK_CHAT, id, match, () => {
      wx.navigateTo({
        url: `/pages/partner-detail/partner-detail?id=${id}`
      });
    });
  },

  // 加载智能推荐（携带定位时后端可做距离与 AI 推荐；登录用户 X-User-Id 会参与反馈降权）
  loadRecommendations() {
    const defaultRecommendations = [
      { id: 1, avatar: '/images/default-avatar.png', cover: '/images/partner-banner.jpg', title: '示例搭子', typeName: '游戏搭子', preference: '排位上分', name: '小明', tags: [], tagsText: '', distance: '', match: 95 },
      { id: 2, avatar: '/images/default-avatar.png', cover: '/images/partner-banner.jpg', title: '示例搭子', typeName: '干饭搭子', preference: '清淡口味', name: '小红', tags: [], tagsText: '', distance: '', match: 88 },
      { id: 3, avatar: '/images/default-avatar.png', cover: '/images/partner-banner.jpg', title: '示例搭子', typeName: '运动搭子', preference: '晨跑', name: '运动达人', tags: [], tagsText: '', distance: '', match: 82 },
      { id: 4, avatar: '/images/default-avatar.png', cover: '/images/partner-banner.jpg', title: '示例搭子', typeName: '聊天搭子', preference: '图书馆', name: '学习控', tags: [], tagsText: '', distance: '', match: 78 }
    ];

    this.setData({
      isLoading: true
    });

    const fetchRecommend = (url) => {
      return request({
        url,
        method: 'GET'
      });
    };

    const baseUrl = app.globalData.baseUrl || 'https://www.aipick.cloud';

    const loadWithGeo = (lat, lon) => {
      let url = `${baseUrl}/api/home/recommend`;
      if (lat != null && lon != null) {
        url += `?latitude=${encodeURIComponent(lat)}&longitude=${encodeURIComponent(lon)}`;
      }
      return fetchRecommend(url);
    };

    const run = () => {
      wx.getLocation({
        type: 'gcj02',
        success: (loc) => {
          loadWithGeo(loc.latitude, loc.longitude).then((res) => this.applyRecommendResult(res, defaultRecommendations, baseUrl)).catch((err) => this.fallbackRecommend(err, defaultRecommendations, baseUrl, fetchRecommend));
        },
        fail: () => {
          loadWithGeo(null, null).then((res) => this.applyRecommendResult(res, defaultRecommendations, baseUrl)).catch((err) => this.fallbackRecommend(err, defaultRecommendations, baseUrl, fetchRecommend));
        }
      });
    };
    run();
  },

  applyRecommendResult(res, defaultRecommendations, baseUrl) {
    const toFullUrl = (path) => {
      if (!path) return '';
      if (path.startsWith('http')) return (app.normalizeImageUrl ? app.normalizeImageUrl(path, baseUrl) : path);
      return baseUrl + '/api' + (path.startsWith('/') ? path : '/' + path);
    };
    const body = res && res.data;
    const data = body && body.data ? body.data : body;
    const rawPartners = (data && data.partners && Array.isArray(data.partners)) ? data.partners : null;
    if (rawPartners && rawPartners.length > 0) {
      const partners = rawPartners.slice(0, 3).map((p) => ({
        id: p.id,
        avatar: toFullUrl(p.avatar) || '/images/default-avatar.png',
        cover: toFullUrl(p.coverImage) || '/images/partner-banner.jpg',
        title: p.title || '',
        typeName: p.typeName || '',
        preference: formatPreferenceDisplay(p.preference),
        name: p.nickname || '用户',
        tags: p.tags || [],
        tagsText: (p.tags && p.tags.length) ? p.tags.join(' · ') : '',
        distance: p.distance != null && p.distance !== '' ? (p.distance + (String(p.distance).match(/km|m$/) ? '' : 'km')) : '',
        match: p.matchScore != null ? p.matchScore : null
      }));
      this.setData({
        recommendations: partners,
        isLoading: false
      });
    } else {
      this.setData({
        recommendations: defaultRecommendations.slice(0, 3),
        isLoading: false
      });
    }
  },

  fallbackRecommend(err, defaultRecommendations, baseUrl, fetchRecommend) {
    console.warn('加载推荐失败，尝试备用接口 /api/partner', err);
    const toFullUrl = (path) => {
      if (!path) return '';
      if (path.startsWith('http')) return (app.normalizeImageUrl ? app.normalizeImageUrl(path, baseUrl) : path);
      return baseUrl + '/api' + (path.startsWith('/') ? path : '/' + path);
    };
    return fetchRecommend(`${baseUrl}/api/partner`)
      .then((res) => {
        const body = res && res.data;
        const records = (body && body.data && body.data.records) ? body.data.records : (body && Array.isArray(body.records) ? body.records : null);
        if (records && records.length > 0) {
          const list = records.slice(0, 3).map((p) => ({
            id: p.id,
            avatar: toFullUrl(p.avatar) || '/images/default-avatar.png',
            cover: toFullUrl(p.coverImage) || '/images/partner-banner.jpg',
            title: p.title || '',
            typeName: p.typeName || '',
            preference: formatPreferenceDisplay(p.preference),
            name: p.nickname || '用户',
            tags: p.tags || [],
            tagsText: (p.tags && p.tags.length) ? p.tags.join(' · ') : '',
            distance: p.location || '',
            match: null
          }));
          this.setData({
            recommendations: list,
            isLoading: false
          });
        } else {
          this.setData({
            recommendations: defaultRecommendations.slice(0, 3),
            isLoading: false
          });
        }
      })
      .catch((err2) => {
        console.warn('备用接口也失败，使用本地默认数据', err2);
        this.setData({
          recommendations: defaultRecommendations.slice(0, 3),
          isLoading: false
        });
      });
  },

  // 跳转AI对话
  goToAIChat() {
    wx.navigateTo({
      url: '/pages/ai-chat/ai-chat'
    });
  },

  // 跳转搜索
  goToSearch() {
    wx.navigateTo({
      url: '/pages/filter/filter'
    });
  },

  // 跳转搭子页面
  goToPartner() {
    wx.switchTab({
      url: '/pages/partner/partner'
    });
  },

  // 跳转AI匹配
  goToAIMatch() {
    wx.navigateTo({
      url: '/pages/ai-chat/ai-chat'
    });
  },

  // 跳转附近动态（更多搭子）
  goToNearby() {
    wx.switchTab({
      url: '/pages/partner/partner'
    });
  },

  // 查看详情
  goToDetail(e) {
    const { type, id } = e.currentTarget.dataset;
    if (type === 'partner' || !type) {
      wx.navigateTo({
        url: `/pages/partner-detail/partner-detail?id=${id}`
      });
    }
  },

  // 查看搭子详情
  goToPartnerDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/partner-detail/partner-detail?id=${id}`
    });
  },

  // 刷新推荐
  refreshRecommend() {
    this.loadRecommendations();
  },

  // 下拉刷新
  onPullDownRefresh() {
    // this.loadFortune();  // 今日运势 - 暂时注释，后期可能恢复使用
    this.loadNearbyDynamics();
    this.loadRecommendations();
    setTimeout(() => {
      wx.stopPullDownRefresh();
    }, 1000);
  }
});