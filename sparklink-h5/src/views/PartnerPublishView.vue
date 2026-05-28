<template>
  <div class="publish page page--no-tab">
    <PageNavBar title="发布搭子" />
    <van-form @submit="onSubmit">
      <van-field v-model="form.title" label="标题" placeholder="给搭子起个标题" maxlength="30" show-word-limit />
      <van-field
        v-model="form.partnerType"
        is-link
        readonly
        label="类型"
        placeholder="选择搭子类型"
        @click="showTypePicker = true"
      />
      <div class="publish__tags">
        <p class="publish__tags-label">搭子偏好（最多 {{ MAX_SELECT }} 个）</p>
        <div class="publish__tag-list">
          <button
            v-for="tag in PREFERENCE_TAGS"
            :key="tag"
            type="button"
            class="publish__tag"
            :class="{ 'publish__tag--on': selectedPreferences.includes(tag) }"
            @click="togglePreference(tag)"
          >
            {{ tag }}
          </button>
        </div>
      </div>
      <van-field v-model="form.description" rows="3" autosize type="textarea" label="描述" placeholder="说说你的搭子计划" maxlength="500" show-word-limit />
      <div class="publish__scopes">
        <p class="publish__tags-label">可见范围</p>
        <van-checkbox v-model="scopeSelected.public">公开</van-checkbox>
        <van-checkbox v-model="scopeSelected.colleague" :disabled="!hasCompany">同事</van-checkbox>
        <van-checkbox v-model="scopeSelected.alumni" :disabled="!hasSchool">校友</van-checkbox>
      </div>
      <van-field v-model="form.planDate" type="date" label="开始日期" />
      <van-field v-model="form.planTimeStr" type="time" label="开始时间" />
      <van-field v-model.number="form.memberCount" type="digit" label="人数" placeholder="最多几人" />
      <van-field v-model="form.locationDisplay" label="地点" placeholder="手动输入地点">
        <template #button>
          <van-button v-if="mapKey" size="small" type="primary" plain @click.prevent="pickMap">地图选点</van-button>
        </template>
      </van-field>
      <div class="publish__cover">
        <van-button size="small" @click.prevent="pickCover">选择封面图</van-button>
        <img v-if="coverPreview" :src="coverPreview" class="publish__preview" alt="" />
      </div>
      <van-button round block type="primary" native-type="submit" color="#5FB3A8" :loading="publishing">
        发布
      </van-button>
    </van-form>
    <van-popup v-model:show="showTypePicker" position="bottom">
      <van-picker :columns="partnerTypes" @confirm="onTypeConfirm" @cancel="showTypePicker = false" />
    </van-popup>
    <input ref="fileInput" type="file" accept="image/*" class="hidden-input" @change="onFileChange" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { showToast } from 'vant';
import { post, uploadFile, get, getApiErrorMessage } from '@/utils/request';
import { PREFERENCE_TAGS, MAX_SELECT } from '@/utils/partnerPreferenceTags';
import { useAuthStore } from '@/stores/auth';
import PageNavBar from '@/components/PageNavBar.vue';

const router = useRouter();
const auth = useAuthStore();

const SCOPE_BIT = { public: 1, colleague: 2, alumni: 4 };
const mapKey = import.meta.env.VITE_MAP_KEY || '';

const fileInput = ref(null);
const showTypePicker = ref(false);
const publishing = ref(false);
const coverPreview = ref('');
const selectedPreferences = ref([]);
const hasCompany = ref(false);
const hasSchool = ref(false);
const scopeSelected = reactive({ public: true, colleague: false, alumni: false });

const partnerTypes = [
  '宠物搭子', '电影搭子', '音乐搭子', '逛街搭子', '运动搭子',
  '摄影搭子', '干饭搭子', '旅游搭子', 'k歌搭子', '喝酒搭子',
  '桌游搭子', '钓鱼搭子', '游戏搭子', '聊天搭子', '户外搭子'
];

const form = reactive({
  title: '',
  partnerType: '',
  description: '',
  locationDisplay: '',
  planDate: '',
  planTimeStr: '',
  memberCount: 3,
  coverImageUrl: '',
  latitude: null,
  longitude: null
});

function todayISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

async function loadOrgFlags() {
  const local = auth.userInfo || {};
  hasCompany.value = !!(local.companyName && String(local.companyName).trim());
  hasSchool.value = !!(local.schoolName && String(local.schoolName).trim());
  if (!auth.isLoggedIn) {
    return;
  }
  try {
    const res = await get('/api/user/info', {}, { suppressErrorToast: true });
    if (res.data) {
      auth.userInfo = res.data;
      hasCompany.value = !!(res.data.companyName && String(res.data.companyName).trim());
      hasSchool.value = !!(res.data.schoolName && String(res.data.schoolName).trim());
    }
  } catch {
    /* ignore */
  }
  if (!hasCompany.value) {
    scopeSelected.colleague = false;
  }
  if (!hasSchool.value) {
    scopeSelected.alumni = false;
  }
}

