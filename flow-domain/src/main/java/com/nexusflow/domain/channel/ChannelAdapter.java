package com.nexusflow.domain.channel;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

/**
 * Core port: every acquiring channel implements this.
 * Add a new channel = implement + @Component → auto-registered.
 */
public interface ChannelAdapter {

    String channelId();          // "BITMART", "BINANCE_PAY"
    String displayName();

    // ── User ──
    ChannelUser openUser(String merchantId, String buyerId);

    // ── Deposit ──
    @Value @Builder
    class CreateDepositRequest {
        String merchantId; String buyerId; String channelUserId;
        String token; String network; BigDecimal cryptoAmount;
        String orderId; String notifyUrl;
    }
    DepositAddress createDepositAddress(CreateDepositRequest req);

    // ── Refund ──
    @Value @Builder
    class RefundRequest {
        String channelOrderId; String channelUserId;
        BigDecimal refundCryptoAmount; String token; String network;
        String toAddress; String notifyUrl; String refundOrderNo;
    }
    ChannelRefund refund(RefundRequest req);
    ChannelRefund queryRefund(String channelRefundId);

    // ── Currencies ──
    List<CurrencyConfig> getSupportedCurrencies();
    ExchangeRate getExchangeRate(String token, String network, String quoteCurrency);

    // ── Health ──
    boolean isHealthy();
}