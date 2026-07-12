export type Membership = {
  merchantId: string;
  merchantCode?: string;
  displayName?: string;
  roleCode?: string;
  permissions?: string[];
};

export type UserInfo = {
  userId: string;
  email?: string;
  displayName?: string;
  activeMerchantId?: string;
  memberships?: Membership[];
};

export type CreateOrderRequest = {
  merchantId: string;
  merchantOrderNo: string;
  amountFiat?: string;
  currencyFiat?: string;
  amountCrypto?: string;
  currencyCrypto?: string;
  network?: string;
  preferredChannel?: string | null;
  notifyUrl?: string | null;
  returnUrl?: string | null;
  extend?: string | null;
};

export type OrderResponse = {
  merchantId: string;
  paymentId: string;
  merchantOrderNo: string;
  status: string;
  amountFiat?: string;
  currencyFiat?: string;
  amountCrypto?: string;
  currencyCrypto?: string;
  network?: string;
  exchangeRate?: string;
  channelId?: string;
  channelName?: string;
  payAddress?: string;
  memo?: string;
  paidAmountCrypto?: string;
  paidAmountFiat?: string;
  pendingAmount?: string;
  txHash?: string;
  payUrl?: string;
  expireTime?: number;
  payTime?: number;
  confirmTime?: number;
  createTime?: number;
};

export type RefundRequest = {
  merchantId: string;
  merchantOrderNo: string;
  refundOrderNo: string;
  refundAmountFiat: string;
  toAddress?: string | null;
  notifyUrl?: string | null;
};

export type RefundResponse = {
  refundOrderNo: string;
  paymentId?: string;
  channelRefundId?: string;
  status?: string;
  refundAmountFiat?: string;
  refundAmountCrypto?: string;
  exchangeRate?: string;
  token?: string;
  network?: string;
  toAddress?: string;
  txHash?: string;
  createTime?: number;
  confirmTime?: number;
};
