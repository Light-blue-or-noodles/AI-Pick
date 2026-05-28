<template>
  <div class="stats-skeleton" aria-hidden="true">
    <div v-for="i in 3" :key="i" class="stats-skeleton__item">
      <span class="stats-skeleton__value" />
      <span class="stats-skeleton__label" />
    </div>
  </div>
</template>

<style scoped>
.stats-skeleton {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(95, 179, 168, 0.14);
}

.stats-skeleton__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.stats-skeleton__value,
.stats-skeleton__label {
  display: block;
  border-radius: 6px;
  background: linear-gradient(90deg, #e8f2f0 25%, #f4faf9 50%, #e8f2f0 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.2s ease-in-out infinite;
}

.stats-skeleton__value {
  width: 36px;
  height: 22px;
}

.stats-skeleton__label {
  width: 56px;
  height: 12px;
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
