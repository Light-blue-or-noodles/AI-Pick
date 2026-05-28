<template>
  <div class="partner-list-skeleton" :style="{ padding }" aria-hidden="true">
    <div v-for="i in count" :key="i" class="partner-list-skeleton__card">
      <span class="partner-list-skeleton__banner" />
      <span class="partner-list-skeleton__line partner-list-skeleton__line--short" />
      <span class="partner-list-skeleton__line" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  count: { type: Number, default: 3 },
  padding: { type: String, default: '12px 16px' }
});
</script>

<style scoped>
.partner-list-skeleton__card {
  margin-bottom: 12px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 2px 12px rgba(74, 154, 144, 0.06);
}

.partner-list-skeleton__banner,
.partner-list-skeleton__line {
  display: block;
  border-radius: 8px;
  background: linear-gradient(90deg, #e8f2f0 25%, #f4faf9 50%, #e8f2f0 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.2s ease-in-out infinite;
}

.partner-list-skeleton__banner {
  width: 100%;
  height: 120px;
  margin-bottom: 12px;
}

.partner-list-skeleton__line {
  width: 100%;
  height: 14px;
  margin-bottom: 8px;
}

.partner-list-skeleton__line--short {
  width: 55%;
}

@keyframes skeleton-shimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
}
</style>
