const TIM = require('./vendor/tim-wx.js');
const { IM_CONFIG, getIMUserInfo, getSdkAppId } = require('../config/im');
const { get } = require('./request');

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
    /** TIM 就绪后方可调用 getTotalUnreadMessageCount 等（否则报「用户正在登录中 / sdk not ready」） */
    this._sdkReady = false;
    this._onSdkReadyListener = null;
  }

  _safeUpdateUnreadBadge(count) {
    try {
      const app = getApp();
      if (app && typeof app.updateImUnreadBadge === 'function') {
        app.updateImUnreadBadge(count);
      }
    } catch (e) {
      console.warn('updateImUnreadBadge', e);
    }
  }

  _bindTotalUnreadListener() {
    const TIM = require('./vendor/tim-wx.js');
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
    const TIM = require('./vendor/tim-wx.js');
    if (this.tim && this._totalUnreadBound && this._onTotalUnreadListener) {
      try {
        this.tim.off(TIM.EVENT.TOTAL_UNREAD_MESSAGE_COUNT_UPDATED, this._onTotalUnreadListener);
      } catch (e) {
        console.warn('TIM off TOTAL_UNREAD', e);
      }
    }
    this._onTotalUnreadListener = null;
    this._totalUnreadBound = false;
  }

  _bindSdkReadyListener() {
    const TIM = require('./vendor/tim-wx.js');
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
    const TIM = require('./vendor/tim-wx.js');
    if (this.tim && this._onSdkReadyListener) {
      try {
        this.tim.off(TIM.EVENT.SDK_READY, this._onSdkReadyListener);
      } catch (e) {
        console.warn('TIM off SDK_READY', e);
      }
    }
    this._onSdkReadyListener = null;
    this._sdkReady = false;
  }

  /**
   * login 成功后会话能力仍可能在「登录中」；须等 SDK_READY 再调 setMessageRead / getMessageList 等。
   */
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
        } catch (e) {
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

  /** 主动拉取当前未读总数并刷新 Tab 角标（须在 SDK_READY 之后；未就绪则自动跳过） */
  syncUnreadBadgeFromSdk() {
    if (!this.tim || !this.loggedIn || !this._sdkReady) {
      return;
    }
    try {
      if (typeof this.tim.getTotalUnreadMessageCount === 'function') {
        const n = this.tim.getTotalUnreadMessageCount();
        this._safeUpdateUnreadBadge(n);
      }
    } catch (e) {
      console.warn('getTotalUnreadMessageCount', e);
    }
  }

  /**
   * 检查用户是否有效（在服务端是否存在）
   * @returns {Promise<boolean>}
   */
  async checkUserValid() {
    try {
      const res = await get('/api/user/info');
      // 用户不存在或 token 失效
      if (!res || res.code !== 0) {
        if (res && (res.code === 400 || res.code === 401)) {
          const msg = res.message || res.msg || '';
          if (msg.indexOf('用户不存在') !== -1 || res.code === 401) {
            // 清除登录状态
            try {
              const app = getApp();
              if (app && typeof app.clearLoginState === 'function') {
                app.clearLoginState();
              } else {
                // 手动清除
                wx.removeStorageSync('token');
                wx.removeStorageSync('userId');
                wx.removeStorageSync('userInfo');
                wx.removeStorageSync('imUserSig');
                wx.removeStorageSync('imUserID');
              }
            } catch (e) {
              console.warn('清除登录状态失败', e);
            }
            wx.showToast({
              title: '账号已失效，请重新登录',
              icon: 'none',
              duration: 2500
            });
            return false;
          }
        }
      }
      return res && res.code === 0;
    } catch (e) {
      console.warn('检查用户有效性失败', e);
      return false;
    }
  }

  /**
   * 从服务端拉取 UserSig / sdkAppId 并写入本地缓存
   */
  async refreshCredentials() {
    // 先检查用户有效性
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
    wx.setStorageSync('imUserSig', d.userSig);
    wx.setStorageSync('imUserID', String(d.userId));
    if (d.sdkAppId != null) {
      wx.setStorageSync('imSdkAppId', Number(d.sdkAppId));
    }
    return d;
  }

  /**
   * 初始化 SDK
   */
  init() {
    const sdkAppID = getSdkAppId();
    if (!sdkAppID) {
      throw new Error('IM 配置缺失：请先登录并拉取 usersig，或在 config/im 中配置 sdkAppID');
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
      } catch (e) {
        console.warn('TIM destroy', e);
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
    this.tim = TIM.create({
      SDKAppID: sdkAppID
    });
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

  /**
   * 初始化并登录
   */
  async initAndLogin(options = {}) {
    let refreshFailReason = '';
    let userInvalid = false;
    
    if (!options.skipRefresh) {
      try {
        await this.refreshCredentials();
      } catch (e) {
        console.warn('IM refreshCredentials 失败，将尝试使用本地缓存', e);
        refreshFailReason =
          (e && (e.message || e.msg)) || (typeof e === 'string' ? e : '') || '';
        // 检查是否是用户无效导致的失败
        if (refreshFailReason.indexOf('用户不存在') !== -1 || 
            refreshFailReason.indexOf('登录已失效') !== -1) {
          userInvalid = true;
        }
      }
    }
    
    // 如果用户无效，直接抛出错误，不再尝试使用本地缓存
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
        : '请使用有效账号登录（测试账号需服务端开启 app.allow-test-login 并 POST /api/user/test-login）';
      throw new Error(`IM 登录失败：缺少 userID 或 userSig。${hint}`);
    }
    if (this.loggedIn && this.currentUserID === String(userID)) {
      this._bindTotalUnreadListener();
      // 未读角标仅在 SDK_READY 后拉取（见 _bindSdkReadyListener）
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
      wx.setStorageSync('imUserID', String(userID));
      this._bindTotalUnreadListener();
      // 登录成功 ≠ SDK 已就绪；勿在此调用 getTotalUnreadMessageCount，等 SDK_READY 回调
      await this.waitForSdkReady();
      return true;
    } catch (error) {
      this.loggedIn = false;
      this.currentUserID = '';
      throw error;
    }
  }

  /**
   * 退出 TIM（切换账号或退出登录时请调用）
   */
  async logout() {
    this._unbindSdkReadyListener();
    this._unbindTotalUnreadListener();
    this._safeUpdateUnreadBadge(0);
    this.messageHandlers = [];
    if (this._boundMessageListener && this.tim) {
      try {
        this.tim.off(TIM.EVENT.MESSAGE_RECEIVED, this._boundMessageListener);
      } catch (e) {
        console.warn('TIM off message', e);
      }
      this._boundMessageListener = null;
    }
    if (this.tim) {
      try {
        await this.tim.logout();
      } catch (e) {
        console.warn('TIM logout', e);
      }
    }
    this.loggedIn = false;
    this.currentUserID = '';
    wx.removeStorageSync('imUserSig');
    wx.removeStorageSync('imSdkAppId');
  }

  /**
   * 发送 C2C 单聊文本消息
   */
  async sendSingleMessage(toUserID, text) {
    if (!this.tim || !this.loggedIn) {
      throw new Error('IM 未登录，请先调用 initAndLogin');
    }
    await this.waitForSdkReady();
    const peer = this.normalizePeerUserId(toUserID);
    if (!peer || !text) {
      throw new Error('发送消息参数无效');
    }
    const message = this.tim.createTextMessage({
      to: peer,
      conversationType: TIM.TYPES.CONV_C2C,
      payload: {
        text
      }
    });
    const result = await this.tim.sendMessage(message);
    return result.data && result.data.message ? result.data.message : message;
  }

  /**
   * 获取会话列表
   */
  async getConversationList() {
    if (!this.tim || !this.loggedIn) {
      throw new Error('IM 未登录，请先调用 initAndLogin');
    }
    await this.waitForSdkReady();
    const result = await this.tim.getConversationList();
    return (result.data && result.data.conversationList) || [];
  }

  /**
   * 获取历史消息
   */
  async getMessageHistory(conversationID, nextReqMessageID = '', count = 20) {
    if (!this.tim || !this.loggedIn) {
      throw new Error('IM 未登录，请先调用 initAndLogin');
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

  /**
   * 监听新消息（返回取消监听函数）
   */
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

  /**
   * 设置会话已读
   */
  async setMessageRead(conversationID) {
    if (!this.tim || !this.loggedIn) {
      throw new Error('IM 未登录，请先调用 initAndLogin');
    }
    if (!conversationID) {
      return false;
    }
    await this.waitForSdkReady();
    await this.tim.setMessageRead({ conversationID });
    return true;
  }

  buildC2CConversationID(userID) {
    return `C2C${this.normalizePeerUserId(userID)}`;
  }

  /** 与后端 IM UserID（数字主键字符串）对齐；去掉误传入的 C2C 前缀 */
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

module.exports = new IMService();
