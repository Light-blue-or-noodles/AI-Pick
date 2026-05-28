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
      <button type="button" class="partner-filter-btn" @click="$router.push({ name: 'partner-filter' })">
        筛选
      </button>
    </div>
    <van-pull-refresh v-model="refreshing" @refresh="loadPartners">
      <van-loading v-if="loading && !displayPartners.length" class="loading-center" />
      <div v-else class="partner__list">
        <PartnerCard
          v-for="p in displayPartners"
          :key="p.id"
          :item="p"
          @click="goDetail(p.id)"
        />
        <p v-if="!loading && !displayPartners.length" class="empty-hint">暂无搭子</p>
      </div>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, inject, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { get } from '@/utils/request';
import { getApiBaseUrl } from '@/config/env';
import { mapPartnerForList } from '@/utils/partnerListMap';
import { loadPartnerFilter } from '@/composables/usePartnerFilter';
import { applyPartnerFilter } from '@/utils/applyPartnerFilter';
import PartnerCard from '@/components/PartnerCard.vue';

const router = useRouter();
const route = useRoute();
const tabReselectKey = inject('tabReselectKey', null);

const scopeTabs = [
  { key: 'platform', label: 'Pick搭' },
  { key: 'company', label: '同事搭' },
  { key: 'school', label: '校友搭' }
];

const activeScope = ref(0);
const partners = ref([]);
const loading = ref(false);
const refreshing = ref(false);
const filter = ref(loadPartnerFilter());

const displayPartners = computed(() => applyPartnerFilter(partners.value, filter.value));

async function loadPartners() {
  loading.value = true;
  try {
    const scope = scopeTabs[activeScope.value]?.key || 'platform';
    const res = await get('/api/partner', { scopeType: scope });
    let rawList = [];
    if (Array.isArray(res.data)) {
      rawList = res.data;
    } else if (res.data) {
      rawList = res.data.records || [];
    }
    const base = getApiBaseUrl();
    partners.value = rawList.map((item) => mapPartnerForList(item, base));
  } catch {
    partners.value = [];
  } finally {
    loading.value = false;
    refreshing.value = false;
  }
}

function selectScope(index) {
  activeScope.value = index;
  loadPartners();
}

function goDetail(id) {
  router.push({ name: 'partner-detail', params: { id: String(id) } });
}

function refreshFilterAndList() {
  filter.value = loadPartnerFilter();
  loadPartners();
}

onMounted(refreshFilterAndList);
onActivated(refreshFilterAndList);

if (tabReselectKey) {
  watch(tabReselectKey, () => {
    if (route.name === 'partner') {
      refreshFilterAndList();
    }
  });
}
</script>

<style scoped>
.partner-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px var(--page-horizontal) 0;
  background: var(--bg-color);
  position: sticky;
  top: 0;
  z-index: 10;
}

.scope-tabs {
  display: flex;
  flex: 1;
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

.partner-filter-btn {
  flex-shrink: 0;
  margin-left: 8px;
  padding: 6px 12px;
  border: 1px solid var(--primary-color);
  border-radius: var(--radius-full);
  background: #fff;
  color: var(--primary-color);
  font-size: 13px;
  cursor: pointer;
}

.partner__list {
  padding: 12px var(--page-horizontal);
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 48px;
}
</style>
