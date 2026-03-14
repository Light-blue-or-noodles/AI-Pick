// pages/activity-create/activity-create.js
const app = getApp();

Page({
  data: {
    coverImage: '',
    title: '',
    category: '',
    date: '',
    time: '',
    location: '',
    maxParticipants: 20,
    price: '',
    priceType: 'free',
    description: '',
    titleLength: 0,
    descLength: 0,
    categories: [
      { value: 'offline', label: '线下' },
      { value: 'online', label: '线上' },
      { value: 'team', label: '组队' },
      { value: 'social', label: '交友' }
    ],
    isPublishing: false
  },

  onLoad() {
    // Initialize
  },

  // Go back
  onGoBack() {
    if (this.data.title || this.data.description) {
      wx.showModal({
        title: '提示',
        content: '确定要放弃创建吗？',
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

  // Choose cover image
  onChooseCover() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        this.setData({
          coverImage: res.tempFilePaths[0]
        });
      }
    });
  },

  // Input title
  onTitleInput(e) {
    const value = e.detail.value;
    if (value.length <= 30) {
      this.setData({
        title: value,
        titleLength: value.length
      });
    }
  },

  // Select category
  onSelectCategory(e) {
    const value = e.currentTarget.dataset.value;
    this.setData({ category: value });
  },

  // Choose date
  onDateChange(e) {
    this.setData({ date: e.detail.value });
  },

  // Choose time
  onTimeChange(e) {
    this.setData({ time: e.detail.value });
  },

  // Input location
  onLocationInput(e) {
    this.setData({ location: e.detail.value });
  },

  // Choose location on map
  onChooseLocation() {
    wx.chooseLocation({
      success: (res) => {
        this.setData({
          location: res.name || res.address
        });
      }
    });
  },

  // Decrease participant count
  onDecreaseCount() {
    if (this.data.maxParticipants > 2) {
      this.setData({ maxParticipants: this.data.maxParticipants - 1 });
    }
  },

  // Increase participant count
  onIncreaseCount() {
    if (this.data.maxParticipants < 100) {
      this.setData({ maxParticipants: this.data.maxParticipants + 1 });
    }
  },

  // Select price type
  onSelectPriceType(e) {
    const type = e.currentTarget.dataset.type;
    this.setData({
      priceType: type,
      price: type === 'free' ? '' : this.data.price
    });
  },

  // Input price
  onPriceInput(e) {
    this.setData({ price: e.detail.value });
  },

  // Input description
  onDescInput(e) {
    const value = e.detail.value;
    if (value.length <= 500) {
      this.setData({
        description: value,
        descLength: value.length
      });
    }
  },

  // AI help to complete
  onAIComplete() {
    wx.showLoading({ title: 'AI 生成中...' });
    
    setTimeout(() => {
      wx.hideLoading();
      this.setData({
        title: '周末桌游交友派对',
        titleLength: 7,
        description: '这是一个轻松愉快的周末桌游活动，旨在让大家在忙碌的工作之余放松身心，结识新朋友。活动涵盖狼人杀、三国杀、卡坦岛等多种经典桌游，无论新手还是老手都欢迎参与！',
        descLength: 56,
        maxParticipants: 20,
        price: '0',
        priceType: 'free'
      });
      wx.showToast({ title: 'AI 帮你完善啦', icon: 'success' });
    }, 1500);
  },

  // Validate form
  validateForm() {
    if (!this.data.title || this.data.title.trim() === '') {
      wx.showToast({ title: '请输入活动标题', icon: 'none' });
      return false;
    }

    if (!this.data.category) {
      wx.showToast({ title: '请选择活动类型', icon: 'none' });
      return false;
    }

    if (!this.data.date || !this.data.time) {
      wx.showToast({ title: '请选择活动时间', icon: 'none' });
      return false;
    }

    if (!this.data.location) {
      wx.showToast({ title: '请输入活动地点', icon: 'none' });
      return false;
    }

    if (!this.data.description || this.data.description.trim() === '') {
      wx.showToast({ title: '请输入活动详情', icon: 'none' });
      return false;
    }

    return true;
  },

  // Submit activity
  onSubmit() {
    if (!this.validateForm()) return;

    this.setData({ isPublishing: true });
    wx.showLoading({ title: '发布中...' });

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
  }
});