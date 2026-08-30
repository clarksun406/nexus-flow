export type UserInfo = {
  userId: string;
  email?: string;
  displayName?: string;
};

export type OpsDashboard = {
  orderStatusCounts?: Record<string, number>;
  paymentStatusCounts?: Record<string, number>;
  orphanStatusCounts?: Record<string, number>;
  channels?: ChannelHealth[];
  recentOrders?: OrderSummary[];
  reconciliation?: ReconciliationSummary;
  alerts?: RiskAlert[];
  generatedAt?: number;
};

export type ChannelHealth = {
  channelId: string;
  displayName?: string;
  status: string;
  supportedCurrencyCount: number;
  message?: string;
};

export type OrderSummary = {
  paymentId: string;
  merchantId: string;
  merchantOrderNo: string;
  status: string;
  amountFiat?: string;
  currencyFiat?: string;
  amountCrypto?: string;
  currencyCrypto?: string;
  network?: string;
  channelId?: string;
  txHash?: string;
  createTime?: number;
  updateTime?: number;
};

export type ReconciliationSummary = {
  pendingExecutionPayments?: number;
  unconfirmedExecutionPayments?: number;
  unmatchedOrphanTransactions?: number;
  partiallyPaidOrders?: number;
  refundProcessingOrders?: number;
};

export type RiskAlert = {
  severity: string;
  code: string;
  message: string;
  count: number;
};

export type OrphanTransaction = {
  id?: string;
  chain: string;
  txHash: string;
  toAddress?: string;
  amount?: string;
  currency?: string;
  blockNumber?: number;
  status?: string;
  firstSeenAt?: number;
  lastSeenAt?: number;
  seenCount?: number;
  resolvedPaymentId?: string;
};

export type DeadLetterStatus = "PENDING" | "REPLAYED" | "IGNORED";

export type OrderListItem = {
  paymentId: string;
  merchantId: string;
  merchantOrderNo: string;
  status: string;
  amountFiat?: string;
  currencyFiat?: string;
  amountCrypto?: string;
  currencyCrypto?: string;
  network?: string;
  channelId?: string;
  paidAmountFiat?: string;
  paidAmountCrypto?: string;
  txHash?: string;
  expireTime?: number;
  payTime?: number;
  createTime?: number;
  updateTime?: number;
};

export type OrderPage = {
  items: OrderListItem[];
  page: number;
  size: number;
  total: number;
};

export type PaymentListItem = {
  id: string;
  orderId: string;
  currency?: string;
  status: string;
  expectedAmount?: string;
  receivedAmount?: string;
  receivingAddress?: string;
  txHash?: string;
  confirmations?: number;
  requiredConfirmations?: number;
  lastFailureReason?: string;
  createdAt?: number;
  detectedAt?: number;
  confirmedAt?: number;
};

export type PaymentPage = {
  items: PaymentListItem[];
  page: number;
  size: number;
  total: number;
};

export type FiatRampListItem = {
  rampOrderId: string;
  merchantId: string;
  merchantOrderNo: string;
  paymentId?: string;
  direction?: string;
  providerId?: string;
  providerOrderId?: string;
  status: string;
  fiatAmount?: string;
  fiatCurrency?: string;
  cryptoAmount?: string;
  token?: string;
  network?: string;
  exchangeRate?: string;
  createTime?: number;
  updateTime?: number;
};

export type FiatRampPage = {
  items: FiatRampListItem[];
  page: number;
  size: number;
  total: number;
};

export type WebhookDeadLetter = {
  id: string;
  deliveryType?: string;
  targetUrl?: string;
  payload?: string;
  eventId?: string;
  eventType?: string;
  paymentId?: string;
  orderId?: string;
  failureReason?: string;
  attempts?: number;
  status?: string;
  createdAt?: number;
  resolvedAt?: number;
};
