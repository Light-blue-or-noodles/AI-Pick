<template>
  <div class="ai-chat page page--no-tab">
    <van-nav-bar title="AI 助手" left-arrow @click-left="$router.back()" />
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
import { ref, onMounted, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showToast } from 'vant';
import { post, get } from '@/utils/request';
import { KEYS, getItem, setItem } from '@/utils/storage';
import { normalizeImageUrl } from '@/utils/mediaUrl';
import { getApiBaseUrl } from '@/config/env';

const route = useRoute();
const router = useRouter();
const scrollEl = ref(null);
const messages = ref([]);
const inputValue = ref('');
const isLoading = ref(false);
const loadingId = ref(null);
const sessionId = ref(getItem(KEYS.sessionId) || '');

function scrollBottom() {
  nextTick(() => {
    if (scrollEl.value) {
      scrollEl.value.scrollTop = scrollEl.value.scrollHeight;
    }
  });
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
    messages.value = list;
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
      setItem(KEYS.sessionId, newSid);
    }
    const idx = messages.value.findIndex((m) => m.id === msgId);
    if (idx >= 0) {
      messages.value[idx] = {
        ...messages.value[idx],
        reply,
        recommends: mapRecommends(recommends)
      };
    }
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
</style>
