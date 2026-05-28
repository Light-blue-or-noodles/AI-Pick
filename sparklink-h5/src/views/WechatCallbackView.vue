<template>
  <div class="wechat-cb page page--no-tab">
    <van-loading v-if="loading" vertical>登录中...</van-loading>
    <p v-else class="empty-hint">{{ message }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showToast } from 'vant';
import { useAuthStore } from '@/stores/auth';
import { post, getApiErrorMessage } from '@/utils/request';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const loading = ref(true);
const message = ref('');

onMounted(async () => {
  const code = route.query.code;
  if (!code || typeof code !== 'string') {
    loading.value = false;
    message.value = '缺少微信授权 code';
    return;
  }
  try {
    const res = await post('/api/user/wechat-login', { code });
    const data = res.data || {};
    if (data.token) {
      auth.persistSession({
        token: data.token,
        userId: data.userId,
        userInfo: data.userInfo || {}
      });
      await auth.fetchUserInfo();
      showToast('登录成功');
      router.replace({ name: 'home' });
      return;
    }
    message.value = '登录响应异常';
  } catch (e) {
    message.value = getApiErrorMessage(e, '微信登录失败（需配置开放平台网站应用）');
    showToast(message.value);
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.wechat-cb {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}
</style>
