<template>
  <div class="profile page">
    <header class="profile-header">
      <div class="profile-hero-sheet">
        <div class="user-info" @click="onAvatarClick">
          <div class="avatar-wrap">
            <NetworkImage
              :url="auth.userInfo?.avatar"
              default-src="/images/default-avatar.png"
              custom-class="avatar"
              alt=""
            />
            <span v-if="!auth.isLoggedIn" class="login-badge">登录</span>
          </div>
          <div class="user-detail">
            <h2 class="nickname">{{ displayName }}</h2>
            <p class="bio bio--single">{{ bioDisplay }}</p>
            <div
              v-if="auth.isLoggedIn && (companyDisplay || schoolDisplay)"
              class="org-info"
              :class="companyDisplay && schoolDisplay ? 'org-info--pair' : 'org-info--single'"
            >
              <div v-if="companyDisplay" class="org-item">
                <span class="org-ico">🏢</span>
                <span class="org-txt-wrap">
                  <span class="org-txt">{{ companyDisplay }}</span>
                </span>
              </div>
              <div v-if="schoolDisplay" class="org-item">
                <span class="org-ico">🎓</span>
                <span class="org-txt-wrap">
                  <span class="org-txt">{{ schoolDisplay }}</span>
                </span>
              </div>
            </div>
          </div>
          <button
            v-if="auth.isLoggedIn"
            type="button"
            class="edit-btn"
            aria-label="编辑资料"
            @click.stop="editProfile"
          >
            <svg class="edit-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill="currentColor"
                d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zm2.92 2.83H5v-.92l9.06-9.06.92.92L5.92 20.08zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"
              />
            </svg>
          </button>
        </div>

        <StatsRowSkeleton v-if="auth.isLoggedIn && statsPending" />
        <div v-else-if="auth.isLoggedIn && stats" class="stats">
          <button type="button" class="stat-item" @click="$router.push({ name: 'my-partners' })">
            <span class="stat-value">{{ stats.partners }}</span>
            <span class="stat-label">我发布的搭子</span>
          </button>
          <span class="stat-divider" aria-hidden="true" />
          <button type="button" class="stat-item" @click="$router.push({ name: 'my-followers' })">
            <span class="stat-value">{{ stats.followers }}</span>
            <span class="stat-label">粉丝</span>
          </button>
          <span class="stat-divider" aria-hidden="true" />
          <button type="button" class="stat-item" @click="$router.push({ name: 'my-following' })">
            <span class="stat-value">{{ stats.following }}</span>
            <span class="stat-label">关注的人</span>
          </button>
        </div>
      </div>
    </header>

    <div class="profile-menus">
      <section
        v-for="group in menuGroups"
        :key="group.key"
        class="menu-section"
      >
        <h2 v-if="group.label" class="menu-section__label">{{ group.label }}</h2>
        <div class="menu-section__card">
          <button
            v-for="item in group.items"
            :key="item.id"
            type="button"
            class="menu-item"
            @click="onMenu(item.id)"
          >
            <span class="menu-left">
              <span class="menu-icon-wrap" :class="`menu-icon-wrap--${item.icon}`">
                <svg class="menu-icon" viewBox="0 0 24 24" aria-hidden="true" v-html="menuIconPath(item.icon)" />
              </span>
              <span class="menu-title">{{ item.title }}</span>
            </span>
            <svg class="menu-arrow" viewBox="0 0 24 24" aria-hidden="true">
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
        </div>
      </section>
    </div>

    <p class="version-info">Spark Link v0.1.0-h5</p>
  </div>
</template>

<script setup>
import { computed, onMounted, inject, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showToast } from 'vant';
import { get } from '@/utils/request';
import { useAuthStore } from '@/stores/auth';
import { usePageLoad } from '@/composables/usePageLoad';
import NetworkImage from '@/components/NetworkImage.vue';
import StatsRowSkeleton from '@/components/skeleton/StatsRowSkeleton.vue';
import IMService from '@/utils/imService';

const PROFILE_BIO_DISPLAY_MAX = 20;
const PROFILE_ORG_DISPLAY_MAX = 10;

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const tabReselectKey = inject('tabReselectKey', null);

const { data: stats, pending: statsPending, refresh: refreshStats } = usePageLoad(
  async () => {
    const [userStats, followStats] = await Promise.all([
      get('/api/user/stats', {}, { suppressErrorToast: true }),
      get('/api/user/follow/stats', {}, { suppressErrorToast: true })
    ]);
    const us = userStats.data || {};
    const fs = followStats.data || {};
    return {
      partners: us.partnerCount ?? us.partners ?? 0,
      followers: fs.followers ?? fs.followerCount ?? 0,
      following: fs.following ?? fs.followingCount ?? 0
    };
  },
  {
    cacheKey: () => (auth.userId ? `profile-stats:${auth.userId}` : null),
    empty: null,
    immediate: false
  }
);

