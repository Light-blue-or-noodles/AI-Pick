// pages/company/join/company-join.js
const app = getApp();

Page({
  data: {
    companyName: '',
    submitting: false,
    hasJoined: false,
    userCompany: ''
  },

  onLoad() {
    this.checkCompanyStatus();
  },

  // 检查公司已加入状态
  checkCompanyStatus() {
    const userInfo = app.globalData.userInfo;
    if (userInfo && userInfo.companyName) {
      this.setData({
        hasJoined: true,
        userCompany: userInfo.companyName
      });
    }
  },

  // 输入公司名称
  onCompanyInput(e) {
    this.setData({
      companyName: e.detail.value
    });
  },

  // 提交加入公司
  onSubmit() {
    const { companyName, submitting, hasJoined } = this.data;

    if (submitting) return;

    // 验证输入
    if (!companyName || !companyName.trim()) {
      wx.showToast({
        title: '请输入公司名称',
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
        content: '确定要更换公司吗？',
        success: (res) => {
          if (res.confirm) {
            this.doJoinCompany(companyName.trim());
          }
        }
      });
      return;
    }

    this.doJoinCompany(companyName.trim());
  },

  // 执行加入公司 API 调用
  doJoinCompany(companyName) {
    this.setData({ submitting: true });

    wx.request({
      url: `${app.globalData.baseUrl}/api/user/company`,
      method: 'POST',
      header: {
        'Authorization': `Bearer ${app.globalData.token}`,
        'Content-Type': 'application/json'
      },
      data: {
        companyName: companyName
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
            userCompany: companyName
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

  // 更换公司
  onChangeCompany() {
    this.setData({
      hasJoined: false,
      companyName: ''
    });
  },

  // 分享
  onShareAppMessage() {
    return {
      title: '加入公司 - AI-Pick',
      path: '/pages/company/join/company-join'
    };
  }
});
