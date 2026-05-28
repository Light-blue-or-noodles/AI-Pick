import TIM from 'tim-js-sdk';
import { get } from '@/utils/request';
import { KEYS, setItem, removeItem } from '@/utils/storage';
import { getIMUserInfo, getSdkAppId, IM_CONFIG } from '@/config/im';
import { useAuthStore } from '@/stores/auth';

class IMService {
  constructor() {
    this.tim = null;
    this.initialized = false;
    this.loggedIn = false;
    this.currentUserID = '';
    this._sdkAppId = 0;
    this.messageHandlers = [];
    this._boundMessageListener = null;
    this._onTotalUnreadListener = null;
    this._totalUnreadBound = false;
    this._sdkReady = false;
    this._onSdkReadyListener = null;
  }

  _safeUpdateUnreadBadge(count) {
    try {
      const auth = useAuthStore();
      auth.setUnreadCount(count);
    } catch {
      /* pinia may not be ready */
    }
  }

  _bindTotalUnreadListener() {
    if (!this.tim || this._totalUnreadBound) {
      return;
    }
    this._onTotalUnreadListener = (event) => {
      const n = typeof event.data === 'number' ? event.data : Number(event.data) || 0;
      this._safeUpdateUnreadBadge(n);
    };
    this.tim.on(TIM.EVENT.TOTAL_UNREAD_MESSAGE_COUNT_UPDATED, this._onTotalUnreadListener);
    this._totalUnreadBound = true;
  }

  _unbindTotalUnreadListener() {
    if (this.tim && this._totalUnreadBound && this._onTotalUnreadListener) {
      try {
        this.tim.off(TIM.EVENT.TOTAL_UNREAD_MESSAGE_COUNT_UPDATED, this._onTotalUnreadListener);
      } catch {
        /* ignore */
      }
    }
    this._onTotalUnreadListener = null;
    this._totalUnreadBound = false;
  }

  _bindSdkReadyListener() {
    if (!this.tim || this._onSdkReadyListener) {
      return;
    }
    this._onSdkReadyListener = () => {
      this._sdkReady = true;
      this.syncUnreadBadgeFromSdk();
    };
    this.tim.on(TIM.EVENT.SDK_READY, this._onSdkReadyListener);
  }

  _unbindSdkReadyListener() {
    if (this.tim && this._onSdkReadyListener) {
      try {
        this.tim.off(TIM.EVENT.SDK_READY, this._onSdkReadyListener);
      } catch {
        /* ignore */
      }
    }
    this._onSdkReadyListener = null;
    this._sdkReady = false;
  }

  waitForSdkReady(timeoutMs = 20000) {
    return new Promise((resolve, reject) => {
      if (!this.tim) {
        reject(new Error('IM SDK 未初始化'));
        return;
      }
      if (this._sdkReady) {
        resolve();
        return;
      }
      let finished = false;
      let timer = null;
      const finish = (ok) => {
        if (finished) {
          return;
        }
        finished = true;
        if (timer != null) {
          clearTimeout(timer);
        }
        try {
          this.tim.off(TIM.EVENT.SDK_READY, onReady);
        } catch {
          /* ignore */
        }
        if (ok || this._sdkReady) {
          resolve();
        } else {
          reject(new Error('等待 IM SDK 就绪超时，请稍后重试'));
        }
      };
      const onReady = () => finish(true);
      this.tim.on(TIM.EVENT.SDK_READY, onReady);
      if (this._sdkReady) {
        finish(true);
        return;
      }
      timer = setTimeout(() => finish(false), timeoutMs);
    });
  }

  syncUnreadBadgeFromSdk() {
    if (!this.tim || !this.loggedIn || !this._sdkReady) {
      return;
    }
    try {
      if (typeof this.tim.getTotalUnreadMessageCount === 'function') {
        const n = this.tim.getTotalUnreadMessageCount();
        this._safeUpdateUnreadBadge(n);
      }
    } catch {
      /* ignore */
    }
  }

