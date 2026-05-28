<template>
  <nav v-if="showTabBar" class="tab-bar">
    <button
      v-for="item in tabList"
      :key="item.key"
      type="button"
      class="tab-item"
      :class="{
        active: !item.isPublish && activeTab === item.key,
        'tab-item--publish': item.isPublish
      }"
      :aria-label="item.isPublish ? '发布搭子' : item.label"
      @click="onTabClick(item)"
    >
      <span
        class="tab-icon-wrap"
        :class="{ 'tab-icon-wrap--publish': item.isPublish }"
      >
        <span
          v-if="item.key === 'message' && unread > 0"
          class="tab-badge"
        >
          {{ unread > 99 ? '99+' : unread }}
        </span>

        <!-- 发布 -->
        <span v-if="item.isPublish" class="icon-publish-fab">
          <span class="icon-publish-cross">
            <span class="icon-publish-bar icon-publish-bar--h" />
            <span class="icon-publish-bar icon-publish-bar--v" />
          </span>
        </span>

        <!-- 首页 -->
        <span v-else-if="item.key === 'home'" class="icon icon-home">
          <span class="icon-home-roof" />
          <span class="icon-home-body" />
          <span class="icon-home-door" />
        </span>

        <!-- 搭子 -->
        <span v-else-if="item.key === 'partner'" class="icon icon-partner">
          <span class="icon-partner-head icon-partner-head-left" />
          <span class="icon-partner-head icon-partner-head-right" />
          <span class="icon-partner-body icon-partner-body-left" />
          <span class="icon-partner-body icon-partner-body-right" />
        </span>

        <!-- 消息 -->
        <span v-else-if="item.key === 'message'" class="icon icon-message">
          <span class="icon-message-bubble">
            <span class="icon-message-dot dot-1" />
            <span class="icon-message-dot dot-2" />
            <span class="icon-message-dot dot-3" />
          </span>
        </span>

        <!-- 我的 -->
        <span v-else-if="item.key === 'profile'" class="icon icon-profile">
          <span class="icon-profile-head" />
          <span class="icon-profile-body" />
        </span>
      </span>
      <span v-if="!item.isPublish" class="tab-text">{{ item.label }}</span>
    </button>
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

const tabList = [
  { key: 'home', label: '首页', name: 'home' },
  { key: 'partner', label: '搭子', name: 'partner' },
  { key: 'publish', isPublish: true },
  { key: 'message', label: '消息', name: 'message' },
  { key: 'profile', label: '我的', name: 'profile' }
];

const activeTab = computed(() => route.meta.tab || '');
const showTabBar = computed(() => !!route.meta.tab);
const unread = computed(() => auth.unreadCount);

function onTabClick(item) {
  if (item.isPublish) {
    goPublish();
    return;
  }
  if (activeTab.value === item.key) {
    if (tabReselectKey) {
      tabReselectKey.value += 1;
    }
    return;
  }
  router.push({ name: item.name });
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
/* 对齐 sparklink-mini-program/custom-tab-bar/index.wxss */
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
  overflow: visible;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 42px;
  min-width: 0;
  border: none;
  background: transparent;
  border-radius: 12px;
  padding: 0;
  cursor: pointer;
  transition: transform 0.2s ease;
  box-sizing: border-box;
  -webkit-tap-highlight-color: transparent;
}

.tab-item.active {
  background: rgba(95, 179, 168, 0.12);
}

.tab-item:active {
  transform: scale(0.94);
}

.tab-icon-wrap {
  position: relative;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 3px;
  flex-shrink: 0;
}

.tab-badge {
  position: absolute;
  top: -3px;
  right: -7px;
  min-width: 14px;
  height: 14px;
  padding: 0 4px;
  box-sizing: border-box;
  font-size: 9px;
  line-height: 14px;
  text-align: center;
  color: #fff;
  background: #ff4d4f;
  border-radius: 7px;
  font-weight: 600;
  z-index: 2;
  border: 1px solid #fff;
}

.icon {
  position: relative;
  width: 20px;
  height: 20px;
}

