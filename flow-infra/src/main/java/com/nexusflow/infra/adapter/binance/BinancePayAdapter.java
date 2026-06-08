package com.nexusflow.infra.adapter.binance;

import com.nexusflow.domain.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Binance Pay channel adapter.
 * Phase 1: stub implementation returning fixed data.
 * TODO: Implement actual Binance Pay REST API calls with HMAC-SHA256 signature.
 *
 * API reference: https://binance-docs.github.io/apidocs/spot/en/
 */
@Slf4j
public class BinancePayAdapter implements ChannelAdapter {

    @Override
    public String channelId() { return "BINANCE"; }

    @Override
    public String displayName() { return "Binance Pay"; }

    @Override
    public ChannelUser openUser(String merchantId, String buyerId) {
        log.debug("BINANCE openUser stub: merchantId={}, buyerId={}", merchantId, buyerId);
        return ChannelUser.builder()
                .channelUserId("BN_USER_" + buyerId)
                .channelId("BINANCE")
                .newlyCreated(true)
                .build();
    }

    @Override
    public DepositAddress createDepositAddress(CreateDepositRequest req) {
        log.debug("BINANCE createDepositAddress stub: orderId={}", req.getOrderId());
        return DepositAddress.builder()
                .address("0xBINANCE_STUB_" + req.getOrderId().substring(0, 8))
                .channelOrderId("BN_ORDER_" + req.getOrderId())
                .requiredConfirmations(12)
                .build();
    }

    @Override
    public ChannelRefund refund(RefundRequest req) {
        log.debug("BINANCE refund stub: refundOrderNo={}", req.getRefundOrderNo());
        return ChannelRefund.builder()
                .channelRefundId("BN_REFUND_" + req.getRefundOrderNo())
                .status("PROCESSING")
                .refundAmount(req.getRefundCryptoAmount())
                .build();
    }

    @Override
    public ChannelRefund queryRefund(String channelRefundId) {
        log.debug("BINANCE queryRefund stub: channelRefundId={}", channelRefundId);
        return ChannelRefund.builder()
                .channelRefundId(channelRefundId)
                .status("SUCCESS")
                .build();
    }

    @Override
    public List<CurrencyConfig> getSupportedCurrencies() {
        return List.of(
                CurrencyConfig.builder()
                        .token("USDT").network("TRC20").decimals(6)
                        .minDeposit(new BigDecimal("1"))
                        .requiredConfirmations(20).enabled(true).build(),
                CurrencyConfig.builder()
                        .token("USDT").network("ERC20").decimals(6)
                        .minDeposit(new BigDecimal("1"))
                        .requiredConfirmations(12).enabled(true).build(),
                CurrencyConfig.builder()
                        .token("USDT").network("BEP20").decimals(6)
                        .minDeposit(new BigDecimal("1"))
                        .requiredConfirmations(15).enabled(true).build(),
                CurrencyConfig.builder()
                        .token("BTC").network("BTC").decimals(8)
                        .minDeposit(new BigDecimal("0.0001"))
                        .requiredConfirmations(2).enabled(true).build()
        );
    }

    @Override
    public ExchangeRate getExchangeRate(String token, String network, String quoteCurrency) {
        log.debug("BINANCE getExchangeRate stub: token={}, network={}, quote={}", token, network, quoteCurrency);
        return ExchangeRate.builder()
                .token(token).network(network)
                .price(new BigDecimal("1.0001"))
                .quoteCurrency(quoteCurrency)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public boolean isHealthy() {
        return true; // stub always healthy
    }
}
