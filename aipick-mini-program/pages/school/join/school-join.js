// pages/school/join/school-join.js
const app = getApp();

Page({
  data: {
    schoolName: '',
    submitting: false,
    hasJoined: false,
    userSchool: ''
  },

  onLoad() {
    this.refreshStatus();
  },

  onShow() {
    this.refreshStatus();
  },

  refreshStatus() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {};
    const name = (userInfo.schoolName && String(userInfo.schoolName).trim()) || '';
    if (name) {
      this.setData({ hasJoined: true, userSchool: name });
    } else {
      this.setData({ hasJoined: false, userSchool: '' });
    }
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    if (!token || userId == null || userId === '') return;
    const baseUrl = app.globalData.baseUrl || 'http://localhost:8080';
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
          const sn = (d.schoolName && String(d.schoolName).trim()) || '';
          app.globalData.userInfo = { ...app.globalData.userInfo, ...d };
          wx.setStorageSync('userInfo', app.globalData.userInfo);
          if (sn) {
            this.setData({ hasJoined: true, userSchool: sn });
          } else {
            this.setData({ hasJoined: false, userSchool: '' });
          }
        }
      }
    });
  },

  // 输入学校名称
  onSchoolInput(e) {
    this.setData({
      schoolName: e.detail.value
    });
  },

  // 提交加入学校
  onSubmit() {
    const { schoolName, submitting, hasJoined } = this.data;

    if (submitting) return;

    // 验证输入
    if (!schoolName || !schoolName.trim()) {
      wx.showToast({
        title: '请输入学校名称',
        icon: 'none'
      });
      return;
    }

    if (!(app.globalData.token || wx.getStorageSync('token'))) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      });
      setTimeout(() => {
        wx.navigateTo({
          url: '/pages/login/login'
        });
      }, 1500);
      return;
    }

    // 如果已加入，提示确认
    if (hasJoined) {
      wx.showModal({
        title: '确认更换',
        content: '确定要更换学校吗？',
        success: (res) => {
          if (res.confirm) {
            this.doJoinSchool(schoolName.trim());
          }
        }
      });
      return;
    }

    this.doJoinSchool(schoolName.trim());
  },

  // 执行加入学校 API 调用
  doJoinSchool(schoolName) {
    this.setData({ submitting: true });

    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    wx.request({
      url: `${app.globalData.baseUrl}/api/user/school`,
      method: 'POST',
      header: {
        Authorization: `Bearer ${app.globalData.token || wx.getStorageSync('token')}`,
        'Content-Type': 'application/json',
        'X-User-Id': userId != null ? String(userId) : ''
      },
      data: {
        schoolName: schoolName
      },
      success: (res) => {
        if (res.data.code === 0) {
          if (res.data.data) {
            app.globalData.userInfo = { ...app.globalData.userInfo, ...res.data.data };
            wx.setStorageSync('userInfo', app.globalData.userInfo);
          }
          
          wx.showToast({
            title: '加入成功',
            icon: 'success'
          });

          this.setData({
            hasJoined: true,
            userSchool: schoolName
          });
        } else {
          wx.showToast({
            title: res.data.message || '加入失败',
            icon: 'none'
          });
        }
      },
      fail: () => {
        wx.showToast({
          title: '网络错误，请重试',
          icon: 'none'
        });
      },
      complete: () => {
        this.setData({ submitting: false });
      }
    });
  },

  // 更换学校
  onChangeSchool() {
    this.setData({
      hasJoined: false,
      schoolName: ''
    });
  },

  // 分享
  onShareAppMessage() {
    return {
      title: '加入学校 - AI-Pick',
      path: '/pages/school/join/school-join'
    };
  }
});
