<template>
  <div class="relation-page page page--no-tab">
    <PageNavBar title="我的粉丝" />
    <FollowUserList
      v-model:refreshing="refreshing"
      :users="users"
      :loading="pending"
      show-chat
      show-bio
      empty-text="暂无粉丝"
      @refresh="onPullRefresh"
      @select="goProfile"
      @chat="startChat"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { showToast } from 'vant';
import { get, post } from '@/utils/request';
import { navigateToChat } from '@/utils/navigateToChat';
import { resolveMediaUrl } from '@/utils/mediaUrl';
import { getApiBaseUrl } from '@/config/env';
import { usePageLoad } from '@/composables/usePageLoad';
import { useAuthStore } from '@/stores/auth';
import PageNavBar from '@/components/PageNavBar.vue';
import FollowUserList from '@/components/FollowUserList.vue';

const router = useRouter();
const auth = useAuthStore();
const refreshing = ref(false);

function mapUser(u) {
  const base = getApiBaseUrl();
  const bio = String(u.bio || u.introduction || '').trim();
  return {
    id: u.id || u.userId,
    nickname: u.nickname,
    username: u.username,
    bio: bio && bio !== 'null' ? bio : '',
    avatar: u.avatar,
    avatarUrl: resolveMediaUrl(u.avatar, { baseUrl: base, fallback: '/images/default-avatar.png' })
  };
}

const { data: users, pending, refresh } = usePageLoad(
  async () => {
    const res = await get('/api/user/followers');
    const raw = Array.isArray(res.data) ? res.data : res.data?.records || [];
    return raw.map(mapUser);
  },
  {
    cacheKey: () => (auth.userId ? `follow-list:followers:${auth.userId}` : null),
    empty: []
  }
);

async function onPullRefresh() {
  refreshing.value = true;
  try {
    await refresh();
  } finally {
    refreshing.value = false;
  }
}

function goProfile(u) {
  router.push({ name: 'user-profile', params: { id: String(u.id) } });
}

async function startChat(u) {
  if (!u?.id) {
    return;
  }
  if (!auth.isLoggedIn) {
    showToast('请先登录');
    router.push({ name: 'login' });
    return;
  }
  try {
    await post('/api/im/prep-peer', { peerUserId: u.id }, { suppressErrorToast: true });
  } catch {
    /* 会话可能已存在 */
  }
  navigateToChat(router, {
    userId: u.id,
    nickname: u.nickname || u.username || '',
    avatar: u.avatar || ''
  });
}
</script>

<style scoped>
.relation-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--bg-color);
}

.relation-page :deep(.van-nav-bar) {
  flex-shrink: 0;
  background: var(--bg-white);
}

.relation-page :deep(.follow-list) {
  flex: 1;
}
</style>
