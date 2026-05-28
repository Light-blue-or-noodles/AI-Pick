<template>
  <div class="list-rows-skeleton" aria-hidden="true">
    <div v-for="i in rows" :key="i" class="list-rows-skeleton__row">
      <span class="list-rows-skeleton__avatar" />
      <span class="list-rows-skeleton__text" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  rows: { type: Number, default: 4 }
});
</script>

<style scoped>
.list-rows-skeleton {
  margin: 16px;
  padding: 4px 0;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.list-rows-skeleton__row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.list-rows-skeleton__row:last-child {
  border-bottom: none;
}

.list-rows-skeleton__avatar,
.list-rows-skeleton__text {
  border-radius: 999px;
  background: linear-gradient(90deg, #ececec 25%, #f8f8f8 50%, #ececec 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.2s ease-in-out infinite;
}

.list-rows-skeleton__avatar {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  border-radius: 50%;
}

.list-rows-skeleton__text {
  flex: 1;
  height: 16px;
  max-width: 140px;
  border-radius: 6px;
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
