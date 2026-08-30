<script setup lang="ts">
import { ref } from 'vue'
import { usePayment } from '@/composables/usePayment'
import { useToast } from '@/composables/useToast'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import PayAmountCard from '@/components/payment/PayAmountCard.vue'
import AddressRow from '@/components/payment/AddressRow.vue'
import QrSection from '@/components/payment/QrSection.vue'
import type { TabType } from '@/types/payment'

const { state } = usePayment()
const { show } = useToast()
const activeTab = ref<TabType>('address')

const emit = defineEmits<{
  back: []
  success: []
}>()

function handleConfirm() {
  show('正在确认支付...')
  setTimeout(() => {
    emit('success')
    show('支付成功！')
  }, 1500)
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <button class="back-btn" @click="$emit('back')">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <h2 class="page-title">支付详情</h2>
    </div>

    <PayAmountCard @back="$emit('back')" />

    <div class="tabs">
      <button
        class="tab"
        :class="{ active: activeTab === 'address' }"
        @click="activeTab = 'address'"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 12V7H5a2 2 0 0 1 0-4h14v4" />
          <path d="M3 5v14a2 2 0 0 0 2 2h16v-5" />
          <path d="M18 12a2 2 0 0 0 0 4h4v-4Z" />
        </svg>
        地址转账
      </button>
      <button
        class="tab"
        :class="{ active: activeTab === 'scan' }"
        @click="activeTab = 'scan'"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="7" height="7" />
          <rect x="14" y="3" width="7" height="7" />
          <rect x="14" y="14" width="7" height="7" />
          <rect x="3" y="14" width="7" height="7" />
        </svg>
        钱包扫码
      </button>
    </div>

    <!-- 地址转账 -->
    <div v-if="activeTab === 'address'" class="tab-content">
      <AddressRow label="订单付款地址" address="TEYq...hG7FJY" />
      <QrSection :amount="state.amount" :coin="state.coin" />
    </div>

    <!-- 钱包扫码 -->
    <div v-if="activeTab === 'scan'" class="tab-content">
      <div class="qr-dual">
        <div class="qr-item">
          <div class="qr-code">
            <div class="qr-pattern" />
          </div>
          <span class="qr-label">收款码</span>
        </div>
        <div class="qr-item">
          <div class="qr-code">
            <div class="qr-pattern" />
          </div>
          <span class="qr-label">支付码</span>
        </div>
      </div>

      <div class="wallets">
        <div class="wallet-icon" style="background: linear-gradient(135deg, #F6851B, #E2761B)" />
        <div class="wallet-icon" style="background: linear-gradient(135deg, #627EEA, #8C9EFF)" />
        <div class="wallet-icon" style="background: linear-gradient(135deg, #FF4654, #FF6B6B)" />
        <div class="wallet-icon" style="background: linear-gradient(135deg, #26A17B, #3ECEB0)" />
      </div>
      <span class="wallets-hint">支持 MetaMask、imToken、TokenPocket 等主流钱包</span>
    </div>

    <PrimaryButton @click="handleConfirm">我已完成支付</PrimaryButton>

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

.back-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.back-btn:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
}

.tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  padding: 4px;
  background: var(--bg-input);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  margin-bottom: 20px;
}

.tab {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: calc(var(--radius-md) - 3px);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-muted);
  transition: all 0.2s ease;
}

.tab:hover {
  color: var(--text-primary);
}

.tab.active {
  background: var(--bg-card);
  border-color: var(--border-subtle);
  color: var(--text-primary);
  font-weight: 600;
  box-shadow: 0 4px 14px -6px rgba(0, 0, 0, 0.25);
}

.tab-content {
  margin-bottom: 20px;
}

.qr-dual {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 24px 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  margin-bottom: 16px;
}

.qr-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.qr-code {
  width: 140px;
  height: 140px;
  background: white;
  border-radius: var(--radius-md);
  padding: 12px;
  box-shadow: 0 12px 28px -12px rgba(0, 0, 0, 0.35);
}

.qr-pattern {
  width: 100%;
  height: 100%;
  background:
    repeating-conic-gradient(#111 0% 25%, white 0% 50%) 0 0 / 10px 10px;
  border-radius: 4px;
}

.qr-label {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}

.wallets {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 8px;
}

.wallet-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  border: 2px solid var(--bg-card);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.wallets-hint {
  display: block;
  text-align: center;
  font-size: 12px;
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
