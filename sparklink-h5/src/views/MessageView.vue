<template>
  <div class="message page">
    <template v-if="needLogin">
      <div class="empty-hint">
        <p>登录后查看消息</p>
        <van-button type="primary" color="#5FB3A8" @click="$router.push({ name: 'login' })">去登录</van-button>
      </div>
    </template>
    <template v-else>
      <van-pull-refresh v-model="refreshing" @refresh="initLoad">
        <van-loading v-if="loading && !messages.length" class="loading-center" />
        <van-cell-group v-else inset>
          <van-cell
            v-for="item in messages"
            :key="item.conversationID"
            :title="item.title"
            :label="item.lastMessage"
            :value="item.time"
            is-link
            @click="openChat(item)"
          >
            <template #icon>
              <img v-if="item.avatar" :src="item.avatar" class="msg-avatar" alt="" />
            </template>
          </van-cell>
        </van-cell-group>
        <p v-if="!loading && !messages.length" class="empty-hint">暂无会话</p>
      </van-pull-refresh>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, inject, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { navigateToChat } from '@/utils/navigateToChat';
import { showToast } from 'vant';
import { useAuthStore } from '@/stores/auth';
import IMService from '@/utils/imService';
import { normalizeImageUrl } from '@/utils/mediaUrl';
import { getApiBaseUrl } from '@/config/env';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const tabReselectKey = inject('tabReselectKey', null);

const messages = ref([]);
const loading = ref(false);
const refreshing = ref(false);
const imReady = ref(false);

const needLogin = computed(() => !auth.isLoggedIn);

function formatMessageTime(lastTime) {
  if (!lastTime) {
    return '';
  }
  const timestamp = Number(lastTime) * 1000;
  if (!Number.isFinite(timestamp)) {
    return '';
  }
  const date = new Date(timestamp);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const m = String(date.getMinutes()).padStart(2, '0');
  return `${month}-${day} ${h}:${m}`;
}

function mapConversation(conv) {
  const profile = conv.userProfile || {};
  const last = conv.lastMessage || {};
  let lastText = '';
  if (last.type === 'TIMTextElem' && last.payload) {
    lastText = last.payload.text || '';
  }
  const peerId = IMService.normalizePeerUserId(profile.userID || conv.conversationID?.replace(/^C2C/, ''));
  const avatar = normalizeImageUrl(profile.avatar || profile.faceURL || '', { baseUrl: getApiBaseUrl() });
  return {
    conversationID: conv.conversationID,
    peerUserId: peerId,
    title: profile.nick || profile.nickname || `用户 ${peerId}`,
    avatar,
    lastMessage: lastText || '[消息]',
    time: formatMessageTime(last.time)
  };
}

async function loadMessages() {
  const list = await IMService.getConversationList();
  messages.value = list
    .filter((c) => c.type === 'C2C' || String(c.conversationID || '').startsWith('C2C'))
    .map(mapConversation);
}

async function initLoad() {
  if (needLogin.value) {
    return;
  }
  loading.value = true;
  try {
    await IMService.initAndLogin();
    await loadMessages();
    IMService.syncUnreadBadgeFromSdk();
    imReady.value = true;
  } catch (e) {
    messages.value = [];
    imReady.value = false;
    showToast('消息服务连接失败');
  } finally {
    loading.value = false;
    refreshing.value = false;
  }
}

function openChat(item) {
  navigateToChat(router, {
    userId: item.peerUserId,
    nickname: item.title,
    avatar: item.avatar
  });
}

onMounted(initLoad);

if (tabReselectKey) {
  watch(tabReselectKey, () => {
    if (route.name === 'message' && !needLogin.value) {
      refreshing.value = true;
      initLoad();
    }
  });
}
</script>

<style scoped>
.msg-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 12px;
  object-fit: cover;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 48px;
}
</style>