  async checkUserValid() {
    try {
      const res = await get('/api/user/info');
      if (!res || res.code !== 0) {
        const auth = useAuthStore();
        auth.clearLoginState();
        return false;
      }
      return true;
    } catch {
      return false;
    }
  }

  async refreshCredentials() {
    const isValid = await this.checkUserValid();
    if (!isValid) {
      throw new Error('用户不存在或登录已失效，请重新登录');
    }
    const res = await get('/api/im/usersig');
    if (!res || res.code !== 0 || !res.data) {
      throw new Error((res && res.message) || '获取 IM 凭证失败');
    }
    const d = res.data;
    if (!d.userSig || d.userId == null) {
      throw new Error('IM 凭证数据不完整');
    }
    setItem(KEYS.imUserSig, d.userSig);
    setItem(KEYS.imUserID, String(d.userId));
    if (d.sdkAppId != null) {
      setItem(KEYS.imSdkAppId, String(d.sdkAppId));
    }
    return d;
  }

  init() {
    const sdkAppID = getSdkAppId();
    if (!sdkAppID) {
      throw new Error('IM 配置缺失');
    }
    if (this.tim && this._sdkAppId !== sdkAppID) {
      try {
        this._unbindSdkReadyListener();
        this._unbindTotalUnreadListener();
        if (this._boundMessageListener) {
          this.tim.off(TIM.EVENT.MESSAGE_RECEIVED, this._boundMessageListener);
          this._boundMessageListener = null;
        }
        this.tim.destroy();
      } catch {
        /* ignore */
      }
      this.tim = null;
      this.initialized = false;
      this.loggedIn = false;
      this.currentUserID = '';
    }
    if (this.initialized && this.tim) {
      if (!this._onSdkReadyListener) {
        this._bindSdkReadyListener();
      }
      return this.tim;
    }
    this._sdkAppId = sdkAppID;
    this.tim = TIM.create({ SDKAppID: sdkAppID });
    this.tim.setLogLevel(IM_CONFIG.logLevel);
    if (IM_CONFIG.uploadLog) {
      this.tim.setLogReport(true);
    }
    this._unbindSdkReadyListener();
    this._sdkReady = false;
    this._bindSdkReadyListener();
    this.initialized = true;
    return this.tim;
  }

  async initAndLogin(options = {}) {
    let refreshFailReason = '';
    let userInvalid = false;
    if (!options.skipRefresh) {
      try {
        await this.refreshCredentials();
      } catch (e) {
        refreshFailReason = (e && e.message) || '';
        if (
          refreshFailReason.indexOf('用户不存在') !== -1
          || refreshFailReason.indexOf('登录已失效') !== -1
        ) {
          userInvalid = true;
        }
      }
    }
    if (userInvalid) {
      throw new Error('IM 登录失败：用户不存在或登录已失效，请重新登录');
    }
    this.init();
    const savedInfo = getIMUserInfo();
    const userID = options.userID || savedInfo.userID;
    const userSig = options.userSig || savedInfo.userSig;
    if (!userID || !userSig) {
      const hint = refreshFailReason
        ? `拉取 IM 凭证失败：${refreshFailReason}`
        : '请使用有效账号登录';
      throw new Error(`IM 登录失败：缺少 userID 或 userSig。${hint}`);
    }
    if (this.loggedIn && this.currentUserID === String(userID)) {
      this._bindTotalUnreadListener();
      this.syncUnreadBadgeFromSdk();
      if (!this._sdkReady) {
        await this.waitForSdkReady();
      }
      return true;
    }
    try {
      this._sdkReady = false;
      await this.tim.login({
        userID: String(userID),
        userSig
      });
      this.loggedIn = true;
      this.currentUserID = String(userID);
      setItem(KEYS.imUserID, String(userID));
      this._bindTotalUnreadListener();
      await this.waitForSdkReady();
      return true;
    } catch (error) {
      this.loggedIn = false;
      this.currentUserID = '';
      throw error;
    }
  }

