import { reactive, readonly } from 'vue'
import type { PaymentState, PaymentMethod, NetworkType, NetworkConfig, OrderInfo } from '@/types/payment'

const state = reactive<PaymentState>({
  currentStep: 1,
  selectedMethod: null,
  selectedNetwork: 'tron',
  coin: 'USDT',
  amount: '1.29',
})

const networks: Record<NetworkType, NetworkConfig> = {
  tron: { name: 'Tron', type: 'TRC20', time: '16秒', color: '#ff4654' },
  ethereum: { name: 'Ethereum', type: 'ERC20', time: '32秒', color: '#627ee0' },
  polygon: { name: 'Polygon', type: 'POS', time: '125秒', color: '#8247e5' },
  bnb: { name: 'BNB Chain', type: 'BEP20', time: '4秒', color: '#f6851b' },
  avalanche: { name: 'Avalanche', type: 'C-Chain', time: '25秒', color: '#e84142' },
}

const orderInfo: OrderInfo = {
  merchantOrderId: '4fcbfd9f...7ef04684',
  paymentOrderId: '131772709113001187',
  fiatAmount: '10.01',
  fiatCurrency: 'HKD',
  productName: 'iPad Pro',
  productDesc: '电子产品',
}

export function usePayment() {
  function setStep(step: number) {
    state.currentStep = step
  }

  function selectMethod(method: PaymentMethod) {
    state.selectedMethod = method
  }

  function selectNetwork(network: NetworkType) {
    state.selectedNetwork = network
  }

  function getNetworkConfig(network?: NetworkType): NetworkConfig {
    return networks[network || state.selectedNetwork]
  }

  function getNetworkList() {
    return Object.entries(networks).map(([key, config]) => ({
      key: key as NetworkType,
      ...config,
    }))
  }

  function getOrderInfo(): OrderInfo {
    return orderInfo
  }

  return {
    state: readonly(state),
    setStep,
    selectMethod,
    selectNetwork,
    getNetworkConfig,
    getNetworkList,
    getOrderInfo,
  }
}
