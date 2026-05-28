const KEYS = {
  token: 'token',
  userId: 'userId',
  userInfo: 'userInfo',
  userAvatar: 'userAvatar',
  userNickname: 'userNickname',
  isLoggedIn: 'isLoggedIn',
  sessionId: 'sessionId',
  imUserSig: 'imUserSig',
  imUserID: 'imUserID',
  imSdkAppId: 'imSdkAppId'
};

export function getItem(key) {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

export function setItem(key, value) {
  try {
    if (value == null) {
      localStorage.removeItem(key);
    } else {
      localStorage.setItem(key, String(value));
    }
  } catch {
    /* ignore */
  }
}

export function removeItem(key) {
  try {
    localStorage.removeItem(key);
  } catch {
    /* ignore */
  }
}

export function getJson(key, fallback = null) {
  const raw = getItem(key);
  if (!raw) {
    return fallback;
  }
  try {
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}

export function setJson(key, value) {
  setItem(key, JSON.stringify(value));
}

export { KEYS };
