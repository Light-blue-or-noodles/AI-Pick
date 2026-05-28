<template>
  <div class="agreement page page--no-tab">
    <PageNavBar :title="title" />
    <div class="agreement__scroll">
      <p class="agreement__updated">{{ updated }}</p>
      <section v-for="(sec, i) in sections" :key="i" class="agreement__section">
        <h3 v-if="sec.subtitle">{{ sec.subtitle }}</h3>
        <p v-for="(p, j) in sec.paragraphs" :key="j">{{ p }}</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import PageNavBar from '@/components/PageNavBar.vue';

defineProps({
  title: { type: String, required: true },
  updated: { type: String, default: '' },
  sections: { type: Array, default: () => [] }
});
</script>

<style scoped>
.agreement__scroll {
  padding: 16px var(--page-horizontal) 32px;
  max-height: calc(100vh - 46px);
  overflow-y: auto;
}

.agreement__updated {
  font-size: 12px;
  color: var(--text-tertiary);
  margin: 0 0 16px;
}

.agreement__section {
  margin-bottom: 20px;
}

.agreement__section h3 {
  margin: 0 0 8px;
  font-size: 15px;
}

.agreement__section p {
  margin: 0 0 8px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-secondary);
}
</style>
