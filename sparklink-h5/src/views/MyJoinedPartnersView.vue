<template>
  <div class="my-list page page--no-tab">
    <PageNavBar title="我参加的搭子" />
    <van-pull-refresh v-model="refreshing" @refresh="onPullRefresh">
      <PartnerListSkeleton v-if="pending && !partners.length" />
      <div v-else class="my-list__body">
        <PartnerCard
          v-for="p in partners"
          :key="p.id"
          :item="p"
          @click="$router.push({ name: 'partner-detail', params: { id: p.id } })"
        />
        <p v-if="!pending && !partners.length" class="empty-hint">暂无参加的搭子</p>
      </div>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { get } from '@/utils/request';
import { getApiBaseUrl } from '@/config/env';
import { mapPartnerForList } from '@/utils/partnerListMap';
import { usePageLoad } from '@/composables/usePageLoad';
import { useAuthStore } from '@/stores/auth';
import PageNavBar from '@/components/PageNavBar.vue';
import PartnerCard from '@/components/PartnerCard.vue';
import PartnerListSkeleton from '@/components/skeleton/PartnerListSkeleton.vue';

const auth = useAuthStore();
const refreshing = ref(false);

const { data: partners, pending, refresh } = usePageLoad(
  async () => {
    const res = await get('/api/partner/my/joined');
    const raw = Array.isArray(res.data) ? res.data : res.data?.records || [];
    return raw.map((item) => mapPartnerForList(item, getApiBaseUrl()));
  },
  {
    cacheKey: () => (auth.userId ? `my-joined-partners:${auth.userId}` : null),
    empty: []
  }
);

async function onPullRefresh() {
  refreshing.value = true;
  try {
    await refresh();
  } finally {
    refreshing.value = false;
  }
}
</script>

<style scoped>
.my-list__body {
  padding: 12px var(--page-horizontal);
}
</style>