.tab-text {
  font-size: 10px;
  line-height: 1.2;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.tab-item.active .tab-text {
  color: #5fb3a8;
  font-weight: 500;
}

/* 首页 */
.icon-home-roof {
  position: absolute;
  top: 2px;
  left: 3px;
  right: 3px;
  height: 7px;
  background: #5fb3a8;
  border-radius: 3px 3px 1px 1px;
}

.icon-home-body {
  position: absolute;
  left: 3px;
  right: 3px;
  bottom: 2px;
  height: 9px;
  background: #fff;
  border-radius: 2px;
  border: 1px solid #5fb3a8;
  box-sizing: border-box;
}

.icon-home-door {
  position: absolute;
  bottom: 2px;
  left: 8px;
  width: 4px;
  height: 5px;
  background: #5fb3a8;
  border-radius: 2px 2px 1px 1px;
}

.tab-item.active .icon-home-body {
  background: rgba(95, 179, 168, 0.1);
}

/* 搭子 */
.icon-partner-head {
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #7bcdd0;
}

.icon-partner-head-left {
  top: 3px;
  left: 3px;
}

.icon-partner-head-right {
  top: 4px;
  right: 3px;
  background: #5fb3a8;
}

.icon-partner-body {
  position: absolute;
  width: 8px;
  height: 7px;
  border-radius: 4px 4px 5px 5px;
  background: rgba(95, 179, 168, 0.15);
}

.icon-partner-body-left {
  bottom: 2px;
  left: 2px;
}

.icon-partner-body-right {
  bottom: 2px;
  right: 2px;
}

.tab-item.active .icon-partner-body {
  background: rgba(95, 179, 168, 0.3);
}

/* 消息 */
.icon-message-bubble {
  position: absolute;
  left: 2px;
  right: 2px;
  top: 3px;
  bottom: 4px;
  border-radius: 5px;
  border: 1px solid #5fb3a8;
  box-sizing: border-box;
}

.icon-message-bubble::after {
  content: '';
  position: absolute;
  left: 5px;
  bottom: -3px;
  width: 4px;
  height: 4px;
  border-radius: 1px;
  border-left: 1px solid #5fb3a8;
  border-bottom: 1px solid #5fb3a8;
  transform: rotate(45deg);
  background: #fff;
}

.icon-message-dot {
  position: absolute;
  top: 50%;
  width: 2px;
  height: 2px;
  border-radius: 50%;
  background: #5fb3a8;
  transform: translateY(-50%);
}

.icon-message-dot.dot-1 {
  left: 5px;
}

.icon-message-dot.dot-2 {
  left: 9px;
}

.icon-message-dot.dot-3 {
  left: 13px;
}

.tab-item.active .icon-message-bubble {
  background: rgba(95, 179, 168, 0.05);
}

/* 我的 */
.icon-profile-head {
  position: absolute;
  top: 2px;
  left: 50%;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #5fb3a8;
  transform: translateX(-50%);
}

.icon-profile-body {
  position: absolute;
  bottom: 2px;
  left: 50%;
  width: 11px;
  height: 8px;
  border-radius: 6px 6px 6px 6px;
  background: rgba(95, 179, 168, 0.18);
  transform: translateX(-50%);
}

.tab-item.active .icon-profile-body {
  background: rgba(95, 179, 168, 0.32);
}

/* 发布 */
.tab-item--publish {
  position: relative;
  z-index: 2;
  justify-content: center;
}

.tab-item--publish.active {
  background: transparent;
}

.tab-item--publish:active {
  transform: none;
}

.tab-icon-wrap--publish {
  width: 34px;
  height: 34px;
  margin-top: 0;
  margin-bottom: 0;
  flex-shrink: 0;
  transform: translateY(-3px);
}

.icon-publish-fab {
  width: 32px;
  height: 32px;
  border-radius: 11px;
  background: linear-gradient(165deg, #eaf8f5 0%, #d8f0eb 40%, #c0e8e0 100%);
  border: 1.5px solid rgba(95, 179, 168, 0.45);
  box-shadow: 0 3px 9px rgba(95, 179, 168, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  transition: transform 0.15s ease, opacity 0.15s ease;
}

.icon-publish-cross {
  position: relative;
  width: 20px;
  height: 20px;
}

.icon-publish-bar {
  position: absolute;
  left: 50%;
  top: 50%;
  background: #5fb3a8;
  border-radius: 9999px;
  box-sizing: border-box;
}

.icon-publish-bar--h {
  width: 18px;
  height: 5px;
  transform: translate(-50%, -50%);
}

.icon-publish-bar--v {
  width: 5px;
  height: 18px;
  transform: translate(-50%, -50%);
}

.tab-item--publish:active .tab-icon-wrap--publish {
  transform: translateY(-3px) scale(0.96);
}

.tab-item--publish:active .icon-publish-fab {
  opacity: 0.92;
}
</style>
