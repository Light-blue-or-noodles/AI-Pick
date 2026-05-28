<template>
  <div class="settings-sub page page--no-tab">
    <PageNavBar title="关于我们" />
    <div class="about card">
      <h2>Spark Link</h2>
      <p>找搭子，上 Spark Link。H5 版与微信小程序功能对齐，可在浏览器中使用。</p>
      <p class="about__ver">版本 {{ version }}</p>
      <p class="about__api">API：{{ apiBase }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import PageNavBar from '@/components/PageNavBar.vue';
import { getApiBaseUrl } from '@/config/env';

const version = '0.1.0-h5';
const apiBase = computed(() => getApiBaseUrl() || '(开发代理 /api)');
</script>

<style scoped>
.about {
  margin: 16px;
  padding: 24px;
  text-align: center;
}

.about h2 {
  margin: 0 0 12px;
  color: var(--primary-color);
}

.about p {
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.about__ver,
.about__api {
  font-size: 12px;
  color: var(--text-tertiary);
}
</style>
