// pages/index/index.js — 新首页
Page({
  onShow() {
    const that = this;
    setTimeout(function () {
      if (typeof that.getTabBar === 'function') {
        const bar = that.getTabBar();
        if (bar) {
          bar.setData({ selected: 0 });
        }
      }
    }, 0);
  },

  onQuickGame() {
    wx.navigateTo({
      url: '/pages/ai-chat/ai-chat?quick=' + encodeURIComponent('推荐一些游戏搭子')
    });
  },

  onQuickSport() {
    wx.navigateTo({
      url: '/pages/ai-chat/ai-chat?quick=' + encodeURIComponent('推荐一些运动搭子')
    });
  },

  goToAiChat() {
    wx.navigateTo({
      url: '/pages/ai-chat/ai-chat'
    });
  }
});
