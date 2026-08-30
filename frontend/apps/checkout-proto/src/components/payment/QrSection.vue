<script setup lang="ts">
defineProps<{
  amount: string
  coin: string
}>()
</script>

<template>
  <div class="qr-section">
    <div class="qr-container">
      <div class="qr-code">
        <div class="qr-pattern" />
        <div class="qr-logo">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z" />
            <path d="M2 17l10 5 10-5" />
            <path d="M2 12l10 5 10-5" />
          </svg>
        </div>
      </div>
      <span class="qr-hint">扫码支付</span>
    </div>

    <div class="qr-tips">
      <div class="tip-item">
        <span class="tip-num">1</span>
        <p>
          请确认向此付款地址转出准确金额
          <strong>{{ amount }} {{ coin }}</strong>（不含 gas 费），否则可能导致订单失败。
        </p>
      </div>
      <div class="tip-item">
        <span class="tip-num">2</span>
        <p>请勿转入<strong>任何非 {{ coin }}</strong> 资金，否则支付将直接失败。</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.qr-section {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 24px;
  padding: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.qr-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.qr-code {
  width: 160px;
  height: 160px;
  background: white;
  border-radius: var(--radius-md);
  padding: 16px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 16px 32px -14px rgba(0, 0, 0, 0.4);
}

.qr-pattern {
  width: 100%;
  height: 100%;
  background:
    repeating-conic-gradient(#111 0% 25%, white 0% 50%) 0 0 / 12px 12px,
    linear-gradient(135deg, #111 25%, transparent 25%) 6px 0 / 6px 6px;
  border-radius: 4px;
}

.qr-logo {
  position: absolute;
  width: 40px;
  height: 40px;
  background: var(--gradient-brand);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px -4px var(--color-brand-glow);
}

.qr-hint {
  font-size: 12px;
  color: var(--text-dim);
  font-weight: 500;
}

.qr-tips {
  display: flex;
  flex-direction: column;
  gap: 16px;
  justify-content: center;
}

.tip-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.tip-num {
  width: 24px;
  height: 24px;
  border-radius: var(--radius-full);
  background: var(--bg-elevated);
  border: 1px solid var(--border-default);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  flex-shrink: 0;
}

.tip-item p {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
}

.tip-item strong {
  color: var(--color-danger);
  font-weight: 600;
}
</style>
