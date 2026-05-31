<template>
  <div class="settings page page--no-tab">
    <PageNavBar title="设置" />

    <div class="settings__body">
      <section class="settings__group">
        <h2 class="settings__heading">账号设置</h2>
        <div class="settings__card">
          <button type="button" class="settings__row" @click="$router.push({ name: 'profile-edit' })">
            <span class="settings__label">编辑资料</span>
            <svg class="settings__arrow" viewBox="0 0 24 24" aria-hidden="true">
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
          <button type="button" class="settings__row" @click="$router.push({ name: 'account-security' })">
            <span class="settings__label">账号与安全</span>
            <svg class="settings__arrow" viewBox="0 0 24 24" aria-hidden="true">
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

      <section class="settings__group">
        <h2 class="settings__heading">通知设置</h2>
        <div class="settings__card">
          <div class="settings__row settings__row--static">
            <span class="settings__label">消息通知</span>
            <van-switch
              v-model="messageNotification"
              size="22px"
              active-color="var(--primary-color)"
              @change="saveNotif"
            />
          </div>
          <div class="settings__row settings__row--static">
            <span class="settings__label">新粉丝提醒</span>
            <van-switch
              v-model="newFollowerNotification"
              size="22px"
              active-color="var(--primary-color)"
              @change="saveNotif"
            />
          </div>
        </div>
      </section>

      <section class="settings__group">
        <h2 class="settings__heading">隐私设置</h2>
        <div class="settings__card">
          <button type="button" class="settings__row" @click="$router.push({ name: 'privacy-settings' })">
            <span class="settings__label">隐私设置</span>
            <svg class="settings__arrow" viewBox="0 0 24 24" aria-hidden="true">
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

      <section class="settings__group">
        <h2 class="settings__heading">其他</h2>
        <div class="settings__card">
          <button type="button" class="settings__row" @click="$router.push({ name: 'about' })">
            <span class="settings__label">关于我们</span>
            <svg class="settings__arrow" viewBox="0 0 24 24" aria-hidden="true">
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
          <button type="button" class="settings__row" @click="onClearCache">
            <span class="settings__label">清空缓存</span>
            <svg class="settings__arrow" viewBox="0 0 24 24" aria-hidden="true">
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

      <button v-if="auth.isLoggedIn" type="button" class="settings__logout" @click="showLogoutDialog = true">
        退出登录
      </button>

      <p class="settings__ver">Spark Link v0.2.1-h5</p>
    </div>

    <van-dialog
      v-model:show="showLogoutDialog"
      class="spark-logout-dialog"
      overlay-class="spark-logout-dialog__overlay"
      :show-confirm-button="false"
      :show-cancel-button="false"
      width="300px"
      teleport="body"
    >
      <div class="logout-dialog">
        <div class="logout-dialog__icon" aria-hidden="true">
          <svg viewBox="0 0 48 48" fill="none">
            <circle cx="24" cy="24" r="24" fill="rgba(95, 179, 168, 0.12)" />
            <path
              d="M18 24h14M28 20l4 4-4 4"
              stroke="var(--primary-color)"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <path
              d="M16 16v16"
              stroke="var(--primary-color)"
              stroke-width="2"
              stroke-linecap="round"
            />
          </svg>
        </div>
        <h3 class="logout-dialog__title">确认退出登录？</h3>
        <p class="logout-dialog__desc">退出后需重新登录，才能使用消息、报名等功能</p>
        <div class="logout-dialog__actions">
          <button type="button" class="logout-dialog__btn logout-dialog__btn--cancel" @click="showLogoutDialog = false">
            取消
          </button>
          <button
            type="button"
            class="logout-dialog__btn logout-dialog__btn--confirm"
            :disabled="logoutLoading"
            @click="confirmLogout"
          >
            {{ logoutLoading ? '退出中...' : '确认退出' }}
          </button>
        </div>
      </div>
    </van-dialog>
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
const showLogoutDialog = ref(false);
const logoutLoading = ref(false);

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

