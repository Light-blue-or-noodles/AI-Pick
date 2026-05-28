<template>
  <div class="edit page page--no-tab">
    <PageNavBar title="编辑资料" />

    <form class="edit__body" @submit.prevent="onSave">
      <section class="edit__hero">
        <button type="button" class="edit__avatar-trigger" @click="pickAvatar">
          <div class="edit__avatar-ring">
            <img
              v-if="avatarDisplay"
              :src="avatarDisplay"
              class="edit__avatar-img"
              alt=""
            />
          </div>
          <span class="edit__avatar-action">更换头像</span>
        </button>
      </section>

      <section class="edit__card">
        <label class="edit__row">
          <span class="edit__label">昵称</span>
          <input
            v-model="form.nickname"
            class="edit__input"
            type="text"
            maxlength="20"
            placeholder="请输入昵称"
            autocomplete="nickname"
          />
        </label>
      </section>

      <section class="edit__card edit__card--bio">
        <div class="edit__bio-head">
          <span class="edit__label">简介</span>
          <span class="edit__count">{{ bioLength }}/200</span>
        </div>
        <textarea
          ref="bioEl"
          v-model="form.bio"
          class="edit__textarea"
          maxlength="200"
          placeholder="写一句简介"
          rows="2"
          @input="onBioInput"
        />
      </section>

      <footer class="edit__footer">
        <button type="submit" class="edit__save" :disabled="saving">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </footer>
    </form>

    <input ref="fileInput" type="file" accept="image/*" class="hidden-input" @change="onAvatarChange" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { showToast, showLoadingToast, closeToast } from 'vant';
import { useAuthStore } from '@/stores/auth';
import { put, uploadFile, getApiErrorMessage } from '@/utils/request';
import { resolveMediaUrl, normalizeImageUrl } from '@/utils/mediaUrl';
import { getApiBaseUrl } from '@/config/env';
import { KEYS, setItem } from '@/utils/storage';
import PageNavBar from '@/components/PageNavBar.vue';

const router = useRouter();
const auth = useAuthStore();
const fileInput = ref(null);
const bioEl = ref(null);
const saving = ref(false);
const localAvatarPreview = ref('');

function snapshotFromUser(u = auth.userInfo || {}) {
  return {
    nickname: u.nickname || u.username || '',
    bio: String(u.bio || u.introduction || '').slice(0, 200),
    avatar: u.avatar || ''
  };
}

const form = reactive(snapshotFromUser());

const avatarDisplay = computed(() => {
  if (localAvatarPreview.value) {
    return localAvatarPreview.value;
  }
  return resolveMediaUrl(form.avatar, {
    baseUrl: getApiBaseUrl(),
    fallback: '/images/default-avatar.png'
  });
});

const bioLength = computed(() => form.bio.length);

function applyUserToForm(u) {
  const snap = snapshotFromUser(u);
  form.nickname = snap.nickname;
  form.bio = snap.bio;
  form.avatar = snap.avatar;
}

function resizeBioField(el) {
  if (!el) {
    return;
  }
  el.style.height = 'auto';
  el.style.height = `${Math.min(el.scrollHeight, 96)}px`;
}

function onBioInput(e) {
  if (form.bio.length > 200) {
    form.bio = form.bio.slice(0, 200);
  }
  resizeBioField(e.target);
}

function pickAvatar() {
  fileInput.value?.click();
}

async function onAvatarChange(e) {
  const file = e.target.files?.[0];
  if (!file) {
    return;
  }
  if (localAvatarPreview.value) {
    URL.revokeObjectURL(localAvatarPreview.value);
  }
  localAvatarPreview.value = URL.createObjectURL(file);
  showLoadingToast({ message: '上传中', forbidClick: true });
  try {
    const res = await uploadFile('/api/user/avatar', file, 'file');
    const url = res.data?.avatar || res.data?.url || res.data;
    if (url) {
      form.avatar = typeof url === 'string' ? url : '';
      setItem(KEYS.userAvatar, normalizeImageUrl(form.avatar, getApiBaseUrl()));
      showToast('头像已更新');
    }
  } catch (err) {
    showToast(getApiErrorMessage(err, '头像上传失败'));
  } finally {
    closeToast();
    e.target.value = '';
  }
}

