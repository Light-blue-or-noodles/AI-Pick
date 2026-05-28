<template>
  <div class="partner page">
    <div class="partner-header">
      <div class="scope-tabs">
        <button
          v-for="(tab, i) in scopeTabs"
          :key="tab.key"
          type="button"
          class="scope-tab"
          :class="{ 'scope-tab--active': activeScope === i }"
          @click="selectScope(i)"
        >
          <span>{{ tab.label }}</span>
          <span v-if="activeScope === i" class="scope-tab__line" />
        </button>
      </div>
    </div>
    <van-pull-refresh v-model="refreshing" @refresh="onPullRefresh">
      <PartnerListSkeleton v-if="pending && !partners.length" />
      <div v-else class="partner__list">
        <PartnerCard
          v-for="p in partners"
          :key="p.id"
          :item="p"
          @click="goDetail(p.id)"
        />
        <p v-if="!pending && !partners.length" class="empty-hint">暂无搭子</p>
      </div>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, computed, watch, inject } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { get } from '@/utils/request';
import { getApiBaseUrl } from '@/config/env';
import { mapPartnerForList } from '@/utils/partnerListMap';
import { usePageLoad } from '@/composables/usePageLoad';
import PartnerCard from '@/components/PartnerCard.vue';
import PartnerListSkeleton from '@/components/skeleton/PartnerListSkeleton.vue';

const router = useRouter();
const route = useRoute();
const tabReselectKey = inject('tabReselectKey', null);

const scopeTabs = [
  { key: 'platform', label: 'Pick搭' },
  { key: 'company', label: '同事搭' },
  { key: 'school', label: '校友搭' }
];

const activeScope = ref(0);
const refreshing = ref(false);

const scopeKey = computed(() => scopeTabs[activeScope.value]?.key || 'platform');

const { data: partners, pending, load, refresh } = usePageLoad(
  async () => {
    const res = await get('/api/partner', { scopeType: scopeKey.value });
    let rawList = [];
    if (Array.isArray(res.data)) {
      rawList = res.data;
    } else if (res.data) {
      rawList = res.data.records || [];
    }
    const base = getApiBaseUrl();
    return rawList.map((item) => mapPartnerForList(item, base));
  },
  {
    cacheKey: () => `partner-list:${scopeKey.value}`,
    empty: [],
    reloadOnActivated: true
  }
);

async function onPullRefresh() {
  refreshing.value = true;
  try {
    await refresh();
  } catch {
    /* ignore */
  } finally {
    refreshing.value = false;
  }
}

function selectScope(index) {
  if (activeScope.value === index) {
    return;
  }
  activeScope.value = index;
  load();
}

function goDetail(id) {
  router.push({ name: 'partner-detail', params: { id: String(id) } });
}

if (tabReselectKey) {
  watch(tabReselectKey, () => {
    if (route.name === 'partner') {
      refresh().catch(() => {});
    }
  });
}
</script>

<style scoped>
.partner-header {
  padding: 8px var(--page-horizontal) 0;
  background: var(--bg-color);
  position: sticky;
  top: 0;
  z-index: 10;
}

.scope-tabs {
  display: flex;
  gap: 4px;
}

.scope-tab {
  flex: 1;
  position: relative;
  border: none;
  background: none;
  padding: 12px 4px 10px;
  font-size: 15px;
  color: var(--text-secondary);
  cursor: pointer;
}

.scope-tab--active {
  color: var(--primary-color);
  font-weight: 600;
}

.scope-tab__line {
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  border-radius: 2px;
  background: var(--primary-color);
}

.partner__list {
  padding: 12px var(--page-horizontal);
}
</style>
