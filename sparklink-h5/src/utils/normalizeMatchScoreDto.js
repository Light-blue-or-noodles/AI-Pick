/**
 * 从 Result 中取出匹配度 DTO，兼容旧字段 interestScore
 */
export function normalizeMatchScoreDto(res) {
  if (!res) {
    return null;
  }
  const dto = res.data != null ? res.data : res;
  if (!dto || typeof dto !== 'object') {
    return null;
  }
  const normalized = { ...dto };
  if (normalized.activityTagScore == null && normalized.interestScore != null) {
    normalized.activityTagScore = normalized.interestScore;
  }
  if (normalized.publisherTagScore == null && normalized.interestScore != null) {
    normalized.publisherTagScore = normalized.interestScore;
  }
  return normalized;
}
