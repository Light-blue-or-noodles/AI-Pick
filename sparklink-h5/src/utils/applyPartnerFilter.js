const TYPE_KEYWORDS = {
  game: ['游戏'],
  sports: ['运动'],
  food: ['干饭', '美食', '饭'],
  study: ['学习', '聊天'],
  travel: ['旅游'],
  other: []
};

function parseDistanceKm(text) {
  if (!text) {
    return null;
  }
  const m = String(text).match(/([\d.]+)\s*km/i);
  return m ? Number(m[1]) : null;
}

export function applyPartnerFilter(list, filter) {
  if (!filter) {
    return list;
  }
  return list.filter((item) => {
    if (filter.partnerStatus !== 'all' && item.status !== filter.partnerStatus) {
      return false;
    }
    if (filter.partnerType !== 'all') {
      const keys = TYPE_KEYWORDS[filter.partnerType] || [];
      const cat = item.category || item.typeName || '';
      if (keys.length && !keys.some((k) => cat.includes(k))) {
        return false;
      }
    }
    if (filter.distance !== 'all') {
      const limit = Number(String(filter.distance).replace('km', ''));
      const km = parseDistanceKm(item.distance);
      if (km != null && limit && km > limit) {
        return false;
      }
    }
    return true;
  });
}
