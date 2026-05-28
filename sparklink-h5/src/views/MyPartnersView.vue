<template>
  <div class="my-list page page--no-tab">
    <PageNavBar title="我发布的搭子" />
    <van-pull-refresh v-model="refreshing" @refresh="onPullRefresh">
      <PartnerListSkeleton v-if="pending && !partners.length" />
      <div v-else class="my-list__body">
        <PartnerCard
          v-for="p in partners"
          :key="p.id"
          :item="p"
          deletable
          @click="$router.push({ name: 'partner-detail', params: { id: p.id } })"
          @delete="onDeletePartner(p)"
        />
        <p v-if="!pending && !partners.length" class="empty-hint">暂无发布的搭子</p>
      </div>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { showConfirmDialog, showToast } from 'vant';
import { get, del, getApiErrorMessage } from '@/utils/request';
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
    const res = await get('/api/partner/my');
    const raw = Array.isArray(res.data) ? res.data : res.data?.records || [];
    return raw.map((item) => mapPartnerForList(item, getApiBaseUrl()));
  },
  {
    cacheKey: () => (auth.userId ? `my-partners:${auth.userId}` : null),
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

async function onDeletePartner(partner) {
  if (!partner?.id) {
    return;
  }
  try {
    await showConfirmDialog({
      title: '删除搭子',
      message: '确定删除该搭子？删除后无法恢复。'
    });
    await del(`/api/partner/${partner.id}`);
    showToast({
      message: '已删除',
      position: 'middle',
      zIndex: 10001
    });
    await refresh();
  } catch (e) {
    if (e === 'cancel' || e?.message === 'cancel') {
      return;
    }
    showToast({
      message: getApiErrorMessage(e, '删除失败'),
      position: 'middle',
      zIndex: 10001
    });
  }
}
</script>

<style scoped>
.my-list__body {
  padding: 12px var(--page-horizontal);
}
</style>
