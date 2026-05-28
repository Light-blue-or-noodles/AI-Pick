<template>
  <div class="chat page page--no-tab">
    <van-nav-bar :title="title" left-arrow @click-left="$router.back()" />
    <div ref="scrollEl" class="chat__messages">
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        class="chat__row"
        :class="msg.type === 'self' ? 'chat__row--self' : 'chat__row--other'"
      >
        <img v-if="msg.avatar" :src="msg.avatar" class="chat__avatar" alt="" />
        <div class="chat__bubble">{{ msg.content }}</div>
      </div>
    </div>
    <div class="chat__input-bar">
      <van-field v-model="inputValue" placeholder="输入消息" @keyup.enter="send" />
      <van-button type="primary" size="small" color="#5FB3A8" :loading="sending" @click="send">发送</van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { showToast } from 'vant';
import IMService from '@/utils/imService';
import { get } from '@/utils/request';
import { normalizeImageUrl, resolveMediaUrl } from '@/utils/mediaUrl';
import { getApiBaseUrl } from '@/config/env';
import { useAuthStore } from '@/stores/auth';
import { KEYS, getItem } from '@/utils/storage';

const route = useRoute();
const auth = useAuthStore();
const scrollEl = ref(null);

const isAI = computed(() => route.query.isAI === 'true' || !route.query.userId);
const userId = computed(() => route.query.userId || '');
const title = computed(() => {
  if (isAI.value) {
    return 'AI 助手';
  }
  const n = route.query.nickname;
  return n ? decodeURIComponent(String(n)) : `用户 ${userId.value}`;
});

const messages = ref([]);
const inputValue = ref('');
const sending = ref(false);
const conversationID = ref('');
let unsubscribe = null;

function mapTimMessage(m, selfId) {
  const from = m.from || m.nick || '';
  const isSelf = String(from) === String(selfId);
  const payload = m.payload || {};
  return {
    type: isSelf ? 'self' : 'other',
    content: payload.text || '[消息]',
    avatar: isSelf
      ? resolveMediaUrl(getItem(KEYS.userAvatar) || auth.userInfo?.avatar || '', { baseUrl: getApiBaseUrl() })
      : normalizeImageUrl(route.query.avatar ? decodeURIComponent(String(route.query.avatar)) : '', {
          baseUrl: getApiBaseUrl()
        })
  };
}

async function loadHistory() {
  if (isAI.value) {
    messages.value = [{ type: 'other', content: '你好，我是 Spark Link AI 助手，有什么可以帮你？', avatar: '' }];
    return;
  }
  conversationID.value = IMService.buildC2CConversationID(userId.value);
  const { messageList } = await IMService.getMessageHistory(conversationID.value, '', 30);
  const selfId = auth.userId;
  messages.value = messageList.map((m) => mapTimMessage(m, selfId));
  await IMService.setMessageRead(conversationID.value);
  scrollBottom();
}

async function send() {
  const text = inputValue.value.trim();
  if (!text || sending.value) {
    return;
  }
  if (isAI.value) {
    showToast('请使用 AI 助手页面');
    return;
  }
  sending.value = true;
  messages.value.push({
    type: 'self',
    content: text,
    avatar: resolveMediaUrl(auth.userInfo?.avatar || '', { baseUrl: getApiBaseUrl() })
  });
  inputValue.value = '';
  scrollBottom();
  try {
    await IMService.sendSingleMessage(userId.value, text);
  } catch (e) {
    showToast(e.message || '发送失败');
  } finally {
    sending.value = false;
  }
}

function scrollBottom() {
  nextTick(() => {
    if (scrollEl.value) {
      scrollEl.value.scrollTop = scrollEl.value.scrollHeight;
    }
  });
}

async function fetchPeerAvatarIfMissing() {
  if (!userId.value || route.query.avatar) {
    return;
  }
  try {
    const res = await get(`/api/user/${userId.value}/profile`, {}, { suppressErrorToast: true });
    if (res?.data?.avatar) {
      const av = normalizeImageUrl(res.data.avatar, { baseUrl: getApiBaseUrl() });
      messages.value = messages.value.map((m) =>
        m.type === 'other' ? { ...m, avatar: av } : m
      );
    }
  } catch {
    /* ignore */
  }
}

onMounted(async () => {
  if (isAI.value) {
    await loadHistory();
    return;
  }
  if (!auth.isLoggedIn) {
    showToast('请先登录');
    return;
  }
  try {
    await IMService.initAndLogin();
    await loadHistory();
    fetchPeerAvatarIfMissing();
    unsubscribe = IMService.onNewMessage((list) => {
      const selfId = auth.userId;
      const peer = IMService.normalizePeerUserId(userId.value);
      list.forEach((m) => {
        if (String(m.from) === peer || String(m.to) === peer) {
          messages.value.push(mapTimMessage(m, selfId));
          scrollBottom();
        }
      });
    });
  } catch (e) {
    showToast(e.message || 'IM 连接失败');
  }
});

onUnmounted(() => {
  if (unsubscribe) {
    unsubscribe();
  }
});
</script>

<style scoped>
.chat {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.chat__messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}

.chat__row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
  gap: 8px;
}

.chat__row--self {
  flex-direction: row-reverse;
}

.chat__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
}

.chat__bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.5;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.chat__row--self .chat__bubble {
  background: var(--primary-bg);
  color: var(--primary-dark);
}

.chat__input-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px calc(8px + var(--safe-bottom));
  background: #fff;
  border-top: 1px solid var(--border-color);
}

.chat__input-bar .van-field {
  flex: 1;
  padding: 4px 8px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
}
</style>
