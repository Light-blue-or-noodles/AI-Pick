// pages/chat/chat.js
const app = getApp();

Page({
  data: {
    messages: [],
    inputValue: '',
    isAiTyping: false,
    userId: null,
    isAI: true
  },

  onLoad(options) {
    const userId = options.userId;
    this.setData({ userId });
    
    // 添加欢迎消息
    this.addWelcomeMessage();
  },

  // 欢迎消息
  addWelcomeMessage() {
    const welcomeMessage = {
      id: 1,
      type: 'ai',
      content: '你好！我是 AI-Pick AI 智能助手 🎉\n\n我可以帮你：\n• 找到志同道合的搭子\n• 推荐附近有趣的活动\n• 智能匹配聊天话题\n• 解答你的一切问题\n\n请问有什么可以帮到你？',
      time: this.formatTime(new Date())
    };
    this.setData({
      messages: [welcomeMessage]
    });
  },

  // 输入框变化
  onInputChange(e) {
    this.setData({
      inputValue: e.detail.value
    });
  },

  // 发送消息
  sendMessage() {
    const content = this.data.inputValue.trim();
    if (!content) return;

    // 添加用户消息
    const userMessage = {
      id: Date.now(),
      type: 'user',
      content: content,
      time: this.formatTime(new Date())
    };

    this.setData({
      messages: [...this.data.messages, userMessage],
      inputValue: '',
      isAiTyping: true
    });

    // 滚动到底部
    this.scrollToBottom();

    // 模拟 AI 响应
    this.getAIResponse(content);
  },

  // 获取 AI 响应
  getAIResponse(content) {
    wx.request({
      url: `${app.globalData.baseUrl}/api/ai/chat`,
      method: 'POST',
      data: {
        message: content,
        history: this.data.messages.slice(-10)
      },
      success: (res) => {
        if (res.data.code === 0) {
          this.addAiMessage(res.data.data.response);
        } else {
          this.addAiMessage(this.getDefaultResponse(content));
        }
      },
      fail: () => {
        // 使用默认响应
        this.addAiMessage(this.getDefaultResponse(content));
      }
    });
  },

  // 默认响应
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

  // 添加 AI 消息
  addAiMessage(content) {
    const aiMessage = {
      id: Date.now() + 1,
      type: 'ai',
      content: content,
      time: this.formatTime(new Date())
    };

    this.setData({
      messages: [...this.data.messages, aiMessage],
      isAiTyping: false
    });

    this.scrollToBottom();
  },

  // 滚动到底部
  scrollToBottom() {
    setTimeout(() => {
      wx.pageScrollTo({
        scrollTop: 99999,
        duration: 300
      });
    }, 100);
  },

  // 格式化时间
  formatTime(date) {
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  },

  // 快速回复
  onQuickReply(e) {
    const reply = e.currentTarget.dataset.reply;
    this.setData({ inputValue: reply });
    this.sendMessage();
  },

  // 分享
  onShareAppMessage() {
    return {
      title: 'AI-Pick AI - 智能聊天',
      path: '/pages/chat/chat'
    };
  }
});