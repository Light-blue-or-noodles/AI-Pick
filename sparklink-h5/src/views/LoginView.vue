<template>
  <div class="login page page--no-tab">
    <div class="login__hero theme-gradient">
      <h1>Spark Link</h1>
      <p>找搭子，上 H5</p>
    </div>
    <van-form class="login__form" @submit="onSubmit">
      <van-field
        v-model="username"
        name="username"
        label="用户名"
        placeholder="请输入用户名"
        :rules="[{ required: true, message: '请输入用户名' }]"
      />
      <van-field
        v-model="password"
        type="password"
        name="password"
        label="密码"
        placeholder="请输入密码"
        :rules="[{ required: true, message: '请输入密码' }]"
      />
      <div class="login__agree">
        <van-checkbox v-model="agreeProtocol" shape="square">
          我已阅读并同意
          <router-link :to="{ name: 'agreement-user' }" class="login__link">用户协议</router-link>
          与
          <router-link :to="{ name: 'agreement-privacy' }" class="login__link">隐私政策</router-link>
        </van-checkbox>
      </div>
      <van-button
        round
        block
        type="primary"
        native-type="submit"
        :loading="loading"
        color="#5FB3A8"
      >
        登录
      </van-button>
      <van-button
        v-if="wechatAppId"
        round
        block
        plain
        type="primary"
        class="login__wx-btn"
        @click="onWechatLogin"
      >
        微信登录
      </van-button>
      <van-button
        v-if="isDev"
        round
        block
        plain
        type="primary"
        class="login__dev-btn"
        :loading="loading"
        @click="onTestLogin"
      >
        开发环境：测试账号登录
      </van-button>
    </van-form>
    <p class="login__hint">H5 版使用账号密码登录；小程序微信登录请继续使用小程序端。</p>
    <p v-if="isDev" class="login__hint">本地后端需开启 allow-test-login；联调线上 API 请使用真实账号。</p>
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

const isDev = import.meta.env.DEV;
const wechatAppId = import.meta.env.VITE_WECHAT_APP_ID || '';
const username = ref('');
const password = ref('');
const agreeProtocol = ref(false);
const loading = ref(false);

function afterLogin() {
  const redirect = route.query.redirect || '/home';
  router.replace(typeof redirect === 'string' ? redirect : '/home');
}

async function onTestLogin() {
  if (!agreeProtocol.value) {
    showToast('请先同意用户协议');
    return;
  }
  loading.value = true;
  try {
    await auth.loginWithTestAccount();
    showToast('测试账号已登录');
    afterLogin();
  } catch (e) {
    showToast(getApiErrorMessage(e, '测试登录未开启或后端不可用'));
  } finally {
    loading.value = false;
  }
}

async function onSubmit() {
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
.login__hero {
  padding: 48px 24px 32px;
  color: #fff;
  text-align: center;
}

.login__hero h1 {
  margin: 0 0 8px;
  font-size: 28px;
}

.login__hero p {
  margin: 0;
  opacity: 0.9;
}

.login__form {
  margin: -16px 16px 0;
  padding: 20px 16px;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md, 0 4px 20px rgba(0, 0, 0, 0.08));
}

.login__agree {
  margin: 12px 0 16px;
  font-size: 12px;
  color: var(--text-secondary);
}

.login__link {
  color: var(--primary-color);
}

.login__wx-btn,
.login__dev-btn {
  margin-top: 10px;
}

.login__hint {
  margin: 24px 16px;
  font-size: 12px;
  color: var(--text-tertiary);
  text-align: center;
}
</style>
