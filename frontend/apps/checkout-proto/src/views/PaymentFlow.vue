<script setup lang="ts">
import { usePayment } from '@/composables/usePayment'
import AppHeader from '@/components/layout/AppHeader.vue'
import OrderSidebar from '@/components/layout/OrderSidebar.vue'
import PayMethodPage from './PayMethodPage.vue'
import NetworkPage from './NetworkPage.vue'
import PayDetailPage from './PayDetailPage.vue'
import SuccessPage from './SuccessPage.vue'

const { state, setStep } = usePayment()

function goToStep(step: number) {
  setStep(step)
}
</script>

<template>
  <div class="flow-container aurora-bg">
    <div class="flow-card">
      <AppHeader />

      <div class="flow-body">
        <OrderSidebar />

        <main class="flow-content">
          <PayMethodPage
            v-if="state.currentStep === 1"
            @next="goToStep(3)"
            @go-network="goToStep(2)"
          />
          <NetworkPage
            v-if="state.currentStep === 2"
            @back="goToStep(1)"
            @confirm="goToStep(1)"
          />
          <PayDetailPage
            v-if="state.currentStep === 3"
            @back="goToStep(1)"
            @success="goToStep(4)"
          />
          <SuccessPage v-if="state.currentStep === 4" />
        </main>
      </div>
    </div>
  </div>
</template>

<style scoped>
.flow-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  transition: background 0.3s ease;
}

.flow-card {
  position: relative;
  width: 100%;
  max-width: 980px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-elevated);
  overflow: hidden;
}

/* 顶部品牌渐变发丝线 */
.flow-card::before {
  content: "";
  position: absolute;
  inset: 0 0 auto 0;
  height: 2px;
  background: var(--gradient-brand);
  opacity: 0.85;
  z-index: 1;
}

.flow-body {
  display: grid;
  grid-template-columns: 300px 1fr;
  min-height: 600px;
}

.flow-content {
  padding: 28px 32px;
  overflow-y: auto;
}

@media (max-width: 840px) {
  .flow-container {
    padding: 0;
    align-items: stretch;
  }

  .flow-card {
    border-radius: 0;
    min-height: 100vh;
  }

  .flow-body {
    grid-template-columns: 1fr;
  }
}
</style>
