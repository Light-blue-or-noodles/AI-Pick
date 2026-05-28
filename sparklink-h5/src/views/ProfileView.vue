<template>
  <div class="profile page">
    <div class="profile__header theme-gradient">
      <div class="profile-hero-sheet card">
        <div class="user-info" @click="onAvatarClick">
          <img :src="avatarUrl" class="profile__avatar" alt="" />
          <div class="user-detail">
            <h2 class="profile__name">{{ displayName }}</h2>
            <p class="profile__bio">{{ bioDisplay }}</p>
            <div v-if="auth.isLoggedIn && (companyDisplay || schoolDisplay)" class="org-info">
              <span v-if="companyDisplay">🏢 {{ companyDisplay }}</span>
              <span v-if="schoolDisplay">🎓 {{ schoolDisplay }}</span>
            </div>
          </div>
        </div>
        <div v-if="auth.isLoggedIn" class="stats">
          <button type="button" class="stat-item" @click="$router.push({ name: 'my-partners' })">
            <span class="stat-value">{{ stats.partners }}</span>
            <span class="stat-label">我发布的搭子</span>
          </button>
          <button type="button" class="stat-item" @click="$router.push({ name: 'my-followers' })">
            <span class="stat-value">{{ stats.followers }}</span>
            <span class="stat-label">粉丝</span>
          </button>
          <button type="button" class="stat-item" @click="$router.push({ name: 'my-following' })">
            <span class="stat-value">{{ stats.following }}</span>
            <span class="stat-label">关注的人</span>
          </button>
        </div>
        <van-button v-if="!auth.isLoggedIn" round size="small" @click="$router.push({ name: 'login' })">
          登录
        </van-button>
      </div>
    </div>
    <div v-if="auth.isLoggedIn" class="menu-section">
      <van-cell
        v-for="item in menuItems"
        :key="item.id"
        :title="item.title"
        is-link
        @click="onMenu(item.id)"
      />
    </div>
    <p class="version-info">Spark Link v0.1.0-h5</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, inject, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { get } from '@/utils/request';
import { useAuthStore } from '@/stores/auth';
import { resolveMediaUrl } from '@/utils/mediaUrl';
import { getApiBaseUrl } from '@/config/env';
import IMService from '@/utils/imService';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const tabReselectKey = inject('tabReselectKey', null);

const stats = ref({ partners: 0, followers: 0, following: 0 });

const menuItems = [
  { id: 'join-company', title: '我的公司' },
  { id: 'join-school', title: '我的学校' },
  { id: 'my-partners', title: '我发布的搭子' },
  { id: 'my-following', title: '我的关注' },
  { id: 'my-followers', title: '我的粉丝' },
  { id: 'settings', title: '设置' }
];

const avatarUrl = computed(() => {
  const av = auth.userInfo?.avatar;
  return resolveMediaUrl(av, { baseUrl: getApiBaseUrl(), fallback: '/images/default-avatar.png' });
});

const displayName = computed(() => {
  if (!auth.isLoggedIn) {
    return '未登录';
  }
  return auth.userInfo?.nickname || auth.userInfo?.username || '用户';
});

const bioDisplay = computed(() => {
  if (!auth.isLoggedIn) {
    return '登录后享受更多功能';
  }
  const bio = auth.userInfo?.bio || auth.userInfo?.introduction || '';
  const s = String(bio).trim();
  if (!s) {
    return '写一句简介吧';
  }
  return s.length > 40 ? `${s.slice(0, 40)}...` : s;
});

const companyDisplay = computed(() => {
  const c = auth.userInfo?.companyName;
  return c ? String(c).trim() : '';
});

const schoolDisplay = computed(() => {
  const s = auth.userInfo?.schoolName;
  return s ? String(s).trim() : '';
});

function onAvatarClick() {
  if (auth.isLoggedIn) {
    router.push({ name: 'profile-edit' });
  } else {
    router.push({ name: 'login' });
  }
}

function onMenu(id) {
  const map = {
    'join-company': 'join-company',
    'join-school': 'join-school',
    'my-partners': 'my-partners',
    'my-following': 'my-following',
    'my-followers': 'my-followers',
    settings: 'settings'
  };
  const name = map[id];
  if (name) {
    router.push({ name });
  }
}

async function loadStats() {
  if (!auth.isLoggedIn) {
    return;
  }
  try {
    const [userStats, followStats] = await Promise.all([
      get('/api/user/stats', {}, { suppressErrorToast: true }),
      get('/api/user/follow/stats', {}, { suppressErrorToast: true })
    ]);
    const us = userStats.data || {};
    const fs = followStats.data || {};
    stats.value = {
      partners: us.partnerCount ?? us.partners ?? 0,
      followers: fs.followers ?? fs.followerCount ?? 0,
      following: fs.following ?? fs.followingCount ?? 0
    };
  } catch {
    /* ignore */
  }
}

async function refresh() {
  if (auth.isLoggedIn) {
    await auth.fetchUserInfo();
    await loadStats();
    try {
      await IMService.initAndLogin();
      IMService.syncUnreadBadgeFromSdk();
    } catch {
      /* ignore */
    }
  }
}

onMounted(refresh);

if (tabReselectKey) {
  watch(tabReselectKey, () => {
    if (route.name === 'profile') {
      refresh();
    }
  });
}
</script>

<style scoped>
.profile__header {
  padding: 24px var(--page-horizontal) 0;
}

.profile-hero-sheet {
  padding: 20px 16px;
  margin-bottom: 12px;
}

.user-info {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  cursor: pointer;
}

.profile__avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.profile__name {
  margin: 0 0 6px;
  font-size: 18px;
}

.profile__bio {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.org-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.stats {
  display: flex;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: none;
  background: none;
  cursor: pointer;
  padding: 0;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.stat-label {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
}

.menu-section {
  margin: 0 var(--page-horizontal) 16px;
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.version-info {
  text-align: center;
  font-size: 12px;
  color: var(--text-tertiary);
  padding-bottom: 24px;
}
</style>
