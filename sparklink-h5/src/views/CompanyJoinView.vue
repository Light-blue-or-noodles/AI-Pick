<template>
  <div class="join page page--no-tab">
    <PageNavBar title="我的公司" />
    <div class="join__body card">
      <template v-if="hasJoined">
        <p class="join__label">当前公司</p>
        <p class="join__value">{{ userCompany }}</p>
        <van-button block plain @click="hasJoined = false">更换公司</van-button>
      </template>
      <template v-else>
        <van-field v-model="companyName" label="公司名称" placeholder="请输入公司名称" />
        <van-button
          round
          block
          type="primary"
          color="#5FB3A8"
          :loading="submitting"
          @click="onSubmit"
        >
          加入公司
        </van-button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { showToast, showConfirmDialog } from 'vant';
import { post } from '@/utils/request';
import { useAuthStore } from '@/stores/auth';
import PageNavBar from '@/components/PageNavBar.vue';

const auth = useAuthStore();
const companyName = ref('');
const submitting = ref(false);
const hasJoined = ref(false);
const userCompany = ref('');

function syncFromUser() {
  const cn = (auth.userInfo?.companyName && String(auth.userInfo.companyName).trim()) || '';
  hasJoined.value = cn.length > 0;
  userCompany.value = cn;
}

async function onSubmit() {
  const name = companyName.value.trim();
  if (!name) {
    showToast('请输入公司名称');
    return;
  }
  if (hasJoined.value) {
    try {
      await showConfirmDialog({ title: '确认更换公司？' });
    } catch {
      return;
    }
  }
  submitting.value = true;
  try {
    const res = await post('/api/user/company', { companyName: name });
    if (res.data) {
      auth.userInfo = { ...auth.userInfo, ...res.data };
    }
    await auth.fetchUserInfo();
    syncFromUser();
    showToast('加入成功');
  } catch (e) {
    showToast(e?.message || '加入失败');
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  if (auth.isLoggedIn) {
    await auth.fetchUserInfo();
  }
  syncFromUser();
});
</script>

<style scoped>
.join__body {
  margin: 16px;
  padding: 20px;
}

.join__label {
  margin: 0;
  font-size: 13px;
  color: var(--text-tertiary);
}

.join__value {
  margin: 8px 0 16px;
  font-size: 18px;
  font-weight: 600;
}
</style>
