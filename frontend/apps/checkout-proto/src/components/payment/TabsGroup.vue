<script setup lang="ts">
import type { TabType } from '@/types/payment'

defineProps<{
  activeTab: TabType
}>()

defineEmits<{
  change: [tab: TabType]
}>()

const tabs: { key: TabType; label: string }[] = [
  { key: 'address', label: '地址转账' },
  { key: 'plugin', label: '浏览器插件' },
  { key: 'scan', label: '钱包扫码' },
]
</script>

<template>
  <div class="tabs">
    <div
      v-for="tab in tabs"
      :key="tab.key"
      class="tab"
      :class="{ active: activeTab === tab.key }"
      @click="$emit('change', tab.key)"
    >
      {{ tab.label }}
    </div>
  </div>
</template>

<style scoped>
.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.tab {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid var(--border-primary);
  border-radius: 10px;
  background: var(--bg-card);
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted-light);
  cursor: pointer;
  transition: all 0.15s;
}

.tab:hover {
  background: var(--bg-secondary);
}

.tab.active {
  border-color: var(--color-primary);
  color: var(--text-primary);
  box-shadow: var(--shadow-sm);
}
</style>
