<template>
  <div class="edit page page--no-tab">
    <van-nav-bar title="编辑资料" left-arrow @click-left="$router.back()" />
    <van-form @submit="onSave">
      <div class="edit__avatar-wrap">
        <img :src="avatarPreview" class="edit__avatar" alt="" @click="pickAvatar" />
        <van-button size="small" @click="pickAvatar">更换头像</van-button>
      </div>
      <van-field v-model="form.nickname" label="昵称" placeholder="昵称" maxlength="20" />
      <van-field v-model="form.bio" rows="2" autosize type="textarea" label="简介" maxlength="200" show-word-limit />
      <van-button round block type="primary" native-type="submit" color="#5FB3A8" :loading="saving">保存</van-button>
    </van-form>
    <input ref="fileInput" type="file" accept="image/*" class="hidden-input" @change="onAvatarChange" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { showToast } from 'vant';
import { useAuthStore } from '@/stores/auth';
import { put, uploadFile, getApiErrorMessage } from '@/utils/request';
import { resolveMediaUrl, normalizeImageUrl } from '@/utils/mediaUrl';
import { getApiBaseUrl } from '@/config/env';
import { KEYS, setItem } from '@/utils/storage';

const router = useRouter();
const auth = useAuthStore();
const fileInput = ref(null);
const saving = ref(false);
const avatarPreview = ref('');

const form = reactive({
  nickname: '',
  bio: '',
  avatar: ''
});

function pickAvatar() {
  fileInput.value?.click();
}

async function onAvatarChange(e) {
  const file = e.target.files?.[0];
  if (!file) {
    return;
  }
  avatarPreview.value = URL.createObjectURL(file);
  try {
    const res = await uploadFile('/api/user/avatar', file, 'file');
    const url = res.data?.avatar || res.data?.url || res.data;
    if (url) {
      form.avatar = typeof url === 'string' ? url : '';
      setItem(KEYS.userAvatar, normalizeImageUrl(form.avatar, getApiBaseUrl()));
    }
  } catch (err) {
    showToast(getApiErrorMessage(err, '头像上传失败'));
  }
}

async function onSave() {
  saving.value = true;
  try {
    const u = auth.userInfo || {};
    await put('/api/user/info', {
      nickname: form.nickname.trim(),
      bio: form.bio.trim(),
      avatar: form.avatar || undefined,
      gender: u.gender != null ? u.gender : 0
    });
    await auth.fetchUserInfo();
    showToast('保存成功');
    router.back();
  } catch (e) {
    showToast(getApiErrorMessage(e, '保存失败'));
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  const u = auth.userInfo || {};
  form.nickname = u.nickname || '';
  form.bio = u.bio || u.introduction || '';
  form.avatar = u.avatar || '';
  avatarPreview.value = resolveMediaUrl(form.avatar, { baseUrl: getApiBaseUrl() }) || avatarPreview.value;
});
</script>

<style scoped>
.edit__avatar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  gap: 12px;
}

.edit__avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
}

.hidden-input {
  display: none;
}
</style>
