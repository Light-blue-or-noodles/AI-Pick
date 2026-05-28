<template>
  <div class="detail-skeleton" aria-hidden="true">
    <span class="detail-skeleton__cover" />
    <div class="detail-skeleton__body">
      <span class="detail-skeleton__title" />
      <span class="detail-skeleton__meta" />
      <span class="detail-skeleton__line" />
      <span class="detail-skeleton__line detail-skeleton__line--short" />
    </div>
  </div>
</template>

<style scoped>
.detail-skeleton__cover {
  display: block;
  width: 100%;
  height: 200px;
  background: linear-gradient(90deg, #e0ebe8 25%, #eef5f3 50%, #e0ebe8 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.2s ease-in-out infinite;
}

.detail-skeleton__body {
  margin: 16px;
  padding: 20px;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(74, 154, 144, 0.06);
}

.detail-skeleton__title,
.detail-skeleton__meta,
.detail-skeleton__line {
  display: block;
  border-radius: 6px;
  margin-bottom: 12px;
  background: linear-gradient(90deg, #e8f2f0 25%, #f4faf9 50%, #e8f2f0 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.2s ease-in-out infinite;
}

.detail-skeleton__title {
  width: 70%;
  height: 22px;
}

.detail-skeleton__meta {
  width: 45%;
  height: 14px;
}

.detail-skeleton__line {
  width: 100%;
  height: 14px;
}

.detail-skeleton__line--short {
  width: 80%;
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
