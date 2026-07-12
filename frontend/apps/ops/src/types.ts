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
