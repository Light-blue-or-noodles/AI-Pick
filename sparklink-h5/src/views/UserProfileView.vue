<template>
  <div class="user-profile page page--no-tab">
    <PageNavBar title="用户主页" />
    <van-loading v-if="loading" class="loading-center" />
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
    <p v-else class="empty-hint">用户不存在</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showToast } from 'vant';
import { get, post } from '@/utils/request';
import { resolveMediaUrl } from '@/utils/mediaUrl';
import { getApiBaseUrl } from '@/config/env';
import { useAuthStore } from '@/stores/auth';
import { navigateToChat } from '@/utils/navigateToChat';
import PageNavBar from '@/components/PageNavBar.vue';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const user = ref(null);
const loading = ref(true);
const following = ref(false);
const followLoading = ref(false);

const isSelf = computed(() => String(user.value?.id) === String(auth.userId));

const avatarUrl = computed(() => {
  const av = user.value?.avatar;
  return resolveMediaUrl(av, { baseUrl: getApiBaseUrl(), fallback: '/images/default-avatar.png' });
});

const bio = computed(() => {
  const b = user.value?.bio || user.value?.introduction || '';
  return String(b).trim();
});

async function load() {
  loading.value = true;
  try {
    const id = route.params.id;
    const res = await get(`/api/user/${id}/profile`);
    user.value = res.data || null;
    if (auth.isLoggedIn && user.value) {
      const chk = await get(`/api/user/follow/check/${id}`, {}, { suppressErrorToast: true });
      following.value = !!chk.data?.following || chk.data === true;
    }
  } catch {
    user.value = null;
  } finally {
    loading.value = false;
  }
}

async function toggleFollow() {
  const id = route.params.id;
  followLoading.value = true;
  try {
    if (following.value) {
      await post(`/api/user/${id}/follow`, { action: 'cancel' });
      following.value = false;
      showToast('已取消关注');
    } else {
      await post(`/api/user/${id}/follow`, { action: 'follow' });
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

onMounted(load);
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
  border: 3px solid rgba(255, 255, 255, 0.5);
}

.user-profile__bio {
  margin: 8px 0 0;
  font-size: 14px;
  opacity: 0.9;
}

.user-profile__actions {
  padding: 20px var(--page-horizontal);
}

.user-profile__chat {
  margin-top: 10px;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 48px;
}
</style>
