// pages/ai-chat/ai-chat.js
Page({
  data: {
    inputValue: '',
    inputFocus: true,
    scrollIntoView: '',
    messages: [],
    sessionId: '',
    userInfo: {
      avatar: '',
      nickname: ''
    },
    isLoading: false
  },

  onLoad() {
    // 获取用户信息
    const userInfo = wx.getStorageSync('userInfo') || {};
    const sessionId = wx.getStorageSync('sessionId') || '';
    this.setData({ userInfo, sessionId });
    this.loadChatHistory();
  },

  loadChatHistory() {
    const sessionId = this.data.sessionId;
    wx.request({
      url: `http://localhost:8080/api/chat/history/${sessionId}`,
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data) {
          const list = Array.isArray(res.data) ? res.data : (res.data.data || res.data.list || []);
          this.setData({ messages: list });
        }
      }
    });
  },

  onInput(e) {
    this.setData({ inputValue: e.detail.value });
  },

  goBack() {
    wx.navigateBack();
  },

  quickAsk(e) {
    const text = e.currentTarget.dataset.text;
    this.setData({ inputValue: text });
    this.sendMessage();
  },

  sendMessage() {
    const content = this.data.inputValue.trim();
    if (!content || this.data.isLoading) return;

    const messageId = Date.now();
    const userMessage = {
      id: messageId,
      content: content,
      reply: '',
      recommends: []
    };

    this.setData({
      messages: [...this.data.messages, userMessage],
      inputValue: '',
      isLoading: true,
      inputFocus: true
    });

    // 滚动到底部
    this.scrollToBottom();

    // 调用后端 API 获取 AI 回复
    const userId = wx.getStorageSync('userId') || '';
    wx.request({
      url: 'http://localhost:8080/api/chat',
      method: 'POST',
      header: {
        'content-type': 'application/json',
        'X-User-Id': userId
      },
      data: {
        message: content,
        sessionId: this.data.sessionId
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data) {
          const { reply = '', recommends = [], sessionId: newSessionId } = res.data;
          if (newSessionId) {
            wx.setStorageSync('sessionId', newSessionId);
            this.setData({ sessionId: newSessionId });
          }
          const updatedMessages = this.data.messages.map(msg => {
            if (msg.id === userMessage.id) {
              return { ...msg, reply, recommends };
            }
            return msg;
          });
          this.setData({
            messages: updatedMessages,
            isLoading: false
          });
          this.scrollToBottom();
        } else {
          this.setData({ isLoading: false });
          wx.showToast({ title: '回复失败', icon: 'none' });
        }
      },
      fail: () => {
        this.setData({ isLoading: false });
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  },

  scrollToBottom() {
    setTimeout(() => {
      const len = this.data.messages.length;
      if (len > 0) {
        this.setData({
          scrollIntoView: `msg-${this.data.messages[len - 1].id}`
        });
      }
    }, 100);
  },

  goToDetail(e) {
    const { type, id } = e.currentTarget.dataset;
    if (type === 'partner') {
      wx.navigateTo({
        url: `/pages/partner-detail/partner-detail?id=${id}`
      });
    } else if (type === 'activity') {
      wx.navigateTo({
        url: `/pages/activity-detail/activity-detail?id=${id}`
      });
    }
  }
});