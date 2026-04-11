Component({
  data: {
    selected: 0,
    imUnreadBadgeText: '',
    list: [
      { pagePath: '/pages/index/index', text: '首页', key: 'home' },
      { pagePath: '/pages/partner/partner', text: '搭子', key: 'partner' },
      { pagePath: '/pages/message/message', text: '消息', key: 'message' },
      { pagePath: '/pages/profile/profile', text: '我的', key: 'profile' }
    ]
  },

  lifetimes: {
    attached() {
      const app = getApp();
      const n = (app.globalData && app.globalData.imUnreadCount) || 0;
      this.applyImUnreadBadge(n);
    }
  },

  pageLifetimes: {
    show() {
      const app = getApp();
      const n = (app.globalData && app.globalData.imUnreadCount) || 0;
      this.applyImUnreadBadge(n);
    }
  },

  methods: {
    applyImUnreadBadge(count) {
      const n = Math.max(0, Math.floor(Number(count) || 0));
      const text = n > 99 ? '99+' : n > 0 ? String(n) : '';
      try {
        const app = getApp();
        if (app && app.globalData) {
          app.globalData.imUnreadCount = n;
        }
      } catch (e) {
        console.warn('tabBar applyImUnreadBadge', e);
      }
      this.setData({ imUnreadBadgeText: text });
    },

    switchTab(e) {
      const index = e.currentTarget.dataset.index;
      const item = this.data.list[index];
      const pages = getCurrentPages();
      const current = pages[pages.length - 1];
      const currentPath = current ? '/' + current.route : '';

      if (currentPath === item.pagePath && typeof current.onTabReselect === 'function') {
        current.onTabReselect();
        return;
      }

      wx.switchTab({ url: item.pagePath });
    }
  }
});
