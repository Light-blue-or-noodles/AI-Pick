// pages/message/message.js
const app = getApp();

Page({
  data: {
    messages: [],
    isLoading: true
  },

  onLoad() {
    this.loadMessages();
  },

  onShow() {
    const that = this;
    setTimeout(function () {
      if (typeof that.getTabBar === 'function') {
        const bar = that.getTabBar();
        if (bar) bar.setData({ selected: 3 });
      }
    }, 0);
  },

  // 加载消息列表 GET /api/message
  loadMessages() {
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    wx.request({
      url: `${baseUrl}/api/message`,
      method: 'GET',
      header: {
        'X-User-Id': wx.getStorageSync('userId') || '',
        'Authorization': 'Bearer ' + (wx.getStorageSync('token') || '')
      },
      success: (res) => {
        const data = res.data;
        if (res.statusCode === 200 && data && (data.code === 0 || data.code === 200)) {
          const list = data.data != null ? data.data : (Array.isArray(data) ? data : []);
          this.setData({ messages: list, isLoading: false });
        } else {
          this.setData({ messages: [], isLoading: false });
          wx.showToast({ title: (data && data.message) || '加载失败', icon: 'none' });
        }
        wx.stopPullDownRefresh();
      },
      fail: (err) => {
        this.setData({ messages: [], isLoading: false });
        wx.showToast({
          title: err.errMsg && err.errMsg.indexOf('url') !== -1 ? '网络错误' : '加载失败',
          icon: 'none'
        });
        wx.stopPullDownRefresh();
      }
    });
  },

  // 跳转聊天页面
  goToChat(e) {
    const userId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/chat/chat?userId=${userId}`
    });
  },

  // 标记已读
  markAsRead(e) {
    const conversationId = e.currentTarget.dataset.id;
    wx.request({
      url: `${app.globalData.baseUrl}/api/message/read`,
      method: 'POST',
      header: {
        'X-User-Id': '1'
      },
      data: { conversationId },
      success: () => {
        this.loadMessages();
      }
    });
  },

  // 删除消息（模拟）
  deleteMessage(e) {
    wx.showToast({
      title: '删除功能待实现',
      icon: 'none'
    });
  },

  onPullDownRefresh() {
    this.loadMessages();
  }
});