<template>
  <div class="publish page page--no-tab">
    <PageNavBar title="发布搭子" />
    <div class="publish-body">
      <div class="publish-form">
      <!-- 基础信息 -->
      <section class="form-card">
      <section class="form-item">
        <div class="form-label-row">
          <span class="form-label">标题</span>
          <span class="form-count">{{ titleLength }}/50</span>
        </div>
        <input
          v-model="form.title"
          class="form-input"
          type="text"
          maxlength="50"
          placeholder="请输入搭子标题"
          @input="onTitleInput"
        />
      </section>

      <!-- 封面 -->
      <section class="form-item">
        <span class="form-label">封面（单张）</span>
        <div class="cover-row">
          <div v-if="coverPreview || form.coverImageUrl" class="cover-preview" @click="pickCover">
            <img :src="coverPreview || coverDisplayUrl" class="cover-img" alt="" />
            <span class="cover-tip">点击更换</span>
          </div>
          <div v-else class="cover-placeholder" @click="pickCover">
            <span class="cover-plus">+</span>
            <span class="cover-upload-text">上传封面</span>
          </div>
          <button
            v-if="coverPreview || form.coverImageUrl"
            type="button"
            class="cover-clear"
            @click.stop="clearCover"
          >
            清除
          </button>
        </div>
      </section>

      <!-- 搭子类型 -->
      <section class="form-item">
        <span class="form-label">搭子类型 <span class="required-mark">*</span></span>
        <div class="type-tags">
          <button
            v-for="(type, index) in partnerTypes"
            :key="type"
            type="button"
            class="type-tag"
            :class="{ active: form.partnerType === type }"
            @click="form.partnerType = type"
          >
            {{ type }}
          </button>
        </div>
      </section>

      <!-- 搭子偏好 -->
      <section class="form-item">
        <div class="form-label-row">
          <span class="form-label">搭子偏好 <span class="required-mark">*</span></span>
          <span
            class="form-count"
            :class="{ 'form-count--full': selectedPreferences.length >= MAX_SELECT }"
          >
            已选 {{ selectedPreferences.length }}/{{ MAX_SELECT }}
          </span>
        </div>
        <p class="form-hint">可多选，最多 {{ MAX_SELECT }} 个</p>
        <div class="pref-tags">
          <button
            v-for="item in PREFERENCE_TAGS"
            :key="item.label"
            type="button"
            class="pref-tag"
            :class="{ active: selectedPreferences.includes(item.label) }"
            @click="togglePreference(item.label)"
          >
            <span class="pref-tag-ico">{{ item.icon }}</span>
            <span class="pref-tag-txt">{{ item.label }}</span>
          </button>
        </div>
      </section>
      </section>

      <!-- 详情 -->
      <section class="form-card">
      <section class="form-item form-item--last">
        <div class="form-label-row">
          <span class="form-label">详情 <span class="required-mark">*</span></span>
          <span class="form-count">{{ descLength }}/300</span>
        </div>
        <textarea
          v-model="form.description"
          class="form-textarea"
          maxlength="300"
          placeholder="描述你的搭子需求、时间期望等"
          @input="onDescInput"
        />
      </section>
      </section>

      <!-- 时间地点 -->
      <section class="form-card">
      <section class="form-item">
        <div class="form-label-row">
          <span class="form-label">开始时间（可选）</span>
          <button
            v-if="hasPlanTime"
            type="button"
            class="form-link"
            @click="clearPlanTime"
          >
            清除
          </button>
        </div>
        <p class="form-hint">需选择日期与时刻；仅选时刻时会自动用当天日期，否则无法保存到服务器</p>
        <div class="datetime-row">
          <div
            class="picker-box"
            role="button"
            tabindex="0"
            @click="onPickerBoxTap"
            @keydown.enter.prevent="onPickerBoxTap"
          >
            <span class="picker-text" :class="{ 'picker-text--filled': form.planDate }">{{ form.planDate || '选择日期' }}</span>
            <input
              v-model="form.planDate"
              type="date"
              :min="planDateStart"
              class="picker-native"
              @click.stop="onPickerInputTap"
            />
          </div>
          <div
            class="picker-box"
            role="button"
            tabindex="0"
            @click="onPickerBoxTap"
            @keydown.enter.prevent="onPickerBoxTap"
          >
            <span class="picker-text" :class="{ 'picker-text--filled': form.planTimeStr }">{{ form.planTimeStr || '选择时间' }}</span>
            <input
              v-model="form.planTimeStr"
              type="time"
              class="picker-native"
              @click.stop="onPickerInputTap"
            />
          </div>
        </div>
      </section>

      <!-- 结束时间 -->
      <section class="form-item">
        <span class="form-label">结束时间（可选）</span>
        <p class="form-hint">与开始时间共同展示为时间范围；需先选开始时间</p>
        <div class="datetime-row">
          <div
            class="picker-box"
            role="button"
            tabindex="0"
            @click="onPickerBoxTap"
            @keydown.enter.prevent="onPickerBoxTap"
          >
            <span class="picker-text" :class="{ 'picker-text--filled': form.planEndDate }">{{ form.planEndDate || '选择日期' }}</span>
            <input
              v-model="form.planEndDate"
              type="date"
              :min="form.planDate || planDateStart"
              class="picker-native"
              @click.stop="onPickerInputTap"
            />
          </div>
          <div
            class="picker-box"
            role="button"
            tabindex="0"
            @click="onPickerBoxTap"
            @keydown.enter.prevent="onPickerBoxTap"
          >
            <span class="picker-text" :class="{ 'picker-text--filled': form.planEndTimeStr }">{{ form.planEndTimeStr || '选择时间' }}</span>
            <input
              v-model="form.planEndTimeStr"
              type="time"
              class="picker-native"
              @click.stop="onPickerInputTap"
            />
          </div>
        </div>
      </section>

      <!-- 集合地点 -->
      <section class="form-item">
        <div class="form-label-row">
          <span class="form-label">集合地点（可选）</span>
          <button v-if="form.locationDisplay" type="button" class="form-link" @click="clearLocation">
            清除
          </button>
        </div>
        <p class="form-hint">地图选点，保存名称与坐标（GCJ-02）便于附近匹配与推荐</p>
        <div class="location-pick" @click="onPickLocation">
          <span v-if="form.locationDisplay">{{ form.locationDisplay }}</span>
          <span v-else class="location-placeholder">点击在地图上选点</span>
        </div>
      </section>
      </section>

      <!-- 人数与可见范围 -->
      <section class="form-card form-card--settings">
      <section class="form-item form-item--counter">
        <span class="form-label">人数</span>
        <p class="form-hint">点击中间数字可直接输入，或使用两侧按钮调整（{{ MEMBER_COUNT_MIN }}–{{ MEMBER_COUNT_MAX }} 人）</p>
        <div class="counter-panel">
        <div class="counter">
          <button
            type="button"
            class="counter-btn"
            :class="{ disabled: form.memberCount <= MEMBER_COUNT_MIN }"
            :disabled="form.memberCount <= MEMBER_COUNT_MIN"
            @click="decreaseCount"
          >
            -
          </button>
          <div class="counter-input-field" role="group" aria-label="人数输入">
            <input
              v-model.number="form.memberCount"
              class="count-input"
              type="tel"
              inputmode="numeric"
              pattern="[0-9]*"
              maxlength="2"
              :placeholder="String(MEMBER_COUNT_MIN)"
              autocomplete="off"
              aria-label="人数"
              @blur="normalizeMemberCount"
            />
            <span class="count-unit" aria-hidden="true">人</span>
          </div>
          <button
            type="button"
            class="counter-btn"
            :class="{ disabled: form.memberCount >= MEMBER_COUNT_MAX }"
            :disabled="form.memberCount >= MEMBER_COUNT_MAX"
            @click="increaseCount"
          >
            +
          </button>
        </div>
        </div>
      </section>

      <!-- 可见范围 -->
      <section class="form-item form-item--scope">
        <div class="form-label-row">
          <span class="form-label">可见范围 <span class="required-mark">*</span></span>
          <span class="form-optional">可多选</span>
        </div>
        <div class="scope-tags">
          <button
            v-for="item in scopeOptions"
            :key="item.value"
            type="button"
            class="scope-tag"
            :class="{
              active: scopeSelected[item.value],
              disabled: (item.value === 'colleague' && !hasCompany) || (item.value === 'alumni' && !hasSchool)
            }"
            @click="toggleScope(item.value)"
          >
            {{ item.label }}
          </button>
        </div>
        <p v-if="!hasCompany || !hasSchool" class="scope-hint">
          开启「同事」「校友」需先在个人中心完成加入公司 / 加入学校
        </p>
      </section>
      </section>
      </div>

      <footer class="publish-footer">
        <button type="button" class="ai-btn" :disabled="aiLoading" @click="onAIComplete">
          <span class="ai-icon">🤖</span>
          <span class="ai-text">{{ aiLoading ? 'AI 生成中...' : 'AI 帮我完善详情' }}</span>
        </button>
        <button type="button" class="submit-btn" :disabled="publishing" @click="onSubmit">
          {{ publishing ? '发布中...' : '发布搭子' }}
        </button>
      </footer>
    </div>

    <input ref="fileInput" type="file" accept="image/*" class="hidden-input" @change="onFileChange" />

    <van-dialog
      v-model:show="showLocationDialog"
      title="集合地点"
      show-cancel-button
      @confirm="confirmLocation"
    >
      <van-field v-model="locationDraft" placeholder="输入地点名称（H5 暂用手动输入）" />
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { showToast, showConfirmDialog, showLoadingToast, closeToast } from 'vant';
import { post, uploadFile, get, getApiErrorMessage } from '@/utils/request';
import { PREFERENCE_TAGS, MAX_SELECT } from '@/utils/partnerPreferenceTags';
import { useAuthStore } from '@/stores/auth';
import { resolveMediaUrl } from '@/utils/mediaUrl';
import { getApiBaseUrl } from '@/config/env';
import PageNavBar from '@/components/PageNavBar.vue';

