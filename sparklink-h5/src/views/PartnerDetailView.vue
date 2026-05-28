<template>
  <div class="detail page page--no-tab">
    <PageNavBar title="搭子详情" />
    <DetailPageSkeleton v-if="pending && !detail" />
    <template v-else-if="detail">
      <div class="detail__cover">
        <NetworkImage
          :url="detail.coverImage || detail.cover"
          default-src="/images/partner-banner.jpg"
          custom-class="detail__cover-img"
          alt=""
        />
      </div>
      <div class="detail__body card">
        <h2>{{ detail.title }}</h2>
        <p class="detail__meta">
          {{ typeName }} · {{ detail.currentParticipants ?? detail.currentCount ?? 0 }}/{{ detail.maxParticipants ?? detail.targetCount ?? '?' }}人
        </p>
        <van-tag v-if="statusLabel" type="primary" plain>{{ statusLabel }}</van-tag>
        <p v-if="detail.content || detail.description" class="detail__desc">
          {{ detail.content || detail.description }}
        </p>
        <p v-if="detail.preference" class="detail__pref">偏好：{{ detail.preference }}</p>
        <p v-if="locationText" class="detail__addr">📍 {{ locationText }}</p>
        <div v-if="publisherName" class="detail__publisher">
          <NetworkImage
            :url="publisherAvatar"
            default-src="/images/default-avatar.png"
            custom-class="detail__pub-avatar"
            alt=""
          />
          <span>{{ publisherName }}</span>
        </div>
        <van-button
          v-if="auth.isLoggedIn && canApply"
          round
          block
          plain
          type="primary"
          class="detail__btn"
          :loading="applying"
          @click="onApply"
        >
          {{ applyLabel }}
        </van-button>
        <van-button
          v-if="auth.isLoggedIn"
          round
          block
          type="primary"
          color="#5FB3A8"
          class="detail__btn"
          @click="goChat"
        >
          联系 TA
        </van-button>
      </div>
    </template>
    <p v-else-if="!pending" class="empty-hint">加载失败</p>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showToast } from 'vant';
import { get, post } from '@/utils/request';
import { useAuthStore } from '@/stores/auth';
import { usePageLoad } from '@/composables/usePageLoad';
import { navigateToChat } from '@/utils/navigateToChat';
import PageNavBar from '@/components/PageNavBar.vue';
import NetworkImage from '@/components/NetworkImage.vue';
import DetailPageSkeleton from '@/components/skeleton/DetailPageSkeleton.vue';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const applying = ref(false);
const applied = ref(false);

const partnerId = computed(() => String(route.params.id || ''));

const { data: detail, pending, load } = usePageLoad(
  async () => {
    const res = await get(`/api/partner/${partnerId.value}`);
    return res.data || null;
  },
  {
    cacheKey: () => (partnerId.value ? `partner-detail:${partnerId.value}` : null),
    empty: null
  }
);

watch(partnerId, () => {
  if (partnerId.value) {
    load();
  }
});

const PARTNER_TYPES = [
  '宠物搭子', '电影搭子', '音乐搭子', '逛街搭子', '运动搭子',
  '摄影搭子', '干饭搭子', '旅游搭子', 'k歌搭子', '喝酒搭子',
  '桌游搭子', '钓鱼搭子', '游戏搭子', '聊天搭子', '户外搭子'
];

const typeName = computed(() => {
  const t = detail.value?.type;
  if (typeof t === 'string' && t) {
    return t;
  }
  const idx = Number(t) - 1;
  return PARTNER_TYPES[idx] || detail.value?.typeName || '搭子';
});

const statusLabel = computed(() => {
  const s = detail.value?.status;
  if (s === 'full' || s === 2) {
    return '已满';
  }
  if (s === 'ended' || s === 3) {
    return '已结束';
  }
  return '招募中';
});

const locationText = computed(() => detail.value?.location || detail.value?.address || '');

const publisherName = computed(() => detail.value?.nickname || detail.value?.publisherName || detail.value?.username || '');

const publisherAvatar = computed(() => detail.value?.avatar || detail.value?.publisherAvatar || '');

const canApply = computed(() => {
  const uid = detail.value?.userId || detail.value?.creatorId;
  return uid && String(uid) !== String(auth.userId);
});

const applyLabel = computed(() => (applied.value ? '已申请' : '申请加入'));

async function goChat() {
  const uid = detail.value?.userId || detail.value?.creatorId;
  if (!uid) {
    return;
  }
  try {
    await post('/api/im/prep-peer', { peerUserId: uid }, { suppressErrorToast: true });
  } catch {
    /* peer may exist */
  }
  navigateToChat(router, {
    userId: uid,
    nickname: publisherName.value,
    avatar: publisherAvatar.value
  });
}

async function onApply() {
  if (applied.value) {
    return;
  }
  applying.value = true;
  try {
    await post(`/api/partner/${partnerId.value}/apply`, {});
    applied.value = true;
    showToast('申请已提交');
  } catch (e) {
    showToast(e?.message || '申请失败');
  } finally {
    applying.value = false;
  }
}
</script>

<style scoped>
.detail__cover {
  height: 200px;
  overflow: hidden;
  background: #e8f6f5;
}

.detail__cover :deep(.detail__cover-img) {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.detail__body {
  margin: 16px;
  padding: 20px;
}

.detail__body h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.detail__meta {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0 0 12px;
}

.detail__desc {
  margin: 12px 0;
  line-height: 1.6;
  font-size: 15px;
}

.detail__pref,
.detail__addr {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 8px 0;
}

.detail__publisher {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 16px 0;
  font-size: 14px;
}

.detail__publisher :deep(.detail__pub-avatar) {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.detail__btn {
  margin-top: 10px;
}
</style>
