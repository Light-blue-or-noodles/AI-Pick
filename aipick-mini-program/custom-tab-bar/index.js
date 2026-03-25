Component({
  data: {
    selected: 0,
    list: [
      { pagePath: '/pages/index/index', text: '首页', key: 'home' },
      { pagePath: '/pages/partner/partner', text: '搭子', key: 'partner' },
      { pagePath: '/pages/activity/activity', text: '活动', key: 'activity' },
      { pagePath: '/pages/message/message', text: '消息', key: 'message' },
      { pagePath: '/pages/profile/profile', text: '我的', key: 'profile' }
    ]
  },
  methods: {
    switchTab(e) {
      const index = e.currentTarget.dataset.index;
      const item = this.data.list[index];
      const pages = getCurrentPages();
      const current = pages[pages.length - 1];
      const currentPath = current ? ('/' + current.route) : '';

      // 若重复点击当前 tab，则通知页面执行刷新逻辑（如首页下拉刷新/重新加载数据）
      if (currentPath === item.pagePath && typeof current.onTabReselect === 'function') {
        current.onTabReselect();
        return;
      }

      wx.switchTab({ url: item.pagePath });
    }
  }
});
