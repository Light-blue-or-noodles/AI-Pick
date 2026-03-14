// pages/partner-publish/partner-publish.js
const app = getApp();

Page({
  data: {
    title: '',
    description: '',
    partnerType: '',
    memberCount: 3,
    scope: 'all',
    titleLength: 0,
    descLength: 0,
    partnerTypes: ['游戏', '运动', '学习', '美食', '旅行', '电影', '音乐', '其他'],
    scopes: [
      { value: 'all', label: '全部' },
      { value: 'colleague', label: '仅同事' },
      { value: 'alumni', label: '仅校友' },
      { value: 'nearby', label: '附近' }
    ],
    isPublishing: false
  },

  onLoad(options) {
    // Pre-fill if editing
    if (options.id) {
      this.setData({ partnerId: options.id });
      this.fetchPartnerData(options.id);
    }
  },

  onUnload() {
    // Check if there are unsaved changes
    if (this.data.title || this.data.description) {
      // In real app, show confirmation dialog
    }
  },

  // Fetch partner data for editing
  fetchPartnerData(id) {
    // Simulated data
    this.setData({
      title: '寻找羽毛球搭子',
      description: '周末想打球，求带一带',
      partnerType: '运动',
      memberCount: 3,
      scope: 'all',
      titleLength: 6,
      descLength: 8
    });
  },

  // Go back
  onGoBack() {
    if (this.data.title || this.data.description) {
      wx.showModal({
        title: '提示',
        content: '确定要放弃发布吗？',
        success: (res) => {
          if (res.confirm) {
            wx.navigateBack();
          }
        }
      });
    } else {
      wx.navigateBack();
    }
  },

  // Input title
  onTitleInput(e) {
    const value = e.detail.value;
    if (value.length <= 20) {
      this.setData({
        title: value,
        titleLength: value.length
      });
    }
  },

  // Input description
  onDescInput(e) {
    const value = e.detail.value;
    if (value.length <= 200) {
      this.setData({
        description: value,
        descLength: value.length
      });
    }
  },

  // Select partner type
  onSelectType(e) {
    const index = e.currentTarget.dataset.index;
    const type = this.data.partnerTypes[index];
    this.setData({ partnerType: type });
  },

  // Decrease member count
  onDecreaseCount() {
    if (this.data.memberCount > 2) {
      this.setData({ memberCount: this.data.memberCount - 1 });
    }
  },

  // Increase member count
  onIncreaseCount() {
    if (this.data.memberCount < 20) {
      this.setData({ memberCount: this.data.memberCount + 1 });
    }
  },

  // Select scope
  onSelectScope(e) {
    const value = e.currentTarget.dataset.value;
    this.setData({ scope: value });
  },

  // AI help to complete
  onAIComplete() {
    wx.showLoading({ title: 'AI 生成中...' });
    
    // Simulated AI completion
    setTimeout(() => {
      wx.hideLoading();
      this.setData({
        title: '寻找志同道合的运动搭子',
        titleLength: 10,
        description: '本人热爱运动，周末喜欢打羽毛球、跑步、健身。希望找到志同道合的伙伴一起锻炼，互相督促，共同进步。要求：年龄相仿，地点相近，最好有共同兴趣。',
        descLength: 58
      });
      wx.showToast({ title: 'AI 帮你完善啦', icon: 'success' });
    }, 1500);
  },

  // Validate form
  validateForm() {
    if (!this.data.title || this.data.title.trim() === '') {
      wx.showToast({ title: '请输入标题', icon: 'none' });
      return false;
    }

    if (!this.data.partnerType) {
      wx.showToast({ title: '请选择搭子类型', icon: 'none' });
      return false;
    }

    if (!this.data.description || this.data.description.trim() === '') {
      wx.showToast({ title: '请输入详细描述', icon: 'none' });
      return false;
    }

    return true;
  },

  // Submit partner
  onSubmit() {
    if (!this.validateForm()) return;

    this.setData({ isPublishing: true });
    wx.showLoading({ title: '发布中...' });

    // Simulated submission
    setTimeout(() => {
      wx.hideLoading();
      this.setData({ isPublishing: false });
      
      wx.showToast({
        title: '发布成功',
        icon: 'success',
        duration: 1500
      });

      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }, 1500);

    // Real implementation:
    // wx.request({
    //   url: app.globalData.apiBase + '/partner/publish',
    //   method: 'POST',
    //   data: {
    //     title: this.data.title,
    //     type: this.data.partnerType,
    //     description: this.data.description,
    //     memberCount: this.data.memberCount,
    //     scope: this.data.scope
    //   },
    //   success: (res) => {
    //     if (res.data.code === 0) {
    //       wx.showToast({ title: '发布成功', icon: 'success' });
    //       setTimeout(() => wx.navigateBack(), 1500);
    //     }
    //   },
    //   fail: () => {
    //     this.setData({ isPublishing: false });
    //     wx.showToast({ title: '发布失败', icon: 'none' });
    //   }
    // });
  }
});