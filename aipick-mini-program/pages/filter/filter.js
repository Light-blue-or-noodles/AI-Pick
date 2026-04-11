// pages/filter/filter.js
Page({
  data: {
    resultCount: 0,

    distance: 'all',
    gender: 'all',
    partnerType: 'all',
    matchLevel: 'all',
    partnerStatus: 'all'
  },

  onLoad() {
    wx.setNavigationBarTitle({
      title: '搭子筛选'
    });

    const filterKey = 'filter_partner';
    const savedFilter = wx.getStorageSync(filterKey);
    if (savedFilter) {
      this.setData({ ...savedFilter });
    }

    this.updateResultCount();
  },

  goBack() {
    wx.navigateBack();
  },

  resetFilter() {
    this.setData({
      distance: 'all',
      gender: 'all',
      partnerType: 'all',
      matchLevel: 'all',
      partnerStatus: 'all'
    });
    this.updateResultCount();
  },

  selectDistance(e) {
    this.setData({ distance: e.currentTarget.dataset.value });
    this.updateResultCount();
  },

  selectGender(e) {
    this.setData({ gender: e.currentTarget.dataset.value });
    this.updateResultCount();
  },

  selectPartnerType(e) {
    this.setData({ partnerType: e.currentTarget.dataset.value });
    this.updateResultCount();
  },

  selectMatchLevel(e) {
    this.setData({ matchLevel: e.currentTarget.dataset.value });
    this.updateResultCount();
  },

  selectPartnerStatus(e) {
    this.setData({ partnerStatus: e.currentTarget.dataset.value });
    this.updateResultCount();
  },

  updateResultCount() {
    const count = Math.floor(Math.random() * 50) + 10;
    this.setData({ resultCount: count });
  },

  applyFilter() {
    const filterKey = 'filter_partner';
    wx.setStorageSync(filterKey, {
      distance: this.data.distance,
      gender: this.data.gender,
      partnerType: this.data.partnerType,
      matchLevel: this.data.matchLevel,
      partnerStatus: this.data.partnerStatus
    });

    const pages = getCurrentPages();
    const prevPage = pages[pages.length - 2];

    if (prevPage) {
      prevPage.setData({
        filter: {
          distance: this.data.distance,
          gender: this.data.gender,
          partnerType: this.data.partnerType,
          matchLevel: this.data.matchLevel,
          partnerStatus: this.data.partnerStatus
        }
      });
    }

    wx.navigateBack();
  }
});
