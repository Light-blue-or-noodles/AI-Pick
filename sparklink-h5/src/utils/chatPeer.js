import { KEYS, getItem } from '@/utils/storage';

export function getCurrentUserId() {
  const raw = getItem(KEYS.userId);
  if (raw == null || raw === '') {
    return '';
  }
  return String(raw).trim();
}

export function isSelfChat(targetUserId) {
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

export function resolvePublisherUserId(detail) {
  if (!detail) {
    return '';
  }
  const raw = detail.userId ?? detail.creatorId;
  if (raw == null || raw === '') {
    return '';
  }
  return String(raw).trim();
}
