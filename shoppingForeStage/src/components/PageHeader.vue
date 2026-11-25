<script setup>
import { computed } from 'vue'
import { usePageBack } from '../composables/usePageBack'

const props = defineProps({
  title: { type: String, required: true },
  icon: { type: String, default: '' },
  fallback: { type: String, default: '/' },
  showBack: { type: Boolean, default: true },
  backText: { type: String, default: '' }
})

const { goBack, backLabel } = usePageBack(props.fallback)
const label = computed(() => props.backText || backLabel.value)
</script>

<template>
  <div class="page-header-bar">
    <div class="left">
      <el-button v-if="showBack" link class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        {{ label }}
      </el-button>
      <h2 class="page-title">
        <el-icon v-if="icon"><component :is="icon" /></el-icon>
        {{ title }}
      </h2>
    </div>
    <div v-if="$slots.actions" class="actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.page-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 16px;
  flex-wrap: wrap;
}
.left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.back-btn {
  padding: 0;
  font-size: 13px;
  color: var(--mall-text-secondary);
  height: auto;
}
.back-btn:hover {
  color: var(--mall-primary);
}
.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 0;
  font-size: 22px;
}
.actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
</style>
