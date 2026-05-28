<template>
  <article class="partner-card card" @click="$emit('click')">
    <div class="partner-card__cover">
      <NetworkImage
        :url="item.cover"
        default-src="/images/partner-banner.jpg"
        custom-class="partner-card__cover-img"
        alt=""
      />
      <span class="partner-card__status" :class="statusClass">{{ item.statusLabel }}</span>
    </div>
    <div class="partner-card__body">
      <h3 class="partner-card__title">{{ item.title }}</h3>
      <p class="partner-card__meta">{{ item.category }} · {{ item.members }}/{{ item.maxMembers }}人</p>
      <p v-if="item.preference" class="partner-card__pref">{{ item.preference }}</p>
      <div class="partner-card__footer">
        <NetworkImage
          v-if="item.author?.avatar"
          :url="item.author.avatar"
          default-src="/images/default-avatar.png"
          custom-class="partner-card__avatar"
          alt=""
        />
        <span class="partner-card__author">{{ item.author.name }}</span>
        <span v-if="item.distance" class="partner-card__dist">{{ item.distance }}</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue';
import NetworkImage from '@/components/NetworkImage.vue';

const props = defineProps({
  item: { type: Object, required: true }
});

defineEmits(['click']);

const statusClass = computed(() => `partner-card__status--${props.item.status}`);
</script>

<style scoped>
.partner-card {
  overflow: hidden;
  margin-bottom: 12px;
  cursor: pointer;
}

.partner-card__cover {
  height: 120px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #e8f6f5, #b8e0da);
}

.partner-card__cover :deep(.partner-card__cover-img) {
  width: 100%;
  height: 120px;
  object-fit: cover;
  display: block;
}

.partner-card__status {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.9);
  color: var(--primary-dark);
}

.partner-card__status--full {
  color: #fa8c16;
}

.partner-card__status--ended {
  color: var(--text-tertiary);
}

.partner-card__body {
  padding: 12px 14px;
}

.partner-card__title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
}

.partner-card__meta,
.partner-card__pref {
  margin: 0 0 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.partner-card__footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.partner-card__footer :deep(.partner-card__avatar) {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

.partner-card__dist {
  margin-left: auto;
}
</style>