function togglePreference(tag) {
  const idx = selectedPreferences.value.indexOf(tag);
  if (idx >= 0) {
    selectedPreferences.value.splice(idx, 1);
  } else if (selectedPreferences.value.length >= MAX_SELECT) {
    showToast(`最多选 ${MAX_SELECT} 个`);
  } else {
    selectedPreferences.value.push(tag);
  }
}

function onTypeConfirm({ selectedValues }) {
  form.partnerType = selectedValues?.[0] || '';
  showTypePicker.value = false;
}

function pickCover() {
  fileInput.value?.click();
}

function pickMap() {
  showToast('H5 地图选点需配置 VITE_MAP_KEY；请使用手动输入地址');
}

async function onFileChange(e) {
  const file = e.target.files?.[0];
  if (!file) {
    return;
  }
  coverPreview.value = URL.createObjectURL(file);
  try {
    const res = await uploadFile('/api/partner/upload-image', file, 'image');
    if (res.data?.url) {
      form.coverImageUrl = res.data.url;
    } else if (typeof res.data === 'string') {
      form.coverImageUrl = res.data;
    }
  } catch (err) {
    showToast(getApiErrorMessage(err, '上传失败'));
  }
}

function buildPlanTimeIso() {
  let date = form.planDate;
  if (form.planTimeStr && !date) {
    date = todayISO();
  }
  if (!date || !form.planTimeStr) {
    return null;
  }
  return `${date}T${form.planTimeStr}:00`;
}

function validate() {
  if (!form.title.trim()) {
    showToast('请输入标题');
    return false;
  }
  if (!form.partnerType) {
    showToast('请选择类型');
    return false;
  }
  if (!selectedPreferences.value.length) {
    showToast('请选择搭子偏好');
    return false;
  }
  if (!form.description.trim()) {
    showToast('请填写详情');
    return false;
  }
  if (!scopeSelected.public && !scopeSelected.colleague && !scopeSelected.alumni) {
    showToast('请至少选择一种可见范围');
    return false;
  }
  return true;
}

async function onSubmit() {
  if (!validate()) {
    return;
  }
  const typeIndex = partnerTypes.indexOf(form.partnerType) + 1;
  const scopes = [];
  if (scopeSelected.public) {
    scopes.push(SCOPE_BIT.public);
  }
  if (scopeSelected.colleague) {
    scopes.push(SCOPE_BIT.colleague);
  }
  if (scopeSelected.alumni) {
    scopes.push(SCOPE_BIT.alumni);
  }
  const body = {
    title: form.title.trim(),
    type: typeIndex,
    content: form.description.trim(),
    preference: selectedPreferences.value.join(','),
    scopes,
    targetCount: form.memberCount || 3,
    coverImage: form.coverImageUrl || undefined
  };
  const planIso = buildPlanTimeIso();
  if (planIso) {
    body.planTime = planIso;
  }
  if (form.locationDisplay.trim()) {
    body.location = form.locationDisplay.trim();
  }
  if (form.latitude != null && form.longitude != null) {
    body.latitude = form.latitude;
    body.longitude = form.longitude;
  }
  publishing.value = true;
  try {
    await post('/api/partner', body);
    showToast('发布成功');
    router.replace({ name: 'partner' });
  } catch (e) {
    showToast(getApiErrorMessage(e, '发布失败'));
  } finally {
    publishing.value = false;
  }
}

onMounted(loadOrgFlags);
</script>

<style scoped>
.publish {
  padding-bottom: 24px;
}

.publish__tags,
.publish__scopes {
  padding: 12px 16px;
  background: #fff;
}

.publish__tags-label {
  margin: 0 0 10px;
  font-size: 14px;
  color: var(--text-secondary);
}

.publish__tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.publish__tag {
  padding: 6px 12px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-color);
  background: #fff;
  font-size: 13px;
  cursor: pointer;
}

.publish__tag--on {
  border-color: var(--primary-color);
  background: var(--primary-bg);
  color: var(--primary-dark);
}

.publish__scopes .van-checkbox {
  margin-bottom: 8px;
}

.publish__cover {
  padding: 16px;
}

.publish__preview {
  display: block;
  max-width: 100%;
  margin-top: 12px;
  border-radius: var(--radius-md);
}

.hidden-input {
  display: none;
}
</style>
