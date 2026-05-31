import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { get, post } from '@/utils/request';
import { KEYS, getItem, setItem, setJson, removeItem, getJson } from '@/utils/storage';
import { normalizeImageUrl } from '@/utils/mediaUrl';
import { getApiBaseUrl } from '@/config/env';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getItem(KEYS.token) || '');
  const userId = ref(getItem(KEYS.userId) || '');
  const userInfo = ref(getJson(KEYS.userInfo, {}));
  const unreadCount = ref(0);

  const isLoggedIn = computed(() => {
    return !!(token.value && userId.value && getItem(KEYS.isLoggedIn) === 'true');
  });

  function persistSession(payload) {
    if (payload.token) {
      token.value = payload.token;
      setItem(KEYS.token, payload.token);
    }
    if (payload.userId != null && payload.userId !== '') {
      userId.value = String(payload.userId);
      setItem(KEYS.userId, String(payload.userId));
    }
    if (payload.nickname) {
      setItem(KEYS.userNickname, payload.nickname);
    }
    if (payload.avatar) {
      const av = normalizeImageUrl(payload.avatar, getApiBaseUrl());
      setItem(KEYS.userAvatar, av || payload.avatar);
    }
    if (payload.userInfo) {
      userInfo.value = payload.userInfo;
      setJson(KEYS.userInfo, payload.userInfo);
    }
    setItem(KEYS.isLoggedIn, 'true');
  }

  function clearLoginState() {
    token.value = '';
    userId.value = '';
    userInfo.value = {};
    unreadCount.value = 0;
    removeItem(KEYS.token);
    removeItem(KEYS.userId);
    removeItem(KEYS.userInfo);
    removeItem(KEYS.userAvatar);
    removeItem(KEYS.userNickname);
    removeItem(KEYS.isLoggedIn);
    removeItem(KEYS.imUserSig);
    removeItem(KEYS.imUserID);
    removeItem(KEYS.imSdkAppId);
    // 仅清理旧版全局会话键；保留按用户隔离的 sessionId:{userId}
    // 这样同一用户重新登录后仍可看到自己的历史对话。
    removeItem(KEYS.sessionId);
  }

  async function login(username, password) {
    const res = await post('/api/user/login', { username, password });
    const d = res.data || {};
    persistSession({
      token: d.token,
      userId: d.userId,
      nickname: d.nickname,
      avatar: d.avatar
    });
    await fetchUserInfo();
    return d;
  }

  /** 仅开发环境：本地后端开启 allow-test-login 时使用 */
  async function loginWithTestAccount() {
    const res = await post('/api/user/test-login', {});
    const d = res.data || {};
    persistSession({
      token: d.token,
      userId: d.userId,
      nickname: d.nickname,
      avatar: d.avatar
    });
    await fetchUserInfo();
    return d;
  }

  async function fetchUserInfo() {
    if (!userId.value) {
      return false;
    }
    try {
      const res = await get('/api/user/info', {}, { suppressErrorToast: true });
      if (res && res.code === 0 && res.data) {
        userInfo.value = res.data;
        setJson(KEYS.userInfo, res.data);
        if (res.data.avatar) {
          setItem(KEYS.userAvatar, normalizeImageUrl(res.data.avatar, getApiBaseUrl()));
        }
        return true;
      }
      clearLoginState();
      return false;
    } catch {
      clearLoginState();
      return false;
    }
  }

  function setUnreadCount(n) {
    unreadCount.value = Math.max(0, Number(n) || 0);
  }

  return {
    token,
    userId,
    userInfo,
    unreadCount,
    isLoggedIn,
    persistSession,
    clearLoginState,
    login,
    loginWithTestAccount,
    fetchUserInfo,
    setUnreadCount
  };
});
