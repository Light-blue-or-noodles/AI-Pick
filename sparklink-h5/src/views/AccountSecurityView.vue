<template>
  <div class="settings-sub page page--no-tab">
    <PageNavBar title="账号与安全" />
    <van-cell-group inset>
      <van-cell title="用户名" :value="auth.userInfo?.username || '-'" />
      <van-cell title="用户 ID" :value="String(auth.userId || '-')" />
      <van-cell title="手机号" :value="maskedPhone" />
    </van-cell-group>
    <p class="settings-sub__hint">修改密码等功能请使用小程序或联系客服（H5 首版只读展示）。</p>
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
.settings-sub__hint {
  margin: 16px;
  font-size: 12px;
  color: var(--text-tertiary);
}
</style>