const menuGroups = [
  {
    key: 'org',
    label: '组织',
    items: [
      { id: 'join-company', title: '我的公司', icon: 'company' },
      { id: 'join-school', title: '我的学校', icon: 'school' }
    ]
  },
  {
    key: 'social',
    label: '社交',
    items: [
      { id: 'my-partners', title: '我发布的搭子', icon: 'partners' },
      { id: 'my-following', title: '我的关注', icon: 'following' },
      { id: 'my-followers', title: '我的粉丝', icon: 'followers' }
    ]
  },
  {
    key: 'system',
    label: '',
    items: [{ id: 'settings', title: '设置', icon: 'settings' }]
  }
];

const MENU_ICON_PATHS = {
  company:
    '<path fill="currentColor" d="M4 20V9l8-5 8 5v11H4zm2-2h4v-4H6v4zm6 0h4v-7h-4v7z"/>',
  school:
    '<path fill="currentColor" d="M12 3 2 9l10 6 10-6-10-6zm0 8.2L5 8v2.3l7 4.2 7-4.2V8l-7 3.2zM4 18h16v2H4v-2z"/>',
  partners:
    '<path fill="currentColor" d="M19 3H5a2 2 0 0 0-2 2v14l4-2 4 2 4-2 4 2V5a2 2 0 0 0-2-2zm-7 9H7v-2h5v2zm0-4H7V6h5v2z"/>',
  following:
    '<path fill="currentColor" d="M16 11c1.66 0 3-1.34 3-3S17.66 5 16 5s-3 1.34-3 3 1.34 3 3 3zm-8 0c1.66 0 3-1.34 3-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5C15 14.17 10.33 13 8 13zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>',
  followers:
    '<path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4zm8.5-7.5a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5zM22 19v-1.5c0-1.38-2.46-2.5-4.5-2.5-.83 0-1.6.1-2.3.28A6.94 6.94 0 0 0 18 19h4z"/>',
  settings:
    '<path fill="currentColor" d="M19.14 12.94a7.49 7.49 0 0 0 .05-.94 7.49 7.49 0 0 0-.05-.94l2.03-1.58a.5.5 0 0 0 .12-.64l-1.92-3.32a.5.5 0 0 0-.6-.22l-2.39.96a7.28 7.28 0 0 0-1.63-.94L15.4 2.5a.5.5 0 0 0-.48-.5h-3.84a.5.5 0 0 0-.48.5l-.36 2.54a7.28 7.28 0 0 0-1.63.94l-2.39-.96a.5.5 0 0 0-.6.22L2.71 8.05a.5.5 0 0 0 .12.64l2.03 1.58c-.03.31-.05.63-.05.94s.02.63.05.94L2.83 13.68a.5.5 0 0 0-.12.64l1.92 3.32a.5.5 0 0 0 .6.22l2.39-.96c.5.39 1.05.71 1.63.94l.36 2.54a.5.5 0 0 0 .48.5h3.84a.5.5 0 0 0 .48-.5l.36-2.54c.58-.23 1.13-.55 1.63-.94l2.39.96a.5.5 0 0 0 .6-.22l1.92-3.32a.5.5 0 0 0-.12-.64l-2.03-1.58zM12 15.5A3.5 3.5 0 1 1 12 8.5a3.5 3.5 0 0 1 0 7z"/>'
};

function menuIconPath(icon) {
  return MENU_ICON_PATHS[icon] || MENU_ICON_PATHS.settings;
}

function truncateBioForDisplay(str, maxUtf16Units) {
  if (str == null) {
    return '';
  }
  let s = String(str).trim();
  if (s === '' || s === 'null') {
    return '';
  }
  if (s.length <= maxUtf16Units) {
    return s;
  }
  let end = maxUtf16Units;
  const hi = s.charCodeAt(end - 1);
  if (hi >= 0xd800 && hi <= 0xdbff) {
    end -= 1;
  }
  return `${s.slice(0, end)}...`;
}

function truncateForProfileDisplay(str, maxUnits) {
  if (str == null) {
    return '';
  }
  const s = String(str).trim();
  if (s === '' || s === 'null') {
    return '';
  }
  const units = Array.from(s);
  if (units.length <= maxUnits) {
    return s;
  }
  return `${units.slice(0, maxUnits).join('')}...`;
}

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
  const trimmed = truncateBioForDisplay(bio, PROFILE_BIO_DISPLAY_MAX);
  return trimmed || '写一句简介吧';
});

const companyDisplay = computed(() => {
  const c = auth.userInfo?.companyName;
  return c ? truncateForProfileDisplay(c, PROFILE_ORG_DISPLAY_MAX) : '';
});

const schoolDisplay = computed(() => {
  const s = auth.userInfo?.schoolName;
  return s ? truncateForProfileDisplay(s, PROFILE_ORG_DISPLAY_MAX) : '';
});

function onAvatarClick() {
  if (auth.isLoggedIn) {
    router.push({ name: 'profile-edit' });
  } else {
    router.push({ name: 'login' });
  }
}

function editProfile() {
  router.push({ name: 'profile-edit' });
}

