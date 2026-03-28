// pages/chat/chat.js
const app = getApp();
const IMService = require('../../utils/im');
const { post } = require('../../utils/request');

Page({
  data: {
    messages: [],
    inputValue: '',
    isAiTyping: false,
    userId: null,
    isAI: true,
    targetUser: {
      id: null,
      nickname: '',
      avatar: ''
    },
    currentUser: {
      id: null,
      avatar: ''
    },
    scrollIntoView: ''
  },

  unsubscribeNewMessage: null,
  conversationID: '',

  onLoad(options) {
    let userId = options.userId || '';
    let nickname = options.nickname || '';
    let avatar = options.avatar || '';
    try {
      if (typeof userId === 'string' && userId.indexOf('%') !== -1) {
        userId = decodeURIComponent(userId);
      }
      if (typeof nickname === 'string') {
        nickname = decodeURIComponent(nickname);
      }
      if (typeof avatar === 'string') {
        avatar = decodeURIComponent(avatar);
      }
    } catch (e) {
      console.warn('chat query decode', e);
    }

    const isAI = options.isAI === 'true' || options.isAI === true || !userId;

    const currentUserId = wx.getStorageSync('userId');
    let currentUserAvatar =
      wx.getStorageSync('userAvatar') || '/images/default-avatar.png';
    if (app.normalizeImageUrl && currentUserAvatar && currentUserAvatar.indexOf('http') !== 0) {
      currentUserAvatar = app.normalizeImageUrl(currentUserAvatar, app.globalData.baseUrl) || currentUserAvatar;
    }
    let targetAvatar = avatar || '/images/default-avatar.png';
    if (app.normalizeImageUrl && targetAvatar && targetAvatar.indexOf('http') !== 0 && targetAvatar.indexOf('/') === 0) {
      targetAvatar = app.normalizeImageUrl(targetAvatar, app.globalData.baseUrl) || targetAvatar;
    }

    const displayNick = nickname || (userId ? `用户 ${userId}` : '聊天');

    this.setData({
      userId,
      isAI,
      targetUser: {
        id: userId,
        nickname: displayNick,
        avatar: targetAvatar
      },
      currentUser: {
        id: currentUserId,
        avatar: currentUserAvatar
      }
    });

    if (!isAI) {
      wx.setNavigationBarTitle({ title: displayNick.length > 10 ? displayNick.slice(0, 10) + '…' : displayNick });
    } else {
      wx.setNavigationBarTitle({ title: 'AI 助手' });
    }

    if (isAI) {
      this.addWelcomeMessage();
      return;
    }
    if (!wx.getStorageSync('token')) {
      wx.showModal({
        title: '提示',
        content: '请先登录后再聊天',
        showCancel: false,
        success: () => {
          wx.navigateTo({ url: '/pages/login/login' });
        }
      });
      return;
    }
    this.initUserChat();
  },

  onUnload() {
    this.teardownIMListener();
  },

  async initUserChat() {
    const { targetUser } = this.data;
    if (!targetUser.id) return;
    const peerId = Number(targetUser.id);
    if (!Number.isFinite(peerId) || peerId <= 0) {
      wx.showToast({ title: '对方用户 ID 无效', icon: 'none' });
      return;
    }
    try {
      wx.showLoading({ title: '连接中', mask:true });
      try {
        await post('/api/im/prep-peer', { peerUserId: peerId }, { suppressErrorToast: true });
      } catch (e) {
        wx.hideLoading();
        const msg =
          (e && (e.message || e.msg)) ||
          (e && e.data && (e.data.message || e.data.msg)) ||
          '无法完成腾讯云 IM 开户，请检查后端日志或 IM 控制台配置';
        console.error('prep-peer（对方 IM 开户）:', e);
        wx.showModal({
          title: 'IM 未就绪',
          content: String(msg),
          showCancel: false
        });
        return;
      }
      await IMService.initAndLogin();
      this.conversationID = IMService.buildC2CConversationID(targetUser.id);
      await IMService.setMessageRead(this.conversationID);
      await this.loadTimHistory();
      this.setupIMListener();
      wx.hideLoading();
    } catch (error) {
      wx.hideLoading();
      console.error('聊天初始化失败:', error);
      wx.showToast({
        title: 'IM 连接失败',
        icon: 'none'
      });
    }
  },

  async loadTimHistory() {
    const { targetUser, currentUser } = this.data;
    if (!targetUser.id || !this.conversationID) return;
    try {
      const { messageList } = await IMService.getMessageHistory(this.conversationID, '', 40);
      const formattedMessages = (messageList || [])
        .map((msg) => this.mapTIMMessage(msg, targetUser, currentUser))
        .filter(Boolean);
      this.setData({ messages: formattedMessages });
      this.scrollToBottom();
    } catch (error) {
      console.error('加载聊天记录失败:', error);
      wx.showToast({
        title: '加载消息失败',
        icon: 'none'
      });
    }
  },

  setupIMListener() {
    this.teardownIMListener();
    this.unsubscribeNewMessage = IMService.onNewMessage((list) => {
      const cid = this.conversationID;
      const incoming = (list || []).filter((m) => m.conversationID === cid);
      const formatted = incoming
        .map((m) => this.mapTIMMessage(m, this.data.targetUser, this.data.currentUser))
        .filter(Boolean);
      if (formatted.length) {
        this.appendMessages(formatted);
        IMService.setMessageRead(cid).catch(() => {});
        this.scrollToBottom();
      }
    });
  },

  teardownIMListener() {
    if (typeof this.unsubscribeNewMessage === 'function') {
      this.unsubscribeNewMessage();
      this.unsubscribeNewMessage = null;
    }
  },

  mapTIMMessage(msg, targetUser, currentUser) {
    if (!msg) return null;
    // tim-wx-sdk 文本消息 type 为 TIMTextElem
    if (msg.type !== 'TIMTextElem') {
      return null;
    }
    const content = (msg.payload && msg.payload.text) || '';
    if (!content) return null;
    const isSelf = msg.flow === 'out';
    return {
      id: msg.ID || `${msg.time}-${Math.random()}`,
      type: isSelf ? 'user' : 'other',
      content,
      time: this.formatTime(new Date((msg.time || Date.now() / 1000) * 1000)),
      avatar: isSelf ? currentUser.avatar : targetUser.avatar
    };
  },

  appendMessages(newMessages) {
    const exists = {};
    (this.data.messages || []).forEach((item) => {
      exists[item.id] = true;
    });
    const merged = [...this.data.messages];
    (newMessages || []).forEach((item) => {
      if (!exists[item.id]) {
        merged.push(item);
        exists[item.id] = true;
      }
    });
    this.setData({ messages: merged });
  },

  addWelcomeMessage() {
    const welcomeMessage = {
      id: 1,
      type: 'ai',
      content: '你好！我是 AI-Pick AI 智能助手 🎉\n\n我可以帮你：\n• 找到志同道合的搭子\n• 推荐附近有趣的活动\n• 智能匹配聊天话题\n• 解答你的一切问题\n\n请问有什么可以帮到你？',
      time: this.formatTime(new Date()),
      avatar: '/images/ai-icon.png'
    };
    this.setData({
      messages: [welcomeMessage]
    });
  },

  onInputChange(e) {
    this.setData({
      inputValue: e.detail.value
    });
  },

  sendMessage() {
    const content = this.data.inputValue.trim();
    if (!content) return;

    if (this.data.isAI) {
      this.sendAIMessage(content);
    } else {
      this.sendUserMessage(content);
    }
  },

  sendAIMessage(content) {
    const userMessage = {
      id: Date.now(),
      type: 'user',
      content: content,
      time: this.formatTime(new Date()),
      avatar: this.data.currentUser.avatar
    };

    this.setData({
      messages: [...this.data.messages, userMessage],
      inputValue: '',
      isAiTyping: true
    });

    this.scrollToBottom();
    this.getAIResponse(content);
  },

  async sendUserMessage(content) {
    const { targetUser, currentUser } = this.data;
    if (!targetUser.id) return;
    try {
      const sent = await IMService.sendSingleMessage(targetUser.id, content);
      this.setData({ inputValue: '' });
      const mapped = this.mapTIMMessage(sent, targetUser, currentUser);
      if (mapped) {
        this.appendMessages([mapped]);
        this.scrollToBottom();
      } else {
        await this.loadTimHistory();
      }
    } catch (error) {
      console.error('发送消息失败:', error);
      wx.showToast({
        title: '发送失败，请稍后重试',
        icon: 'none'
      });
    }
  },

  getAIResponse(content) {
    const token = wx.getStorageSync('token');
    const userId = wx.getStorageSync('userId');
    wx.request({
      url: `${app.globalData.baseUrl}/api/ai/chat`,
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(userId != null && userId !== '' ? { 'X-User-Id': String(userId) } : {})
      },
      data: {
        message: content,
        sessionId: wx.getStorageSync('sessionId') || ''
      },
      success: (res) => {
        if (res.data && res.data.code === 0 && res.data.data) {
          const d = res.data.data;
          const text = d.reply || d.response || '';
          if (d.sessionId) {
            wx.setStorageSync('sessionId', d.sessionId);
          }
          this.addAiMessage(text || this.getDefaultResponse(content));
        } else {
          this.addAiMessage(this.getDefaultResponse(content));
        }
      },
      fail: () => {
        this.addAiMessage(this.getDefaultResponse(content));
      }
    });
  },

  getDefaultResponse(content) {
    const responses = [
      '听起来很有趣！能不能告诉我更多关于你的兴趣爱好？',
      '我明白了～根据你的需求，我可以为你推荐一些附近的搭子和活动',
      '太棒了！让我帮你分析一下最适合你的社交方式',
      '好的，我正在为你匹配合适的搭子，请稍等...',
      '根据你的描述，我发现有几个活动非常适合你参加'
    ];
    return responses[Math.floor(Math.random() * responses.length)];
  },

  addAiMessage(content) {
    const aiMessage = {
      id: Date.now() + 1,
      type: 'ai',
      content: content,
      time: this.formatTime(new Date()),
      avatar: '/images/ai-icon.png'
    };

    this.setData({
      messages: [...this.data.messages, aiMessage],
      isAiTyping: false
    });

    this.scrollToBottom();
  },

  scrollToBottom() {
    setTimeout(() => {
      const list = this.data.messages || [];
      if (!list.length) return;
      this.setData({
        scrollIntoView: `msg-${list[list.length - 1].id}`
      });
    }, 100);
  },

  formatTime(date) {
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  },

  onQuickReply(e) {
    const reply = e.currentTarget.dataset.reply;
    this.setData({ inputValue: reply });
    this.sendMessage();
  },

  onShareAppMessage() {
    return {
      title: 'AI-Pick AI - 智能聊天',
      path: '/pages/chat/chat'
    };
  }
});