async function onSave() {
  const nickname = form.nickname.trim();
  if (!nickname) {
    showToast('请输入昵称');
    return;
  }
  saving.value = true;
  showLoadingToast({ message: '保存中', forbidClick: true });
  try {
    const u = auth.userInfo || {};
    await put('/api/user/info', {
      nickname,
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
    closeToast();
  }
}

onMounted(async () => {
  if (auth.isLoggedIn) {
    await auth.fetchUserInfo();
    applyUserToForm(auth.userInfo);
  }
  await nextTick();
  resizeBioField(bioEl.value);
});
</script>

<style scoped>
.edit {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--bg-color);
}

.edit :deep(.van-nav-bar) {
  background: var(--bg-white);
}

.edit :deep(.van-hairline--bottom::after) {
  border-color: var(--border-color);
}

.edit__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-bottom: calc(16px + var(--safe-bottom));
}

.edit__hero {
  display: flex;
  justify-content: center;
  padding: 28px var(--page-horizontal) 20px;
  background: linear-gradient(180deg, var(--primary-bg) 0%, var(--bg-color) 100%);
}

.edit__avatar-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.edit__avatar-ring {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  padding: 3px;
  background: linear-gradient(135deg, var(--primary-light) 0%, var(--primary-color) 100%);
  box-shadow: 0 4px 16px rgba(74, 154, 144, 0.22);
  overflow: hidden;
}

.edit__avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  background: var(--bg-white);
}

.edit__avatar-action {
  margin-top: 14px;
  padding: 6px 18px;
  border-radius: var(--radius-full);
  background: var(--bg-white);
  font-size: 14px;
  color: var(--primary-dark);
  box-shadow: 0 2px 8px rgba(74, 154, 144, 0.12);
  line-height: 1.3;
}

.edit__avatar-trigger:active .edit__avatar-action {
  background: var(--primary-bg);
}

.edit__card {
  margin: 0 var(--page-horizontal) 12px;
  background: var(--bg-white);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(74, 154, 144, 0.06);
}

.edit__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 52px;
  padding: 0 15px;
  box-sizing: border-box;
}

.edit__label {
  flex-shrink: 0;
  font-size: 15px;
  line-height: 1.35;
  color: var(--text-primary);
}

.edit__input {
  flex: 1;
  min-width: 0;
  height: 44px;
  border: none;
  background: transparent;
  font-family: inherit;
  font-size: 15px;
  line-height: 44px;
  color: var(--text-primary);
  text-align: right;
  outline: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.edit__input::placeholder {
  color: var(--text-tertiary);
}

.edit__card--bio {
  padding: 14px 15px 16px;
}

.edit__bio-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.edit__count {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--text-tertiary);
  line-height: 1.3;
}

.edit__textarea {
  display: block;
  width: 100%;
  min-height: 56px;
  max-height: 96px;
  padding: 0;
  border: none;
  background: transparent;
  font-family: inherit;
  font-size: 15px;
  line-height: 1.5;
  color: var(--text-primary);
  resize: none;
  outline: none;
  overflow: hidden;
  field-sizing: content;
}

.edit__textarea::placeholder {
  color: var(--text-tertiary);
}

.edit__footer {
  margin-top: auto;
  padding: 20px var(--page-horizontal) 8px;
}

.edit__save {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 48px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--primary-color);
  font-family: inherit;
  font-size: 16px;
  font-weight: 500;
  color: #fff;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(95, 179, 168, 0.35);
  -webkit-tap-highlight-color: transparent;
  transition: opacity 0.15s ease;
}

.edit__save:active:not(:disabled) {
  opacity: 0.92;
}

.edit__save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.hidden-input {
  display: none;
}
</style>
