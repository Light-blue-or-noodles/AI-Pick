<template>
  <div class="detail page page--no-tab" :class="{ 'detail--has-toolbar': detail && !isOwner && auth.isLoggedIn }">
    <PageNavBar title="搭子详情" />
    <DetailPageSkeleton v-if="pending && !detail" />
    <template v-else-if="detail">
      <section class="detail-hero">
        <NetworkImage
          :url="detail.coverImage || detail.cover"
          default-src="/images/partner-banner.jpg"
          custom-class="detail-hero__cover"
          alt=""
        />
        <div class="detail-hero__mask">
          <h1 class="detail-hero__title">{{ detail.title }}</h1>
        </div>
      </section>

      <div class="detail-stack">
        <section class="card-surface meta-card">
          <div class="meta-row">
            <span class="meta-k">搭子类型</span>
            <span class="meta-v">{{ typeName || '-' }}</span>
          </div>
          <div class="meta-row meta-row--pref">
            <span class="meta-k">搭子偏好</span>
            <div v-if="preferenceTags.length" class="pref-chips">
              <span v-for="tag in preferenceTags" :key="tag" class="pref-chip">{{ tag }}</span>
            </div>
            <span v-else class="meta-v">-</span>
          </div>
          <div class="meta-row">
            <span class="meta-k">可见范围</span>
            <span class="meta-v">{{ detail.scopeName || '公开' }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-k">人数</span>
            <span class="meta-v">
              {{ currentCount }}/{{ targetCount }}人
              <template v-if="targetCount > 0">（还剩{{ remainSpots }}个名额）</template>
            </span>
          </div>
          <div class="meta-row-group">
            <div class="meta-row meta-row--inline">
              <span class="meta-k">时间范围</span>
              <span class="meta-v" :class="{ 'meta-v--muted': !planTimeMeta.hasPlanTime }">
                {{ planTimeMeta.primary }}
              </span>
            </div>
            <div v-if="planTimeMeta.secondary" class="meta-sub-line">{{ planTimeMeta.secondary }}</div>
          </div>
          <div v-if="locationMeta" class="meta-row meta-row--stack">
            <span class="meta-k">地点</span>
            <div class="meta-v-stack">
              <span class="meta-v-primary">{{ locationMeta.primary }}</span>
              <span v-if="locationMeta.secondary" class="meta-v-sub">{{ locationMeta.secondary }}</span>
            </div>
          </div>
        </section>

        <section v-if="publisherName" class="card-surface publisher">
          <NetworkImage
            :url="publisherAvatar"
            default-src="/images/default-avatar.png"
            custom-class="publisher__avatar"
            alt=""
          />
          <span class="publisher__name">{{ publisherName }}</span>
        </section>

        <section class="card-surface detail-section">
          <h2 class="detail-section__title">详情</h2>
          <p class="detail-section__body">{{ descriptionText }}</p>
        </section>
      </div>

      <footer v-if="!isOwner" class="bottom-actions" :class="{ 'bottom-actions--with-toolbar': auth.isLoggedIn }">
        <div v-if="auth.isLoggedIn" class="bottom-actions__toolbar">
          <button
            type="button"
            class="btn-action btn-follow"
            :class="{ 'btn-follow--active': isFollowing }"
            :disabled="followLoading"
            @click="onToggleFollow"
          >
            {{ isFollowing ? '已关注' : '+关注' }}
          </button>
          <button type="button" class="btn-action btn-chat" @click="goChat">💬 聊天</button>
          <button type="button" class="btn-action btn-match" :disabled="matchLoading" @click="onShowMatch">🎯 匹配</button>
        </div>
        <button
          type="button"
          class="join-btn"
          :class="{
            'join-btn--cancel': hasApplied,
            'join-btn--disabled': !hasApplied && isFull
          }"
          :disabled="joining"
          @click="onJoinPartner"
        >
          <template v-if="joining">处理中...</template>
          <template v-else-if="hasApplied">取消报名</template>
          <template v-else-if="isFull">名额已满</template>
          <template v-else-if="!auth.isLoggedIn">登录后报名</template>
          <template v-else>报名参加</template>
        </button>
      </footer>
    </template>
    <p v-else-if="!pending" class="empty-hint">搭子不存在或已删除</p>

    <van-popup
      v-model:show="showMatchPanel"
      round
      position="center"
      class="match-popup"
      :style="{ width: '88%', maxHeight: '82vh' }"
    >
      <div v-if="matchResult" class="match-panel">
        <header class="match-panel__hd">
          <h3>匹配度分析</h3>
          <button type="button" class="match-panel__close" aria-label="关闭" @click="showMatchPanel = false">×</button>
        </header>
        <div class="match-panel__body">
          <div class="match-total">
            <span class="match-total__val">{{ matchResult.totalScore ?? '--' }}</span>
            <span class="match-total__unit">分</span>
          </div>
          <div class="match-dims">
            <div class="match-dim">
              <span class="match-dim__k">活动</span>
              <span class="match-dim__v">{{ matchResult.activityTagScore ?? matchResult.interestScore ?? '—' }}</span>
            </div>
            <div class="match-dim">
              <span class="match-dim__k">发布者</span>
              <span class="match-dim__v">{{ matchResult.publisherTagScore ?? '—' }}</span>
            </div>
            <div class="match-dim">
              <span class="match-dim__k">位置</span>
              <span class="match-dim__v">{{ matchResult.locationScore ?? '—' }}</span>
            </div>
            <div class="match-dim match-dim--ai">
              <span class="match-dim__k">AI</span>
              <span class="match-dim__v">{{ matchResult.aiScore ?? '—' }}</span>
            </div>
          </div>
          <p v-if="matchResult.reason" class="match-reason">{{ matchResult.reason }}</p>
          <ul v-if="matchResult.suggestions?.length" class="match-sug">
            <li v-for="(item, idx) in matchResult.suggestions" :key="idx">{{ idx + 1 }}. {{ item }}</li>
          </ul>
        </div>
        <footer class="match-panel__ft">
          <button type="button" class="match-panel__ok" @click="showMatchPanel = false">知道了</button>
        </footer>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showToast, showConfirmDialog, showLoadingToast, closeToast } from 'vant';
