<template>
  <van-nav-bar :title="title" left-arrow @click-left="onBack" />
</template>

<script setup>
import { useRouter } from 'vue-router';

defineProps({
  title: { type: String, default: '' }
});

const router = useRouter();

function onBack() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push({ name: 'home' });
  }
}
</script>
