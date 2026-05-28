<template>
  <div class="user-profile page page--no-tab">
    <PageNavBar title="用户主页" />
    <UserProfileSkeleton v-if="pending && !user" />
    <template v-else-if="user">
      <div class="user-profile__head theme-gradient">
        <img :src="avatarUrl" class="user-profile__avatar" alt="" />
        <h2>{{ user.nickname || user.username || '用户' }}</h2>
        <p v-if="bio" class="user-profile__bio">{{ bio }}</p>
      </div>
      <div class="user-profile__actions">
        <van-button
          v-if="auth.isLoggedIn && !isSelf"
          round
          block
          :type="following ? 'default' : 'primary'"
          :color="following ? undefined : '#5FB3A8'"
          :loading="followLoading"
          @click="toggleFollow"
        >
          {{ following ? '已关注' : '关注' }}
        </van-button>
        <van-button
          v-if="auth.isLoggedIn && !isSelf"
          round
          block
          plain
          class="user-profile__chat"
          @click="goChat"
        >
          发消息
        </van-button>
      </div>
    </template>
    <p v-else-if="!pending" class="empty-hint">用户不存在</p>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showToast } from 'vant';
import { get, post } from '@/utils/request';
import { resolveMediaUrl } from '@/utils/mediaUrl';
import { getApiBaseUrl } from '@/config/env';
import { useAuthStore } from '@/stores/auth';
import { usePageLoad } from '@/composables/usePageLoad';
import { navigateToChat } from '@/utils/navigateToChat';
import PageNavBar from '@/components/PageNavBar.vue';
import UserProfileSkeleton from '@/components/skeleton/UserProfileSkeleton.vue';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const following = ref(false);
const followLoading = ref(false);

const profileId = computed(() => String(route.params.id || ''));

const { data: user, pending, load } = usePageLoad(
  async () => {
    const res = await get(`/api/user/${profileId.value}/profile`);
    return res.data || null;
  },
  {
    cacheKey: () => (profileId.value ? `user-profile:${profileId.value}` : null),
    empty: null
  }
);

watch(profileId, () => {
  if (profileId.value) {
    load();
  }
});

const isSelf = computed(() => String(user.value?.id) === String(auth.userId));

const avatarUrl = computed(() => {
  const av = user.value?.avatar;
  return resolveMediaUrl(av, { baseUrl: getApiBaseUrl(), fallback: '/images/default-avatar.png' });
});

const bio = computed(() => {
  const b = user.value?.bio || user.value?.introduction || '';
  return String(b).trim();
});

async function loadFollowState() {
  if (!auth.isLoggedIn || !user.value || isSelf.value) {
    following.value = false;
    return;
  }
  try {
    const chk = await get(`/api/user/follow/check/${profileId.value}`, {}, { suppressErrorToast: true });
    following.value = !!chk.data?.following || chk.data === true;
  } catch {
    following.value = false;
  }
}

watch(user, () => {
  loadFollowState();
});

async function toggleFollow() {
  followLoading.value = true;
  try {
    if (following.value) {
      await post(`/api/user/${profileId.value}/follow`, { action: 'cancel' });
      following.value = false;
      showToast('已取消关注');
    } else {
      await post(`/api/user/${profileId.value}/follow`, { action: 'follow' });
      following.value = true;
      showToast('关注成功');
    }
  } catch (e) {
    showToast(e?.message || '操作失败');
  } finally {
    followLoading.value = false;
  }
}

function goChat() {
  navigateToChat(router, {
    userId: user.value?.id,
    nickname: user.value?.nickname,
    avatar: user.value?.avatar
  });
}
</script>

<style scoped>
.user-profile__head {
  padding: 32px 24px;
  text-align: center;
  color: #fff;
}

.user-profile__avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 12px;
}

.user-profile__bio {
  margin: 8px 0 0;
  font-size: 14px;
  opacity: 0.9;
}

.user-profile__actions {
  padding: 20px 16px;
}

.user-profile__chat {
  margin-top: 12px;
}
</style>