import { get, post, del, getApiErrorMessage } from '@/utils/request';
import { useAuthStore } from '@/stores/auth';
import { usePageLoad } from '@/composables/usePageLoad';
import { navigateToChat } from '@/utils/navigateToChat';
import { normalizeMatchScoreDto } from '@/utils/normalizeMatchScoreDto';
import { isSelfChat, resolvePublisherUserId } from '@/utils/chatPeer';
import {
  resolvePlanTimeMeta,
  parseLocationMeta,
  splitPreferenceTags
} from '@/utils/partnerPlanTime';
import PageNavBar from '@/components/PageNavBar.vue';
import NetworkImage from '@/components/NetworkImage.vue';
import DetailPageSkeleton from '@/components/skeleton/DetailPageSkeleton.vue';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const joining = ref(false);
const isFollowing = ref(false);
const followLoading = ref(false);
const showMatchPanel = ref(false);
const matchResult = ref(null);
const matchLoading = ref(false);

const partnerId = computed(() => String(route.params.id || ''));

const { data: detail, pending, load, refresh } = usePageLoad(
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

watch(detail, (d) => {
  if (!d) {
    isFollowing.value = false;
    return;
  }
  isFollowing.value = !!(d.isFollowed || d.isFollowing);
  loadFollowState();
}, { immediate: true });

const publisherUserId = computed(() => resolvePublisherUserId(detail.value));

const PARTNER_TYPES = [
  '宠物搭子', '电影搭子', '音乐搭子', '逛街搭子', '运动搭子',
  '摄影搭子', '干饭搭子', '旅游搭子', 'k歌搭子', '喝酒搭子',
  '桌游搭子', '钓鱼搭子', '游戏搭子', '聊天搭子', '户外搭子'
];

const typeName = computed(() => {
  const t = detail.value?.type;
  if (typeof t === 'string' && t && Number.isNaN(Number(t))) {
    return t;
  }
  const idx = Number(t) - 1;
  return PARTNER_TYPES[idx] || detail.value?.typeName || '搭子';
});

const currentCount = computed(() => detail.value?.currentParticipants ?? detail.value?.currentCount ?? 0);

const targetCount = computed(() => detail.value?.maxParticipants ?? detail.value?.targetCount ?? 0);

const remainSpots = computed(() => {
  const max = Number(targetCount.value) || 0;
  const cur = Number(currentCount.value) || 0;
  return max > 0 ? Math.max(0, max - cur) : 0;
});

const isFull = computed(() => {
  const s = detail.value?.status;
  if (s === 1 || s === 'full') {
    return true;
  }
  const max = Number(targetCount.value) || 0;
  const cur = Number(currentCount.value) || 0;
  return max > 0 && cur >= max;
});

const isOwner = computed(() => {
  if (detail.value?.isOwner != null) {
    return !!detail.value.isOwner;
  }
  const uid = publisherUserId.value;
  return uid && String(uid) === String(auth.userId);
});

const hasApplied = computed(() => !!detail.value?.hasApplied);

const preferenceTags = computed(() => splitPreferenceTags(detail.value?.preference));

const planTimeMeta = computed(() => {
  const d = detail.value;
  if (!d) {
    return { hasPlanTime: false, primary: '待定', secondary: '' };
  }
  return resolvePlanTimeMeta(d);
});

const locationMeta = computed(() => {
  const raw = detail.value?.address || detail.value?.location || '';
  return parseLocationMeta(raw);
});

const publisherName = computed(() => detail.value?.nickname || detail.value?.publisherName || detail.value?.username || '');

const publisherAvatar = computed(() => detail.value?.avatar || detail.value?.publisherAvatar || '');

const descriptionText = computed(() => detail.value?.description || detail.value?.content || '暂无详情');

function ensureLogin() {
  if (auth.isLoggedIn) {
    return true;
  }
  router.push({ name: 'login', query: { redirect: route.fullPath } });
  return false;
}

async function loadFollowState() {
  const targetId = publisherUserId.value;
  if (!auth.isLoggedIn || !targetId || isOwner.value) {
    return;
  }
  try {
    const chk = await get(`/api/user/follow/check/${targetId}`, {}, { suppressErrorToast: true });
    isFollowing.value = !!chk.data?.isFollowing;
  } catch {
    /* keep detail fallback */
  }
}

async function goChat() {
  if (!ensureLogin()) {
    return;
  }
  const uid = publisherUserId.value;
  if (!uid) {
    showToast('无法发起聊天');
    return;
  }
  if (isSelfChat(uid)) {
    showToast('不能与自己聊天');
    return;
  }
  try {
    await post('/api/im/prep-peer', { peerUserId: Number(uid) }, { suppressErrorToast: true });
  } catch (e) {
    const msg = getApiErrorMessage(e, '');
    if (msg && msg.indexOf('不能与自身') !== -1) {
      showToast('不能与自己聊天');
      return;
    }
    /* peer may exist or prep optional */
  }
  navigateToChat(router, {
    userId: uid,
    nickname: publisherName.value,
    avatar: publisherAvatar.value
  });
}

async function onToggleFollow() {
  if (!ensureLogin() || followLoading.value) {
    return;
  }
  const userId = publisherUserId.value;
  if (!userId) {
    showToast('无法关注');
    return;
  }
  if (isSelfChat(userId)) {
    showToast('不能关注自己');
    return;
  }
  const willFollow = !isFollowing.value;
  followLoading.value = true;
  try {
    const res = await post(
      `/api/user/${userId}/follow`,
      { action: willFollow ? 'follow' : 'cancel' },
      { suppressErrorToast: true }
    );
    if (res.data?.isFollowing != null) {
      isFollowing.value = !!res.data.isFollowing;
    } else {
      isFollowing.value = willFollow;
    }
    showToast(isFollowing.value ? '已关注' : '已取消关注');
  } catch (e) {
    showToast(getApiErrorMessage(e, '操作失败'));
  } finally {
    followLoading.value = false;
  }
}

async function onShowMatch() {
  if (!ensureLogin() || matchLoading.value) {
    return;
  }
  const partner = detail.value;
  const me = String(auth.userId || '').trim();
  const target = publisherUserId.value;
  if (!target) {
    showToast('无法计算匹配度');
    return;
  }
  if (!me) {
    showToast('请先登录');
    return;
  }
  if (me === target) {
    showToast('不能与自己计算匹配度');
    return;
  }
  matchLoading.value = true;
  showLoadingToast({ message: '分析中...', forbidClick: true, duration: 0 });
  try {
    const res = await post('/api/ai/match-score', {
      userId: Number(me),
      targetId: Number(target),
      partnerId: partner?.id != null ? Number(partner.id) : undefined
    }, { suppressErrorToast: true });
    const dto = normalizeMatchScoreDto(res);
    if (!dto) {
      showToast('匹配分析失败');
      return;
    }
    matchResult.value = dto;
    showMatchPanel.value = true;
  } catch (e) {
    showToast(getApiErrorMessage(e, '匹配分析失败'));
  } finally {
    matchLoading.value = false;
    closeToast();
  }
}

async function onJoinPartner() {
  if (isOwner.value) {
    return;
  }
  if (!auth.isLoggedIn) {
    ensureLogin();
    return;
  }
  const title = detail.value?.title || '该搭子';

  if (hasApplied.value) {
    try {
      await showConfirmDialog({
        title: '取消报名',
        message: `确定要取消「${title}」的报名吗？`
      });
    } catch {
      return;
    }
    joining.value = true;
    try {
      await del(`/api/partner/${partnerId.value}/apply`);
      showToast('已取消报名');
      await refresh();
    } catch (e) {
      showToast(getApiErrorMessage(e, '取消失败'));
    } finally {
      joining.value = false;
    }
    return;
  }

  if (isFull.value) {
    showToast('名额已满');
    return;
  }

  try {
    await showConfirmDialog({
      title: '报名参加',
      message: `确定要报名「${title}」吗？`
    });
  } catch {
    return;
  }

  joining.value = true;
  try {
    await post(`/api/partner/${partnerId.value}/apply`, {});
    showToast('报名成功');
    await refresh();
  } catch (e) {
    showToast(getApiErrorMessage(e, '报名失败'));
  } finally {
    joining.value = false;
  }
}
</script>

<style scoped>
.detail {
  background: #f5f5f5;
  padding-bottom: calc(88px + var(--safe-bottom));
}

.detail--has-toolbar {
  padding-bottom: calc(148px + var(--safe-bottom));
}

.detail-hero {
  position: relative;
  height: 200px;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.detail-hero :deep(.detail-hero__cover) {
  width: 100%;
  height: 200px;
  object-fit: cover;
  display: block;
}

.detail-hero__mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 0 14px 18px;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.04) 0%,
    rgba(0, 0, 0, 0.15) 42%,
    rgba(0, 0, 0, 0.62) 100%
  );
}

