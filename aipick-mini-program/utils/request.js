// utils/request.js
const app = getApp();

// 请求封装
const request = (options) => {
  return new Promise((resolve, reject) => {
    const header = options.header || {};
    
    // 添加 token
    if (app.globalData.token) {
      header['Authorization'] = `Bearer ${app.globalData.token}`;
    }
    
    // 添加 X-User-Id
    if (app.globalData.userId) {
      header['X-User-Id'] = app.globalData.userId;
    }
    
    // 添加 Content-Type
    if (!header['Content-Type']) {
      header['Content-Type'] = 'application/json';
    }

    wx.request({
      url: options.baseUrl || app.globalData.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: header,
      success: (res) => {
        if (res.data.code === 0) {
          resolve(res.data);
        } else if (res.data.code === 401) {
          // 未登录，跳转登录页
          console.warn('未登录，请先到登录页登录');
          // wx.navigateTo({
          //   url: '/pages/login/login'
          // });
          reject(res.data);
        } else {
          wx.showToast({
            title: res.data.msg || '请求失败',
            icon: 'none'
          });
          reject(res.data);
        }
      },
      fail: (err) => {
        wx.showToast({
          title: '网络请求失败',
          icon: 'none'
        });
        reject(err);
      }
    });
  });
};

// GET 请求
const get = (url, data, options = {}) => {
  return request({
    url,
    data,
    method: 'GET',
    ...options
  });
};

// POST 请求
const post = (url, data, options = {}) => {
  return request({
    url,
    data,
    method: 'POST',
    ...options
  });
};

// PUT 请求
const put = (url, data, options = {}) => {
  return request({
    url,
    data,
    method: 'PUT',
    ...options
  });
};

// DELETE 请求
const del = (url, data, options = {}) => {
  return request({
    url,
    data,
    method: 'DELETE',
    ...options
  });
};

module.exports = {
  request,
  get,
  post,
  put,
  delete: del
};