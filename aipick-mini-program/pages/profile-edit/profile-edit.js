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

  // 先请求接口拉取最新个人信息，再填充表单（避免编辑资料显示错误）
  fetchUserProfile() {
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    const fallback = () => {
      const userInfo = wx.getStorageSync('userInfo') || {};
      this.fillFormFromUserInfo(userInfo);
    };
    if (!userId || !token) {
      fallback();
      return;
    }
    wx.request({
      url: `${baseUrl}/api/user/info`,
      method: 'GET',
      header: {
        'Authorization': token ? 'Bearer ' + token : '',
        'X-User-Id': String(userId)
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 0 && res.data.data) {
          const data = res.data.data;
          const merged = { ...wx.getStorageSync('userInfo') || {}, ...data };
          wx.setStorageSync('userInfo', merged);
          this.fillFormFromUserInfo(merged);
        } else {
          fallback();
        }
      },
      fail: () => fallback()
    });
  },

  fillFormFromUserInfo(userInfo) {
    const genderMap = { 0: '保密', 1: '男', 2: '女' };
    const rawGender = userInfo.gender;
    const genderText = (typeof rawGender === 'number' && genderMap[rawGender]) ? genderMap[rawGender] : (userInfo.gender || '');
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    let avatar = userInfo.avatar || '/images/default-avatar.png';
    if (avatar && (avatar.indexOf('__tmp__') !== -1 || avatar.indexOf('://tmp/') !== -1 || (avatar.indexOf('127.0.0.1') !== -1 && avatar.indexOf(':8080') === -1))) {
      avatar = '/images/default-avatar.png';
    }
    if (avatar && !avatar.startsWith('http') && !avatar.startsWith('data:') && !avatar.startsWith('/images')) {
      avatar = baseUrl + (avatar.startsWith('/') ? avatar : '/' + avatar);
    }
    if (avatar && avatar.startsWith('http') && /localhost|127\.0\.0\.1/.test(avatar) && app.normalizeImageUrl) {
      avatar = app.normalizeImageUrl(avatar, baseUrl);
    }
    // 兴趣标签：接口返回为 JSON 字符串或已解析数组，保证历史保存的标签能回显
    let tags = userInfo.tags;
    if (!Array.isArray(tags)) {
      if (typeof tags === 'string' && tags.trim()) {
        try { tags = JSON.parse(tags); } catch (e) { tags = []; }
      } else {
        tags = [];
      }
    }
    this.setData({
      avatar,
      nickname: userInfo.nickname || '',
      gender: genderText,
      birthday: userInfo.birthday || '',
      bio: userInfo.bio || '',
      tags,
      bioLength: (userInfo.bio || '').length,
      _userInfo: userInfo
    });
  },

  // Go back
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
          let errMsg = '上传失败';
          try {
            const errData = typeof res.data === 'string' ? JSON.parse(res.data) : (res.data || {});
            if (errData.message) errMsg = errData.message;
          } catch (e) {}
          wx.showToast({ title: errMsg, icon: 'none' });
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
          wx.showToast({ title: (data && data.message) || '头像上传失败', icon: 'none' });
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

    const base = app.globalData.baseUrl || 'http://localhost:8080';
    let avatarVal = this.data.avatar || existing.avatar || '';
    // 不保存本地临时地址（开发者工具 __tmp__、wxfile 等），只保存已上传或后端返回的 URL
    const isTempLocalUrl = (url) => !url || typeof url !== 'string' || url.startsWith('wxfile://') ||
      (url.indexOf('__tmp__') !== -1) || (url.indexOf('://tmp/') !== -1) || (url.indexOf('127.0.0.1') !== -1 && url.indexOf(':8080') === -1);
    if (isTempLocalUrl(avatarVal)) {
      avatarVal = existing.avatar || '';
    } else if (avatarVal && !avatarVal.startsWith('http') && !avatarVal.startsWith('data:') && avatarVal !== '/images/default-avatar.png') {
      avatarVal = base + (avatarVal.startsWith('/') ? avatarVal : '/' + avatarVal);
    }
    const payload = {
      nickname: (this.data.nickname || '').trim(),
      gender: this._genderToCode(),
      avatar: avatarVal,
      birthday: (this.data.birthday || '').trim() || null,
      bio: this.data.bio || '',
      tags: (this.data.tags && this.data.tags.length) ? JSON.stringify(this.data.tags) : null,
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
            avatar: payload.avatar || this.data.avatar,
            bio: this.data.bio,
            birthday: this.data.birthday || null,
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