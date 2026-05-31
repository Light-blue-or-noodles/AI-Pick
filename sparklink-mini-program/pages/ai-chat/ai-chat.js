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

  onLoad(options) {
    let quick = '';
    if (options && (options.quick || options.q)) {
      const raw = options.quick || options.q;
      try {
        quick = decodeURIComponent(String(raw));
      } catch (e) {
        quick = String(raw);
      }
    }
    this._pendingEntryQuick = quick;

    const userInfo = wx.getStorageSync('userInfo') || {};
    const sessionId = wx.getStorageSync('sessionId') || '';
    if (userInfo.avatar && (String(userInfo.avatar).indexOf('__tmp__') !== -1 || String(userInfo.avatar).indexOf('://tmp/') !== -1 || (String(userInfo.avatar).indexOf('127.0.0.1') !== -1 && String(userInfo.avatar).indexOf(':8080') === -1))) {
      userInfo.avatar = '/images/default-avatar.png';
    }
    if (userInfo.avatar && userInfo.avatar.startsWith('http') && /localhost|127\.0\.0\.1/.test(userInfo.avatar)) {
      const app = getApp();
      if (app.normalizeImageUrl) userInfo.avatar = app.normalizeImageUrl(userInfo.avatar);
    }
    this.setData({ userInfo, sessionId });
    this.loadChatHistory();
  },

  /**
   * 首页等入口用 ?quick= 传入首条问题；仅在当前无历史消息时自动发送
   */
  tryEntryQuick() {
    const text = (this._pendingEntryQuick || '').trim();
    if (!text) {
      return;
    }
    if (this.data.messages && this.data.messages.length > 0) {
      this._pendingEntryQuick = '';
      return;
    }
    this._pendingEntryQuick = '';
    this.sendMessage(text);
  },

  loadChatHistory() {
    const sessionId = (this.data.sessionId || '').trim();
    if (!sessionId) {
      this.setData({ messages: [] }, () => {
        this.tryEntryQuick();
      });
      return;
    }
    const app = getApp();
    const baseUrl = (app && app.globalData && app.globalData.baseUrl) ? String(app.globalData.baseUrl).replace(/\/$/, '') : 'https://www.aipick.cloud';
    const token = wx.getStorageSync('token');
    const userId = wx.getStorageSync('userId');
    wx.request({
      url: `${baseUrl}/api/chat/ai/history/${sessionId}`,
      method: 'GET',
      header: {
        Authorization: token ? `Bearer ${token}` : '',
        'X-User-Id': userId ? String(userId) : ''
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 0) {
          const raw = Array.isArray(res.data.data) ? res.data.data : [];
          const list = [];
          for (let i = 0; i < raw.length; i++) {
            if (raw[i].type === 1 && i + 1 < raw.length && raw[i + 1].type === 2) {
              list.push({
                id: raw[i].id,
                content: raw[i].content || '',
                reply: raw[i + 1].content || '',
                recommends: []
              });
              i++;
            }
          }
          this.setData({ messages: list }, () => {
            this.tryEntryQuick();
          });
        } else {
          this.setData({ messages: [] }, () => {
            this.tryEntryQuick();
          });
        }
      },
      fail: () => {
        this.setData({ messages: [] }, () => {
          this.tryEntryQuick();
        });
      }
    });
  },

  onInput(e) {
    this.setData({ inputValue: e.detail.value });
  },

  quickAsk(e) {
    const text = (e.currentTarget && e.currentTarget.dataset && e.currentTarget.dataset.text) || '';
    const trimmed = String(text).trim();
    if (!trimmed || this.data.isLoading) return;
    this.sendMessage(trimmed);
  },

  sendMessage(contentFromInput) {
    const content = typeof contentFromInput === 'string'
      ? contentFromInput.trim()
      : (this.data.inputValue || '').trim();
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
    const baseUrl = (getApp() && getApp().globalData && getApp().globalData.baseUrl) ? String(getApp().globalData.baseUrl).replace(/\/$/, '') : 'https://www.aipick.cloud';
    wx.request({
      url: `${baseUrl}/api/chat`,
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
          // 后端统一包装为 Result，真实数据在 res.data.data
          const payload = (res.data && res.data.data !== undefined) ? res.data.data : res.data;
          let { reply = '', recommends = [], sessionId: newSessionId } = payload || {};
          const app = getApp();
          const baseUrl = (app.globalData && app.globalData.baseUrl) ? String(app.globalData.baseUrl).replace(/\/$/, '') : 'https://www.aipick.cloud';
          const toFullUrl = (path) => {
            if (!path) return '';
            if (path.startsWith('http')) return (app.normalizeImageUrl ? app.normalizeImageUrl(path, baseUrl) : path);
            const p = path.startsWith('/') ? path : '/' + path;
            if (p.indexOf('/api/') === 0) {
              return baseUrl + p;
            }
            return baseUrl + '/api' + p;
          };
          if (recommends && recommends.length) {
            recommends = recommends.map(r => {
              let avatar = r.avatar;
              if (!avatar || avatar === '') {
                avatar = '/images/partner-banner.jpg';
              } else {
                avatar = toFullUrl(avatar);
              }
              return { ...r, avatar };
            });
          }
          if (newSessionId) {
            wx.setStorageSync('sessionId', newSessionId);
            this.setData({ sessionId: newSessionId });
          }
          const updatedMessages = this.data.messages.map(msg => {
            if (msg.id === userMessage.id) {
              return { ...msg, reply, recommends: recommends || [] };
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
          wx.showToast({ title: res.data && res.data.message ? res.data.message : '回复失败', icon: 'none' });
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
          scrollIntoView: `msg-ai-${this.data.messages[len - 1].id}`
        });
      }
    }, 100);
  },

  goToDetail(e) {
    const { id, type } = e.currentTarget.dataset;
    if (!id) return;
    if (type === 'activity') {
      wx.showToast({ title: '活动详情暂未开放', icon: 'none' });
      return;
    }
    wx.navigateTo({
      url: `/pages/partner-detail/partner-detail?id=${id}`
    });
  }
});