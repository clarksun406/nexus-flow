export type CashierStatus = {
  paymentId: string;
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
  transactionCount?: number;
  requiredConfirmations?: number;
  expireTime?: number;
  payTime?: number;
  confirmTime?: number;
};

export type CashierSubmitRequest = {
  paymentId: string;
  token: string;
  network: string;
  channelId?: string;
};

export type CashierSubmitResponse = {
  flowNo?: string;
  paymentId: string;
  token?: string;
  network?: string;
  cryptoAmount?: string;
  fiatAmount?: string;
  fiatCurrency?: string;
  exchangeRate?: string;
  channelId?: string;
  payAddress?: string;
  memo?: string;
  requiredConfirmations?: number;
  expireTime?: number;
  status?: string;
};
