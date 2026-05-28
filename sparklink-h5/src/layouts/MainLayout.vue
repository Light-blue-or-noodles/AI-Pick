<template>
  <div class="main-layout">
    <router-view v-slot="{ Component }">
      <component :is="Component" class="page page--with-tab" />
    </router-view>
    <AppTabBar />
  </div>
</template>

<script setup>
import { ref, provide } from 'vue';
import AppTabBar from '@/components/AppTabBar.vue';

const tabReselectKey = ref(0);
provide('tabReselectKey', tabReselectKey);
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background: var(--bg-color);
}
</style>
