/**
 * 搭子列表项映射（与后端 PartnerVO 字段对齐）
 */
const TYPE_NAMES = {
  1: '宠物搭子',
  2: '电影搭子',
  3: '音乐搭子',
  4: '逛街搭子',
  5: '运动搭子',
  6: '摄影搭子',
  7: '干饭搭子',
  8: '旅游搭子',
  9: 'k歌搭子',
  10: '喝酒搭子',
  11: '桌游搭子',
  12: '钓鱼搭子',
  13: '游戏搭子',
  14: '聊天搭子',
  15: '户外搭子'
};

function mapPartnerForList(p, baseUrl) {
  const app = getApp();
  const base = baseUrl || 'https://www.aipick.cloud';
  const norm = (u) => {
    if (!u || (typeof u === 'string' && u.startsWith('/images'))) return u || '';
    return app.normalizeImageUrl ? app.normalizeImageUrl(u, base) : (u.startsWith('http') ? u : (base + '/api' + (u.startsWith('/') ? u : '/' + u)));
  };
  const cur =
    p.currentParticipants != null ? p.currentParticipants : p.currentCount != null ? p.currentCount : 0;
  const max =
    p.maxParticipants != null ? p.maxParticipants : p.targetCount != null ? p.targetCount : 2;
  let status = p.status;
  if (status === undefined || status === null) {
    if (cur >= max && max > 0) {
      status = 1;
    } else {
      status = 0;
    }
  }
  const statusText = status === 0 ? 'recruiting' : status === 1 ? 'full' : 'ended';
  const statusLabel = status === 0 ? '招募中' : status === 1 ? '已满' : '已结束';
  const createTime = p.createTime
    ? typeof p.createTime === 'string'
      ? p.createTime.replace('T', ' ').substring(0, 16)
      : ''
    : '';
  const cover = norm(p.coverImage) || '/images/partner-banner.jpg';
  const authorAvatar = norm(p.avatar) || '/images/default-avatar.png';
  const typeCode = p.typeCode != null ? p.typeCode : parseInt(p.type, 10);
  const typeName = p.typeName || TYPE_NAMES[typeCode] || '其他';
  const prefRaw = p.preference || '';
  const preferenceDisplay = prefRaw
    ? String(prefRaw)
        .split(/[,，、]+/)
        .map((s) => s.trim())
        .filter(Boolean)
        .join(' · ')
    : '';
  return {
    id: p.id,
    title: p.title || '未命名',
    category: typeName,
    typeName,
    preference: preferenceDisplay,
    status: statusText,
    statusLabel,
    cover,
    author: {
      name: p.nickname || '用户',
      avatar: authorAvatar
    },
    members: cur,
    maxMembers: max,
    createTime,
    tags: p.tags || [],
    distance: p.address || p.location || '',
    match: p.matchScore
  };
}

module.exports = {
  TYPE_NAMES,
  mapPartnerForList
};
