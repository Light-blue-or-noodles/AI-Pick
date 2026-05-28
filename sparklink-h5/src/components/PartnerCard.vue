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
          :url="item.author?.avatar"
          default-src="/images/default-avatar.png"
          custom-class="partner-card__avatar"
          alt=""
        />
        <div class="partner-card__nickname" aria-label="发布者昵称">
          <span
            v-for="(line, index) in nicknameLines"
            :key="index"
            class="partner-card__nickname-line"
          >
            {{ line }}
          </span>
        </div>
        <p v-if="addressText" class="partner-card__address" :title="addressText">
          {{ addressText }}
        </p>
      </div>
      <div v-if="deletable" class="partner-card__actions" @click.stop>
        <button type="button" class="partner-card__delete" @click="$emit('delete')">
          删除
        </button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue';
import NetworkImage from '@/components/NetworkImage.vue';
import { formatCardNicknameLines, formatCardAddressText } from '@/utils/partnerCardDisplay';

const props = defineProps({
  item: { type: Object, required: true },
  deletable: { type: Boolean, default: false }
});

defineEmits(['click', 'delete']);

const statusClass = computed(() => `partner-card__status--${props.item.status}`);

const nicknameLines = computed(() => formatCardNicknameLines(props.item.author?.name));

const addressText = computed(() => {
  return formatCardAddressText(props.item.distance || props.item.address || props.item.location);
});
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
  line-height: 1.35;
}

.partner-card__meta,
.partner-card__pref {
  margin: 0 0 4px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.4;
}

.partner-card__footer {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--border-color);
}

.partner-card__footer :deep(.partner-card__avatar) {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  margin-top: 1px;
}

.partner-card__nickname {
  flex-shrink: 0;
  width: 4.2em;
  font-size: 12px;
  line-height: 1.35;
  color: var(--text-secondary);
}

.partner-card__nickname-line {
  display: block;
  max-width: 4.2em;
  overflow: hidden;
  white-space: nowrap;
}

.partner-card__address {
  flex: 1;
  min-width: 0;
  margin: 0;
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-tertiary);
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
  word-break: break-all;
}

.partner-card__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.partner-card__delete {
  padding: 7px 18px;
  border: none;
  border-radius: 16px;
  background: rgba(255, 77, 79, 0.12);
  color: #ff4d4f;
  font-family: inherit;
  font-size: 14px;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.partner-card__delete:active {
  background: rgba(255, 77, 79, 0.2);
}
</style>
