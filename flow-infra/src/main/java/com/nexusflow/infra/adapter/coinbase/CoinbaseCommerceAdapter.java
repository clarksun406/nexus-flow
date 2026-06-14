package com.nexusflow.infra.adapter.coinbase;

import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.channel.ChannelRefund;
import com.nexusflow.domain.channel.ChannelUser;
import com.nexusflow.domain.channel.CurrencyConfig;
import com.nexusflow.domain.channel.DepositAddress;
import com.nexusflow.domain.channel.ExchangeRate;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Coinbase Commerce channel adapter.
 * Phase 2: stub implementation; replace with signed Coinbase Commerce REST calls.
 */
@Slf4j
public class CoinbaseCommerceAdapter implements ChannelAdapter {

    @Override
    public String channelId() {
        return "COINBASE_COMMERCE";
    }

    @Override
    public String displayName() {
        return "Coinbase Commerce";
    }

    @Override
    public ChannelUser openUser(String merchantId, String buyerId) {
        log.debug("COINBASE_COMMERCE openUser stub: merchantId={}, buyerId={}", merchantId, buyerId);
        return ChannelUser.builder()
                .channelUserId("CB_USER_" + buyerId)
                .channelId(channelId())
                .newlyCreated(true)
                .build();
    }

    @Override
    public DepositAddress createDepositAddress(CreateDepositRequest req) {
        log.debug("COINBASE_COMMERCE createDepositAddress stub: orderId={}", req.getOrderId());
        return DepositAddress.builder()
                .address("0xCOINBASE_STUB_" + req.getOrderId().substring(0, 8))
                .channelOrderId("CB_CHARGE_" + req.getOrderId())
                .requiredConfirmations(12)
                .build();
    }

    @Override
    public ChannelRefund refund(RefundRequest req) {
        log.debug("COINBASE_COMMERCE refund stub: refundOrderNo={}", req.getRefundOrderNo());
        return ChannelRefund.builder()
                .channelRefundId("CB_REFUND_" + req.getRefundOrderNo())
                .status("PROCESSING")
                .refundAmount(req.getRefundCryptoAmount())
                .build();
    }

    @Override
    public ChannelRefund queryRefund(String channelRefundId) {
        log.debug("COINBASE_COMMERCE queryRefund stub: channelRefundId={}", channelRefundId);
        return ChannelRefund.builder()
                .channelRefundId(channelRefundId)
                .status("SUCCESS")
                .build();
    }

    @Override
    public List<CurrencyConfig> getSupportedCurrencies() {
        return List.of(
                CurrencyConfig.builder()
                        .token("USDC").network("ERC20").decimals(6)
                        .minDeposit(new BigDecimal("1"))
                        .requiredConfirmations(12).enabled(true).build(),
                CurrencyConfig.builder()
                        .token("USDT").network("ERC20").decimals(6)
                        .minDeposit(new BigDecimal("1"))
                        .requiredConfirmations(12).enabled(true).build(),
                CurrencyConfig.builder()
                        .token("BTC").network("BTC").decimals(8)
                        .minDeposit(new BigDecimal("0.0001"))
                        .requiredConfirmations(2).enabled(true).build()
        );
    }

    @Override
    public ExchangeRate getExchangeRate(String token, String network, String quoteCurrency) {
        log.debug("COINBASE_COMMERCE getExchangeRate stub: token={}, network={}, quote={}",
                token, network, quoteCurrency);
        return ExchangeRate.builder()
                .token(token)
                .network(network)
                .price(new BigDecimal("1.0000"))
                .quoteCurrency(quoteCurrency)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public boolean isHealthy() {
        return true;
    }
}