function onMenu(id) {
  if (!auth.isLoggedIn) {
    showToast('请先登录');
    router.push({ name: 'login' });
    return;
  }
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

async function refresh() {
  if (auth.isLoggedIn) {
    const hadUser = !!(auth.userInfo?.nickname || auth.userInfo?.username);
    if (!hadUser) {
      await auth.fetchUserInfo();
    } else {
      auth.fetchUserInfo().catch(() => {});
    }
    await refreshStats().catch(() => {});
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
.profile {
  min-height: 100vh;
  background: var(--bg-color);
}

/* 顶区薄荷渐变，与小程序 profile-header 一致 */
.profile-header {
  position: relative;
  padding: 20px var(--page-horizontal) 14px;
  box-sizing: border-box;
  background: linear-gradient(
    180deg,
    #f4fbf9 0%,
    #eaf6f2 18%,
    #dff2ec 52%,
    var(--bg-color) 100%
  );
}

.profile-hero-sheet {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.78);
  border-radius: var(--radius-lg);
  padding: 14px 12px 12px;
  box-shadow: 0 4px 18px rgba(74, 154, 144, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.95);
  box-sizing: border-box;
  backdrop-filter: blur(8px);
}

.user-info {
  position: relative;
  display: flex;
  align-items: center;
  min-width: 0;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
  width: 59px;
  height: 59px;
  border-radius: 50%;
  border: 2.5px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 3px 10px rgba(95, 179, 168, 0.22);
  overflow: hidden;
}

.avatar-wrap :deep(.avatar) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.login-badge {
  position: absolute;
  bottom: 0;
  right: 0;
  padding: 2px 6px;
  background: var(--primary-color);
  color: #fff;
  font-size: 10px;
  border-radius: 10px;
  line-height: 1.2;
}

.user-detail {
  flex: 1 1 0%;
  min-width: 0;
  margin-left: 9px;
  padding-right: 30px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nickname {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.25;
}

.bio {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.35;
}

.bio--single {
  display: block;
  width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.org-info {
  display: grid;
  width: 100%;
  min-width: 0;
  column-gap: 14px;
  align-items: center;
}

.org-info--pair {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 8px;
}

.org-info--single {
  grid-template-columns: minmax(0, 1fr);
  justify-items: start;
}

.org-item {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  min-width: 0;
  max-width: 100%;
  background: rgba(95, 179, 168, 0.14);
  padding: 5px 8px 5px 7px;
  border-radius: var(--radius-full);
  gap: 6px;
}

.org-ico {
  flex-shrink: 0;
  font-size: 17px;
  line-height: 1;
}

.org-txt-wrap {
  flex: 0 1 auto;
  min-width: 0;
  max-width: 140px;
  overflow: hidden;
}

.org-txt {
  display: block;
  font-size: 13px;
  color: var(--primary-dark);
  line-height: 1.25;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.edit-btn {
  position: absolute;
  right: 0;
  top: 0;
  z-index: 2;
  border: none;
  background: transparent;
  padding: 5px;
  color: var(--text-tertiary);
  cursor: pointer;
}

.edit-icon {
  width: 20px;
  height: 20px;
  display: block;
}

.stats {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(95, 179, 168, 0.14);
}

.stat-item {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 0;
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.15s ease;
}

.stat-item:active {
  background: rgba(95, 179, 168, 0.08);
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--primary-dark);
  line-height: 1.2;
}

.stat-label {
  font-size: 10px;
  line-height: 1.3;
  color: var(--text-secondary);
  margin-top: 4px;
  text-align: center;
  padding: 0 2px;
}

.stat-divider {
  width: 1px;
  height: 30px;
  background: rgba(95, 179, 168, 0.22);
  flex-shrink: 0;
}

.profile-menus {
  padding: 4px var(--page-horizontal) 8px;
}

.menu-section {
  margin-bottom: 14px;
}

.menu-section__label {
  margin: 0 0 8px 4px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-tertiary);
  line-height: 1.3;
}

.menu-section__card {
  background: var(--bg-white);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(74, 154, 144, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.9);
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  min-height: 54px;
  padding: 12px 16px;
  gap: 12px;
  border: none;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-white);
  cursor: pointer;
  text-align: left;
  transition: background 0.15s ease;
  -webkit-tap-highlight-color: transparent;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item:active {
  background: rgba(95, 179, 168, 0.06);
}

.menu-left {
  display: flex;
  align-items: center;
  min-width: 0;
  flex: 1;
  gap: 12px;
}

.menu-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 10px;
  background: var(--primary-bg);
  color: var(--primary-dark);
}

.menu-icon-wrap--following,
.menu-icon-wrap--followers {
  color: var(--primary-color);
}

.menu-icon {
  width: 20px;
  height: 20px;
  display: block;
}

.menu-title {
  font-size: 15px;
  line-height: 1.35;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-arrow {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  opacity: 0.4;
  color: var(--text-tertiary);
}

.version-info {
  text-align: center;
  padding: 16px var(--page-horizontal) 8px;
  font-size: 12px;
  color: var(--text-tertiary);
  margin: 0;
}
</style>
