package com.nexusflow.infra.adapter.selfhosted;

import com.nexusflow.application.PaymentApplicationService;
import com.nexusflow.application.dto.CreatePaymentCommand;
import com.nexusflow.application.dto.PaymentResponse;
import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.channel.ChannelRefund;
import com.nexusflow.domain.channel.ChannelUser;
import com.nexusflow.domain.channel.CurrencyConfig;
import com.nexusflow.domain.channel.DepositAddress;
import com.nexusflow.domain.channel.ExchangeRate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nexusflow.self-hosted-channel.enabled", havingValue = "true")
public class SelfHostedNodeAdapter implements ChannelAdapter {

    public static final String CHANNEL_ID = "SELF_HOSTED_NODE";
    private static final int DEFAULT_REQUIRED_CONFIRMATIONS = 12;

    private final PaymentApplicationService paymentService;

    @Override
    public String channelId() {
        return CHANNEL_ID;
    }

    @Override
    public String displayName() {
        return "Self-hosted Node";
    }

    @Override
    public ChannelUser openUser(String merchantId, String buyerId) {
        return ChannelUser.builder()
                .channelId(CHANNEL_ID)
                .channelUserId(CHANNEL_ID + "_" + merchantId + "_" + buyerId)
                .newlyCreated(false)
                .build();
    }

    @Override
    public DepositAddress createDepositAddress(CreateDepositRequest req) {
        String currency = toExecutionCurrency(req.getToken(), req.getNetwork());
        PaymentResponse payment = paymentService.createPayment(CreatePaymentCommand.builder()
                .orderId(req.getOrderId())
                .currency(currency)
                .amount(req.getCryptoAmount().toPlainString())
                .callbackUrl(req.getNotifyUrl())
                .idempotencyKey(CHANNEL_ID + ":" + req.getOrderId())
                .build());

        log.info("Self-hosted deposit delegated to execution payment: orderId={}, paymentId={}",
                req.getOrderId(), payment.getPaymentId());

        return DepositAddress.builder()
                .address(payment.getReceivingAddress())
                .channelOrderId(req.getOrderId())
                .requiredConfirmations(DEFAULT_REQUIRED_CONFIRMATIONS)
                .build();
    }

    @Override
    public ChannelRefund refund(RefundRequest req) {
        throw new UnsupportedOperationException("Self-hosted node refunds are not implemented");
    }

    @Override
    public ChannelRefund queryRefund(String channelRefundId) {
        throw new UnsupportedOperationException("Self-hosted node refunds are not implemented");
    }

    @Override
    public List<CurrencyConfig> getSupportedCurrencies() {
        return List.of(
                CurrencyConfig.builder()
                        .token("USDT")
                        .network("TRC20")
                        .decimals(6)
                        .minDeposit(BigDecimal.ONE)
                        .requiredConfirmations(DEFAULT_REQUIRED_CONFIRMATIONS)
                        .enabled(true)
                        .build(),
                CurrencyConfig.builder()
                        .token("USDT")
                        .network("ERC20")
                        .decimals(6)
                        .minDeposit(BigDecimal.ONE)
                        .requiredConfirmations(DEFAULT_REQUIRED_CONFIRMATIONS)
                        .enabled(true)
                        .build()
        );
    }

    @Override
    public ExchangeRate getExchangeRate(String token, String network, String quoteCurrency) {
        if (!"USDT".equalsIgnoreCase(token)) {
            throw new IllegalArgumentException("Self-hosted channel supports USDT deposits only");
        }
        if (!"TRC20".equalsIgnoreCase(network) && !"ERC20".equalsIgnoreCase(network)) {
            throw new IllegalArgumentException("Unsupported self-hosted USDT network: " + network);
        }
        if (!"USD".equalsIgnoreCase(quoteCurrency) && !"USDT".equalsIgnoreCase(quoteCurrency)) {
            throw new IllegalArgumentException("Self-hosted channel has no quote source for: " + quoteCurrency);
        }
        return ExchangeRate.builder()
                .token("USDT")
                .network(network.toUpperCase())
                .quoteCurrency(quoteCurrency.toUpperCase())
                .price(BigDecimal.ONE)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    private String toExecutionCurrency(String token, String network) {
        if (!"USDT".equalsIgnoreCase(token)) {
            throw new IllegalArgumentException("Unsupported self-hosted token: " + token);
        }
        return switch (network.toUpperCase()) {
            case "TRC20" -> "USDT_TRC20";
            case "ERC20" -> "USDT_ERC20";
            default -> throw new IllegalArgumentException("Unsupported self-hosted network: " + network);
        };
    }
}
