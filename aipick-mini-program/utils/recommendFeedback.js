/**
 * 推荐反馈：跳过 / 聊聊 / 不合 / 举报 等，与后端 POST /api/recommend/feedback 一致。
 * 页面需已登录（本地 userId），请求会带 Authorization 与 X-User-Id。
 */

const app = getApp();

const FEEDBACK_SKIP = 1;
const FEEDBACK_CHAT = 2;
const FEEDBACK_REPORT = 3;
const FEEDBACK_NOT_COMPATIBLE = 4;

const TARGET_PARTNER = 1;
const TARGET_ACTIVITY = 2;

function getAuthHeaders() {
  const header = {};
  const token = app.globalData.token || wx.getStorageSync('token');
  const userId = app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
  if (token) header['Authorization'] = 'Bearer ' + token;
  if (userId) header['X-User-Id'] = String(userId);
  return header;
}

function getCurrentUserId() {
  return app.globalData.userId != null ? app.globalData.userId : wx.getStorageSync('userId');
}

/**
 * @param {Object} options
 * @param {number} options.feedbackType 1-跳过 2-聊聊 3-举报 4-不合
 * @param {number} [options.targetType=1] 1-搭子帖 2-活动
 * @param {string|number} options.targetId
 * @param {number|string} [options.matchScore]
 * @param {string} [options.baseUrl] 默认 app.globalData.baseUrl
 * @param {function} [options.onSuccess]
 * @param {function} [options.onFail]
 * @param {function} [options.onNeedLogin] 未登录时回调，默认 toast
 * @param {boolean} [options.showToastOnFail=true]
 * @returns {Promise<void>}
 */
function postRecommendFeedback(options) {
  const {
    feedbackType,
    targetType = TARGET_PARTNER,
    targetId,
    matchScore,
    baseUrl: baseUrlOpt,
    onSuccess,
    onFail,
    onNeedLogin,
    showToastOnFail = true
  } = options;

  const userId = getCurrentUserId();
  if (!userId) {
    if (typeof onNeedLogin === 'function') {
      onNeedLogin();
    } else {
      wx.showToast({ title: '请先登录后再操作', icon: 'none' });
    }
    return Promise.reject(new Error('not_logged_in'));
  }

  const baseUrl = baseUrlOpt || app.globalData.baseUrl || 'http://localhost:8080';
  let qs = `userId=${encodeURIComponent(userId)}&targetType=${encodeURIComponent(targetType)}&targetId=${encodeURIComponent(targetId)}&feedbackType=${encodeURIComponent(feedbackType)}`;
  if (matchScore != null && matchScore !== '') {
    qs += `&matchScore=${encodeURIComponent(matchScore)}`;
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${baseUrl}/api/recommend/feedback?${qs}`,
      method: 'POST',
      header: { ...getAuthHeaders() },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          if (typeof onSuccess === 'function') onSuccess(res);
          resolve(res);
        } else {
          if (showToastOnFail) {
            wx.showToast({ title: '反馈提交失败', icon: 'none' });
          }
          if (typeof onFail === 'function') onFail(res);
          reject(res);
        }
      },
      fail: (err) => {
        if (showToastOnFail) {
          wx.showToast({ title: '反馈提交失败', icon: 'none' });
        }
        if (typeof onFail === 'function') onFail(err);
        reject(err);
      }
    });
  });
}

module.exports = {
  FEEDBACK_SKIP,
  FEEDBACK_CHAT,
  FEEDBACK_REPORT,
  FEEDBACK_NOT_COMPATIBLE,
  TARGET_PARTNER,
  TARGET_ACTIVITY,
  getCurrentUserId,
  postRecommendFeedback
};
