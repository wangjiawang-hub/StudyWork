<script setup>
import { computed } from 'vue'
import { useRoute, RouterView } from 'vue-router'
import AppHeader from './components/AppHeader.vue'
import AppFooter from './components/AppFooter.vue'

const route = useRoute()
const showShell = computed(() => {
  const p = route.path
  return !p.startsWith('/admin') && p !== '/login' && p !== '/register'
})
</script>

<template>
  <div class="app">
    <AppHeader v-if="showShell" />
    <main :class="['main', { 'main--full': !showShell }]">
      <RouterView />
    </main>
    <AppFooter v-if="showShell" />
  </div>
</template>

<style>
.main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 28px 20px 0;
  min-height: calc(100vh - 64px - 180px);
}
.main--full {
  max-width: none;
  padding: 0;
  min-height: 100vh;
}
</style>
