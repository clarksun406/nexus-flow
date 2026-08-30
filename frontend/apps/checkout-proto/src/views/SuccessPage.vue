<script setup lang="ts">
import { usePayment } from '@/composables/usePayment'
import { useToast } from '@/composables/useToast'

const { state, getNetworkConfig } = usePayment()
const { show } = useToast()
const network = getNetworkConfig()

const now = new Date()
const timeStr =
  now.getFullYear() +
  '-' +
  String(now.getMonth() + 1).padStart(2, '0') +
  '-' +
  String(now.getDate()).padStart(2, '0') +
  ' ' +
  String(now.getHours()).padStart(2, '0') +
  ':' +
  String(now.getMinutes()).padStart(2, '0') +
  ':' +
  String(now.getSeconds()).padStart(2, '0')

function copyText(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    show('已复制到剪贴板')
  })
}
</script>

<template>
  <div class="page">
    <div class="success-container">
      <div class="success-icon">
        <div class="icon-ring" />
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="20 6 9 17 4 12" />
        </svg>
      </div>

      <h2 class="success-title">支付成功</h2>
      <p class="success-desc">您的订单已支付完成，感谢您的购买！</p>

      <div class="receipt-card">
        <div class="receipt-row">
          <span class="receipt-label">订单金额</span>
          <div class="receipt-value amount">
            <span class="num">{{ state.amount }}</span>
            <span class="unit">{{ state.coin }}</span>
          </div>
        </div>

        <div class="receipt-divider" />

        <div class="receipt-row">
          <span class="receipt-label">支付网络</span>
          <span class="receipt-value">
            <span class="network-dot" :style="{ background: network.color }" />
            {{ network.name }} ({{ network.type }})
          </span>
        </div>

        <div class="receipt-row">
          <span class="receipt-label">交易哈希</span>
          <span class="receipt-value mono">
            0x7f3a...8b2c
            <button class="copy-btn" @click="copyText('0x7f3a8b2c...')">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
              </svg>
            </button>
          </span>
        </div>

        <div class="receipt-row">
          <span class="receipt-label">完成时间</span>
          <span class="receipt-value mono">{{ timeStr }}</span>
        </div>
      </div>

      <div class="actions">
        <button class="primary-btn" @click="show('正在跳转...')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" />
            <line x1="16" y1="17" x2="8" y2="17" />
            <polyline points="10 9 9 9 8 9" />
          </svg>
          查看订单
        </button>
        <button class="secondary-btn" @click="show('已返回首页')">
          返回首页
        </button>
      </div>

      <div class="footer-note">
        <p>订单确认邮件已发送至您的邮箱</p>
        <p>如有问题请联系<a href="#">客服支持</a></p>
      </div>
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

.success-container {
  text-align: center;
  padding: 40px 20px;
}

.success-icon {
  width: 88px;
  height: 88px;
  margin: 0 auto 28px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-success), #16A34A);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  position: relative;
  box-shadow: 0 18px 40px -14px rgba(34, 197, 94, 0.55);
  animation: popIn 0.5s ease;
}

.icon-ring {
  position: absolute;
  inset: -8px;
  border-radius: var(--radius-full);
  border: 2px solid var(--color-success);
  opacity: 0.3;
  animation: ringPulse 2s infinite;
}

@keyframes popIn {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes ringPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 0.3;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.1;
  }
}

.success-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.success-desc {
  font-size: 15px;
  color: var(--text-muted);
  margin-bottom: 32px;
}

.receipt-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 4px 0;
  margin-bottom: 28px;
  text-align: left;
}

.receipt-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
}

.receipt-label {
  font-size: 13px;
  color: var(--text-muted);
}

.receipt-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.receipt-value.amount {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.receipt-value .num {
  font-size: 20px;
  font-weight: 700;
}

.receipt-value .unit {
  font-size: 14px;
  color: var(--text-muted);
}

.network-dot {
  width: 10px;
  height: 10px;
  border-radius: var(--radius-full);
}

.receipt-value.mono {
  font-family: var(--font-mono);
  font-size: 13px;
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
}

.copy-btn:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.receipt-divider {
  height: 1px;
  background: var(--border-subtle);
  margin: 0 20px;
}

.actions {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.primary-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 52px;
  background: var(--gradient-brand);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  color: white;
  box-shadow: 0 10px 24px -10px var(--color-brand-glow);
  transition: all 0.2s ease;
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px var(--color-brand-glow);
}

.secondary-btn {
  flex: 1;
  height: 52px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  transition: all 0.2s ease;
}

.secondary-btn:hover {
  background: var(--bg-card-hover);
}

.footer-note {
  font-size: 13px;
  color: var(--text-dim);
  line-height: 1.8;
}

.footer-note a {
  color: var(--color-brand-light);
  font-weight: 500;
}

.footer-note a:hover {
  text-decoration: underline;
}
</style>
