<script setup lang="ts">
defineProps<{
  disabled?: boolean
  variant?: 'primary' | 'secondary'
}>()

defineEmits<{
  click: []
}>()
</script>

<template>
  <button
    class="btn"
    :class="[variant || 'primary', { disabled }]"
    :disabled="disabled"
    @click="$emit('click')"
  >
    <slot />
  </button>
</template>

<style scoped>
.btn {
  width: 100%;
  height: 52px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.3px;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.primary {
  background: var(--gradient-brand);
  color: white;
  box-shadow: 0 10px 26px -10px var(--color-brand-glow);
}

.primary:hover:not(:disabled) {
  transform: translateY(-2px);
  filter: brightness(1.07);
  box-shadow: 0 14px 34px -12px var(--color-brand-glow);
}

.primary:active:not(:disabled) {
  transform: translateY(0);
}

.secondary {
  background: var(--bg-elevated);
  color: var(--text-primary);
  border: 1px solid var(--border-default);
}

.secondary:hover:not(:disabled) {
  background: var(--bg-card-hover);
  border-color: var(--text-dim);
}

.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
