<script setup lang="ts">
import { usePayment } from '@/composables/usePayment'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import RadioButton from '@/components/common/RadioButton.vue'
import type { NetworkType } from '@/types/payment'

const { state, selectNetwork, getNetworkList } = usePayment()
const networks = getNetworkList()

const emit = defineEmits<{
  back: []
  confirm: []
}>()

function getNetworkColor(key: NetworkType) {
  const colorMap: Record<NetworkType, string> = {
    tron: '#FF4654',
    ethereum: '#627EEA',
    polygon: '#8247E5',
    bnb: '#F0B90B',
    avalanche: '#E84142',
  }
  return colorMap[key]
}

function getNetworkDesc(key: NetworkType) {
  const descMap: Record<NetworkType, string> = {
    ethereum: '推荐 · 手续费较低',
    tron: '手续费低 · 到账快',
    bnb: '币安智能链',
    polygon: 'Layer2 扩容方案',
    avalanche: '高性能公链',
  }
  return descMap[key]
}

function getNetworkIcon(key: NetworkType) {
  const iconMap: Record<NetworkType, string> = {
    ethereum: 'Ξ',
    tron: 'T',
    polygon: '⬡',
    bnb: 'B',
    avalanche: 'A',
  }
  return iconMap[key]
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
      <h2 class="page-title">选择处理网络</h2>
    </div>

    <div class="network-card">
      <button
        v-for="network in networks"
        :key="network.key"
        class="network-item"
        :class="{ selected: state.selectedNetwork === network.key }"
        @click="selectNetwork(network.key)"
      >
        <div
          class="network-icon"
          :style="{
            background: getNetworkColor(network.key) + '15',
            color: getNetworkColor(network.key)
          }"
        >
          {{ getNetworkIcon(network.key) }}
        </div>
        <div class="network-info">
          <span class="network-name">{{ network.name }} ({{ network.type }})</span>
          <span class="network-desc">{{ getNetworkDesc(network.key) }}</span>
        </div>
        <RadioButton :active="state.selectedNetwork === network.key" />
      </button>

      <div class="coin-info">
        <div class="coin-dot" />
        <span class="coin-name">{{ state.coin }}</span>
        <span class="coin-label">· Tether USD</span>
      </div>

      <div class="warning">
        <div class="warning-icon">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
        </div>
        <span>请确保选择正确的网络，错误的网络可能导致资产丢失且无法恢复。</span>
      </div>
    </div>

    <PrimaryButton @click="$emit('confirm')">确认网络</PrimaryButton>

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

.network-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  margin-bottom: 20px;
}

.network-item {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 16px 20px;
  text-align: left;
  transition: all 0.2s ease;
  border-bottom: 1px solid var(--border-subtle);
}

.network-item:last-of-type {
  border-bottom: none;
}

.network-item:hover {
  background: var(--bg-input);
}

.network-item.selected {
  background: var(--color-brand-glow);
  box-shadow: inset 0 0 0 1px var(--color-brand);
}

.network-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 800;
}

.network-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.network-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.network-desc {
  font-size: 13px;
  color: var(--text-muted);
}

.coin-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  background: var(--bg-input);
  border-top: 1px solid var(--border-subtle);
}

.coin-dot {
  width: 18px;
  height: 18px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, #26A17B, #3ECEB0);
}

.coin-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.coin-label {
  font-size: 13px;
  color: var(--text-muted);
}

.warning {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 20px;
  background: var(--color-warning-glow);
  border-top: 1px solid rgba(245, 158, 11, 0.2);
}

.warning-icon {
  color: var(--color-warning);
  flex-shrink: 0;
  margin-top: 2px;
}

.warning span {
  font-size: 13px;
  color: var(--color-warning);
  line-height: 1.5;
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
