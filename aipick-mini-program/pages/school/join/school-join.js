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
    this.checkSchoolStatus();
  },

  // 检查学校已加入状态
  checkSchoolStatus() {
    const userInfo = app.globalData.userInfo;
    if (userInfo && userInfo.schoolName) {
      this.setData({
        hasJoined: true,
        userSchool: userInfo.schoolName
      });
    }
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

    // 检查登录状态
    if (!app.globalData.token) {
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

    wx.request({
      url: `${app.globalData.baseUrl}/api/user/school`,
      method: 'POST',
      header: {
        'Authorization': `Bearer ${app.globalData.token}`,
        'Content-Type': 'application/json'
      },
      data: {
        schoolName: schoolName
      },
      success: (res) => {
        if (res.data.code === 0) {
          // 更新全局用户信息
          if (res.data.data) {
            app.globalData.userInfo = res.data.data;
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
