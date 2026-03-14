// pages/activity/activity.js
const app = getApp();

/** 活动状态：0待开始 1报名中 2进行中 3已结束 4已取消 -> 前端 recruiting/full/ended */
function mapActivityForList(a) {
  const baseUrl = app.globalData.baseUrl || '';
  const statusNum = a.status != null ? a.status : 1;
  const statusText = statusNum <= 1 ? 'recruiting' : statusNum === 2 ? 'full' : 'ended';
  const maxP = a.maxParticipants != null && a.maxParticipants > 0 ? a.maxParticipants : 999;
  const curP = a.currentParticipants != null ? a.currentParticipants : 0;
  const progressPercent = maxP > 0 ? Math.min(100, Math.round((curP / maxP) * 100)) : 0;
  let timeStr = '';
  if (a.startTime) {
    const s = typeof a.startTime === 'string' ? a.startTime : (a.startTime + '');
    timeStr = s.replace('T', ' ').substring(0, 16);
  }
  const cover = (a.coverImage && (a.coverImage.startsWith('http') ? a.coverImage : (baseUrl + a.coverImage))) || '/images/activity-banner.png';
  return {
    ...a,
    status: statusText,
    time: timeStr,
    location: a.location || '',
    category: a.category || '活动',
    cover,
    author: { name: '发起人', avatar: '/images/default-avatar.png' },
    currentParticipants: curP,
    maxParticipants: maxP,
    progressPercent,
    participantLabel: maxP >= 999 ? curP + '人报名' : curP + '/' + maxP + '人'
  };
}

Page({
  data: {
    activities: [],
    categories: ['全部', '运动', '美食', '学习', '娱乐', '社交'],
    timeFilters: ['全部', '今天', '明天', '周末', '本月'],
    activeCategory: 0,
    activeTime: 0,
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
    this.loadActivities();
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({
        selected: 2
      });
    }
  },

  // 封装活动列表请求为 Promise
  fetchActivities() {
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    const header = {};
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    if (token) header['Authorization'] = 'Bearer ' + token;
    if (userId) header['X-User-Id'] = String(userId);
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${baseUrl}/api/activity`,
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
      const activities = await this.fetchActivities();
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

  // 选择分类
  selectCategory(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({ activeCategory: index });
    this.loadActivities();
  },

  // 选择时间
  selectTime(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({ activeTime: index });
    this.loadActivities();
  },

  // 跳转筛选页面
  goToFilter() {
    wx.navigateTo({
      url: '/pages/filter/filter?type=activity'
    });
  },

  // 跳转日历页面
  goToCalendar() {
    wx.navigateTo({
      url: '/pages/calendar/calendar'
    });
  },

  // 显示发布弹窗
  showPublishModal() {
    this.setData({ showPublishModal: true });
  },

  // 隐藏发布弹窗
  hidePublishModal() {
    this.setData({ showPublishModal: false });
  },

  // 更新表单
  updateForm(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail.value;
    this.setData({
      [`publishForm.${field}`]: value
    });
  },

  // 报名活动
  joinActivity(e) {
    const id = e.currentTarget.dataset.id;
    wx.showToast({
      title: '报名成功',
      icon: 'success'
    });
  },

  // 发布活动
  submitActivity() {
    const form = this.data.publishForm;
    if (!form.title || !form.time || !form.location) {
      wx.showToast({
        title: '请填写完整信息',
        icon: 'none'
      });
      return;
    }

    // 模拟成功
    wx.showToast({
      title: '发布成功',
      icon: 'success'
    });
    this.hidePublishModal();
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