// pages/filter/filter.js
Page({
  data: {
    pageType: 'partner', // partner | activity
    resultCount: 0,
    
    // 距离
    distance: 'all',
    // 性别
    gender: 'all',
    // 搭子类型
    partnerType: 'all',
    // 活动类型
    activityType: 'all',
    // 匹配度
    matchLevel: 'all',
    // 时间范围
    timeRange: 'all',
    // 活动状态
    activityStatus: 'all',
    // 搭子状态
    partnerStatus: 'all'
  },

  onLoad(options) {
    const pageType = options.type || 'partner';
    this.setData({ pageType });

    // 二级菜单标题展示在顶部导航栏
    wx.setNavigationBarTitle({
      title: pageType === 'partner' ? '搭子筛选' : '活动筛选'
    });
    
    // 读取之前的筛选条件
    const filterKey = `filter_${pageType}`;
    const savedFilter = wx.getStorageSync(filterKey);
    if (savedFilter) {
      this.setData({ ...savedFilter });
    }
    
    this.updateResultCount();
  },

  goBack() {
    wx.navigateBack();
  },

  // 重置筛选
  resetFilter() {
    this.setData({
      distance: 'all',
      gender: 'all',
      partnerType: 'all',
      activityType: 'all',
      matchLevel: 'all',
      timeRange: 'all',
      activityStatus: 'all',
      partnerStatus: 'all'
    });
    this.updateResultCount();
  },

  // 距离选择
  selectDistance(e) {
    this.setData({ distance: e.currentTarget.dataset.value });
    this.updateResultCount();
  },

  // 性别选择
  selectGender(e) {
    this.setData({ gender: e.currentTarget.dataset.value });
    this.updateResultCount();
  },

  // 搭子类型选择
  selectPartnerType(e) {
    this.setData({ partnerType: e.currentTarget.dataset.value });
    this.updateResultCount();
  },

  // 活动类型选择
  selectActivityType(e) {
    this.setData({ activityType: e.currentTarget.dataset.value });
    this.updateResultCount();
  },

  // 匹配度选择
  selectMatchLevel(e) {
    this.setData({ matchLevel: e.currentTarget.dataset.value });
    this.updateResultCount();
  },

  // 时间范围选择
  selectTimeRange(e) {
    this.setData({ timeRange: e.currentTarget.dataset.value });
    this.updateResultCount();
  },

  // 活动状态选择
  selectActivityStatus(e) {
    this.setData({ activityStatus: e.currentTarget.dataset.value });
    this.updateResultCount();
  },

  // 搭子状态选择
  selectPartnerStatus(e) {
    this.setData({ partnerStatus: e.currentTarget.dataset.value });
    this.updateResultCount();
  },

  // 更新结果数量
  updateResultCount() {
    // 模拟根据筛选条件计算结果数量
    let count = Math.floor(Math.random() * 50) + 10;
    this.setData({ resultCount: count });
  },

  // 应用筛选
  applyFilter() {
    const filterKey = `filter_${this.data.pageType}`;
    wx.setStorageSync(filterKey, {
      distance: this.data.distance,
      gender: this.data.gender,
      partnerType: this.data.partnerType,
      activityType: this.data.activityType,
      matchLevel: this.data.matchLevel,
      timeRange: this.data.timeRange,
      activityStatus: this.data.activityStatus,
      partnerStatus: this.data.partnerStatus
    });

    // 返回上一页并传递筛选结果
    const pages = getCurrentPages();
    const prevPage = pages[pages.length - 2];
    
    if (prevPage) {
      prevPage.setData({
        filter: {
          distance: this.data.distance,
          gender: this.data.gender,
          partnerType: this.data.partnerType,
          activityType: this.data.activityType,
          matchLevel: this.data.matchLevel,
          timeRange: this.data.timeRange,
          activityStatus: this.data.activityStatus,
          partnerStatus: this.data.partnerStatus
        }
      });
    }

    wx.navigateBack();
  }
});