<template>
  <div class="home page">
    <div class="home-main">
      <div class="mascot-wrap">
        <img class="mascot-img" src="/images/home-ai-mascot.png" alt="AI 助手" />
      </div>
      <h1 class="home-title">你好！我是 AI 助手</h1>
      <p class="home-sub">告诉我你想找什么样的搭子</p>
      <div class="chips">
        <button type="button" class="chip" @click="goAiChat('推荐一些游戏搭子')">🎮 游戏搭子</button>
        <button type="button" class="chip" @click="goAiChat('推荐一些运动搭子')">🏃 运动搭子</button>
        <button type="button" class="chip chip-primary" @click="goAiChat()">💬 AI 对话</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { inject, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import IMService from '@/utils/imService';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const tabReselectKey = inject('tabReselectKey', null);

function goAiChat(quick) {
  const q = quick ? { quick } : {};
  router.push({ name: 'ai-chat', query: q });
}

async function initIm() {
  if (!auth.isLoggedIn) {
    return;
  }
  try {
    await IMService.initAndLogin();
    IMService.syncUnreadBadgeFromSdk();
  } catch {
    /* ignore */
  }
}

onMounted(initIm);

if (tabReselectKey) {
  watch(tabReselectKey, () => {
    if (route.name === 'home') {
      initIm();
    }
  });
}
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 100px);
  padding: 24px var(--page-horizontal);
  background: var(--bg-color);
}

.home-main {
  width: 100%;
  max-width: 400px;
  text-align: center;
}

.mascot-wrap {
  margin-bottom: 16px;
}

.mascot-img {
  width: 160px;
  height: 160px;
  object-fit: contain;
}

.home-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
}

.home-sub {
  margin: 0 0 24px;
  font-size: 14px;
  color: var(--text-secondary);
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

.chip {
  padding: 10px 16px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-color);
  background: #fff;
  font-size: 14px;
  color: var(--text-primary);
  cursor: pointer;
}

.chip-primary {
  border-color: var(--primary-color);
  background: var(--primary-bg);
  color: var(--primary-dark);
}
</style>
