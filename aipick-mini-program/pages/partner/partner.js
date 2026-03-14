// pages/partner/partner.js
const app = getApp();

const TYPE_NAMES = { 1: '美食', 2: '旅游', 3: '运动', 4: '学习', 5: '游戏', 6: '其他' };

/**
 * 封装 wx.request 为 Promise，请求搭子列表接口
 * 后端返回 { code: 0, data: IPage }，列表在 data.records
 * @returns {Promise<Array>} 搭子列表（已映射为页面所需结构）
 */
function fetchPartnerList() {
  const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
  const header = { 'Content-Type': 'application/json' };
  const token = app.globalData.token || wx.getStorageSync('token');
  const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
  if (token) header['Authorization'] = 'Bearer ' + token;
  if (userId) header['X-User-Id'] = String(userId);

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${baseUrl}/api/partner`,
      method: 'GET',
      header,
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          const body = res.data;
          let rawList = [];
          if (Array.isArray(body)) {
            rawList = body;
          } else if (body && body.code === 0 && body.data) {
            rawList = Array.isArray(body.data) ? body.data : (body.data.records || []);
          }
          const baseUrl = app.globalData.baseUrl || '';
          const list = rawList.map((item) => mapPartnerForList(item, baseUrl));
          resolve(list);
        } else {
          reject(new Error(body && (body.msg || body.message) || `请求失败 ${res.statusCode}`));
        }
      },
      fail: (err) => {
        reject(err || new Error('网络请求失败'));
      }
    });
  });
}

function mapPartnerForList(p, baseUrl) {
  const status = p.status;
  const statusText = status === 0 ? 'recruiting' : status === 1 ? 'full' : 'ended';
  const statusLabel = status === 0 ? '招募中' : status === 1 ? '已满' : '已结束';
  const createTime = p.createTime ? (typeof p.createTime === 'string' ? p.createTime.replace('T', ' ').substring(0, 16) : '') : '';
  const cover = (p.coverImage && (p.coverImage.startsWith('http') ? p.coverImage : (baseUrl + p.coverImage))) || '/images/partner-banner.png';
  return {
    id: p.id,
    title: p.title || '未命名',
    category: TYPE_NAMES[p.type] || '其他',
    description: p.content || '',
    status: statusText,
    statusLabel,
    cover,
    author: {
      name: p.nickname || '用户',
      avatar: p.avatar || '/images/default-avatar.png'
    },
    members: p.currentCount != null ? p.currentCount : 0,
    maxMembers: p.targetCount != null ? p.targetCount : 2,
    createTime,
    tags: p.tags || [],
    distance: p.location || '',
    match: p.matchScore
  };
}

Page({
  data: {
    partners: [],
    categories: ['全部', '游戏', '运动', '美食', '学习', '旅游'],
    activeCategory: 0,
    isLoading: true,
    showPublishModal: false,
    publishForm: {
      title: '',
      category: '游戏',
      description: '',
      requirements: '',
      contact: ''
    }
  },

  onLoad(options) {
    if (options.filter) {
      // 处理筛选参数
    }
    this.loadPartners();
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({
        selected: 1
      });
    }
  },

  // 加载搭子列表
  loadPartners() {
    this.setData({ isLoading: true });

    return fetchPartnerList()
      .then((list) => {
        this.setData({
          partners: list,
          isLoading: false
        });
      })
      .catch((err) => {
        const msg = err && (err.message || err.errMsg) || '加载失败';
        wx.showToast({
          title: msg,
          icon: 'none'
        });
        this.setData({
          partners: [],
          isLoading: false
        });
      });
  },

  // 选择分类
  selectCategory(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({ activeCategory: index });
    this.loadPartners();
  },

  // 跳转筛选页面
  goToFilter() {
    wx.navigateTo({
      url: '/pages/filter/filter?type=partner'
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

  // 发布搭子
  submitPartner() {
    const form = this.data.publishForm;
    if (!form.title || !form.description) {
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
      url: `/pages/partner-detail/partner-detail?id=${id}`
    });
  },

  onPullDownRefresh() {
    this.loadPartners().finally(() => {
      wx.stopPullDownRefresh();
    });
  }
});