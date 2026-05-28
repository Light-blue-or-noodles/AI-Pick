<template>
  <div class="message page">
    <template v-if="needLogin">
      <div class="auth-gate">
        <span class="auth-gate-icon" aria-hidden="true">💬</span>
        <h2 class="auth-gate-title">登录后查看消息</h2>
        <p class="auth-gate-desc">与搭子聊天、接收即时消息</p>
        <van-button
          round
          block
          class="auth-gate-btn"
          color="#5FB3A8"
          @click="$router.push({ name: 'login' })"
        >
          去登录
        </van-button>
      </div>
    </template>

    <template v-else>
      <van-pull-refresh v-model="refreshing" class="message-refresh" @refresh="initLoad">
        <div v-if="loading && !messages.length" class="loading">
          <van-loading type="spinner" color="#5FB3A8" />
          <span>连接消息服务…</span>
        </div>

        <div v-else class="message-list">
          <button
            v-for="item in messages"
            :key="item.conversationID"
            type="button"
            class="message-item"
            :class="{ unread: item.unreadCount > 0 }"
            @click="openChat(item)"
          >
            <div class="message-avatar-wrap">
              <NetworkImage
                :url="item.avatar"
                default-src="/images/default-avatar.png"
                custom-class="message-avatar-img"
                alt=""
              />
            </div>
            <div class="message-content">
              <div class="session-top">
                <span class="session-name">{{ item.title }}</span>
                <span v-if="item.time" class="session-time">{{ item.time }}</span>
              </div>
              <div class="session-bottom">
                <span class="session-preview">{{ item.lastMessage }}</span>
                <span v-if="item.unreadCount > 0" class="unread-badge">
                  {{ item.unreadCount > 99 ? '99+' : item.unreadCount }}
                </span>
              </div>
            </div>
            <svg class="message-arrow" viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M9 6l6 6-6 6"
              />
            </svg>
          </button>

          <div v-if="!messages.length" class="empty-state">
            <span class="empty-state-icon" aria-hidden="true">📭</span>
            <p class="empty-state-title">暂无会话</p>
            <p class="empty-state-hint">在搭子详情或用户主页发起聊天</p>
          </div>
        </div>
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
import NetworkImage from '@/components/NetworkImage.vue';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const tabReselectKey = inject('tabReselectKey', null);

const messages = ref([]);
const loading = ref(true);
const refreshing = ref(false);

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
  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();
  const h = String(date.getHours()).padStart(2, '0');
  const m = String(date.getMinutes()).padStart(2, '0');
  if (isToday) {
    return `${h}:${m}`;
  }
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${month}-${day} ${h}:${m}`;
}

function mapConversation(conv) {
  const profile = conv.userProfile || {};
  const last = conv.lastMessage || {};
  let lastText = '';
  if (last.type === 'TIMTextElem' && last.payload) {
    lastText = last.payload.text || '';
  } else if (last.messageForShow) {
    lastText = String(last.messageForShow);
  }
  const peerId = IMService.normalizePeerUserId(profile.userID || conv.conversationID?.replace(/^C2C/, ''));
  const avatar = normalizeImageUrl(profile.avatar || profile.faceURL || '', { baseUrl: getApiBaseUrl() });
  return {
    conversationID: conv.conversationID,
    peerUserId: peerId,
    title: profile.nick || profile.nickname || `用户 ${peerId}`,
    avatar,
    lastMessage: (lastText || '').trim() || '[暂无预览]',
    time: formatMessageTime(last.time),
    unreadCount: Math.max(0, Number(conv.unreadCount) || 0)
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
    loading.value = false;
    return;
  }
  if (!messages.value.length) {
    loading.value = true;
  }
  try {
    await IMService.initAndLogin();
    await loadMessages();
    IMService.syncUnreadBadgeFromSdk();
  } catch {
    messages.value = [];
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
.message {
  min-height: 100vh;
  background: var(--bg-color);
}

.message-refresh {
  min-height: calc(100vh - var(--tab-bar-height) - var(--tab-bar-float-gap) - var(--safe-bottom));
}

.message-list {
  padding: 14px var(--page-horizontal) 8px;
  box-sizing: border-box;
}

.message-item {
  display: flex;
  align-items: center;
  width: 100%;
  text-align: left;
  padding: 14px 14px 13px;
  margin-bottom: 10px;
  background: var(--bg-white);
  border-radius: var(--radius-lg);
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.12s ease, opacity 0.12s ease;
  -webkit-tap-highlight-color: transparent;
}

.message-item:active {
  opacity: 0.96;
  transform: scale(0.995);
}

.message-item.unread {
  background: linear-gradient(90deg, rgba(232, 246, 245, 0.95) 0%, #fff 28%);
  box-shadow: 0 2px 12px rgba(95, 179, 168, 0.12);
  border-color: rgba(95, 179, 168, 0.22);
}

.message-avatar-wrap {
  position: relative;
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  margin-right: 12px;
  border-radius: 50%;
  overflow: hidden;
  background: #f5f5f5;
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.9),
    0 2px 7px rgba(0, 0, 0, 0.06);
}

.message-avatar-wrap :deep(.message-avatar-img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.message-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-top: 2px;
}

.session-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 6px;
  gap: 8px;
}

.session-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.3;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-item.unread .session-name {
  font-weight: 700;
  color: #1a1a1a;
}

.session-time {
  flex-shrink: 0;
  font-size: 11px;
  color: var(--text-tertiary);
  line-height: 1.2;
}

.session-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 20px;
  gap: 8px;
}

.session-preview {
  font-size: 13px;
  line-height: 1.45;
  color: var(--text-secondary);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-item.unread .session-preview {
  color: var(--text-primary);
  font-weight: 500;
}

.unread-badge {
  flex-shrink: 0;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ff4d4f 100%);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 4px rgba(255, 77, 79, 0.35);
}

.message-arrow {
  flex-shrink: 0;
  width: 14px;
  height: 14px;
  margin-left: 6px;
  color: #c8c8c8;
  opacity: 0.85;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 80px 16px;
  color: var(--text-tertiary);
  font-size: 14px;
}

.auth-gate {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
  padding: 48px 20px;
  box-sizing: border-box;
}

.auth-gate-icon {
  font-size: 56px;
  margin-bottom: 16px;
  opacity: 0.35;
  line-height: 1;
}

.auth-gate-title {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
}

.auth-gate-desc {
  margin: 0 0 24px;
  font-size: 14px;
  color: var(--text-tertiary);
  text-align: center;
}

.auth-gate-btn {
  max-width: 200px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 24px 48px;
  text-align: center;
}

.empty-state-icon {
  font-size: 48px;
  opacity: 0.45;
  margin-bottom: 12px;
}

.empty-state-title {
  margin: 0 0 8px;
  font-size: 15px;
  color: var(--text-secondary);
}

.empty-state-hint {
  margin: 0;
  font-size: 13px;
  color: var(--text-tertiary);
  line-height: 1.5;
}
</style>
