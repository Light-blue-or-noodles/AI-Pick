/**
 * 对齐小程序聊天跳转：普通用户 → /chat，AI → /ai-chat
 */
export function navigateToChat(router, { userId, nickname = '', avatar = '', isAI = false } = {}) {
  if (isAI) {
    router.push({ name: 'ai-chat' });
    return;
  }
  if (!userId) {
    return;
  }
  router.push({
    name: 'chat',
    query: {
      userId: String(userId),
      nickname: nickname || '',
      avatar: avatar || ''
    }
  });
}
