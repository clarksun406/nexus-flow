<script setup lang="ts">
import { usePayment } from '@/composables/usePayment'
import { useToast } from '@/composables/useToast'
import ThemeToggle from '@/components/common/ThemeToggle.vue'

const { state, getOrderInfo } = usePayment()
const { show } = useToast()
const order = getOrderInfo()

function copyText(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    show('已复制到剪贴板')
  })
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <span class="label">订单详情</span>
      <div class="header-actions">
        <ThemeToggle />
        <div class="status">
          <span class="status-dot" />
          <span>待支付</span>
        </div>
      </div>
    </div>

    <div class="product-card">
      <div class="product-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
          <line x1="3" y1="6" x2="21" y2="6" />
          <path d="M16 10a4 4 0 0 1-8 0" />
        </svg>
      </div>
      <div class="product-info">
        <div class="product-name">{{ order.productName }}</div>
        <div class="product-desc">{{ order.productDesc }}</div>
      </div>
    </div>

    <div class="info-list">
      <div class="info-row">
        <span class="info-label">商户订单号</span>
        <span class="info-value">
          {{ order.merchantOrderId }}
          <button class="copy-btn" @click="copyText(order.merchantOrderId)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
            </svg>
          </button>
        </span>
      </div>
      <div class="info-row">
        <span class="info-label">支付订单号</span>
        <span class="info-value">
          {{ order.paymentOrderId }}
          <button class="copy-btn" @click="copyText(order.paymentOrderId)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
            </svg>
          </button>
        </span>
      </div>
      <div class="info-row">
        <span class="info-label">法币参考金额</span>
        <span class="info-value">{{ order.fiatAmount }} {{ order.fiatCurrency }}</span>
      </div>
    </div>

    <div class="total-card">
      <div class="total-label">应付金额</div>
      <div class="total-amount">
        <span class="amount-value">{{ state.amount }}</span>
        <span class="amount-unit">{{ state.coin }}</span>
      </div>
      <div class="total-fiat">≈ {{ order.fiatCurrency }} {{ order.fiatAmount }}</div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  padding: 22px 20px;
  background: var(--bg-card);
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 1.2px;
}

.status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-warning);
  background: var(--color-warning-glow);
  padding: 4px 12px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-subtle);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
  background: var(--color-warning);
  box-shadow: 0 0 8px var(--color-warning);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.product-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: var(--bg-input);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-subtle);
}

.product-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  background: var(--gradient-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 10px 22px -10px var(--color-brand-glow);
  flex-shrink: 0;
}

.product-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.product-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 2px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-subtle);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 12px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.info-value {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-mono);
  text-align: right;
}

.copy-btn {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.copy-btn:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
  border-color: var(--border-default);
}

.total-card {
  margin-top: auto;
  position: relative;
  padding: 16px;
  background: var(--bg-input);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.total-card::before {
  content: "";
  position: absolute;
  inset: 0 0 auto 0;
  height: 2px;
  background: var(--gradient-brand);
  opacity: 0.7;
}

.total-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 8px;
}

.total-amount {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.amount-value {
  font-size: 26px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.amount-unit {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
}

.total-fiat {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 6px;
}
</style>
