<template>
  <img
    :src="currentSrc"
    :class="customClass"
    :alt="alt"
    @error="onError"
  />
</template>

<script setup>
import { ref, watch } from 'vue';
import { resolveMediaUrl } from '@/utils/mediaUrl';
import { getApiBaseUrl } from '@/config/env';

const props = defineProps({
  url: { type: String, default: '' },
  defaultSrc: { type: String, default: '/images/partner-banner.jpg' },
  customClass: { type: String, default: '' },
  alt: { type: String, default: '' }
});

const currentSrc = ref('');

function resolve() {
  const resolved = resolveMediaUrl(props.url, { baseUrl: getApiBaseUrl(), fallback: '' });
  currentSrc.value = resolved || props.defaultSrc;
}

function onError() {
  if (currentSrc.value !== props.defaultSrc) {
    currentSrc.value = props.defaultSrc;
  }
}

watch(() => props.url, resolve, { immediate: true });
</script>
