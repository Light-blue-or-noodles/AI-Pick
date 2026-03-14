Component({
  data: {
    selected: 0,
    list: [
      { pagePath: '/pages/index/index', text: '首页', icon: '🏠' },
      { pagePath: '/pages/partner/partner', text: '搭子', icon: '👥' },
      { pagePath: '/pages/activity/activity', text: '活动', icon: '📅' },
      { pagePath: '/pages/message/message', text: '消息', icon: '💬' },
      { pagePath: '/pages/profile/profile', text: '我的', icon: '👤' }
    ]
  },
  methods: {
    switchTab(e) {
      const index = e.currentTarget.dataset.index;
      const item = this.data.list[index];
      wx.switchTab({ url: item.pagePath });
      this.setData({ selected: index });
    }
  }
});