const router = useRouter();
const auth = useAuthStore();

const SCOPE_BIT = { public: 1, colleague: 2, alumni: 4 };

const partnerTypes = [
  '宠物搭子', '电影搭子', '音乐搭子', '逛街搭子', '运动搭子',
  '摄影搭子', '干饭搭子', '旅游搭子', 'k歌搭子', '喝酒搭子',
  '桌游搭子', '钓鱼搭子', '游戏搭子', '聊天搭子', '户外搭子'
];

const MEMBER_COUNT_MIN = 1;
const MEMBER_COUNT_MAX = 20;

const scopeOptions = [
  { value: 'public', label: '公开' },
  { value: 'colleague', label: '同事' },
  { value: 'alumni', label: '校友' }
];

const fileInput = ref(null);
const publishing = ref(false);
const aiLoading = ref(false);
const coverPreview = ref('');
const selectedPreferences = ref([]);
const hasCompany = ref(false);
const hasSchool = ref(false);
const scopeSelected = reactive({ public: true, colleague: false, alumni: false });
const showLocationDialog = ref(false);
const locationDraft = ref('');
const form = reactive({
  title: '',
  partnerType: '',
  description: '',
  locationDisplay: '',
  planDate: '',
  planTimeStr: '',
  planEndDate: '',
  planEndTimeStr: '',
  memberCount: MEMBER_COUNT_MIN,
  coverImageUrl: '',
  latitude: null,
  longitude: null
});

