<template>
  <div class="settings-sub page page--no-tab">
    <PageNavBar title="隐私设置" />
    <div class="settings-sub__body">
      <div class="settings-sub__card">
        <div class="settings-sub__row settings-sub__row--static">
          <span class="settings-sub__label">允许陌生人查看资料</span>
          <van-switch
            v-model="profilePublic"
            size="22px"
            active-color="var(--primary-color)"
            @change="save"
          />
        </div>
        <div class="settings-sub__row settings-sub__row--static">
          <span class="settings-sub__label">允许接收搭子推荐</span>
          <van-switch
            v-model="allowRecommend"
            size="22px"
            active-color="var(--primary-color)"
            @change="save"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getItem, setItem } from '@/utils/storage';
import PageNavBar from '@/components/PageNavBar.vue';

const profilePublic = ref(true);
const allowRecommend = ref(true);

function load() {
  const raw = getItem('privacy_settings');
  if (raw) {
    try {
      const o = JSON.parse(raw);
      profilePublic.value = o.profilePublic !== false;
      allowRecommend.value = o.allowRecommend !== false;
    } catch {
      /* ignore */
    }
  }
}

function save() {
  setItem('privacy_settings', JSON.stringify({
    profilePublic: profilePublic.value,
    allowRecommend: allowRecommend.value
  }));
}

onMounted(load);
</script>

<style scoped>
.settings-sub {
  min-height: 100vh;
  background: var(--bg-color);
}

.settings-sub :deep(.van-nav-bar) {
  background: var(--bg-white);
}

.settings-sub__body {
  padding: 12px var(--page-horizontal) 24px;
}

.settings-sub__card {
  background: var(--bg-white);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(74, 154, 144, 0.06);
}

.settings-sub__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 52px;
  padding: 0 15px;
  border-bottom: 1px solid var(--border-color);
}

.settings-sub__row:last-child {
  border-bottom: none;
}

.settings-sub__label {
  flex: 1;
  min-width: 0;
  font-size: 15px;
  line-height: 1.35;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.settings-sub__row--static :deep(.van-switch) {
  flex-shrink: 0;
}
</style>
