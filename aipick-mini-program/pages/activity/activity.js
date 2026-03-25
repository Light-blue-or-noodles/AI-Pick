// pages/activity/activity.js
const app = getApp();

/** 活动状态：0待开始 1报名中 2进行中 3已结束 4已取消 -> 前端 recruiting/full/ended；已满仅按人数判断 */
function mapActivityForList(a) {
  const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
  const norm = app.normalizeImageUrl ? (u) => app.normalizeImageUrl(u, baseUrl) : (u) => (u && u.startsWith('http') ? u : (baseUrl + '/api' + (u && u.startsWith('/') ? u : '/' + (u || ''))));
  const statusNum = a.status != null ? a.status : 1;
  const maxP = a.maxParticipants != null && a.maxParticipants > 0 ? a.maxParticipants : 999;
  const curP = a.currentParticipants != null ? a.currentParticipants : 0;
  const isFull = maxP > 0 && curP >= maxP;
  const isEnded = statusNum === 3 || statusNum === 4;
  const statusText = isEnded ? 'ended' : (isFull ? 'full' : 'recruiting');
  const progressPercent = maxP > 0 ? Math.min(100, Math.round((curP / maxP) * 100)) : 0;
  let timeStr = '';
  if (a.startTime) {
    const s = typeof a.startTime === 'string' ? a.startTime : (a.startTime + '');
    timeStr = s.replace('T', ' ').substring(0, 16);
  }
  const cover = norm(a.coverImage) || (baseUrl + '/api/static/covers/activity-default.png');
  const rawAuthorAvatar =
    (a.organizer && a.organizer.avatar) ||
    a.organizerAvatar ||
    a.avatar ||
    '';
  const defaultAvatar = rawAuthorAvatar ? norm(rawAuthorAvatar) : (baseUrl + '/api/static/covers/activity-default.png');
  const hasRegistered =
    a.hasRegistered === true ||
    a.hasRegistered === 1 ||
    a.hasRegistered === '1';
  return {
    ...a,
    status: statusText,
    time: timeStr,
    location: a.location || '',
    category: a.category || '活动',
    cover,
    author: { name: a.organizerName || '发起人', avatar: defaultAvatar },
    currentParticipants: curP,
    maxParticipants: maxP,
    progressPercent,
    participantLabel: maxP >= 999 ? curP + '人报名' : curP + '/' + maxP + '人',
    hasRegistered
  };
}

Page({
  data: {
    allActivities: [],
    activities: [],
    searchKeyword: '',
    isLoading: true,
    showPublishModal: false,
    publishForm: {
      title: '',
      category: '运动',
      time: '',
      location: '',
      description: '',
      maxParticipants: ''
    }
  },

  onLoad() {
    // #region agent log
    try {
      fetch('http://127.0.0.1:7765/ingest/f4d89e39-b9eb-4a44-9895-809c60c7efc4', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Debug-Session-Id': '24c833'
        },
        body: JSON.stringify({
          sessionId: '24c833',
          runId: 'activity-page-load',
          hypothesisId: 'H1',
          location: 'pages/activity/activity.js:onLoad',
          message: 'activity page loaded',
          data: {
            hasGoToCreateFn: typeof this.goToCreate === 'function'
          },
          timestamp: Date.now()
        })
      }).catch(() => {});
    } catch (e) {}
    // #endregion agent log
    this.loadActivities();
  },

  onShow() {
    const that = this;
    setTimeout(function () {
      if (typeof that.getTabBar === 'function') {
        const bar = that.getTabBar();
        if (bar) bar.setData({ selected: 2 });
      }
    }, 0);
  },

  // 封装活动列表请求为 Promise（支持按标题关键字模糊搜索）
  fetchActivities(keyword) {
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    const header = {};
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    if (token) header['Authorization'] = 'Bearer ' + token;
    if (userId) header['X-User-Id'] = String(userId);
    const query = (keyword && keyword.trim())
      ? `?keyword=${encodeURIComponent(keyword.trim())}`
      : '';
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${baseUrl}/api/activity${query}`,
        method: 'GET',
        header,
        success: (res) => {
          // 后端返回 { code: 0, data: IPage }，列表在 data.records
          if (res.statusCode === 200) {
            const body = res.data;
            let list = null;
            if (Array.isArray(body)) {
              list = body;
            } else if (body && body.code === 0 && body.data) {
              list = Array.isArray(body.data) ? body.data : (body.data.records || null);
            }
            if (Array.isArray(list)) {
              resolve(list.map(mapActivityForList));
            } else {
              reject(new Error('活动数据格式不正确'));
            }
          } else {
            reject(new Error(`请求失败，状态码：${res.statusCode}`));
          }
        },
        fail: (err) => {
          reject(err || new Error('网络请求失败'));
        }
      });
    });
  },

  // 加载活动列表
  async loadActivities() {
    this.setData({
      isLoading: true
    });

    try {
      const activities = await this.fetchActivities(this.data.searchKeyword);
      this.setData({
        activities
      });
    } catch (error) {
      wx.showToast({
        title: '加载活动失败',
        icon: 'none'
      });
      console.error('加载活动失败', error);
    } finally {
      this.setData({
        isLoading: false
      });
    }
  },

  // 搜索输入
  onSearchInput(e) {
    const value = (e.detail && e.detail.value) || '';
    this.setData({
      searchKeyword: value
    });
  },

  // 键盘搜索
  onSearchConfirm() {
    this.loadActivities();
  },

  // 点击搜索按钮
  onSearchTap() {
    this.loadActivities();
  },

  // 跳转日历页面
  goToCalendar() {
    wx.navigateTo({
      url: '/pages/calendar/calendar'
    });
  },

  // 跳转筛选页面
  goToFilter() {
    wx.navigateTo({
      url: '/pages/filter/filter?type=activity'
    });
  },

  // 跳转到创建活动页（使用新版创建流程）
  goToCreate() {
    wx.navigateTo({
      url: '/pages/activity-create/activity-create'
    });
  },

  // 报名活动
  joinActivity(e) {
    const id = e.currentTarget.dataset.id;
    // 列表页点击报名统一跳转到详情页，在详情页完成真实报名逻辑
    wx.navigateTo({
      url: `/pages/activity-detail/activity-detail?id=${id}`
    });
  },

  // 查看详情
  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/activity-detail/activity-detail?id=${id}`
    });
  },

  onPullDownRefresh() {
    this.loadActivities();
    setTimeout(() => {
      wx.stopPullDownRefresh();
    }, 1000);
  }
});