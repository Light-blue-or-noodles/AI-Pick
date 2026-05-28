/**
 * 搭子列表卡片：昵称 / 地址展示规则（全局统一）
 */

/**
 * 昵称：最多 2 行，每行最多 4 个字；第 2 行超过 4 字时显示前 3 字 + ...
 * @param {string} name
 * @returns {string[]}
 */
export function formatCardNicknameLines(name) {
  const s = String(name ?? '').trim() || '用户';
  const units = Array.from(s);
  if (units.length <= 4) {
    return [s];
  }
  const line1 = units.slice(0, 4).join('');
  if (units.length <= 8) {
    return [line1, units.slice(4, 8).join('')];
  }
  return [line1, `${units.slice(4, 7).join('')}...`];
}

/**
 * 地址：原样返回，由 CSS 限制最多 2 行省略
 * @param {string} address
 * @returns {string}
 */
export function formatCardAddressText(address) {
  const s = String(address ?? '').trim();
  return s;
}