.detail-hero__title {
  margin: 0;
  max-width: 92%;
  font-size: 19px;
  font-weight: 600;
  color: #fff;
  line-height: 1.45;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.55);
}

.detail-stack {
  margin: 10px var(--page-horizontal) 0;
  padding-bottom: 16px;
}

.card-surface {
  background: #fff;
  border-radius: var(--radius-md);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.06);
}

.card-surface + .card-surface {
  margin-top: 10px;
}

.meta-card {
  padding: 10px 14px 6px;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}

.meta-row:last-child {
  border-bottom: none;
}

.meta-row--pref {
  flex-direction: column;
  align-items: stretch;
}

.meta-k {
  flex-shrink: 0;
  color: var(--text-tertiary);
}

.meta-v {
  color: var(--text-primary);
  text-align: right;
  line-height: 1.5;
  word-break: break-word;
}

.meta-row--pref .meta-k {
  margin-bottom: 8px;
}

.meta-row--pref .meta-v {
  text-align: left;
}

.meta-row-group {
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.meta-row-group .meta-row {
  padding: 0;
  border-bottom: none;
}

.meta-row--inline {
  align-items: center;
}

.meta-v--muted {
  color: var(--text-secondary);
}

.meta-sub-line {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.45;
  color: var(--text-tertiary);
  text-align: right;
}

.meta-row--stack {
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
}

.meta-row--stack .meta-k {
  margin-bottom: 2px;
}

.meta-v-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
}

