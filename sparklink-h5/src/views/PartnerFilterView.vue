<template>
  <div class="filter page page--no-tab">
    <PageNavBar title="搭子筛选" />
    <div class="filter__body">
      <section v-for="block in blocks" :key="block.key" class="filter__section">
        <h3>{{ block.title }}</h3>
        <div class="filter__options">
          <button
            v-for="opt in block.options"
            :key="opt.value"
            type="button"
            class="filter__opt"
            :class="{ 'filter__opt--active': filter[block.key] === opt.value }"
            @click="filter[block.key] = opt.value"
          >
            {{ opt.label }}
          </button>
        </div>
      </section>
    </div>
    <div class="filter__bar">
      <button type="button" class="filter__reset" @click="onReset">重置</button>
      <span class="filter__count">找到 {{ resultCount }} 个结果</span>
      <button type="button" class="filter__apply" @click="onApply">应用筛选</button>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import PageNavBar from '@/components/PageNavBar.vue';
import {
  defaultPartnerFilter,
  loadPartnerFilter,
  savePartnerFilter
} from '@/composables/usePartnerFilter';

const router = useRouter();
const filter = reactive(loadPartnerFilter());

const blocks = [
  {
    key: 'distance',
    title: '距离',
    options: [
      { value: 'all', label: '不限' },
      { value: '1km', label: '1km内' },
      { value: '3km', label: '3km内' },
      { value: '5km', label: '5km内' },
      { value: '10km', label: '10km内' }
    ]
  },
  {
    key: 'gender',
    title: '性别',
    options: [
      { value: 'all', label: '不限' },
      { value: 'male', label: '男' },
      { value: 'female', label: '女' }
    ]
  },
  {
    key: 'partnerType',
    title: '搭子类型',
    options: [
      { value: 'all', label: '全部' },
      { value: 'game', label: '游戏' },
      { value: 'sports', label: '运动' },
      { value: 'food', label: '美食' },
      { value: 'study', label: '学习' },
      { value: 'travel', label: '旅行' },
      { value: 'other', label: '其他' }
    ]
  },
  {
    key: 'matchLevel',
    title: '匹配度',
    options: [
      { value: 'all', label: '不限' },
      { value: '90', label: '90%以上' },
      { value: '80', label: '80%以上' },
      { value: '70', label: '70%以上' }
    ]
  },
  {
    key: 'partnerStatus',
    title: '搭子状态',
    options: [
      { value: 'all', label: '全部' },
      { value: 'recruiting', label: '招募中' },
      { value: 'full', label: '已满' }
    ]
  }
];

const resultCount = computed(() => {
  const active = Object.values(filter).filter((v) => v !== 'all').length;
  return Math.max(10, 50 - active * 8);
});

function onReset() {
  Object.assign(filter, defaultPartnerFilter());
}

function onApply() {
  savePartnerFilter({ ...filter });
  router.back();
}
</script>

<style scoped>
.filter__body {
  padding: 12px var(--page-horizontal) 88px;
}

.filter__section {
  margin-bottom: 20px;
}

.filter__section h3 {
  margin: 0 0 10px;
  font-size: 15px;
}

.filter__options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter__opt {
  padding: 8px 14px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-color);
  background: #fff;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
}

.filter__opt--active {
  border-color: var(--primary-color);
  background: var(--primary-bg);
  color: var(--primary-dark);
}

.filter__bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px var(--page-horizontal) calc(12px + var(--safe-bottom));
  background: #fff;
  border-top: 1px solid var(--border-color);
}

.filter__reset {
  border: none;
  background: none;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
}

.filter__count {
  flex: 1;
  font-size: 13px;
  color: var(--text-tertiary);
  text-align: center;
}

.filter__apply {
  padding: 10px 20px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--primary-color);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
</style>