const planDateStart = computed(() => todayISO());

const titleLength = computed(() => form.title.length);
const descLength = computed(() => form.description.length);

const hasPlanTime = computed(() => {
  return !!(form.planDate || form.planTimeStr || form.planEndDate || form.planEndTimeStr);
});

const coverDisplayUrl = computed(() => {
  if (!form.coverImageUrl) {
    return '';
  }
  return resolveMediaUrl(form.coverImageUrl, { baseUrl: getApiBaseUrl(), fallback: '' });
});

function todayISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** 唤起原生 date/time 选择器（兼容 iOS / 桌面：opacity 0 的 input 常无法点击） */
function openNativePicker(input) {
  if (!input || input.disabled) {
    return;
  }
  if (typeof input.showPicker === 'function') {
    try {
      input.showPicker();
      return;
    } catch {
      /* 部分浏览器在无用户手势或不可见时会抛错，走 fallback */
    }
  }
  input.focus();
  input.click();
}

function onPickerBoxTap(e) {
  const input = e.currentTarget?.querySelector('input.picker-native');
  openNativePicker(input);
}

function onPickerInputTap(e) {
  openNativePicker(e.currentTarget);
}

function onTitleInput() {
  if (form.title.length > 50) {
    form.title = form.title.slice(0, 50);
  }
}

