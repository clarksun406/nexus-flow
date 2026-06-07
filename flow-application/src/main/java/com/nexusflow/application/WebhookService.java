package com.nexusflow.application;

import com.nexusflow.domain.event.DomainEvent;
import com.nexusflow.domain.order.PaymentOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookClient webhookClient;

    @Async
    public void notifyMerchant(PaymentOrder order, List<DomainEvent> events) {
        String payload = buildPayload(order);
        webhookClient.sendWithRetry(order.getNotifyUrl(), payload);
    }

    private String buildPayload(PaymentOrder order) {
        return String.format(
                "{\"payment_id\":\"%s\",\"merchant_order_no\":\"%s\",\"status\":\"%s\"," +
                "\"amount\":\"%s\",\"paid_amount\":\"%s\",\"paid_crypto_amount\":\"%s\"," +
                "\"tx_hash\":\"%s\",\"currency\":\"%s\",\"timestamp\":%d}",
                order.getPaymentId(), order.getMerchantOrderNo(), order.getStatus().name(),
                order.getAmountFiat().toPlainString(), order.getPaidAmountFiat().toPlainString(),
                order.getPaidAmountCrypto().toPlainString(), order.getTxHash(),
                order.getCurrencyFiat(), System.currentTimeMillis());
    }
}