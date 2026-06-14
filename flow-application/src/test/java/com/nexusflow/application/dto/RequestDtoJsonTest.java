package com.nexusflow.application.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestDtoJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createPaymentCommandDeserializesFromJson() throws Exception {
        CreatePaymentCommand command = objectMapper.readValue("""
                {
                  "orderId": "order-1",
                  "currency": "USDT_TRC20",
                  "amount": "12.34",
                  "callbackUrl": "https://merchant.example/callback",
                  "idempotencyKey": "idem-1"
                }
                """, CreatePaymentCommand.class);

        assertThat(command.getOrderId()).isEqualTo("order-1");
        assertThat(command.getAmount()).isEqualTo("12.34");
        assertThat(command.getIdempotencyKey()).isEqualTo("idem-1");
    }

    @Test
    void createOrderRequestDeserializesFromJson() throws Exception {
        CreateOrderRequest request = objectMapper.readValue("""
                {
                  "merchantId": "merchant-1",
                  "merchantOrderNo": "order-2",
                  "amountCrypto": "10",
                  "currencyCrypto": "USDT",
                  "network": "TRC20",
                  "preferredChannel": "SELF_HOSTED_NODE"
                }
                """, CreateOrderRequest.class);

        assertThat(request.getMerchantId()).isEqualTo("merchant-1");
        assertThat(request.getAmountCrypto()).isEqualTo("10");
        assertThat(request.getPreferredChannel()).isEqualTo("SELF_HOSTED_NODE");
    }

    @Test
    void refundRequestDeserializesFromJson() throws Exception {
        RefundRequestDto request = objectMapper.readValue("""
                {
                  "merchantId": "merchant-1",
                  "merchantOrderNo": "order-3",
                  "refundOrderNo": "refund-1",
                  "refundAmountFiat": "5",
                  "toAddress": "TDEST"
                }
                """, RefundRequestDto.class);

        assertThat(request.getRefundOrderNo()).isEqualTo("refund-1");
        assertThat(request.getRefundAmountFiat()).isEqualTo("5");
        assertThat(request.getToAddress()).isEqualTo("TDEST");
    }

    @Test
    void cashierSubmitRequestDeserializesFromJson() throws Exception {
        CashierSubmitRequest request = objectMapper.readValue("""
                {
                  "paymentId": "pay-1",
                  "token": "USDT",
                  "network": "TRC20",
                  "channelId": "SELF_HOSTED_NODE"
                }
                """, CashierSubmitRequest.class);

        assertThat(request.getPaymentId()).isEqualTo("pay-1");
        assertThat(request.getToken()).isEqualTo("USDT");
        assertThat(request.getChannelId()).isEqualTo("SELF_HOSTED_NODE");
    }
}
