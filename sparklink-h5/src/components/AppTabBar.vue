<template>
  <nav v-if="showTabBar" class="tab-bar">
    <button
      v-for="item in tabs"
      :key="item.name"
      type="button"
      class="tab-bar__item"
      :class="{ 'tab-bar__item--active': activeTab === item.name }"
      @click="onTabClick(item.name)"
    >
      <span class="tab-bar__icon-wrap">
        <span class="tab-bar__icon">{{ item.icon }}</span>
        <span v-if="item.name === 'message' && unread > 0" class="tab-bar__badge">
          {{ unread > 99 ? '99+' : unread }}
        </span>
      </span>
      <span class="tab-bar__label">{{ item.label }}</span>
    </button>
    <button type="button" class="tab-bar__publish" aria-label="发布搭子" @click="goPublish">+</button>
  </nav>
</template>

<script setup>
import { computed, inject } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const tabReselectKey = inject('tabReselectKey', null);

const tabs = [
  { name: 'home', label: '首页', icon: '⌂' },
  { name: 'partner', label: '搭子', icon: '◎' },
  { name: 'message', label: '消息', icon: '✉' },
  { name: 'profile', label: '我的', icon: '☺' }
];

const activeTab = computed(() => route.meta.tab || '');
const showTabBar = computed(() => !!route.meta.tab);
const unread = computed(() => auth.unreadCount);

function onTabClick(name) {
  if (activeTab.value === name) {
    if (tabReselectKey) {
      tabReselectKey.value += 1;
    }
    return;
  }
  router.push({ name });
}

function goPublish() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: '/partner-publish' } });
    return;
  }
  router.push({ name: 'partner-publish' });
}
</script>

<style scoped>
.tab-bar {
  position: fixed;
  left: 12px;
  right: 12px;
  bottom: calc(10px + var(--safe-bottom));
  height: 70px;
  padding: 14px 12px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-evenly;
  background: rgba(255, 255, 255, 0.98);
  border-radius: var(--tab-bar-float-radius, 24px);
  box-shadow: var(--tab-bar-shadow);
  border: 1px solid rgba(0, 0, 0, 0.04);
  z-index: 1000;
  backdrop-filter: blur(12px);
}

.tab-bar__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 0;
  border: none;
  background: transparent;
  border-radius: 12px;
  padding: 4px 0;
  cursor: pointer;
  color: var(--text-tertiary);
  transition: transform 0.2s ease, background 0.2s ease;
}

.tab-bar__item:active {
  transform: scale(0.94);
}

.tab-bar__item--active {
  background: rgba(95, 179, 168, 0.12);
  color: var(--primary-color);
}

.tab-bar__icon-wrap {
  position: relative;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}

.tab-bar__icon {
  font-size: 20px;
  line-height: 1;
}

.tab-bar__label {
  font-size: 11px;
}

.tab-bar__badge {
  position: absolute;
  top: -6px;
  right: -12px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
  color: #fff;
  background: #ff4d4f;
  border-radius: var(--radius-full);
}

.tab-bar__publish {
  position: absolute;
  left: 50%;
  transform: translateX(-50%) translateY(-22px);
  width: 52px;
  height: 52px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #5fb3a8, #7fc4ba);
  color: #fff;
  font-size: 28px;
  line-height: 1;
  box-shadow: 0 4px 16px rgba(95, 179, 168, 0.45);
  cursor: pointer;
  z-index: 2;
}
</style>
