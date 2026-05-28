export const IM_CONFIG = {
  sdkAppID: 1600133993,
  logLevel: 1,
  uploadLog: false
};

export function getIMUserInfo() {
  const userID = localStorage.getItem('imUserID') || localStorage.getItem('userId') || '';
  const userSig = localStorage.getItem('imUserSig') || '';
  return {
    userID: userID ? String(userID) : '',
    userSig: userSig || ''
  };
}

export function getSdkAppId() {
  const fromStore = localStorage.getItem('imSdkAppId');
  if (fromStore != null && fromStore !== '') {
    const n = Number(fromStore);
    if (Number.isFinite(n) && n > 0) {
      return n;
    }
  }
  return IM_CONFIG.sdkAppID || 0;
}
