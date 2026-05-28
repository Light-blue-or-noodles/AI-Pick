<template>
  <div class="join page page--no-tab">
    <PageNavBar title="我的学校" />
    <div class="join__body card">
      <template v-if="hasJoined">
        <p class="join__label">当前学校</p>
        <p class="join__value">{{ userSchool }}</p>
        <van-button block plain @click="hasJoined = false">更换学校</van-button>
      </template>
      <template v-else>
        <van-field v-model="schoolName" label="学校名称" placeholder="请输入学校名称" />
        <van-button
          round
          block
          type="primary"
          color="#5FB3A8"
          :loading="submitting"
          @click="onSubmit"
        >
          加入学校
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
const schoolName = ref('');
const submitting = ref(false);
const hasJoined = ref(false);
const userSchool = ref('');

function syncFromUser() {
  const sn = (auth.userInfo?.schoolName && String(auth.userInfo.schoolName).trim()) || '';
  hasJoined.value = sn.length > 0;
  userSchool.value = sn;
}

async function onSubmit() {
  const name = schoolName.value.trim();
  if (!name) {
    showToast('请输入学校名称');
    return;
  }
  if (hasJoined.value) {
    try {
      await showConfirmDialog({ title: '确认更换学校？' });
    } catch {
      return;
    }
  }
  submitting.value = true;
  try {
    const res = await post('/api/user/school', { schoolName: name });
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

onMounted(() => {
  syncFromUser();
  if (auth.isLoggedIn) {
    auth.fetchUserInfo().then(() => syncFromUser()).catch(() => {});
  }
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
