<template>
  <div class="my-list page page--no-tab">
    <PageNavBar title="我的粉丝" />
    <van-pull-refresh v-model="refreshing" @refresh="load">
      <van-loading v-if="loading && !users.length" class="loading-center" />
      <van-cell-group v-else inset>
        <van-cell
          v-for="u in users"
          :key="u.id"
          :title="u.nickname || u.username"
          is-link
          @click="$router.push({ name: 'user-profile', params: { id: u.id } })"
        >
          <template #icon>
            <img :src="u.avatarUrl" class="user-row__avatar" alt="" />
          </template>
        </van-cell>
      </van-cell-group>
      <p v-if="!loading && !users.length" class="empty-hint">暂无粉丝</p>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { get } from '@/utils/request';
import { resolveMediaUrl } from '@/utils/mediaUrl';
import { getApiBaseUrl } from '@/config/env';
import PageNavBar from '@/components/PageNavBar.vue';

const users = ref([]);
const loading = ref(false);
const refreshing = ref(false);

function mapUser(u) {
  const base = getApiBaseUrl();
  return {
    id: u.id || u.userId,
    nickname: u.nickname,
    username: u.username,
    avatarUrl: resolveMediaUrl(u.avatar, { baseUrl: base, fallback: '/images/default-avatar.png' })
  };
}

async function load() {
  loading.value = true;
  try {
    const res = await get('/api/user/followers');
    const raw = Array.isArray(res.data) ? res.data : res.data?.records || [];
    users.value = raw.map(mapUser);
  } catch {
    users.value = [];
  } finally {
    loading.value = false;
    refreshing.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.user-row__avatar {
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