function onDescInput() {
  if (form.description.length > 300) {
    form.description = form.description.slice(0, 300);
  }
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

function showLimitToast(message) {
  showToast({
    message,
    position: 'middle',
    zIndex: 10001,
    duration: 2200,
    className: 'app-toast'
  });
}

function togglePreference(label) {
  const idx = selectedPreferences.value.indexOf(label);
  if (idx >= 0) {
    selectedPreferences.value.splice(idx, 1);
  } else if (selectedPreferences.value.length >= MAX_SELECT) {
    showLimitToast(`最多只能选择 ${MAX_SELECT} 个偏好`);
  } else {
    selectedPreferences.value.push(label);
  }
}

function clampMemberCount(value) {
  const n = Math.floor(Number(value));
  if (!Number.isFinite(n)) {
    return MEMBER_COUNT_MIN;
  }
  return Math.min(MEMBER_COUNT_MAX, Math.max(MEMBER_COUNT_MIN, n));
}

function normalizeMemberCount() {
  form.memberCount = clampMemberCount(form.memberCount);
}

function decreaseCount() {
  if (form.memberCount > MEMBER_COUNT_MIN) {
    form.memberCount -= 1;
  }
}

function increaseCount() {
  if (form.memberCount < MEMBER_COUNT_MAX) {
    form.memberCount += 1;
  }
}

async function toggleScope(key) {
  if (scopeSelected[key]) {
    const kept = Object.keys(scopeSelected).filter((k) => scopeSelected[k]);
    if (kept.length <= 1) {
      showToast('至少保留一种可见范围');
      return;
    }
    scopeSelected[key] = false;
    return;
  }
  if (key === 'colleague' && !hasCompany.value) {
    try {
      await showConfirmDialog({
        title: '需要先加入公司',
        message: '选择「同事」可见前，请先在个人中心完成「加入公司」。',
        confirmButtonText: '去加入'
      });
      router.push({ name: 'join-company' });
    } catch {
      /* cancel */
    }
    return;
  }
  if (key === 'alumni' && !hasSchool.value) {
    try {
      await showConfirmDialog({
        title: '需要先加入学校',
        message: '选择「校友」可见前，请先在个人中心完成「加入学校」。',
        confirmButtonText: '去加入'
      });
      router.push({ name: 'join-school' });
    } catch {
      /* cancel */
    }
    return;
  }
  scopeSelected[key] = true;
}

function pickCover() {
  fileInput.value?.click();
}

function clearCover() {
  coverPreview.value = '';
  form.coverImageUrl = '';
}

function onPickLocation() {
  locationDraft.value = form.locationDisplay;
  showLocationDialog.value = true;
}

function confirmLocation() {
  form.locationDisplay = locationDraft.value.trim();
}

function clearLocation() {
  form.locationDisplay = '';
  form.latitude = null;
  form.longitude = null;
}

function clearPlanTime() {
  form.planDate = '';
  form.planTimeStr = '';
  form.planEndDate = '';
  form.planEndTimeStr = '';
}

async function onFileChange(e) {
  const file = e.target.files?.[0];
  if (!file) {
    return;
  }
  coverPreview.value = URL.createObjectURL(file);
  showLoadingToast({ message: '上传中', forbidClick: true });
  try {
    const res = await uploadFile('/api/partner/upload-image', file, 'file');
    const url = res.data?.url || (typeof res.data === 'string' ? res.data : '');
    if (url) {
      form.coverImageUrl = url;
      showToast('封面已上传');
    } else {
      showToast('上传失败');
    }
  } catch (err) {
    showToast(getApiErrorMessage(err, '上传失败'));
  } finally {
    closeToast();
  }
}

function buildPlanTimeIso() {
  let date = form.planDate;
  if (form.planTimeStr && !date) {
    date = todayISO();
    form.planDate = date;
  }
  if (!date || !form.planTimeStr) {
    return null;
  }
  return `${date}T${form.planTimeStr}:00`;
}

function buildPlanEndTimeIso() {
  let endDate = form.planEndDate;
  if (form.planEndTimeStr && !endDate) {
    endDate = form.planDate || todayISO();
    form.planEndDate = endDate;
  }
  if (!endDate || !form.planEndTimeStr) {
    return null;
  }
  return `${endDate}T${form.planEndTimeStr}:00`;
}

function buildAiHints() {
  const planIso = buildPlanTimeIso();
  const endIso = buildPlanEndTimeIso();
  let planTimeHint = '';
  if (planIso && endIso) {
    planTimeHint = `${planIso.replace('T', ' ').substring(0, 16)} ～ ${endIso.replace('T', ' ').substring(0, 16)}`;
  } else if (planIso) {
    planTimeHint = planIso.replace('T', ' ').substring(0, 16);
  }
  return {
    planTimeHint,
    locationHint: form.locationDisplay || ''
  };
}

async function onAIComplete() {
  if (!form.title.trim() || !form.partnerType) {
    showToast('请先填写标题和类型');
    return;
  }
  const hints = buildAiHints();
  aiLoading.value = true;
  showLoadingToast({ message: 'AI 生成中...', forbidClick: true });
  try {
    const res = await post('/api/partner/ai/description', {
      title: form.title.trim(),
      typeName: form.partnerType,
      preference: selectedPreferences.value.join('、'),
      currentDesc: form.description || '',
      planTimeHint: hints.planTimeHint || undefined,
      locationHint: hints.locationHint || undefined
    });
    const text = String(res.data || '');
    const clipped = text.length > 300 ? text.substring(0, 300) : text;
    form.description = clipped;
    showToast('已生成详情');
  } catch (e) {
    showToast(getApiErrorMessage(e, '生成失败'));
  } finally {
    aiLoading.value = false;
    closeToast();
  }
}

function validate() {
  if (!form.title.trim()) {
    showToast('请输入标题');
    return false;
  }
  if (!form.partnerType) {
    showToast('请选择搭子类型');
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
  if (scopeSelected.colleague && !hasCompany.value) {
    showToast('请先加入公司后再勾选同事可见');
    return false;
  }
  if (scopeSelected.alumni && !hasSchool.value) {
    showToast('请先加入学校后再勾选校友可见');
    return false;
  }
  const endIso = buildPlanEndTimeIso();
  const startIso = buildPlanTimeIso();
  if (endIso && !startIso) {
    showToast('选择结束时间前请先选开始时间');
    return false;
  }
  if (startIso && endIso) {
    const t0 = new Date(startIso.replace(' ', 'T'));
    const t1 = new Date(endIso.replace(' ', 'T'));
    if (Number.isFinite(t0.getTime()) && Number.isFinite(t1.getTime()) && t1 < t0) {
      showToast('结束时间不能早于开始时间');
      return false;
    }
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
    targetCount: clampMemberCount(form.memberCount)
  };
  const planIso = buildPlanTimeIso();
  const planEndIso = buildPlanEndTimeIso();
  if (planIso) {
    body.planTime = planIso;
  }
  if (planEndIso) {
    body.planEndTime = planEndIso;
  }
  if (form.locationDisplay.trim()) {
    body.location = form.locationDisplay.trim();
  }
  if (form.coverImageUrl) {
    body.coverImage = form.coverImageUrl;
  }
  if (form.latitude != null && form.longitude != null) {
    body.latitude = form.latitude;
    body.longitude = form.longitude;
  }
  publishing.value = true;
  showLoadingToast({ message: '发布中', forbidClick: true });
  try {
    await post('/api/partner', body);
    showToast('发布成功');
    router.replace({ name: 'partner' });
  } catch (e) {
    showToast(getApiErrorMessage(e, '发布失败'));
  } finally {
    publishing.value = false;
    closeToast();
  }
}

onMounted(loadOrgFlags);
</script>

<style scoped>
.publish {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: linear-gradient(180deg, #f4fbf9 0%, #eef8f5 120px, var(--bg-color) 220px);
}

.publish-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.publish-form {
  flex: 1;
  padding: 12px var(--page-horizontal) 20px;
}

.form-card {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 14px;
  padding: 16px 14px 4px;
  margin-bottom: 12px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 4px 20px rgba(74, 154, 144, 0.08);
  backdrop-filter: blur(6px);
}

.form-card--settings {
  padding-bottom: 8px;
}

.form-item {
  margin-bottom: 18px;
}

.form-item--last {
  margin-bottom: 12px;
}

.form-item--counter {
  margin-bottom: 16px;
}

.form-item--scope {
  margin-bottom: 4px;
}

.form-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.form-label {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
}

.form-count {
  font-size: 12px;
  color: #999;
}

.form-count--full {
  color: #e85d5d;
  font-weight: 600;
}

.form-input,
.form-textarea,
.picker-box,
.location-pick,
.counter-panel {
  width: 100%;
  box-sizing: border-box;
  background: #fafcfc;
  border-radius: 10px;
  border: 1px solid #e8eeec;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}

.form-input,
.form-textarea {
  padding: 13px 14px;
  font-size: 15px;
  color: #333;
  font-family: inherit;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: rgba(95, 179, 168, 0.55);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(95, 179, 168, 0.12);
}

.form-input::placeholder,
.form-textarea::placeholder {
  color: #b0b8b5;
}

.form-hint {
  display: block;
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
  line-height: 1.45;
}

.form-link {
  border: none;
  background: none;
  font-size: 13px;
  color: var(--primary-color);
  cursor: pointer;
  padding: 0;
}

.datetime-row {
  display: flex;
  gap: 10px;
}

.picker-box {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 46px;
  padding: 13px 10px;
  text-align: center;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.picker-box:active {
  background: #f0f7f5;
}

.picker-text {
  position: relative;
  z-index: 1;
  font-size: 14px;
  color: #b0b8b5;
  pointer-events: none;
  user-select: none;
}

.picker-native {
  position: absolute;
  inset: 0;
  z-index: 2;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  border: none;
  opacity: 0.01;
  cursor: pointer;
  font-size: 16px;
  background: transparent;
  color: transparent;
}

.picker-text--filled {
  color: #333;
  font-weight: 500;
}

.location-pick {
  padding: 13px 14px;
  font-size: 14px;
  color: #333;
  line-height: 1.45;
  cursor: pointer;
}

.location-pick:active {
  background: #f0f7f5;
}

.location-placeholder {
  color: #999;
}

.type-tags {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 10px;
}

.pref-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.scope-tags {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 10px;
}

.pref-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border: 1px solid #e8eeec;
  border-radius: 18px;
  background: #fafcfc;
  font-family: inherit;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  transition: all 0.15s ease;
  -webkit-tap-highlight-color: transparent;
}

.pref-tag.active {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #fff;
  box-shadow: 0 2px 8px rgba(95, 179, 168, 0.28);
}

.pref-tag-ico {
  font-size: 14px;
  line-height: 1;
}

.type-tag {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 8px 6px;
  border: 1px solid #e8eeec;
  border-radius: 18px;
  background: #fafcfc;
  font-family: inherit;
  font-size: 13px;
  color: #666;
  line-height: 1.2;
  cursor: pointer;
  transition: all 0.15s ease;
  -webkit-tap-highlight-color: transparent;
}

.type-tag.active {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #fff;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(95, 179, 168, 0.28);
}

.scope-tag {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 40px;
  padding: 10px 8px;
  box-sizing: border-box;
  border: 1px solid #e5e5e5;
  border-radius: 20px;
  background: #fff;
  font-family: inherit;
  font-size: 14px;
  color: #666;
  line-height: 1.2;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;
  -webkit-tap-highlight-color: transparent;
}

.scope-tag.active {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #fff;
  font-weight: 500;
}

.scope-tag.disabled:not(.active) {
  opacity: 0.45;
  cursor: not-allowed;
  background: #f8f8f8;
}

.required-mark {
  color: #ff5a5f;
}

.form-optional {
  font-size: 12px;
  color: var(--text-tertiary);
  font-weight: 400;
}

.form-item--counter .form-hint {
  margin-bottom: 10px;
}

.counter-panel {
  margin-top: 0;
  padding: 18px 14px;
}

.scope-hint {
  display: block;
  margin-top: 10px;
  padding: 0 2px;
  font-size: 12px;
  color: var(--text-tertiary);
  line-height: 1.45;
}

.form-textarea {
  min-height: 120px;
  resize: vertical;
  line-height: 1.55;
}

.counter {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 4px 0;
}

.counter-btn {
  width: 40px;
  height: 40px;
  background: var(--primary-color);
  border: none;
  border-radius: 50%;
  font-size: 20px;
  color: #fff;
  cursor: pointer;
  line-height: 1;
}

.counter-btn.disabled {
  background: #e5e5e5;
  color: #999;
  cursor: not-allowed;
}

/* 与 form-input 同系：浅底 + 描边，虚线内框提示可编辑 */
.counter-input-field {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 128px;
  padding: 6px 12px 6px 8px;
  background: #fafcfc;
  border: 1px solid #e8eeec;
  border-radius: 10px;
  cursor: text;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}

.counter-input-field:focus-within {
  border-color: rgba(95, 179, 168, 0.55);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(95, 179, 168, 0.12);
}

.count-input {
  width: 60px;
  min-height: 44px;
  padding: 8px 6px;
  box-sizing: border-box;
  border: 1.5px dashed #a8cfc6;
  border-radius: 8px;
  background: #fff;
  font-family: inherit;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: #1a1a1a;
  text-align: center;
  caret-color: var(--primary-color);
  cursor: text;
  outline: none;
  -moz-appearance: textfield;
  appearance: textfield;
  transition: border-color 0.15s ease, border-style 0.15s ease, box-shadow 0.15s ease;
}

.count-input::placeholder {
  color: #b8c4c0;
  font-weight: 500;
}

.count-input::-webkit-outer-spin-button,
.count-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.count-input:focus {
  border-style: solid;
  border-color: var(--primary-color);
  box-shadow: inset 0 0 0 1px rgba(95, 179, 168, 0.15);
}

.count-unit {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-secondary);
  flex-shrink: 0;
  user-select: none;
}

.publish-footer {
  flex-shrink: 0;
  padding: 12px var(--page-horizontal) calc(16px + var(--safe-bottom));
  background: linear-gradient(180deg, rgba(245, 250, 249, 0) 0%, rgba(245, 250, 249, 0.92) 24%, #f5faf9 100%);
  border-top: 1px solid rgba(95, 179, 168, 0.1);
  backdrop-filter: blur(10px);
}

.ai-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 13px 16px;
  border: none;
  background: linear-gradient(135deg, #6b7fd7 0%, #7b5bb8 50%, #8e6cc0 100%);
  border-radius: 26px;
  margin-bottom: 12px;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(107, 127, 215, 0.35);
  transition: transform 0.12s ease, opacity 0.12s ease;
  -webkit-tap-highlight-color: transparent;
}

.ai-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.ai-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.ai-icon {
  font-size: 18px;
}

.ai-text {
  font-size: 15px;
  color: #fff;
  font-weight: 500;
}

.submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 50px;
  border: none;
  background: linear-gradient(135deg, #5fb3a8 0%, #6ec4b8 100%);
  border-radius: 26px;
  font-size: 17px;
  color: #fff;
  font-weight: 600;
  letter-spacing: 0.02em;
  cursor: pointer;
  box-shadow: 0 4px 18px rgba(95, 179, 168, 0.4);
  transition: transform 0.12s ease, opacity 0.12s ease;
  -webkit-tap-highlight-color: transparent;
}

.submit-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.submit-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.cover-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
}

.cover-preview {
  position: relative;
  width: 100%;
  height: 168px;
  border-radius: 12px;
  overflow: hidden;
  background: #eef5f3;
  border: 1px solid #e0ebe8;
  cursor: pointer;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-tip {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 6px;
  font-size: 11px;
  color: #fff;
  text-align: center;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.55));
}

.cover-placeholder {
  width: 100%;
  height: 168px;
  border-radius: 12px;
  border: 1.5px dashed rgba(95, 179, 168, 0.45);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: linear-gradient(145deg, #f8fcfb 0%, #eef6f4 100%);
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.cover-placeholder:active {
  background: #e8f4f1;
  border-color: var(--primary-color);
}

.cover-plus {
  font-size: 28px;
  line-height: 1;
  color: var(--primary-color);
  font-weight: 300;
}

.cover-upload-text {
  font-size: 14px;
  color: var(--primary-dark);
}

.cover-clear {
  padding: 8px 14px;
  border: 1px solid #e8eeec;
  border-radius: 18px;
  background: #fff;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
}

.hidden-input {
  display: none;
}
</style>
