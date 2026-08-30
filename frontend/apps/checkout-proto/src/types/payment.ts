export type PaymentMethod = 'address' | 'wallet'

export type NetworkType = 'tron' | 'ethereum' | 'polygon' | 'bnb' | 'avalanche'

export type TabType = 'address' | 'plugin' | 'scan'

export interface NetworkConfig {
  name: string
  type: string
  time: string
  color: string
}

export interface PaymentState {
  currentStep: number
  selectedMethod: PaymentMethod | null
  selectedNetwork: NetworkType
  coin: string
  amount: string
}

export interface OrderInfo {
  merchantOrderId: string
  paymentOrderId: string
  fiatAmount: string
  fiatCurrency: string
  productName: string
  productDesc: string
}
