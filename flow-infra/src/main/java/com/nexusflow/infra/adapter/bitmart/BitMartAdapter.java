package com.nexusflow.infra.adapter.bitmart;

import com.nexusflow.domain.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * BitMart channel adapter.
 * Phase 1: stub implementation returning fixed data.
 * TODO: Implement actual BitMart REST API calls with signature.
 */
@Slf4j
public class BitMartAdapter implements ChannelAdapter {

    @Override public String channelId() { return "BITMART"; }
    @Override public String displayName() { return "BitMart"; }

    @Override
    public ChannelUser openUser(String merchantId, String buyerId) {
        // TODO: POST /api/v1/user/open
        return ChannelUser.builder()
                .channelUserId("BM_USER_" + buyerId)
                .channelId("BITMART").newlyCreated(true).build();
    }

    @Override
    public DepositAddress createDepositAddress(CreateDepositRequest req) {
        // TODO: POST /api/v1/payment/order or /api/v1/deposit/address
        return DepositAddress.builder()
                .address("TNPaxLFVxxxxxxxxxxxxxx")
                .channelOrderId(req.getOrderId())
                .requiredConfirmations(20).build();
    }

    @Override
    public ChannelRefund refund(RefundRequest req) {
        // TODO: POST /api/v1/refund
        return ChannelRefund.builder()
                .channelRefundId("BM_REFUND_" + req.getRefundOrderNo())
                .status("PROCESSING").refundAmount(req.getRefundCryptoAmount()).build();
    }

    @Override
    public ChannelRefund queryRefund(String channelRefundId) {
        return ChannelRefund.builder()
                .channelRefundId(channelRefundId).status("SUCCESS").build();
    }

    @Override
    public List<CurrencyConfig> getSupportedCurrencies() {
        return List.of(
                CurrencyConfig.builder().token("USDT").network("TRC20").decimals(6)
                        .minDeposit(new BigDecimal("1")).requiredConfirmations(20).enabled(true).build(),
                CurrencyConfig.builder().token("USDT").network("ERC20").decimals(6)
                        .minDeposit(new BigDecimal("1")).requiredConfirmations(12).enabled(true).build(),
                CurrencyConfig.builder().token("BTC").network("BTC").decimals(8)
                        .minDeposit(new BigDecimal("0.001")).requiredConfirmations(6).enabled(true).build()
        );
    }

    @Override
    public ExchangeRate getExchangeRate(String token, String network, String quoteCurrency) {
        // TODO: GET /api/v1/pricing
        return ExchangeRate.builder()
                .token(token).network(network)
                .price(new BigDecimal("1.0002")).quoteCurrency(quoteCurrency)
                .timestamp(Instant.now()).build();
    }

    @Override
    public boolean isHealthy() { return true; }
}