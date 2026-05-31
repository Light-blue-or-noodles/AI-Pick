<template>
  <div class="ai-chat page page--no-tab">
    <van-nav-bar
      title="AI 助手"
      left-arrow
      @click-left="$router.back()"
      @click-right="startNewSession"
    >
      <template #right>
        <span class="ai-chat__nav-action">开启新会话</span>
      </template>
    </van-nav-bar>
    <div ref="scrollEl" class="ai-chat__list">
      <div v-for="msg in messages" :key="msg.id" class="ai-chat__pair">
        <div class="ai-chat__user">{{ msg.content }}</div>
        <div v-if="msg.reply" class="ai-chat__reply">{{ msg.reply }}</div>
        <div v-else-if="loadingId === msg.id" class="ai-chat__loading">
          <van-loading size="20px" />
        </div>
        <div v-if="msg.recommends?.length" class="ai-chat__recs">
          <div
            v-for="r in msg.recommends"
            :key="`${r.type || 'partner'}-${r.id}`"
            class="ai-chat__rec card"
            @click="openRecommend(r)"
          >
            {{ r.title || r.name }}
          </div>
        </div>
      </div>
    </div>
    <div class="ai-chat__bar">
      <van-field v-model="inputValue" placeholder="问 AI 找搭子..." @keyup.enter="send" />
      <van-button type="primary" size="small" color="#5FB3A8" :loading="isLoading" @click="send">发送</van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showConfirmDialog, showToast } from 'vant';
import { post, get } from '@/utils/request';
import { KEYS, getItem, removeItem, setItem } from '@/utils/storage';
import { normalizeImageUrl } from '@/utils/mediaUrl';
import { getApiBaseUrl } from '@/config/env';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const scrollEl = ref(null);
const messages = ref([]);
const inputValue = ref('');
const isLoading = ref(false);
const loadingId = ref(null);
const sessionKey = computed(() => {
  const uid = auth.userId ? String(auth.userId) : '';
  return uid ? `${KEYS.sessionId}:${uid}` : KEYS.sessionId;
});
const sessionId = ref('');
const CACHE_PREFIX = 'aiChat:conversation:';

function scrollBottom() {
  nextTick(() => {
    if (scrollEl.value) {
      scrollEl.value.scrollTop = scrollEl.value.scrollHeight;
    }
  });
}

function getConversationCacheKey(sid) {
  const uid = auth.userId ? String(auth.userId) : 'guest';
  if (!sid) {
    return '';
  }
  return `${CACHE_PREFIX}${uid}:${sid}`;
}

