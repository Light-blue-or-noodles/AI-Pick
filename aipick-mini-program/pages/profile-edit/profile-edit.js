// pages/profile-edit/profile-edit.js
const app = getApp();

Page({
  data: {
    avatar: '',
    nickname: '',
    gender: '',
    birthday: '',
    bio: '',
    tags: [],
    availableTags: ['游戏', '运动', '学习', '美食', '旅行', '电影', '音乐', '摄影', '健身', '读书', '烹饪', '编程'],
    genderOptions: ['男', '女', '保密'],
    bioLength: 0,
    isSaving: false
  },

  onLoad() {
    this.fetchUserProfile();
  },

  // Fetch user profile（本地 + 可选的接口拉取最新）
  fetchUserProfile() {
    const userInfo = wx.getStorageSync('userInfo') || {};
    const genderMap = { 0: '保密', 1: '男', 2: '女' };
    const rawGender = userInfo.gender;
    const genderText = (typeof rawGender === 'number' && genderMap[rawGender]) ? genderMap[rawGender] : (userInfo.gender || '');
    this.setData({
      avatar: userInfo.avatar || '/images/default-avatar.png',
      nickname: userInfo.nickname || '',
      gender: genderText,
      birthday: userInfo.birthday || '',
      bio: userInfo.bio || '',
      tags: userInfo.tags || [],
      bioLength: (userInfo.bio || '').length,
      _userInfo: userInfo
    });
  },

  // Go back
  onGoBack() {
    wx.navigateBack();
  },

  // Choose avatar：选图后上传到后端，用返回的 URL 作为头像
  onChooseAvatar() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempPath = res.tempFilePaths[0];
        this.setData({ avatar: tempPath });
        this.uploadAvatar(tempPath);
      }
    });
  },

  uploadAvatar(filePath) {
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    if (!userId || !token) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '上传中...' });
    wx.uploadFile({
      url: `${baseUrl}/api/user/avatar`,
      filePath: filePath,
      name: 'file',
      header: {
        'Authorization': 'Bearer ' + token,
        'X-User-Id': String(userId)
      },
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode !== 200) {
          wx.showToast({ title: '上传失败', icon: 'none' });
          return;
        }
        let data;
        try {
          data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
        } catch (e) {
          wx.showToast({ title: '解析失败', icon: 'none' });
          return;
        }
        if (data && (data.code === 0 || data.code === 200) && data.data && data.data.url) {
          const avatarUrl = baseUrl + '/api' + data.data.url;
          this.setData({ avatar: avatarUrl });
          wx.showToast({ title: '上传成功', icon: 'success' });
        } else {
          wx.showToast({ title: (data && data.message) || '上传失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络异常', icon: 'none' });
      }
    });
  },

  // Input nickname
  onNicknameInput(e) {
    this.setData({ nickname: e.detail.value });
  },

  // Select gender（界面为 男/女/保密，提交给后端为 1/2/0）
  onSelectGender(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({ gender: this.data.genderOptions[index] });
  },

  _genderToCode() {
    const g = this.data.gender;
    if (g === '男') return 1;
    if (g === '女') return 2;
    return 0;
  },

  // Choose birthday
  onBirthdayChange(e) {
    this.setData({ birthday: e.detail.value });
  },

  // Input bio
  onBioInput(e) {
    const value = e.detail.value;
    if (value.length <= 100) {
      this.setData({
        bio: value,
        bioLength: value.length
      });
    }
  },

  // Add tag
  onAddTag(e) {
    const tag = e.currentTarget.dataset.tag;
    const currentTags = this.data.tags;
    
    if (currentTags.length >= 5) {
      wx.showToast({ title: '最多添加5个标签', icon: 'none' });
      return;
    }
    
    if (currentTags.includes(tag)) {
      wx.showToast({ title: '标签已存在', icon: 'none' });
      return;
    }
    
    this.setData({
      tags: [...currentTags, tag]
    });
  },

  // Remove tag
  onRemoveTag(e) {
    const index = e.currentTarget.dataset.index;
    const tags = [...this.data.tags];
    tags.splice(index, 1);
    this.setData({ tags });
  },

  // Validate form
  validateForm() {
    if (!this.data.nickname || this.data.nickname.trim() === '') {
      wx.showToast({ title: '请输入昵称', icon: 'none' });
      return false;
    }

    if (this.data.nickname.length < 2 || this.data.nickname.length > 12) {
      wx.showToast({ title: '昵称长度2-12字', icon: 'none' });
      return false;
    }

    return true;
  },

  // Save profile（提交到后端 + 更新本地）
  onSave() {
    if (!this.validateForm()) return;

    this.setData({ isSaving: true });
    wx.showLoading({ title: '保存中...' });

    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    const existing = this.data._userInfo || wx.getStorageSync('userInfo') || {};

    const payload = {
      nickname: (this.data.nickname || '').trim(),
      gender: this._genderToCode(),
      avatar: (this.data.avatar && (this.data.avatar.startsWith('http') || this.data.avatar.startsWith('/api'))) ? this.data.avatar : (existing.avatar || ''),
      bio: this.data.bio || '',
      companyName: existing.companyName || null,
      schoolName: existing.schoolName || null
    };

    wx.request({
      url: `${baseUrl}/api/user/info`,
      method: 'PUT',
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : '',
        'X-User-Id': String(userId || '')
      },
      data: payload,
      success: (res) => {
        wx.hideLoading();
        this.setData({ isSaving: false });
        const data = res.data;
        if (res.statusCode === 200 && data && (data.code === 0 || data.code === 200)) {
          const updated = data.data || {};
          const merged = {
            ...existing,
            ...updated,
            nickname: this.data.nickname,
            avatar: this.data.avatar,
            bio: this.data.bio,
            birthday: this.data.birthday,
            tags: this.data.tags
          };
          wx.setStorageSync('userInfo', merged);
          wx.showToast({ title: '保存成功', icon: 'success' });
          setTimeout(() => wx.navigateBack(), 800);
        } else {
          wx.showToast({ title: data && data.message ? data.message : '保存失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        this.setData({ isSaving: false });
        wx.showToast({ title: '网络异常，请重试', icon: 'none' });
      }
    });
  }
});