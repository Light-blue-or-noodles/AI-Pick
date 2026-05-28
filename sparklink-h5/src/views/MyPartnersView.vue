<template>
  <div class="my-list page page--no-tab">
    <PageNavBar title="我发布的搭子" />
    <van-pull-refresh v-model="refreshing" @refresh="load">
      <van-loading v-if="loading && !partners.length" class="loading-center" />
      <div v-else class="my-list__body">
        <PartnerCard
          v-for="p in partners"
          :key="p.id"
          :item="p"
          @click="$router.push({ name: 'partner-detail', params: { id: p.id } })"
        />
        <p v-if="!loading && !partners.length" class="empty-hint">暂无发布的搭子</p>
      </div>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { get } from '@/utils/request';
import { getApiBaseUrl } from '@/config/env';
import { mapPartnerForList } from '@/utils/partnerListMap';
import PageNavBar from '@/components/PageNavBar.vue';
import PartnerCard from '@/components/PartnerCard.vue';

const partners = ref([]);
const loading = ref(false);
const refreshing = ref(false);

async function load() {
  loading.value = true;
  try {
    const res = await get('/api/partner/my');
    const raw = Array.isArray(res.data) ? res.data : res.data?.records || [];
    partners.value = raw.map((item) => mapPartnerForList(item, getApiBaseUrl()));
  } catch {
    partners.value = [];
  } finally {
    loading.value = false;
    refreshing.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.my-list__body {
  padding: 12px var(--page-horizontal);
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 48px;
}
</style>
