<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'

const route = useRoute()

const hideHeader = computed(() => Boolean(route.meta.hideHeader))
const hideFooter = computed(() => Boolean(route.meta.hideFooter))
const fullWidth = computed(() => Boolean(route.meta.fullWidth))
</script>

<template>
  <a-layout class="basic-layout">
    <GlobalHeader v-if="!hideHeader" />
    <a-layout-content :class="['main-content', { 'main-content-full': fullWidth }]">
      <router-view />
    </a-layout-content>
    <GlobalFooter v-if="!hideFooter" />
  </a-layout>
</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background: #f5f8ff;
}

.main-content {
  width: min(1200px, calc(100% - 48px));
  padding: 24px;
  background: #fff;
  margin: 16px auto 56px;
  border-radius: 12px;
}

.main-content-full {
  width: 100%;
  padding: 0;
  margin: 0;
  background: transparent;
  border-radius: 0;
}
</style>
