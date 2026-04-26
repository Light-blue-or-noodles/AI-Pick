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
    this.refreshStatus();
  },

  onShow() {
    this.refreshStatus();
  },

  /** 从本地与接口同步是否已加入公司 */
  refreshStatus() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {};
    const name = (userInfo.companyName && String(userInfo.companyName).trim()) || '';
    if (name) {
      this.setData({ hasJoined: true, userCompany: name });
    } else {
      this.setData({ hasJoined: false, userCompany: '' });
    }
    const token = app.globalData.token || wx.getStorageSync('token');
    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    if (!token || userId == null || userId === '') return;
    const baseUrl = app.globalData.baseUrl || 'https://www.aipick.cloud';
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
          const cn = (d.companyName && String(d.companyName).trim()) || '';
          app.globalData.userInfo = { ...app.globalData.userInfo, ...d };
          wx.setStorageSync('userInfo', app.globalData.userInfo);
          if (cn) {
            this.setData({ hasJoined: true, userCompany: cn });
          } else {
            this.setData({ hasJoined: false, userCompany: '' });
          }
        }
      }
    });
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

    const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
    wx.request({
      url: `${app.globalData.baseUrl}/api/user/company`,
      method: 'POST',
      header: {
        Authorization: `Bearer ${app.globalData.token || wx.getStorageSync('token')}`,
        'Content-Type': 'application/json',
        'X-User-Id': userId != null ? String(userId) : ''
      },
      data: {
        companyName: companyName
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
      title: '我的公司 - Spark Link',
      path: '/pages/company/join/company-join'
    };
  }
});
