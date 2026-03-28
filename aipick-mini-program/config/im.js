/**
 * 腾讯云 IM 配置
 * 说明：
 * 1. 请将 sdkAppID 替换为腾讯云控制台申请的真实值。
 * 2. userSig 建议由后端签发后下发到小程序，避免在前端明文生成。
 */
const IM_CONFIG = {
  // 腾讯云 IM SDKAppID（与后端 tencent.im.sdk-app-id 一致；登录后会以服务端 /api/im/usersig 为准写入缓存）
  sdkAppID: 1600133993,
  // 运行环境：dev/test/prod
  env: 'dev',
  // TIM 日志等级：0-4（0 最详细，4 关闭）
  logLevel: 1,
  // 是否开启上传 SDK 日志（生产环境建议按需开启）
  uploadLog: false
};

function getIMUserInfo() {
  const userID = wx.getStorageSync('imUserID') || wx.getStorageSync('userId');
  const userSig = wx.getStorageSync('imUserSig');
  return {
    userID: userID ? String(userID) : '',
    userSig: userSig || ''
  };
}

function getSdkAppId() {
  const fromStore = wx.getStorageSync('imSdkAppId');
  if (fromStore != null && fromStore !== '') {
    const n = Number(fromStore);
    if (Number.isFinite(n) && n > 0) return n;
  }
  return IM_CONFIG.sdkAppID || 0;
}

module.exports = {
  IM_CONFIG,
  getIMUserInfo,
  getSdkAppId
};
