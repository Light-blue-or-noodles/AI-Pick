// pages/message/message.js
const app = getApp();
const IMService = require('../../utils/im');
const { navigateToChatWithPeer } = require('../../utils/navigateToChat.js');

Page({
  data: {
    messages: [],
    isLoading: true,
    needLogin: false,
    imReady: false,
    refresherTriggered: false
  },

  onLoad() {
    if (!wx.getStorageSync('token')) {
      this.setData({ needLogin: true, isLoading: false });
      return;
    }
    this.setData({ needLogin: false });
    this.initIMAndLoadMessages();
  },

  onShow() {
    const that = this;
    setTimeout(function () {
      if (typeof that.getTabBar === 'function') {
        const bar = that.getTabBar();
        if (bar) bar.setData({ selected: 2 });
      }
    }, 0);

    const token = wx.getStorageSync('token');
    if (!token) {
      this.setData({ needLogin: true, isLoading: false, messages: [], imReady: false });
      return;
    }
    if (this.data.needLogin) {
      this.setData({ needLogin: false });
      this.initIMAndLoadMessages();
      return;
    }
    if (this.data.imReady) {
      this.loadMessages().catch(() => {});
    }
  },

  /** 自定义 TabBar 重复点击当前 tab 时刷新 */
  onTabReselect() {
    if (this.data.needLogin || !wx.getStorageSync('token')) return;
    this.setData({ refresherTriggered: true });
    this.initIMAndLoadMessages()
      .finally(() => {
        this.setData({ refresherTriggered: false });
      });
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  resolveAvatar(url) {
    if (!url || typeof url !== 'string') return '';
    const resolved = app.normalizeImageUrl
      ? app.normalizeImageUrl(url, app.globalData.baseUrl)
      : url;
    return resolved || url;
  },

  async initIMAndLoadMessages() {
    try {
      this.setData({ isLoading: true });
      await IMService.initAndLogin();
      IMService.syncUnreadBadgeFromSdk();
      await this.loadMessages();
      IMService.syncUnreadBadgeFromSdk();
      this.setData({ imReady: true });
    } catch (error) {
      console.error('初始化 IM 失败:', error);
      this.setData({ messages: [], imReady: false });
      wx.showToast({
        title: '消息服务连接失败',
        icon: 'none',
        duration: 2500
      });
    } finally {
      this.setData({ isLoading: false, refresherTriggered: false });
    }
  },

  formatMessageTime(lastTime) {
    if (!lastTime) return '';
    const timestamp = Number(lastTime) * 1000;
    if (!Number.isFinite(timestamp)) return '';
    const date = new Date(timestamp);
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');
    return `${month}-${day} ${hour}:${minute}`;
  },

  peerUserIdFromConversation(conversation) {
    const userProfile = conversation.userProfile || {};
    let userId = userProfile.userID || '';
    const cid = conversation.conversationID || '';
    if (!userId && cid.indexOf('C2C') === 0) {
      userId = cid.slice(3);
    }
    return userId;
  },

  async loadMessages() {
    try {
      const conversationList = await IMService.getConversationList();
      const messages = (conversationList || []).map((conversation) => {
        const userProfile = conversation.userProfile || {};
        const lastMessage = conversation.lastMessage || {};
        const userId = this.peerUserIdFromConversation(conversation);
        const nick = userProfile.nick || '';
        const preview = (lastMessage.messageForShow || '').trim();
        const displayName = nick || (userId ? `用户 ${userId}` : '陌生人');
        // IM 各版本/资料源字段不一致：除 avatar 外尝试 faceURL（腾讯云文档常见）
        const avatarRaw =
          userProfile.avatar || userProfile.faceURL || userProfile.faceUrl || '';
        return {
          id: conversation.conversationID,
          conversationID: conversation.conversationID,
          userId,
          nickname: nick,
          displayName,
          avatar: this.resolveAvatar(avatarRaw) || avatarRaw,
          preview: preview || '[暂无预览]',
          time: this.formatMessageTime(lastMessage.lastTime),
          unreadCount: conversation.unreadCount || 0
        };
      });
      this.setData({
        messages,
        isLoading: false
      });
      IMService.syncUnreadBadgeFromSdk();
    } catch (error) {
      console.error('加载会话列表失败:', error);
      this.setData({ messages: [], isLoading: false });
      wx.showToast({
        title: '加载会话失败',
        icon: 'none'
      });
    }
  },

  async onRefresherRefresh() {
    if (this.data.needLogin) {
      this.setData({ refresherTriggered: false });
      return;
    }
    this.setData({ refresherTriggered: true });
    try {
      if (this.data.imReady) {
        await IMService.initAndLogin();
        await this.loadMessages();
      } else {
        await this.initIMAndLoadMessages();
      }
    } finally {
      this.setData({ refresherTriggered: false });
    }
  },

  goToChat(e) {
    const item = e.currentTarget.dataset.item;
    if (!item) return;

    const userId = item.userId || item.targetUserId;
    if (!userId) {
      wx.showToast({ title: '无法打开会话', icon: 'none' });
      return;
    }
    const nickname = item.nickname || item.displayName || '';
    const avatar = item.avatar || '';

    navigateToChatWithPeer({ userId, nickname, avatar });
  },

  deleteMessage() {
    wx.showToast({
      title: '删除会话请使用长按（功能待接入）',
      icon: 'none'
    });
  }
});
