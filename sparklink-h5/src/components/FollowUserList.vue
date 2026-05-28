<template>
  <van-pull-refresh
    :model-value="refreshing"
    class="follow-list"
    @update:model-value="$emit('update:refreshing', $event)"
    @refresh="$emit('refresh')"
  >
    <ListRowsSkeleton v-if="loading && !users.length" class="follow-list__skeleton" />
    <div
      v-else-if="users.length"
      class="follow-list__card"
      :class="{ 'follow-list__card--split': showChat || showFollowing }"
    >
      <div
        v-for="u in users"
        :key="u.id"
        class="follow-list__row"
      >
        <button type="button" class="follow-list__main" @click="$emit('select', u)">
          <img :src="u.avatarUrl" class="follow-list__avatar" alt="" />
          <div class="follow-list__text">
            <span class="follow-list__name">{{ displayName(u) }}</span>
            <span v-if="showBio && bioText(u)" class="follow-list__bio">{{ bioText(u) }}</span>
          </div>
        </button>
        <div v-if="showChat || showFollowing" class="follow-list__actions">
          <button
            v-if="showChat"
            type="button"
            class="follow-list__chat"
            aria-label="发消息"
            @click="$emit('chat', u)"
          >
            <span class="follow-list__chat-icon" aria-hidden="true">💬</span>
          </button>
          <button
            v-if="showFollowing"
            type="button"
            class="follow-list__following"
            @click="$emit('unfollow', u)"
          >
            已关注
          </button>
        </div>
        <svg v-else class="follow-list__arrow" viewBox="0 0 24 24" aria-hidden="true">
          <path
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M9 6l6 6-6 6"
          />
        </svg>
      </div>
    </div>
    <p v-else-if="!loading" class="follow-list__empty">{{ emptyText }}</p>
  </van-pull-refresh>
</template>

<script setup>
import ListRowsSkeleton from '@/components/skeleton/ListRowsSkeleton.vue';

defineProps({
  users: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  refreshing: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无数据' },
  showChat: { type: Boolean, default: false },
  showBio: { type: Boolean, default: false },
  showFollowing: { type: Boolean, default: false }
});

defineEmits(['refresh', 'select', 'chat', 'unfollow', 'update:refreshing']);

function displayName(u) {
  const name = String(u.nickname || u.username || '').trim();
  return name || '用户';
}

function bioText(u) {
  const b = String(u.bio || '').trim();
  return b && b !== 'null' ? b : '';
}
</script>

<style scoped>
.follow-list {
  padding: 16px var(--page-horizontal) 28px;
}

.follow-list__skeleton {
  margin-top: 0;
}

.follow-list__card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.follow-list__card--split .follow-list__row {
  padding-right: 12px;
}

.follow-list__row {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 68px;
  padding: 12px 16px;
  gap: 10px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
  box-sizing: border-box;
}

.follow-list__row:last-child {
  border-bottom: none;
}

.follow-list__main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.follow-list__main:active {
  opacity: 0.85;
}

.follow-list__avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.follow-list__text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.follow-list__name {
  font-size: 15px;
  line-height: 1.35;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.follow-list__bio {
  font-size: 13px;
  line-height: 1.35;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.follow-list__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.follow-list__chat {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: #f5f5f5;
  cursor: pointer;
  transition: background 0.15s ease, transform 0.12s ease;
  -webkit-tap-highlight-color: transparent;
}

.follow-list__chat:active {
  background: var(--primary-bg);
  transform: scale(0.96);
}

.follow-list__chat-icon {
  font-size: 18px;
  line-height: 1;
}

.follow-list__following {
  flex-shrink: 0;
  padding: 8px 14px;
  border: none;
  border-radius: 20px;
  background: #f5f5f5;
  font-family: inherit;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.2;
  white-space: nowrap;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
  -webkit-tap-highlight-color: transparent;
}

.follow-list__following:active {
  background: #ebebeb;
  color: var(--text-primary);
}

.follow-list__row > .follow-list__arrow {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  margin-left: auto;
  color: #c8c8c8;
  pointer-events: none;
}

.follow-list__empty {
  margin: 48px 0 0;
  text-align: center;
  font-size: 14px;
  color: var(--text-tertiary);
}
</style>
