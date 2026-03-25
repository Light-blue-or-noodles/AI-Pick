// pages/activity-create/activity-create.js
const app = getApp();
const { post } = require('../../utils/request');

Page({
  data: {
    imageList: [],
    title: '',
    category: '',
    date: '',
    time: '',
    endDate: '',
    endTime: '',
    // 多列时间选择器数据与当前选中索引
    dateTimeRange: [],
    startDateTimeIndex: [0, 18, 0],
    endDateTimeIndex: [0, 20, 0],
    startDateTimeText: '',
    endDateTimeText: '',
    location: '',
    latitude: null,
    longitude: null,
    maxParticipants: 20,
    price: '',
    priceType: 'free',
    description: '',
    titleLength: 0,
    descLength: 0,
    categories: [
      { value: '运动', label: '运动' },
      { value: '美食', label: '美食' },
      { value: '学习', label: '学习' },
      { value: '娱乐', label: '娱乐' },
      { value: '社交', label: '社交' }
    ],
    isPublishing: false
  },

  onLoad() {
    this.initDateTimeRange();
  },

  // 构建「日期 + 小时 + 分钟」多列选择器数据
  initDateTimeRange() {
    const dates = [];
    const dateValues = [];
    const now = new Date();
    for (let i = 0; i < 30; i++) {
      const d = new Date(now.getTime() + i * 24 * 60 * 60 * 1000);
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      const label = `${m}-${day}`;
      dates.push(label);
      dateValues.push(`${y}-${m}-${day}`);
    }
    const hours = [];
    for (let h = 0; h < 24; h++) {
      hours.push(String(h).padStart(2, '0') + '时');
    }
    const minutes = ['00分', '30分'];
    this._dateValues = dateValues;
    this._hourValues = hours.map((h, idx) => String(idx).padStart(2, '0'));
    this._minuteValues = ['00', '30'];
    this.setData({
      dateTimeRange: [dates, hours, minutes]
    });
  },

  // 多图选择（首张为封面，最多9张）
  onChooseImages() {
    const remain = 9 - this.data.imageList.length;
    if (remain <= 0) return;
    wx.chooseMedia({
      count: remain,
      mediaType: ['image'],
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const newPaths = (res.tempFiles || []).map(f => f.tempFilePath);
        this.setData({
          imageList: this.data.imageList.concat(newPaths)
        });
      }
    });
  },

  onRemoveImage(e) {
    const index = e.currentTarget.dataset.index;
    const imageList = this.data.imageList.filter((_, i) => i !== index);
    this.setData({ imageList });
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

  // 多列选择开始时间
  onStartDateTimeChange(e) {
    const indices = e.detail.value || [0, 0, 0];
    const [dIdx, hIdx, mIdx] = indices;
    const date = this._dateValues[dIdx];
    const hour = this._hourValues[hIdx] || '00';
    const minute = this._minuteValues[mIdx] || '00';
    const text = `${date} ${hour}:${minute}`;
    this.setData({
      startDateTimeIndex: indices,
      date,
      time: `${hour}:${minute}`,
      startDateTimeText: text
    });
  },

  // 多列选择结束时间
  onEndDateTimeChange(e) {
    const indices = e.detail.value || [0, 0, 0];
    const [dIdx, hIdx, mIdx] = indices;
    const date = this._dateValues[dIdx];
    const hour = this._hourValues[hIdx] || '00';
    const minute = this._minuteValues[mIdx] || '00';
    const text = `${date} ${hour}:${minute}`;
    this.setData({
      endDateTimeIndex: indices,
      endDate: date,
      endTime: `${hour}:${minute}`,
      endDateTimeText: text
    });
  },

  // Input location
  onLocationInput(e) {
    this.setData({ location: e.detail.value });
  },

  // 腾讯地图选点（小程序内为腾讯地图）
  onChooseLocation() {
    wx.chooseLocation({
      success: (res) => {
        this.setData({
          location: res.name || res.address,
          latitude: res.latitude,
          longitude: res.longitude
        });
      },
      fail: (err) => {
        if (err.errMsg && err.errMsg.indexOf('cancel') === -1) {
          wx.showToast({ title: '请授权位置或稍后重试', icon: 'none' });
        }
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

  // 调用后端 Spring AI Alibaba 服务，生成/优化活动详情
  onAIComplete() {
    const { title, category, location, date, time, description } = this.data;
    if (!title || !title.trim()) {
      wx.showToast({ title: '请先填写活动标题', icon: 'none' });
      return;
    }

    wx.showLoading({ title: 'AI 优化中...' });

    const timeText = date && time ? `${date} ${time}` : '';

    post('/api/activity/ai/description', {
      title,
      category,
      location,
      timeText,
      currentDesc: description
    })
      .then((res) => {
        const text = res.data || '';
        this.setData({
          description: text,
          descLength: text.length
        });
        wx.hideLoading();
        wx.showToast({ title: 'AI 已优化', icon: 'success' });
      })
      .catch((err) => {
        wx.hideLoading();
        wx.showToast({
          title: (err && (err.msg || err.message)) || 'AI 优化失败，请稍后重试',
          icon: 'none'
        });
      });
  },

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
      wx.showToast({ title: '请选择开始时间', icon: 'none' });
      return false;
    }
    if (!this.data.endDate || !this.data.endTime) {
      wx.showToast({ title: '请选择结束时间', icon: 'none' });
      return false;
    }
    if (!this.data.location) {
      wx.showToast({ title: '请选择活动地点', icon: 'none' });
      return false;
    }
    if (!this.data.description || this.data.description.trim() === '') {
      wx.showToast({ title: '请输入活动详情', icon: 'none' });
      return false;
    }
    if (!this.data.imageList || this.data.imageList.length === 0) {
      wx.showToast({ title: '请至少上传一张图片（首张为封面）', icon: 'none' });
      return false;
    }
    return true;
  },

  // 上传单张图片，返回 URL
  uploadOneImage(filePath) {
    const baseUrl = (app.globalData.baseUrl || 'http://localhost:8080').replace(/\/$/, '');
    const userId = app.globalData.userId || wx.getStorageSync('userId');
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: baseUrl + '/api/activity/upload-image',
        filePath: filePath,
        name: 'file',
        header: {
          'X-User-Id': String(userId || '')
        },
        success: (res) => {
          try {
            const data = JSON.parse(res.data);
            if (data.code === 0 && data.data && data.data.url) {
              resolve(data.data.url);
            } else {
              reject(new Error(data.msg || '上传失败'));
            }
          } catch (e) {
            reject(e);
          }
        },
        fail: reject
      });
    });
  },

  onSubmit() {
    if (!this.validateForm()) return;

    this.setData({ isPublishing: true });
    wx.showLoading({ title: '上传图片中...' });

    const imageList = this.data.imageList;
    const uploadPromises = imageList.map(path => this.uploadOneImage(path));

    Promise.all(uploadPromises)
      .then((imageUrls) => {
        wx.showLoading({ title: '发布中...' });
        const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
        const startTime = this.data.date + 'T' + (this.data.time || '00:00') + ':00';
        const endTime = this.data.endDate + 'T' + (this.data.endTime || '00:00') + ':00';
        const registerEndTime = startTime;
        const fee = this.data.priceType === 'free' ? 0 : (parseFloat(this.data.price) || 0);
        const payload = {
          title: this.data.title.trim(),
          description: this.data.description.trim(),
          type: 1,
          category: this.data.category,
          startTime: startTime,
          endTime: endTime,
          registerEndTime: registerEndTime,
          location: this.data.location,
          latitude: this.data.latitude,
          longitude: this.data.longitude,
          fee: fee,
          maxParticipants: this.data.maxParticipants,
          imageUrls: imageUrls
        };
        return post('/api/activity', payload);
      })
      .then(() => {
        wx.hideLoading();
        this.setData({ isPublishing: false });
        wx.showToast({ title: '发布成功', icon: 'success', duration: 1500 });
        setTimeout(() => wx.navigateBack(), 1500);
      })
      .catch((err) => {
        wx.hideLoading();
        this.setData({ isPublishing: false });
        wx.showToast({ title: err.msg || err.message || '发布失败', icon: 'none' });
      });
  }
});