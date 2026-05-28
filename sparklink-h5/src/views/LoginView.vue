<template>
  <div class="login page page--no-tab">
    <div class="login__bg" aria-hidden="true">
      <span class="login__orb login__orb--1" />
      <span class="login__orb login__orb--2" />
    </div>

    <main class="login__main">
      <header class="login__brand">
        <div class="login__logo-wrap">
          <img
            src="/images/sparklink-logo.png"
            class="login__logo"
            width="88"
            height="88"
            alt="Spark Link"
          />
        </div>
        <h1 class="login__title">Spark Link</h1>
        <p class="login__slogan">找搭子，遇见同频的人</p>
      </header>

      <form class="login__card" @submit.prevent="onSubmit">
        <label class="login-field">
          <span class="login-field__label">用户名</span>
          <input
            v-model="username"
            class="login-field__input"
            type="text"
            name="username"
            autocomplete="username"
            placeholder="请输入用户名"
          />
        </label>

        <label class="login-field">
          <span class="login-field__label">密码</span>
          <input
            v-model="password"
            class="login-field__input"
            type="password"
            name="password"
            autocomplete="current-password"
            placeholder="请输入密码"
          />
        </label>

        <label class="login-agree">
          <input v-model="agreeProtocol" type="checkbox" class="login-agree__input" />
          <span class="login-agree__text">
            我已阅读并同意
            <router-link :to="{ name: 'agreement-user' }" class="login-agree__link">用户协议</router-link>
            与
            <router-link :to="{ name: 'agreement-privacy' }" class="login-agree__link">隐私政策</router-link>
          </span>
        </label>

        <button type="submit" class="login-btn login-btn--primary" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <button
          v-if="wechatAppId"
          type="button"
          class="login-btn login-btn--wechat"
          @click="onWechatLogin"
        >
          微信登录
        </button>
      </form>

      <footer class="login__footer">
        <p>H5 使用账号密码登录；微信一键登录请使用小程序。</p>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showToast } from 'vant';
import { useAuthStore } from '@/stores/auth';
import { getApiErrorMessage } from '@/utils/request';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const wechatAppId = import.meta.env.VITE_WECHAT_APP_ID || '';
const username = ref('');
const password = ref('');
const agreeProtocol = ref(false);
const loading = ref(false);

function afterLogin() {
  const redirect = route.query.redirect || '/home';
  router.replace(typeof redirect === 'string' ? redirect : '/home');
}

async function onSubmit() {
  if (!username.value.trim()) {
    showToast('请输入用户名');
    return;
  }
  if (!password.value) {
    showToast('请输入密码');
    return;
  }
  if (!agreeProtocol.value) {
    showToast('请先同意用户协议');
    return;
  }
  loading.value = true;
  try {
    await auth.login(username.value.trim(), password.value);
    showToast('登录成功');
    afterLogin();
  } catch (e) {
    showToast(getApiErrorMessage(e, '登录失败'));
  } finally {
    loading.value = false;
  }
}

function onWechatLogin() {
  if (!agreeProtocol.value) {
    showToast('请先同意用户协议');
    return;
  }
  const redirectUri = encodeURIComponent(`${window.location.origin}${import.meta.env.BASE_URL}login/wechat-callback`);
  const url = `https://open.weixin.qq.com/connect/qrconnect?appid=${wechatAppId}&redirect_uri=${redirectUri}&response_type=code&scope=snsapi_login#wechat_redirect`;
  window.location.href = url;
}
</script>

<style scoped>
.login {
  position: relative;
  min-height: 100vh;
  min-height: 100dvh;
  overflow: hidden;
  background: #f4fbf9;
}

.login__bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(
    165deg,
    #e8f6f5 0%,
    #f4fbf9 38%,
    #f8fcfb 72%,
    #eef8f5 100%
  );
}

.login__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(48px);
  opacity: 0.55;
}

.login__orb--1 {
  width: 220px;
  height: 220px;
  top: -40px;
  right: -60px;
  background: rgba(95, 179, 168, 0.35);
}

.login__orb--2 {
  width: 180px;
  height: 180px;
  bottom: 12%;
  left: -50px;
  background: rgba(127, 196, 186, 0.28);
}

.login__main {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
  padding: calc(28px + env(safe-area-inset-top, 0px)) var(--page-horizontal)
    calc(24px + env(safe-area-inset-bottom, 0px));
  box-sizing: border-box;
}

.login__brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 8px 0 28px;
}

.login__logo-wrap {
  width: 96px;
  height: 96px;
  padding: 4px;
  border-radius: 22px;
  background: #fff;
  box-shadow:
    0 8px 28px rgba(74, 154, 144, 0.22),
    0 0 0 1px rgba(255, 255, 255, 0.9);
}

.login__logo {
  width: 100%;
  height: 100%;
  display: block;
  border-radius: 18px;
  object-fit: cover;
}

.login__title {
  margin: 18px 0 6px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: #1a1a1a;
  line-height: 1.2;
}

.login__slogan {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.45;
}

.login__card {
  width: 100%;
  max-width: 400px;
  margin: 0 auto;
  padding: 22px 18px 20px;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 8px 32px rgba(74, 154, 144, 0.1);
  backdrop-filter: blur(10px);
  box-sizing: border-box;
}

.login-field {
  display: block;
  margin-bottom: 16px;
}

.login-field__label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.login-field__input {
  display: block;
  width: 100%;
  min-height: 48px;
  padding: 12px 14px;
  box-sizing: border-box;
  border: 1px solid #e8eeec;
  border-radius: 10px;
  background: #fafcfc;
  font-family: inherit;
  font-size: 16px;
  color: var(--text-primary);
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}

.login-field__input::placeholder {
  color: #b0b8b5;
}

.login-field__input:focus {
  border-color: rgba(95, 179, 168, 0.55);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(95, 179, 168, 0.12);
}

.login-agree {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 4px 0 20px;
  cursor: pointer;
}

.login-agree__input {
  width: 18px;
  height: 18px;
  margin-top: 2px;
  flex-shrink: 0;
  accent-color: var(--primary-color);
  cursor: pointer;
}

.login-agree__text {
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-secondary);
}

.login-agree__link {
  color: var(--primary-color);
  text-decoration: none;
}

.login-agree__link:hover {
  text-decoration: underline;
}

.login-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 48px;
  margin-top: 10px;
  padding: 0 16px;
  border: none;
  border-radius: 24px;
  font-family: inherit;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.2;
  cursor: pointer;
  transition: opacity 0.15s ease, transform 0.12s ease;
  -webkit-tap-highlight-color: transparent;
}

.login-btn:first-of-type {
  margin-top: 0;
}

.login-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.login-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.login-btn--primary {
  margin-top: 4px;
  color: #fff;
  background: linear-gradient(135deg, #5fb3a8 0%, #6ec4b8 100%);
  box-shadow: 0 4px 16px rgba(95, 179, 168, 0.38);
}

.login-btn--wechat {
  color: #fff;
  background: #07c160;
  box-shadow: 0 4px 14px rgba(7, 193, 96, 0.28);
}

.login__footer {
  margin-top: auto;
  padding-top: 24px;
  text-align: center;
}

.login__footer p {
  margin: 0 0 6px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-tertiary);
}

.login__footer p:last-child {
  margin-bottom: 0;
}
</style>
