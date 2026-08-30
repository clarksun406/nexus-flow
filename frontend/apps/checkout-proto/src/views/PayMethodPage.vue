<script setup lang="ts">
import { usePayment } from '@/composables/usePayment'
import { useToast } from '@/composables/useToast'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import RadioButton from '@/components/common/RadioButton.vue'
import type { PaymentMethod } from '@/types/payment'

const { state, selectMethod, getNetworkConfig } = usePayment()
const { show } = useToast()

const emit = defineEmits<{
  next: []
  'go-network': []
}>()

const methods: { key: PaymentMethod; icon: string; title: string; sub: string }[] = [
  { key: 'address', icon: 'wallet', title: '地址转账', sub: '转账到钱包地址完成支付' },
  { key: 'wallet', icon: 'link', title: '链接钱包', sub: '连接到浏览器插件完成支付' },
]

const network = getNetworkConfig()

function handleConfirm() {
  if (!state.selectedMethod) {
    show('请选择支付方式')
    return
  }
  emit('next')
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div class="back-placeholder" />
      <h2 class="page-title">选择支付方式</h2>
    </div>

    <div class="method-card">
      <button
        v-for="method in methods"
        :key="method.key"
        class="method-item"
        :class="{ selected: state.selectedMethod === method.key }"
        @click="selectMethod(method.key)"
      >
        <div class="method-icon" :class="method.key">
          <svg v-if="method.key === 'address'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12V7H5a2 2 0 0 1 0-4h14v4" />
            <path d="M3 5v14a2 2 0 0 0 2 2h16v-5" />
            <path d="M18 12a2 2 0 0 0 0 4h4v-4Z" />
          </svg>
          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
            <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
          </svg>
        </div>
        <div class="method-info">
          <span class="method-title">{{ method.title }}</span>
          <span class="method-sub">{{ method.sub }}</span>
        </div>
        <RadioButton :active="state.selectedMethod === method.key" />
      </button>

      <div class="coin-badge">
        <div class="coin-dot" />
        <span class="coin-name">{{ state.coin }}</span>
        <span class="coin-tag">TRC20</span>
      </div>
    </div>

    <button class="network-entry" @click="$emit('go-network')">
      <div class="network-icon" :style="{ background: network.color + '20', color: network.color }">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" />
          <line x1="2" y1="12" x2="22" y2="12" />
          <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
        </svg>
      </div>
      <div class="network-info">
        <span class="network-title">处理网络</span>
        <span class="network-desc">{{ network.name }} ({{ network.type }}) · 预计到账: {{ network.time }}</span>
      </div>
      <svg class="arrow" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="9 18 15 12 9 6" />
      </svg>
    </button>

    <PrimaryButton @click="handleConfirm">确认支付方式</PrimaryButton>

    <div class="footer">
      <button class="lang-btn">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" />
          <line x1="2" y1="12" x2="22" y2="12" />
          <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
        </svg>
        简体中文
      </button>
      <span class="support">遇到问题？<a href="#">联系客服</a></span>
    </div>
  </div>
</template>

<style scoped>
.page {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.back-placeholder {
  width: 36px;
  height: 36px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
}

.method-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  margin-bottom: 16px;
}

.method-item {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 18px 20px;
  text-align: left;
  transition: all 0.2s ease;
  border-bottom: 1px solid var(--border-subtle);
}

.method-item:last-of-type {
  border-bottom: none;
}

.method-item:hover {
  background: var(--bg-input);
}

.method-item.selected {
  background: var(--color-brand-glow);
  box-shadow: inset 0 0 0 1px var(--color-brand);
}

.method-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.method-icon.address {
  background: linear-gradient(135deg, #F7931A20, #F7931A10);
  color: #F7931A;
}

.method-icon.wallet {
  background: linear-gradient(135deg, #8B5CF620, #8B5CF610);
  color: #8B5CF6;
}

.method-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.method-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.method-sub {
  font-size: 13px;
  color: var(--text-muted);
}

.coin-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  background: var(--bg-input);
  border-top: 1px solid var(--border-subtle);
}

.coin-dot {
  width: 20px;
  height: 20px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, #26A17B, #3ECEB0);
  box-shadow: 0 4px 10px -3px rgba(38, 161, 123, 0.6);
}

.coin-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.coin-tag {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-dim);
  background: var(--bg-elevated);
  padding: 3px 8px;
  border-radius: var(--radius-sm);
}

.network-entry {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 16px 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  margin-bottom: 20px;
  transition: all 0.2s ease;
  text-align: left;
}

.network-entry:hover {
  background: var(--bg-card-hover);
  border-color: var(--border-default);
  transform: translateY(-1px);
}

.network-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.network-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.network-title {
  font-size: 12px;
  color: var(--text-dim);
}

.network-desc {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.arrow {
  color: var(--text-dim);
}

.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid var(--border-subtle);
}

.lang-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--text-muted);
  transition: all 0.15s ease;
}

.lang-btn:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.support {
  font-size: 13px;
  color: var(--text-dim);
}

.support a {
  color: var(--color-brand-light);
  font-weight: 500;
}

.support a:hover {
  text-decoration: underline;
}
</style>