  async logout() {
    this._unbindSdkReadyListener();
    this._unbindTotalUnreadListener();
    this._safeUpdateUnreadBadge(0);
    this.messageHandlers = [];
    if (this._boundMessageListener && this.tim) {
      try {
        this.tim.off(TIM.EVENT.MESSAGE_RECEIVED, this._boundMessageListener);
      } catch {
        /* ignore */
      }
      this._boundMessageListener = null;
    }
    if (this.tim) {
      try {
        await this.tim.logout();
      } catch {
        /* ignore */
      }
    }
    this.loggedIn = false;
    this.currentUserID = '';
    removeItem(KEYS.imUserSig);
    removeItem(KEYS.imSdkAppId);
  }

  async sendSingleMessage(toUserID, text) {
    if (!this.tim || !this.loggedIn) {
      throw new Error('IM 未登录');
    }
    await this.waitForSdkReady();
    const peer = this.normalizePeerUserId(toUserID);
    if (!peer || !text) {
      throw new Error('发送消息参数无效');
    }
    const message = this.tim.createTextMessage({
      to: peer,
      conversationType: TIM.TYPES.CONV_C2C,
      payload: { text }
    });
    const result = await this.tim.sendMessage(message);
    return result.data && result.data.message ? result.data.message : message;
  }

  async getConversationList() {
    if (!this.tim || !this.loggedIn) {
      throw new Error('IM 未登录');
    }
    await this.waitForSdkReady();
    const result = await this.tim.getConversationList();
    return (result.data && result.data.conversationList) || [];
  }

  async getMessageHistory(conversationID, nextReqMessageID = '', count = 20) {
    if (!this.tim || !this.loggedIn) {
      throw new Error('IM 未登录');
    }
    if (!conversationID) {
      throw new Error('conversationID 不能为空');
    }
    await this.waitForSdkReady();
    const result = await this.tim.getMessageList({
      conversationID,
      nextReqMessageID,
      count
    });
    return {
      messageList: (result.data && result.data.messageList) || [],
      nextReqMessageID: (result.data && result.data.nextReqMessageID) || '',
      isCompleted: !!(result.data && result.data.isCompleted)
    };
  }

  onNewMessage(handler) {
    if (typeof handler !== 'function') {
      return () => {};
    }
    this.messageHandlers.push(handler);
    if (!this._boundMessageListener && this.tim) {
      this._boundMessageListener = (event) => {
        const list = (event.data || []).slice();
        this.messageHandlers.forEach((item) => {
          try {
            item(list);
          } catch (error) {
            console.error('IM 新消息回调执行失败:', error);
          }
        });
      };
      this.tim.on(TIM.EVENT.MESSAGE_RECEIVED, this._boundMessageListener);
    }
    return () => {
      this.messageHandlers = this.messageHandlers.filter((fn) => fn !== handler);
      if (this.messageHandlers.length === 0 && this._boundMessageListener && this.tim) {
        this.tim.off(TIM.EVENT.MESSAGE_RECEIVED, this._boundMessageListener);
        this._boundMessageListener = null;
      }
    };
  }

  async setMessageRead(conversationID) {
    if (!this.tim || !this.loggedIn || !conversationID) {
      return false;
    }
    await this.waitForSdkReady();
    await this.tim.setMessageRead({ conversationID });
    return true;
  }

  buildC2CConversationID(userID) {
    return `C2C${this.normalizePeerUserId(userID)}`;
  }

  normalizePeerUserId(raw) {
    if (raw == null || raw === '') {
      return '';
    }
    let s = String(raw).trim();
    if (s.indexOf('C2C') === 0) {
      s = s.slice(3).trim();
    }
    return s;
  }
}

export default new IMService();
