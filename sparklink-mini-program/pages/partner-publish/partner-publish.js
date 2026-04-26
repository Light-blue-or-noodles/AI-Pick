// pages/partner-publish/partner-publish.js
const app = getApp();
const { PREFERENCE_TAGS, MAX_SELECT } = require('../../utils/partnerPreferenceTags.js');

/** 与后端 PartnerTypeConstants 一致，顺序即 type 1～15 */
const PARTNER_TYPES = [
  '宠物搭子',
  '电影搭子',
  '音乐搭子',
  '逛街搭子',
  '运动搭子',
  '摄影搭子',
  '干饭搭子',
  '旅游搭子',
  'k歌搭子',
  '喝酒搭子',
  '桌游搭子',
  '钓鱼搭子',
  '游戏搭子',
  '聊天搭子',
  '户外搭子'
];

/** 与后端 PartnerScopeConstants 位值一致：1 公开、2 同事、4 校友 */
const SCOPE_BIT = { public: 1, colleague: 2, alumni: 4 };

function todayISODate() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

Page({
  data: {
    title: '',
    partnerType: '',
    selectedPreferences: [],
    selectedCount: 0,
    maxPrefSelect: MAX_SELECT,
    preferenceTagList: PREFERENCE_TAGS,
    description: '',
    coverImage: '',
    planDate: '',
    planTimeStr: '',
    planEndDate: '',
    planEndTimeStr: '',
    planDateStart: todayISODate(),
    latitude: null,
    longitude: null,
    locationDisplay: '',
    memberCount: 3,
    scopeSelected: { public: true, colleague: false, alumni: false },
    titleLength: 0,
    descLength: 0,
    partnerTypes: PARTNER_TYPES,
    scopes: [
      { value: 'public', label: '公开' },
      { value: 'colleague', label: '同事' },
      { value: 'alumni', label: '校友' }
    ],
    hasCompany: false,
    hasSchool: false,
    isPublishing: false
  },

  onLoad() {
    this.loadOrgFlags();
  },

  onShow() {
    this.loadOrgFlags();
  },

  /** 是否已填写公司/学校名称（用于同事、校友可见范围） */
  loadOrgFlags() {
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    const local = app.globalData.userInfo || wx.getStorageSync('userInfo') || {};
    const cn0 = (local.companyName && String(local.companyName).trim()) || '';
    const sn0 = (local.schoolName && String(local.schoolName).trim()) || '';
    const hasCompany = cn0.length > 0;
    const hasSchool = sn0.length > 0;
    let sel0 = { ...this.data.scopeSelected };
    if (!hasCompany) sel0.colleague = false;
    if (!hasSchool) sel0.alumni = false;
    if (!sel0.public && !sel0.colleague && !sel0.alumni) sel0.public = true;
    this.setData({
      hasCompany,
      hasSchool,
      scopeSelected: sel0
    });
    if (!token || userId == null || userId === '') {
      return;
    }
    wx.request({
      url: `${baseUrl}/api/user/info`,
      method: 'GET',
      header: {
        Authorization: 'Bearer ' + token,
        'X-User-Id': String(userId)
      },
      success: (res) => {
        const body = res.data;
        if (body && body.code === 0 && body.data) {
          const d = body.data;
          app.globalData.userInfo = { ...app.globalData.userInfo, ...d };
          wx.setStorageSync('userInfo', app.globalData.userInfo);
          const cn = (d.companyName && String(d.companyName).trim()) || '';
          const sn = (d.schoolName && String(d.schoolName).trim()) || '';
          const hasCompany = cn.length > 0;
          const hasSchool = sn.length > 0;
          let sel = { ...this.data.scopeSelected };
          if (!hasCompany) sel.colleague = false;
          if (!hasSchool) sel.alumni = false;
          if (!sel.public && !sel.colleague && !sel.alumni) {
            sel.public = true;
          }
          this.setData({
            hasCompany,
            hasSchool,
            scopeSelected: sel
          });
        }
      }
    });
  },

  onPlanDateChange(e) {
    this.setData({ planDate: e.detail.value || '' });
  },

  onPlanTimeChange(e) {
    const planTimeStr = e.detail.value || '';
    // 常漏选：只滚了时间未选日期 → buildPlanTimeIso 为 null、后端 plan_time 为空
    if (planTimeStr && !this.data.planDate) {
      this.setData({
        planTimeStr,
        planDate: this.data.planDateStart || todayISODate()
      });
      return;
    }
    this.setData({ planTimeStr });
  },

  onPlanEndDateChange(e) {
    this.setData({ planEndDate: e.detail.value || '' });
  },

  onPlanEndTimeChange(e) {
    const planEndTimeStr = e.detail.value || '';
    const d = this.data;
    if (planEndTimeStr && !d.planEndDate) {
      this.setData({
        planEndTimeStr,
        planEndDate: d.planDate || d.planDateStart || todayISODate()
      });
      return;
    }
    this.setData({ planEndTimeStr });
  },

  clearPlanTime() {
    this.setData({ planDate: '', planTimeStr: '', planEndDate: '', planEndTimeStr: '' });
  },

  clearLocation() {
    this.setData({ latitude: null, longitude: null, locationDisplay: '' });
  },

  onChooseLocation() {
    wx.chooseLocation({
      success: (res) => {
        const name = res.name || '';
        const address = res.address || '';
        const parts = [name, address].filter((s) => s && String(s).trim());
        let display = parts.join(' · ');
        if (display.length > 200) {
          display = display.substring(0, 200);
        }
        this.setData({
          latitude: res.latitude,
          longitude: res.longitude,
          locationDisplay: display
        });
      },
      fail: (err) => {
        if (err.errMsg && err.errMsg.indexOf('cancel') !== -1) return;
        wx.showToast({ title: '选点失败，请检查定位权限', icon: 'none' });
      }
    });
  },

  buildPlanTimeIso() {
    let { planDate, planTimeStr, planDateStart } = this.data;
    if (planTimeStr && !planDate) {
      planDate = planDateStart || todayISODate();
    }
    if (!planDate || !planTimeStr) {
      return null;
    }
    return `${planDate}T${planTimeStr}:00`;
  },

  buildPlanEndTimeIso() {
    const d = this.data;
    let { planEndDate, planEndTimeStr } = d;
    if (planEndTimeStr && !planEndDate) {
      planEndDate = d.planDate || d.planDateStart || todayISODate();
    }
    if (!planEndDate || !planEndTimeStr) {
      return null;
    }
    return `${planEndDate}T${planEndTimeStr}:00`;
  },

  buildAiHints() {
    const planIso = this.buildPlanTimeIso();
    const endIso = this.buildPlanEndTimeIso();
    let planTimeHint = '';
    if (planIso && endIso) {
      planTimeHint =
        planIso.replace('T', ' ').substring(0, 16) + ' ～ ' + endIso.replace('T', ' ').substring(0, 16);
    } else if (planIso) {
      planTimeHint = planIso.replace('T', ' ').substring(0, 16);
    }
    const locationHint = this.data.locationDisplay || '';
    return { planTimeHint, locationHint };
  },

  onTogglePreference(e) {
    const label = e.currentTarget.dataset.label;
    if (!label) return;
    let next = (this.data.selectedPreferences || []).slice();
    const idx = next.indexOf(label);
    if (idx >= 0) {
      next.splice(idx, 1);
    } else {
      if (next.length >= MAX_SELECT) {
        wx.showToast({ title: '最多选 ' + MAX_SELECT + ' 个', icon: 'none' });
        return;
      }
      next.push(label);
    }
    this.setData({
      selectedPreferences: next,
      selectedCount: next.length
    });
  },

  onTitleInput(e) {
    const value = e.detail.value || '';
    if (value.length <= 50) {
      this.setData({ title: value, titleLength: value.length });
    }
  },

  onDescInput(e) {
    const value = e.detail.value || '';
    if (value.length <= 300) {
      this.setData({ description: value, descLength: value.length });
    }
  },

  onSelectType(e) {
    const index = e.currentTarget.dataset.index;
    const type = this.data.partnerTypes[index];
    this.setData({ partnerType: type });
  },

  onDecreaseCount() {
    if (this.data.memberCount > 2) {
      this.setData({ memberCount: this.data.memberCount - 1 });
    }
  },

  onIncreaseCount() {
    if (this.data.memberCount < 20) {
      this.setData({ memberCount: this.data.memberCount + 1 });
    }
  },

  onToggleScope(e) {
    const key = e.currentTarget.dataset.value;
    if (!key) return;
    const sel = { ...this.data.scopeSelected };
    if (sel[key]) {
      const kept = Object.keys(sel).filter((k) => sel[k]);
      if (kept.length <= 1) {
        wx.showToast({ title: '至少保留一种可见范围', icon: 'none' });
        return;
      }
      sel[key] = false;
      this.setData({ scopeSelected: sel });
      return;
    }
    if (key === 'colleague' && !this.data.hasCompany) {
      wx.showModal({
        title: '需要先加入公司',
        content: '选择「同事」可见前，请先在个人中心完成「加入公司」。',
        confirmText: '去加入',
        cancelText: '取消',
        success: (modalRes) => {
          if (modalRes.confirm) {
            wx.navigateTo({ url: '/pages/company/join/company-join' });
          }
        }
      });
      return;
    }
    if (key === 'alumni' && !this.data.hasSchool) {
      wx.showModal({
        title: '需要先加入学校',
        content: '选择「校友」可见前，请先在个人中心完成「加入学校」。',
        confirmText: '去加入',
        cancelText: '取消',
        success: (modalRes) => {
          if (modalRes.confirm) {
            wx.navigateTo({ url: '/pages/school/join/school-join' });
          }
        }
      });
      return;
    }
    sel[key] = true;
    this.setData({ scopeSelected: sel });
  },

  /** 选择单张封面 */
  chooseCover() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const path = res.tempFiles && res.tempFiles[0] && res.tempFiles[0].tempFilePath;
        if (!path) return;
        this.uploadCover(path);
      }
    });
  },

  uploadCover(filePath) {
    const baseUrl = (app.globalData && app.globalData.baseUrl) || 'http://localhost:8080';
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    wx.showLoading({ title: '上传中' });
    wx.uploadFile({
      url: `${baseUrl}/api/partner/upload-image`,
      filePath,
      name: 'file',
      header: {
        Authorization: token ? `Bearer ${token}` : '',
        'X-User-Id': userId != null ? String(userId) : ''
      },
      success: (res) => {
        wx.hideLoading();
        try {
          const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
          if (data.code === 0 && data.data && data.data.url) {
            this.setData({ coverImage: data.data.url });
            wx.showToast({ title: '封面已上传', icon: 'success' });
          } else {
            wx.showToast({ title: (data && data.message) || '上传失败', icon: 'none' });
          }
        } catch (err) {
          wx.showToast({ title: '解析失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '上传失败', icon: 'none' });
      }
    });
  },

  clearCover() {
    this.setData({ coverImage: '' });
  },

  onAIComplete() {
    const { title, partnerType, selectedPreferences, description } = this.data;
    if (!title || !partnerType) {
      wx.showToast({ title: '请先填写标题和类型', icon: 'none' });
      return;
    }
    const hints = this.buildAiHints();
    const baseUrl = (app.globalData && app.globalData.baseUrl) || 'http://localhost:8080';
    wx.showLoading({ title: 'AI 生成中...' });
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    wx.request({
      url: `${baseUrl}/api/partner/ai/description`,
      method: 'POST',
      header: {
        'content-type': 'application/json',
        Authorization: token ? `Bearer ${token}` : '',
        'X-User-Id': userId != null ? String(userId) : ''
      },
      data: {
        title,
        typeName: partnerType,
        preference: (selectedPreferences || []).join('、'),
        currentDesc: description || '',
        planTimeHint: hints.planTimeHint || undefined,
        locationHint: hints.locationHint || undefined
      },
      success: (res) => {
        wx.hideLoading();
        const body = res.data;
        if (res.statusCode === 200 && body && body.code === 0 && body.data) {
          const text = String(body.data);
          const clipped = text.length > 300 ? text.substring(0, 300) : text;
          this.setData({ description: clipped, descLength: clipped.length });
          wx.showToast({ title: '已生成详情', icon: 'success' });
        } else {
          wx.showToast({ title: (body && body.message) || '生成失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  },

  validateForm() {
    if (!this.data.title || !this.data.title.trim()) {
      wx.showToast({ title: '请输入标题', icon: 'none' });
      return false;
    }
    if (!this.data.partnerType) {
      wx.showToast({ title: '请选择搭子类型', icon: 'none' });
      return false;
    }
    if (!this.data.selectedPreferences || this.data.selectedPreferences.length === 0) {
      wx.showToast({ title: '请选择搭子偏好', icon: 'none' });
      return false;
    }
    if (!this.data.description || !this.data.description.trim()) {
      wx.showToast({ title: '请填写详情', icon: 'none' });
      return false;
    }
    const s = this.data.scopeSelected || {};
    if (!s.public && !s.colleague && !s.alumni) {
      wx.showToast({ title: '请至少选择一种可见范围', icon: 'none' });
      return false;
    }
    if (s.colleague && !this.data.hasCompany) {
      wx.showToast({ title: '请先加入公司后再勾选同事可见', icon: 'none' });
      return false;
    }
    if (s.alumni && !this.data.hasSchool) {
      wx.showToast({ title: '请先加入学校后再勾选校友可见', icon: 'none' });
      return false;
    }
    const endIso = this.buildPlanEndTimeIso();
    const startIso = this.buildPlanTimeIso();
    if (endIso && !startIso) {
      wx.showToast({ title: '选择结束时间前请先选开始时间', icon: 'none' });
      return false;
    }
    if (startIso && endIso) {
      const t0 = new Date(startIso.replace(' ', 'T'));
      const t1 = new Date(endIso.replace(' ', 'T'));
      if (Number.isFinite(t0.getTime()) && Number.isFinite(t1.getTime()) && t1 < t0) {
        wx.showToast({ title: '结束时间不能早于开始时间', icon: 'none' });
        return false;
      }
    }
    return true;
  },

  onSubmit() {
    if (!this.validateForm()) return;
    const baseUrl = (app.globalData && app.globalData.baseUrl) || 'http://localhost:8080';
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    if (!token || userId == null) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    const ti = this.data.partnerTypes.indexOf(this.data.partnerType);
    const typeInt = ti >= 0 ? ti + 1 : 1;
    const s = this.data.scopeSelected || {};
    const scopes = [];
    if (s.public) scopes.push(SCOPE_BIT.public);
    if (s.colleague) scopes.push(SCOPE_BIT.colleague);
    if (s.alumni) scopes.push(SCOPE_BIT.alumni);
    const preferenceStr = (this.data.selectedPreferences || []).join(',');
    const planIso = this.buildPlanTimeIso();
    const planEndIso = this.buildPlanEndTimeIso();
    const post = {
      title: this.data.title.trim(),
      content: this.data.description.trim(),
      preference: preferenceStr,
      type: typeInt,
      scopes,
      targetCount: this.data.memberCount,
      coverImage: this.data.coverImage || undefined
    };
    if (planIso) {
      post.planTime = planIso;
    }
    if (planEndIso) {
      post.planEndTime = planEndIso;
    }
    if (this.data.locationDisplay && String(this.data.locationDisplay).trim()) {
      post.location = String(this.data.locationDisplay).trim();
    }
    if (this.data.latitude != null && this.data.longitude != null) {
      post.latitude = this.data.latitude;
      post.longitude = this.data.longitude;
    }
    this.setData({ isPublishing: true });
    wx.showLoading({ title: '发布中...' });
    wx.request({
      url: `${baseUrl}/api/partner`,
      method: 'POST',
      header: {
        'content-type': 'application/json',
        Authorization: `Bearer ${token}`,
        'X-User-Id': String(userId)
      },
      data: post,
      success: (res) => {
        wx.hideLoading();
        this.setData({ isPublishing: false });
        const body = res.data;
        if (res.statusCode === 200 && body && body.code === 0) {
          wx.showToast({ title: '发布成功', icon: 'success' });
          setTimeout(() => wx.navigateBack(), 1200);
        } else {
          wx.showToast({ title: (body && body.message) || '发布失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        this.setData({ isPublishing: false });
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  }
});
