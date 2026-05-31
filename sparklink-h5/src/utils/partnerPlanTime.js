function pad2(n) {
  return String(Number(n)).padStart(2, '0');
}

export function formatPlanDateTime(v) {
  if (v == null || v === '') {
    return '';
  }
  if (typeof v === 'number' && !Number.isNaN(v)) {
    const ms = v < 1e12 ? v * 1000 : v;
    const d = new Date(ms);
    if (Number.isNaN(d.getTime())) {
      return '';
    }
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
  }
  if (v && typeof v === 'object' && !Array.isArray(v)) {
    if (typeof v.year === 'number' && typeof v.monthValue === 'number' && typeof v.dayOfMonth === 'number') {
      const h = v.hour != null ? Number(v.hour) : 0;
      const mi = v.minute != null ? Number(v.minute) : 0;
      return `${v.year}-${pad2(v.monthValue)}-${pad2(v.dayOfMonth)} ${pad2(h)}:${pad2(mi)}`;
    }
  }
  if (Array.isArray(v)) {
    const a = v.map((x) => Number(x));
    if (a.length >= 5 && !a.some((n) => Number.isNaN(n))) {
      return `${a[0]}-${pad2(a[1])}-${pad2(a[2])} ${pad2(a[3])}:${pad2(a[4])}`;
    }
  }
  const s = String(v).replace('T', ' ').trim();
  if (!s || s === 'null' || s === 'undefined') {
    return '';
  }
  const norm = s.match(/^(\d{4})-(\d{1,2})-(\d{1,2})[ T](\d{1,2}):(\d{1,2})/);
  if (norm) {
    return `${norm[1]}-${pad2(norm[2])}-${pad2(norm[3])} ${pad2(norm[4])}:${pad2(norm[5])}`;
  }
  return s.length > 32 ? s.substring(0, 32) : s;
}

export function buildPlanTimeRangeDisplay(planStart, planEnd) {
  const start = formatPlanDateTime(planStart);
  const end = formatPlanDateTime(planEnd);
  if (!start && !end) {
    return '';
  }
  if (start && !end) {
    return start;
  }
  if (!start && end) {
    return `截止 ${end}`;
  }
  if (start === end) {
    return start;
  }
  return `${start} ～ ${end}`;
}

export function buildPlanTimeRangeWithFallback(raw, planRange) {
  if (planRange) {
    return planRange;
  }
  const createRaw = raw?.createTime;
  const c = createRaw != null && createRaw !== '' ? formatPlanDateTime(createRaw) : '';
  if (c) {
    return `计划时间未设置 · 发布于 ${c}`;
  }
  return '未设置';
}

/**
 * 详情页时间展示：计划时间与发布时间分开展示，避免混在一行
 */
export function resolvePlanTimeMeta(raw) {
  const range = buildPlanTimeRangeDisplay(raw?.planTime, raw?.planEndTime);
  const publishTime = formatPlanDateTime(raw?.createTime);
  if (range) {
    return {
      hasPlanTime: true,
      primary: range,
      secondary: ''
    };
  }
  return {
    hasPlanTime: false,
    primary: '待定',
    secondary: publishTime ? `发布于 ${publishTime}` : ''
  };
}

/**
 * 地点展示：按「 · 」拆成主地点 + 详细地址两行
 */
export function parseLocationMeta(raw) {
  if (raw == null || String(raw).trim() === '') {
    return null;
  }
  const text = String(raw).trim();
  const parts = text.split(/\s*[·•|｜]\s*/).map((p) => p.trim()).filter(Boolean);
  if (parts.length >= 2) {
    return {
      primary: parts[0],
      secondary: parts.slice(1).join(' ')
    };
  }
  return {
    primary: text,
    secondary: ''
  };
}

export function splitPreferenceTags(prefRaw) {
  if (!prefRaw) {
    return [];
  }
  return String(prefRaw)
    .split(/[,，、\s]+/)
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
}
