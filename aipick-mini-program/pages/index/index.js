// pages/index/index.js
const app = getApp();

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
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({
        selected: 0
      });
    }
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
  //     url: 'http://localhost:8080/api/fortune/today',
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

  // 加载附近动态（真实数据：后端活动列表分页，按创建时间倒序）
  loadNearbyDynamics() {
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    request({
      url: `${baseUrl}/api/activity`,
      method: 'GET',
      data: {
        pageNum: 1,
        pageSize: 3
      }
    })
      .then((res) => {
        // 后端返回 { code: 0, data: { records: [], total, ... } }，每条为活动
        const body = res && res.data;
        const records = (body && body.data && Array.isArray(body.data.records)) ? body.data.records : [];
        const list = records.slice(0, 3).map(a => ({
          id: a.id,
          type: 'activity',
          name: a.title || '活动',
          content: a.description || '',
          time: a.startTime ? (String(a.startTime).replace('T', ' ').substring(0, 16)) : '',
          location: a.location || '',
          avatar: a.coverImage || '/images/default-avatar.png',
          likeCount: a.viewCount != null ? a.viewCount : 0,
          commentCount: 0
        }));
        this.setData({ nearbyDynamics: list });
      })
      .catch((err) => {
        console.warn('加载附近动态失败', err);
        this.setData({ nearbyDynamics: [] });
      });
  },

  // 加载智能推荐
  loadRecommendations() {
    const defaultRecommendations = [
      {
        id: 1,
        avatar: '/images/default-avatar.png',
        name: '小明',
        tags: ['游戏', '健身'],
        distance: '500m',
        match: 95,
        bio: '周末王者上分，一起吗？'
      },
      {
        id: 2,
        avatar: '/images/default-avatar.png',
        name: '小红',
        tags: ['美食', '探店'],
        distance: '1.2km',
        match: 88,
        bio: '寻找一起探店的伙伴'
      },
      {
        id: 3,
        avatar: '/images/default-avatar.png',
        name: '运动达人',
        tags: ['跑步', '篮球'],
        distance: '800m',
        match: 82,
        bio: '早起跑步搭子来'
      },
      {
        id: 4,
        avatar: '/images/default-avatar.png',
        name: '学习控',
        tags: ['学习', '英语'],
        distance: '2km',
        match: 78,
        bio: '周末图书馆约起'
      }
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

    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    fetchRecommend(`${baseUrl}/api/home/recommend`)
      .then((res) => {
        // 推荐接口返回 { code: 0, data: { partners: [], activities: [] } }
        const body = res && res.data;
        const data = body && body.data ? body.data : body;
        const rawPartners = (data && data.partners && Array.isArray(data.partners)) ? data.partners : null;
        if (rawPartners && rawPartners.length > 0) {
          const partners = rawPartners.slice(0, 3).map(p => ({
            id: p.id,
            avatar: p.avatar || '/images/default-avatar.png',
            name: p.nickname || '用户',
            tags: p.tags || [],
            distance: p.distance != null && p.distance !== '' ? (p.distance + (String(p.distance).match(/km|m$/) ? '' : 'km')) : '',
            match: p.matchScore != null ? p.matchScore : null,
            bio: (p.description || p.bio || p.title || '') || '暂无简介'
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
      })
      .catch((err) => {
        console.warn('加载推荐失败，尝试备用接口 /api/partner', err);
        // 备用接口：直接返回搭子列表
        return fetchRecommend(`${baseUrl}/api/partner`)
          .then((res) => {
            const body = res && res.data;
            const records = (body && body.data && body.data.records) ? body.data.records : (body && Array.isArray(body.records) ? body.records : null);
            if (records && records.length > 0) {
              const list = records.slice(0, 3).map(p => ({
                id: p.id,
                avatar: '/images/default-avatar.png',
                name: p.title || '搭子',
                tags: [],
                distance: p.location || '',
                match: null,
                bio: (p.content || '') || '暂无简介'
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
      url: '/pages/filter/filter?type=partner'
    });
  },

  // 跳转搭子页面
  goToPartner() {
    wx.switchTab({
      url: '/pages/partner/partner'
    });
  },

  // 跳转活动页面
  goToActivity() {
    wx.switchTab({
      url: '/pages/activity/activity'
    });
  },

  // 跳转AI匹配
  goToAIMatch() {
    wx.navigateTo({
      url: '/pages/ai-chat/ai-chat'
    });
  },

  // 跳转日历
  goToCalendar() {
    wx.navigateTo({
      url: '/pages/calendar/calendar'
    });
  },

  // 跳转附近动态
  goToNearby() {
    wx.switchTab({
      url: '/pages/activity/activity'
    });
  },

  // 查看详情
  goToDetail(e) {
    const { type, id } = e.currentTarget.dataset;
    if (type === 'partner') {
      wx.navigateTo({
        url: `/pages/partner-detail/partner-detail?id=${id}`
      });
    } else if (type === 'activity') {
      wx.navigateTo({
        url: `/pages/activity-detail/activity-detail?id=${id}`
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