<template>
  <div class="settings-sub page page--no-tab">
    <PageNavBar title="隐私设置" />
    <van-cell-group inset>
      <van-cell title="允许陌生人查看资料">
        <template #right-icon>
          <van-switch v-model="profilePublic" size="20px" active-color="#5FB3A8" @change="save" />
        </template>
      </van-cell>
      <van-cell title="允许接收搭子推荐">
        <template #right-icon>
          <van-switch v-model="allowRecommend" size="20px" active-color="#5FB3A8" @change="save" />
        </template>
      </van-cell>
    </van-cell-group>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getItem, setItem } from '@/utils/storage';

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
