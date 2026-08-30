<script setup lang="ts">
import { usePayment } from '@/composables/usePayment'

const { state, getNetworkConfig, getOrderInfo } = usePayment()
const network = getNetworkConfig()
const order = getOrderInfo()

defineEmits<{
  back: []
}>()
</script>

<template>
  <div class="pay-card">
    <div class="pay-top">
      <div class="pay-amount">
        <div class="coin-icon" :style="{ background: `linear-gradient(135deg, ${network.color}, ${network.color}88)` }" />
        <div>
          <div class="amount-row">
            <span class="num">{{ state.amount }}</span>
            <span class="unit">{{ state.coin }}</span>
          </div>
          <div class="paid">≈ {{ order.fiatCurrency }} {{ order.fiatAmount }}</div>
        </div>
      </div>
      <button class="change-btn" @click="$emit('back')">
        <span class="chain-tag" :style="{ background: network.color + '20', color: network.color }">
          {{ network.type }}
        </span>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="9 18 15 12 9 6" />
        </svg>
      </button>
    </div>

    <div class="pay-meta">
      <div class="meta-item">
        <span class="meta-label">订单号</span>
        <span class="meta-value">{{ order.paymentOrderId }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">剩余时间</span>
        <span class="meta-value timer">14:59</span>
      </div>
      <button class="trace-btn">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
          <circle cx="12" cy="12" r="3" />
        </svg>
        追踪
      </button>
    </div>
  </div>
</template>

<style scoped>
.pay-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  margin-bottom: 20px;
}

.pay-top {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.pay-amount {
  display: flex;
  align-items: center;
  gap: 14px;
}

.coin-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-full);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.35), 0 10px 20px -10px rgba(0, 0, 0, 0.5);
  flex-shrink: 0;
}

.amount-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.num {
  font-size: 30px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.unit {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-muted);
}

.paid {
  font-size: 13px;
  color: var(--text-dim);
  margin-top: 4px;
}

.change-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--bg-input);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  transition: all 0.15s ease;
}

.change-btn:hover {
  background: var(--bg-elevated);
  border-color: var(--border-default);
}

.chain-tag {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
}

.change-btn svg {
  color: var(--text-dim);
}

.pay-meta {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 14px 20px;
  background: var(--bg-input);
  border-top: 1px solid var(--border-subtle);
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.meta-label {
  font-size: 10px;
  font-weight: 700;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.meta-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.timer {
  color: var(--color-warning);
  font-variant-numeric: tabular-nums;
}

.trace-btn {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: all 0.15s ease;
}

.trace-btn:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}
</style>
