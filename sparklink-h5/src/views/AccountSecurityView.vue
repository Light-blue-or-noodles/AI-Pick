<template>
  <div class="settings-sub page page--no-tab">
    <PageNavBar title="账号与安全" />
    <div class="settings-sub__body">
      <div class="settings-sub__card">
        <div class="settings-sub__row">
          <span class="settings-sub__label">用户名</span>
          <span class="settings-sub__value">{{ auth.userInfo?.username || '-' }}</span>
        </div>
        <div class="settings-sub__row">
          <span class="settings-sub__label">用户 ID</span>
          <span class="settings-sub__value">{{ String(auth.userId || '-') }}</span>
        </div>
        <div class="settings-sub__row">
          <span class="settings-sub__label">手机号</span>
          <span class="settings-sub__value">{{ maskedPhone }}</span>
        </div>
      </div>
      <p class="settings-sub__hint">修改密码等功能请使用小程序或联系客服（H5 首版只读展示）。</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useAuthStore } from '@/stores/auth';
import PageNavBar from '@/components/PageNavBar.vue';

const auth = useAuthStore();

const maskedPhone = computed(() => {
  const p = auth.userInfo?.phone || '';
  if (!p || p.length < 11) {
    return p || '未绑定';
  }
  return `${p.slice(0, 3)}****${p.slice(7)}`;
});
</script>

<style scoped>
.settings-sub {
  min-height: 100vh;
  background: var(--bg-color);
}

.settings-sub :deep(.van-nav-bar) {
  background: var(--bg-white);
}

.settings-sub__body {
  padding: 12px var(--page-horizontal) 24px;
}

.settings-sub__card {
  background: var(--bg-white);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(74, 154, 144, 0.06);
}

.settings-sub__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 52px;
  padding: 0 15px;
  border-bottom: 1px solid var(--border-color);
}

.settings-sub__row:last-child {
  border-bottom: none;
}

.settings-sub__label {
  flex-shrink: 0;
  font-size: 15px;
  color: var(--text-primary);
}

.settings-sub__value {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: var(--text-secondary);
  text-align: right;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.settings-sub__hint {
  margin: 16px 4px 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-tertiary);
}
</style>
