<script setup lang="ts">
import { useToast } from '@/composables/useToast'

defineProps<{
  label: string
  address: string
}>()

const { show } = useToast()

function copyText(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    show('已复制到剪贴板')
  })
}
</script>

<template>
  <div class="address-row">
    <div class="address-info">
      <span class="address-label">{{ label }}</span>
      <span class="address-value">{{ address }}</span>
    </div>
    <button class="copy-btn" @click="copyText(address)">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
        <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
      </svg>
      复制
    </button>
  </div>
</template>

<style scoped>
.address-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  margin-bottom: 16px;
}

.address-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.address-label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  color: var(--text-dim);
}

.address-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  font-family: var(--font-mono);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.copy-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  background: var(--gradient-brand);
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 8px 18px -8px var(--color-brand-glow);
  transition: all 0.15s ease;
}

.copy-btn:hover {
  filter: brightness(1.08);
  transform: translateY(-1px);
}
</style>
