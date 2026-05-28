<template>
  <div class="settings page page--no-tab">
    <PageNavBar title="设置" />
    <div class="settings__section">
      <p class="settings__title">账号设置</p>
      <van-cell title="编辑资料" is-link @click="$router.push({ name: 'profile-edit' })" />
      <van-cell title="账号与安全" is-link @click="$router.push({ name: 'account-security' })" />
    </div>
    <div class="settings__section">
      <p class="settings__title">通知设置</p>
      <van-cell title="消息通知">
        <template #right-icon>
          <van-switch v-model="messageNotification" size="20px" active-color="#5FB3A8" @change="saveNotif" />
        </template>
      </van-cell>
      <van-cell title="新粉丝提醒">
        <template #right-icon>
          <van-switch v-model="newFollowerNotification" size="20px" active-color="#5FB3A8" @change="saveNotif" />
        </template>
      </van-cell>
    </div>
    <div class="settings__section">
      <p class="settings__title">隐私设置</p>
      <van-cell title="隐私设置" is-link @click="$router.push({ name: 'privacy-settings' })" />
    </div>
    <div class="settings__section">
      <p class="settings__title">其他</p>
      <van-cell title="关于我们" is-link @click="$router.push({ name: 'about' })" />
      <van-cell title="清空缓存" is-link @click="onClearCache" />
    </div>
    <van-button v-if="auth.isLoggedIn" block round class="settings__logout" @click="onLogout">
      退出登录
    </van-button>
    <p class="settings__ver">Spark Link v0.1.0-h5</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { showConfirmDialog, showToast } from 'vant';
import { useAuthStore } from '@/stores/auth';
import { getItem, setItem } from '@/utils/storage';
import IMService from '@/utils/imService';
import PageNavBar from '@/components/PageNavBar.vue';

const router = useRouter();
const auth = useAuthStore();

const messageNotification = ref(true);
const newFollowerNotification = ref(true);

function loadNotif() {
  try {
    const raw = getItem('notification_settings');
    if (raw) {
      const o = JSON.parse(raw);
      messageNotification.value = o.messageNotification !== false;
      newFollowerNotification.value = o.newFollowerNotification !== false;
    }
  } catch {
    /* ignore */
  }
}

function saveNotif() {
  setItem('notification_settings', JSON.stringify({
    messageNotification: messageNotification.value,
    newFollowerNotification: newFollowerNotification.value
  }));
}

async function onClearCache() {
  try {
    await showConfirmDialog({ title: '清空本地缓存？' });
    const keep = ['token', 'userId', 'userInfo', 'isLoggedIn'];
    const keys = Object.keys(localStorage);
    keys.forEach((k) => {
      if (!keep.some((p) => k.includes(p))) {
        localStorage.removeItem(k);
      }
    });
    showToast('已清空');
  } catch {
    /* cancel */
  }
}

async function onLogout() {
  try {
    await showConfirmDialog({ title: '确认退出登录？' });
    await IMService.logout();
    auth.clearLoginState();
    showToast('已退出');
    router.replace({ name: 'login' });
  } catch {
    /* cancel */
  }
}

onMounted(loadNotif);
</script>

<style scoped>
.settings__section {
  margin-bottom: 8px;
  background: #fff;
}

.settings__title {
  margin: 0;
  padding: 12px 16px 4px;
  font-size: 13px;
  color: var(--text-tertiary);
}

.settings__logout {
  margin: 24px var(--page-horizontal);
  color: #ff4d4f;
  border-color: #ff4d4f;
}

.settings__ver {
  text-align: center;
  font-size: 12px;
  color: var(--text-tertiary);
  padding-bottom: 24px;
}
</style>
