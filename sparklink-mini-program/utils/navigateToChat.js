// utils/navigateToChat.js — 私聊跳转前校验（禁止与自身发起 C2C）

function getAppSafe() {
  try {
    return getApp();
  } catch (e) {
    return undefined;
  }
}

function getCurrentUserId() {
  const app = getAppSafe();
  const raw =
    (app && app.globalData && app.globalData.userId != null && app.globalData.userId !== ''
      ? app.globalData.userId
      : null) || wx.getStorageSync('userId');
  if (raw == null || raw === '') {
    return '';
  }
  return String(raw).trim();
}

function isSelfChat(targetUserId) {
  if (targetUserId == null || targetUserId === '') {
    return false;
  }
  const me = getCurrentUserId();
  if (!me) {
    return false;
  }
  const peer = String(targetUserId).trim();
  if (!peer) {
    return false;
  }
  const meNum = Number(me);
  const peerNum = Number(peer);
  if (Number.isFinite(meNum) && Number.isFinite(peerNum) && meNum > 0) {
    return meNum === peerNum;
  }
  return me === peer;
}

function showSelfChatBlocked() {
  wx.showToast({
    title: '不能与自己聊天',
    icon: 'none',
    duration: 2000
  });
}

/**
 * @param {{ userId: string|number, nickname?: string, avatar?: string }} peer
 */
function navigateToChatWithPeer(peer) {
  const userId = peer && peer.userId;
  if (!userId) {
    wx.showToast({ title: '无法发起聊天', icon: 'none' });
    return;
  }
  if (isSelfChat(userId)) {
    showSelfChatBlocked();
    return;
  }
  const nickname = peer.nickname != null ? String(peer.nickname) : '';
  const avatar = peer.avatar != null ? String(peer.avatar) : '';
  wx.navigateTo({
    url:
      '/pages/chat/chat?userId=' +
      encodeURIComponent(String(userId)) +
      '&nickname=' +
      encodeURIComponent(nickname) +
      '&avatar=' +
      encodeURIComponent(avatar)
  });
}

module.exports = {
  getCurrentUserId,
  isSelfChat,
  navigateToChatWithPeer,
  showSelfChatBlocked
};