.meta-v-primary {
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.meta-v-primary--muted {
  color: var(--text-secondary);
}

.meta-v-sub {
  font-size: 12px;
  line-height: 1.45;
  color: var(--text-tertiary);
  word-break: break-word;
}

.pref-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pref-chip {
  padding: 4px 10px;
  font-size: 12px;
  border-radius: var(--radius-full);
  background: rgba(82, 196, 26, 0.12);
  color: #389e0d;
}

.publisher {
  display: flex;
  align-items: center;
  padding: 12px 14px;
  min-height: 48px;
}

.publisher :deep(.publisher__avatar) {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  margin-right: 10px;
}

.publisher__name {
  flex: 1;
  min-width: 0;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

.action-buttons,
.bottom-actions__toolbar {
  display: flex;
  gap: 10px;
}

.action-buttons {
  padding: 12px 14px;
}

.bottom-actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  min-height: calc(60px + var(--safe-bottom));
  padding: 8px var(--page-horizontal) calc(8px + var(--safe-bottom));
  background: #fff;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.08);
  box-sizing: border-box;
}

.bottom-actions--with-toolbar {
  min-height: calc(116px + var(--safe-bottom));
}

.bottom-actions__toolbar {
  width: 100%;
}

.btn-action {
  flex: 1;
  min-width: 0;
  height: 40px;
  border: none;
  border-radius: 20px;
  font-family: inherit;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.btn-follow {
  background: #fff;
  color: var(--primary-color);
  border: 1px solid var(--primary-color);
}

.btn-follow--active {
  background: rgba(95, 179, 168, 0.22);
  color: #1f6b62;
  border-color: rgba(95, 179, 168, 0.55);
  font-weight: 600;
}

.btn-chat {
  color: #fff;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
}

.btn-match {
  color: #fff;
  background: linear-gradient(135deg, #ffb800 0%, #ffd93d 100%);
}

.detail-section {
  padding: 12px 14px 14px;
}

.detail-section__title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.detail-section__body {
  margin: 0;
  font-size: 14px;
  line-height: 1.65;
  color: #555;
  white-space: pre-wrap;
  word-break: break-word;
}

.join-btn {
  flex: 1;
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 22px;
  font-family: inherit;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  cursor: pointer;
}

.join-btn--cancel {
  color: var(--text-primary);
  background: #f5f5f5;
}

.join-btn--disabled {
  color: var(--text-tertiary);
  background: #e8e8e8;
  cursor: not-allowed;
}

.join-btn:disabled {
  opacity: 0.75;
}

.match-panel {
  display: flex;
  flex-direction: column;
  max-height: 82vh;
}

.match-panel__hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 10px;
  border-bottom: 1px solid #f0f0f0;
}

.match-panel__hd h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.match-panel__close {
  border: none;
  background: none;
  font-size: 24px;
  line-height: 1;
  color: #999;
  cursor: pointer;
  padding: 0 4px;
}

.match-panel__body {
  padding: 12px 16px;
  overflow-y: auto;
}

.match-total {
  text-align: center;
  margin-bottom: 12px;
}

.match-total__val {
  font-size: 36px;
  font-weight: 700;
  color: var(--primary-color);
}

.match-total__unit {
  margin-left: 4px;
  font-size: 14px;
  color: var(--text-secondary);
}

.match-dims {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.match-dim {
  text-align: center;
  padding: 8px 4px;
  border-radius: 8px;
  background: var(--primary-bg);
}

.match-dim--ai {
  grid-column: 1 / -1;
}

.match-dim__k {
  display: block;
  font-size: 12px;
  color: var(--text-tertiary);
}

.match-dim__v {
  display: block;
  margin-top: 4px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.match-reason {
  margin: 0 0 10px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--text-secondary);
}

.match-sug {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--text-secondary);
}

.match-panel__ft {
  padding: 10px 16px 16px;
  border-top: 1px solid #f0f0f0;
}

.match-panel__ok {
  width: 100%;
  height: 40px;
  border: none;
  border-radius: 20px;
  font-family: inherit;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: var(--primary-color);
  cursor: pointer;
}
</style>
