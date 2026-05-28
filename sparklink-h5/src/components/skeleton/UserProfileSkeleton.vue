<template>
  <div class="user-profile-skeleton" aria-hidden="true">
    <div class="user-profile-skeleton__head">
      <span class="user-profile-skeleton__avatar" />
      <span class="user-profile-skeleton__name" />
      <span class="user-profile-skeleton__bio" />
    </div>
    <span class="user-profile-skeleton__btn" />
    <span class="user-profile-skeleton__btn" />
  </div>
</template>

<style scoped>
.user-profile-skeleton__head {
  padding: 32px 24px;
  text-align: center;
  background: linear-gradient(135deg, #e8f6f5 0%, #d4ebe8 100%);
}

.user-profile-skeleton__avatar,
.user-profile-skeleton__name,
.user-profile-skeleton__bio,
.user-profile-skeleton__btn {
  display: block;
  margin-left: auto;
  margin-right: auto;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.35) 25%, rgba(255, 255, 255, 0.55) 50%, rgba(255, 255, 255, 0.35) 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.2s ease-in-out infinite;
}

.user-profile-skeleton__avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  margin-bottom: 16px;
}

.user-profile-skeleton__name {
  width: 120px;
  height: 22px;
  border-radius: 6px;
  margin-bottom: 10px;
}

.user-profile-skeleton__bio {
  width: 180px;
  height: 14px;
  border-radius: 6px;
}

.user-profile-skeleton__btn {
  width: calc(100% - 32px);
  height: 44px;
  border-radius: 22px;
  margin: 12px 16px 0;
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
