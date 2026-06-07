package com.nexusflow.infra.adapter.stub;

import com.nexusflow.domain.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Stub adapter for testing — returns fixed responses.
 */
@Slf4j
@Component
public class StubAdapter implements ChannelAdapter {

    @Override public String channelId() { return "STUB"; }
    @Override public String displayName() { return "Stub Channel"; }

    @Override
    public ChannelUser openUser(String merchantId, String buyerId) {
        return ChannelUser.builder()
                .channelUserId("STUB_USER_" + buyerId)
                .channelId("STUB").newlyCreated(true).build();
    }

    @Override
    public DepositAddress createDepositAddress(CreateDepositRequest req) {
        return DepositAddress.builder()
                .address("0xSTUB_" + req.getOrderId().substring(0, 8))
                .channelOrderId(req.getOrderId())
                .requiredConfirmations(1).build();
    }

    @Override
    public ChannelRefund refund(RefundRequest req) {
        return ChannelRefund.builder()
                .channelRefundId("STUB_REFUND_" + req.getRefundOrderNo())
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
                CurrencyConfig.builder().token("USDT").network("TRC20").decimals(6).minDeposit(new BigDecimal("1")).requiredConfirmations(1).enabled(true).build(),
                CurrencyConfig.builder().token("USDT").network("ERC20").decimals(6).minDeposit(new BigDecimal("1")).requiredConfirmations(1).enabled(true).build()
        );
    }

    @Override
    public ExchangeRate getExchangeRate(String token, String network, String quoteCurrency) {
        return ExchangeRate.builder()
                .token(token).network(network)
                .price(new BigDecimal("1.0002")).quoteCurrency(quoteCurrency)
                .timestamp(Instant.now()).build();
    }

    @Override
    public boolean isHealthy() { return true; }
}