async function confirmLogout() {
  if (logoutLoading.value) {
    return;
  }
  logoutLoading.value = true;
  try {
    await IMService.logout();
    auth.clearLoginState();
    showLogoutDialog.value = false;
    showToast('已退出');
    router.replace({ name: 'login' });
  } catch {
    showToast('退出失败，请重试');
  } finally {
    logoutLoading.value = false;
  }
}

onMounted(loadNotif);
</script>

<style scoped>
.settings {
  min-height: 100vh;
  background: var(--bg-color);
}

.settings :deep(.van-nav-bar) {
  background: var(--bg-white);
}

.settings :deep(.van-hairline--bottom::after) {
  border-color: var(--border-color);
}

.settings__body {
  padding: 12px var(--page-horizontal) calc(24px + var(--safe-bottom));
}

.settings__group {
  margin-bottom: 16px;
}

.settings__group:first-child {
  margin-top: 4px;
}

.settings__heading {
  margin: 0 0 8px 4px;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.3;
  color: var(--text-tertiary);
}

.settings__card {
  background: var(--bg-white);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(74, 154, 144, 0.06);
}

.settings__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  min-height: 52px;
  padding: 0 15px;
  border: none;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-white);
  text-align: left;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.settings__row:last-child {
  border-bottom: none;
}

.settings__row:active:not(.settings__row--static) {
  background: #f7f7f7;
}

.settings__row--static {
  cursor: default;
}

.settings__label {
  flex: 1;
  min-width: 0;
  font-size: 15px;
  line-height: 1.35;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.settings__arrow {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  color: var(--text-tertiary);
  opacity: 0.45;
}

.settings__row--static :deep(.van-switch) {
  flex-shrink: 0;
}

.settings__logout {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 48px;
  margin-top: 8px;
  padding: 0 16px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--bg-white);
  color: var(--primary-color);
  font-family: inherit;
  font-size: 16px;
  font-weight: 500;
  line-height: 1;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(74, 154, 144, 0.06);
  transition: opacity 0.15s ease, background 0.15s ease;
  -webkit-tap-highlight-color: transparent;
}

.settings__logout:active {
  opacity: 0.9;
  background: var(--primary-bg);
}

.settings__ver {
  margin: 24px 0 0;
  padding: 0;
  text-align: center;
  font-size: 12px;
  color: var(--text-tertiary);
}
</style>

<style>
.spark-logout-dialog__overlay {
  background: rgba(20, 40, 38, 0.42) !important;
  backdrop-filter: blur(4px);
}

.spark-logout-dialog.van-dialog {
  top: 50%;
  border-radius: 18px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 16px 48px rgba(74, 154, 144, 0.18);
}

.spark-logout-dialog .van-dialog__content {
  padding: 0;
}

.logout-dialog {
  padding: 28px 22px 22px;
  text-align: center;
}

.logout-dialog__icon {
  display: flex;
  justify-content: center;
  margin-bottom: 14px;
}

.logout-dialog__icon svg {
  width: 52px;
  height: 52px;
}

.logout-dialog__title {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text-primary);
}

.logout-dialog__desc {
  margin: 0 0 22px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--text-secondary);
}

.logout-dialog__actions {
  display: flex;
  gap: 12px;
}

.logout-dialog__btn {
  flex: 1;
  height: 44px;
  border: none;
  border-radius: 22px;
  font-family: inherit;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: opacity 0.15s ease, transform 0.1s ease;
}

.logout-dialog__btn:active:not(:disabled) {
  transform: scale(0.98);
}

.logout-dialog__btn--cancel {
  background: #f3f6f6;
  color: var(--text-secondary);
}

.logout-dialog__btn--confirm {
  color: #fff;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  box-shadow: 0 4px 14px rgba(95, 179, 168, 0.35);
}

.logout-dialog__btn--confirm:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
</style>
