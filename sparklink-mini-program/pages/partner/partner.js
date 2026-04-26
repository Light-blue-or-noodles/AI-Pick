// pages/partner/partner.js
const app = getApp();
const { mapPartnerForList } = require('../../utils/partnerListMap.js');

/**
 * 封装 wx.request 为 Promise，请求搭子列表接口
 * @param scopeType 展示范围：company-同事搭，school-校友搭，platform-Pick搭（全平台）
 * @returns {Promise<Array>} 搭子列表（已映射为页面所需结构）
 */
function fetchPartnerList(scopeType) {
  const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
  const header = { 'Content-Type': 'application/json' };
  const token = app.globalData.token || wx.getStorageSync('token');
  const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
  if (token) header['Authorization'] = 'Bearer ' + token;
  if (userId) header['X-User-Id'] = String(userId);

  const query = scopeType ? `?scopeType=${encodeURIComponent(scopeType)}` : '';
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${baseUrl}/api/partner${query}`,
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

Page({
  data: {
    partners: [],
    scopeTabs: [
      // 将 Pick 搭子放在第一个，进入页面默认展示 Pick 搭子
      { key: 'platform', label: 'Pick搭' },
      { key: 'company', label: '同事搭' },
      { key: 'school', label: '校友搭' }
    ],
    activeScope: 0,
    isLoading: true
  },

  onLoad(options) {
    if (options.filter) {
      // 处理筛选参数
    }
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
          runId: 'partner-page-load',
          hypothesisId: 'H1',
          location: 'pages/partner/partner.js:onLoad',
          message: 'partner page loaded',
          data: {
            hasShowPublishModalFn: typeof this.showPublishModal === 'function'
          },
          timestamp: Date.now()
        })
      }).catch(() => {});
    } catch (e) {}
    // #endregion agent log
    this.loadPartners();
  },

  onShow() {
    const app = getApp();
    const that = this;
    setTimeout(function () {
      if (typeof that.getTabBar === 'function') {
        const bar = that.getTabBar();
        if (bar) bar.setData({ selected: 1 });
      }
      if (app && typeof app.refreshTabBarUnreadBadge === 'function') {
        app.refreshTabBarUnreadBadge();
      }
    }, 0);
    if (app && typeof app.trySyncImUnreadForTabPages === 'function') {
      app.trySyncImUnreadForTabPages();
    }
  },

  // 加载搭子列表（按当前选中的展示范围：同事搭/校友搭/Pick搭）
  loadPartners() {
    this.setData({ isLoading: true });
    const scopeType = this.data.scopeTabs[this.data.activeScope].key;

    return fetchPartnerList(scopeType)
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

  // 选择展示范围：同事搭 / 校友搭 / Pick搭
  selectScope(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({ activeScope: index });
    this.loadPartners();
  },

  // 跳转筛选页面
  goToFilter() {
    wx.navigateTo({
      url: '/pages/filter/filter'
    });
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