function loadConversationCache(sid) {
  const key = getConversationCacheKey(sid);
  if (!key) {
    return [];
  }
  const raw = getItem(key);
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function saveConversationCache(sid, list) {
  const key = getConversationCacheKey(sid);
  if (!key) {
    return;
  }
  setItem(key, JSON.stringify(Array.isArray(list) ? list : []));
}

function mergeRecommendsFromCache(list, cachedList) {
  if (!Array.isArray(list) || !Array.isArray(cachedList) || !cachedList.length) {
    return list;
  }
  const cacheMap = new Map();
  cachedList.forEach((item) => {
    const key = `${item?.content || ''}__${item?.reply || ''}`;
    if (!cacheMap.has(key) && Array.isArray(item?.recommends)) {
      cacheMap.set(key, item.recommends);
    }
  });
  return list.map((item) => {
    const key = `${item?.content || ''}__${item?.reply || ''}`;
    const recommends = cacheMap.get(key);
    return recommends ? { ...item, recommends } : item;
  });
}

function persistCurrentConversation() {
  const sid = sessionId.value.trim();
  if (!sid) {
    return;
  }
  saveConversationCache(sid, messages.value);
}

async function loadHistory() {
  const sid = sessionId.value.trim();
  if (!sid) {
    return;
  }
  try {
    const res = await get(`/api/chat/ai/history/${sid}`, {}, { suppressErrorToast: true });
    const raw = Array.isArray(res.data) ? res.data : [];
    const list = [];
    for (let i = 0; i < raw.length; i++) {
      if (raw[i].type === 1 && i + 1 < raw.length && raw[i + 1].type === 2) {
        list.push({
          id: raw[i].id || i,
          content: raw[i].content || '',
          reply: raw[i + 1].content || '',
          recommends: []
        });
        i++;
      }
    }
    const cached = loadConversationCache(sid);
    messages.value = mergeRecommendsFromCache(list, cached);
    persistCurrentConversation();
  } catch {
    messages.value = [];
  }
}

function mapRecommends(recommends) {
  const base = getApiBaseUrl();
  return (recommends || []).map((r) => ({
    ...r,
    avatar: r.avatar ? normalizeImageUrl(r.avatar, base) : ''
  }));
}

function openRecommend(rec) {
  if (!rec?.id) {
    return;
  }
  if (rec.type === 'activity') {
    showToast('活动详情暂未开放');
    return;
  }
  router.push({ name: 'partner-detail', params: { id: rec.id } });
}

async function startNewSession() {
  if (isLoading.value) {
    return;
  }
  const hasConversation = messages.value.length > 0 || !!sessionId.value.trim();
  if (!hasConversation) {
    const oldSid = sessionId.value.trim();
    sessionId.value = '';
    messages.value = [];
    removeItem(sessionKey.value);
    const cacheKey = getConversationCacheKey(oldSid);
    if (cacheKey) {
      removeItem(cacheKey);
    }
    showToast('已开启新会话');
    return;
  }
  try {
    await showConfirmDialog({
      title: '开启新会话',
      message: '将清空当前对话窗口，并从新会话开始。'
    });
    const oldSid = sessionId.value.trim();
    sessionId.value = '';
    messages.value = [];
    removeItem(sessionKey.value);
    const cacheKey = getConversationCacheKey(oldSid);
    if (cacheKey) {
      removeItem(cacheKey);
    }
    showToast('已新建会话');
  } catch {
    // 用户取消
  }
}

async function send(textFromQuick) {
  const content = typeof textFromQuick === 'string'
    ? textFromQuick.trim()
    : inputValue.value.trim();
  if (!content || isLoading.value) {
    return;
  }
  const msgId = Date.now();
  const userMessage = { id: msgId, content, reply: '', recommends: [] };
  messages.value.push(userMessage);
  inputValue.value = '';
  isLoading.value = true;
  loadingId.value = msgId;
  scrollBottom();
  try {
    const res = await post('/api/chat', {
      message: content,
      sessionId: sessionId.value
    });
    const payload = res.data || {};
    const { reply = '', recommends = [], sessionId: newSid } = payload;
    if (newSid) {
      sessionId.value = newSid;
      setItem(sessionKey.value, newSid);
    }
    const idx = messages.value.findIndex((m) => m.id === msgId);
    if (idx >= 0) {
      messages.value[idx] = {
        ...messages.value[idx],
        reply,
        recommends: mapRecommends(recommends)
      };
    }
    persistCurrentConversation();
  } catch {
    const idx = messages.value.findIndex((m) => m.id === msgId);
    if (idx >= 0) {
      messages.value[idx].reply = '回复失败，请稍后重试';
    }
  } finally {
    isLoading.value = false;
    loadingId.value = null;
    scrollBottom();
  }
}

onMounted(async () => {
  sessionId.value = getItem(sessionKey.value) || '';
  await loadHistory();
  const quick = route.query.quick;
  if (quick && !messages.value.length) {
    send(decodeURIComponent(String(quick)));
  }
});
</script>

<style scoped>
.ai-chat {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.ai-chat__list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}

.ai-chat__pair {
  display: flex;
  flex-direction: column;
  margin-bottom: 12px;
}

.ai-chat__user {
  align-self: flex-end;
  max-width: 85%;
  margin: 0 0 8px 0;
  margin-left: auto;
  padding: 10px 14px;
  background: var(--primary-bg);
  border-radius: 12px 12px 4px 12px;
  font-size: 15px;
}

.ai-chat__loading {
  align-self: flex-start;
  padding: 8px;
}

.ai-chat__reply {
  align-self: flex-start;
  max-width: 90%;
  margin-bottom: 8px;
  padding: 10px 14px;
  background: #fff;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.5;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.ai-chat__recs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.ai-chat__rec {
  padding: 8px 12px;
  font-size: 13px;
  cursor: pointer;
}

.ai-chat__bar {
  display: flex;
  gap: 8px;
  padding: 8px 12px calc(8px + var(--safe-bottom));
  background: #fff;
  border-top: 1px solid var(--border-color);
}

.ai-chat__bar .van-field {
  flex: 1;
}

.ai-chat__nav-action {
  color: #5FB3A8;
  font-size: 14px;
  font-weight: 500;
}
</style>
