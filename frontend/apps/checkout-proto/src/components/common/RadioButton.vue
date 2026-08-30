<script setup lang="ts">
defineProps<{
  active?: boolean
}>()
</script>

<template>
  <div class="radio" :class="{ active }">
    <div class="inner" />
  </div>
</template>

<style scoped>
.radio {
  width: 22px;
  height: 22px;
  border-radius: var(--radius-full);
  border: 2px solid var(--border-default);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.radio.active {
  border-color: var(--color-brand);
  background: var(--color-brand);
  box-shadow: 0 0 12px var(--color-brand-glow);
}

.inner {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
  background: white;
  opacity: 0;
  transform: scale(0);
  transition: all 0.2s ease;
}

.radio.active .inner {
  opacity: 1;
  transform: scale(1);
}
</style>
