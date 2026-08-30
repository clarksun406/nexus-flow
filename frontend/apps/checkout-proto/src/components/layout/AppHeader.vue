<script setup lang="ts">
import { usePayment } from '@/composables/usePayment'

const { state } = usePayment()

const steps = [
  { num: 1, label: '支付方式' },
  { num: 2, label: '选择网络' },
  { num: 3, label: '确认支付' },
  { num: 4, label: '完成' },
]

function getStepClass(stepNum: number) {
  if (stepNum < state.currentStep) return 'done'
  if (stepNum === state.currentStep) return 'active'
  return ''
}
</script>

<template>
  <header class="header">
    <div class="brand">
      <div class="brand-mark">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2L2 7l10 5 10-5-10-5z" />
          <path d="M2 17l10 5 10-5" />
          <path d="M2 12l10 5 10-5" />
        </svg>
      </div>
      <span class="brand-name">NexusFlow</span>
    </div>

    <div class="progress">
      <template v-for="(step, index) in steps" :key="step.num">
        <div class="step" :class="getStepClass(step.num)">
          <div class="step-dot">
            <svg v-if="step.num < state.currentStep" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12" />
            </svg>
            <span v-else>{{ step.num }}</span>
          </div>
          <span class="step-label">{{ step.label }}</span>
        </div>
        <div
          v-if="index < steps.length - 1"
          class="step-line"
          :class="{ done: step.num < state.currentStep }"
        />
      </template>
    </div>

    <div class="header-side" />
  </header>
</template>

<style scoped>
.header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 12px;
  padding: 18px 24px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-subtle);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  background: var(--gradient-brand);
  box-shadow: 0 8px 18px -8px var(--color-brand-glow);
}

.brand-name {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
}

.header-side {
  justify-self: end;
}

.progress {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
}

.step {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  border-radius: var(--radius-full);
  transition: all 0.3s ease;
}

.step.active {
  background: var(--color-brand-glow);
}

.step-dot {
  width: 30px;
  height: 30px;
  border-radius: var(--radius-full);
  border: 2px solid var(--border-default);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-dim);
  background: transparent;
  transition: all 0.3s ease;
}

.step.active .step-dot {
  border-color: transparent;
  background: var(--gradient-brand);
  color: white;
  box-shadow: 0 0 20px var(--color-brand-glow);
}

.step.done .step-dot {
  border-color: var(--color-success);
  background: var(--color-success);
  color: white;
}

.step-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-dim);
  transition: all 0.3s ease;
}

.step.active .step-label {
  color: var(--text-primary);
  font-weight: 600;
}

.step.done .step-label {
  color: var(--color-success);
}

.step-line {
  width: 36px;
  height: 2px;
  border-radius: 2px;
  background: var(--border-subtle);
  transition: all 0.3s ease;
}

.step-line.done {
  background: var(--color-success);
}

@media (max-width: 720px) {
  .header {
    grid-template-columns: 1fr;
    justify-items: center;
    row-gap: 12px;
  }

  .brand,
  .header-side {
    display: none;
  }
}
</style>
