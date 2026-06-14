package com.nexusflow.infra.adapter.selfhosted;

import com.nexusflow.application.PaymentApplicationService;
import com.nexusflow.application.dto.CreatePaymentCommand;
import com.nexusflow.application.dto.PaymentResponse;
import com.nexusflow.domain.channel.ChannelAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SelfHostedNodeAdapterTest {

    private PaymentApplicationService paymentService;
    private SelfHostedNodeAdapter adapter;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentApplicationService.class);
        adapter = new SelfHostedNodeAdapter(paymentService);
    }

    @Test
    void createDepositAddressDelegatesToExecutionPayment() {
        when(paymentService.createPayment(org.mockito.ArgumentMatchers.any()))
                .thenReturn(PaymentResponse.builder()
                        .paymentId("crypto-pay-1")
                        .orderId("order-1")
                        .receivingAddress("TADDR")
                        .status("PENDING")
                        .build());
        ChannelAdapter.CreateDepositRequest request = ChannelAdapter.CreateDepositRequest.builder()
                .merchantId("merchant-1")
                .channelUserId("user-1")
                .token("USDT")
                .network("TRC20")
                .cryptoAmount(new BigDecimal("12.34"))
                .orderId("order-1")
                .notifyUrl("https://api.example/callback/SELF_HOSTED_NODE/payment")
                .build();

        var deposit = adapter.createDepositAddress(request);

        assertEquals("TADDR", deposit.getAddress());
        assertEquals("order-1", deposit.getChannelOrderId());
        assertEquals(12, deposit.getRequiredConfirmations());
        ArgumentCaptor<CreatePaymentCommand> captor = ArgumentCaptor.forClass(CreatePaymentCommand.class);
        verify(paymentService).createPayment(captor.capture());
        CreatePaymentCommand command = captor.getValue();
        assertEquals("order-1", command.getOrderId());
        assertEquals("USDT_TRC20", command.getCurrency());
        assertEquals("12.34", command.getAmount());
        assertEquals("https://api.example/callback/SELF_HOSTED_NODE/payment", command.getCallbackUrl());
        assertEquals("SELF_HOSTED_NODE:order-1", command.getIdempotencyKey());
    }

    @Test
    void getExchangeRateReturnsUsdStablecoinParity() {
        var rate = adapter.getExchangeRate("USDT", "ERC20", "USD");

        assertEquals("USDT", rate.getToken());
        assertEquals("ERC20", rate.getNetwork());
        assertEquals("USD", rate.getQuoteCurrency());
        assertEquals(0, BigDecimal.ONE.compareTo(rate.getPrice()));
    }

    @Test
    void rejectsUnsupportedToken() {
        assertThrows(IllegalArgumentException.class,
                () -> adapter.getExchangeRate("BTC", "BTC", "USD"));
    }

    @Test
    void refundCreatesProcessingTaskForExternalSigner() {
        ChannelAdapter.RefundRequest request = ChannelAdapter.RefundRequest.builder()
                .refundOrderNo("ref-1")
                .refundCryptoAmount(new BigDecimal("12.34"))
                .token("USDT")
                .network("TRC20")
                .toAddress("TREFUND")
                .notifyUrl("https://api.example/callback/SELF_HOSTED_NODE/refund")
                .build();

        var refund = adapter.refund(request);

        assertEquals("SELF_HOSTED_NODE_REFUND_ref-1", refund.getChannelRefundId());
        assertEquals("PROCESSING", refund.getStatus());
        assertEquals(0, new BigDecimal("12.34").compareTo(refund.getRefundAmount()));
    }

    @Test
    void refundRequiresDestinationAddress() {
        ChannelAdapter.RefundRequest request = ChannelAdapter.RefundRequest.builder()
                .refundOrderNo("ref-1")
                .refundCryptoAmount(new BigDecimal("12.34"))
                .token("USDT")
                .network("TRC20")
                .build();

        assertThrows(IllegalArgumentException.class, () -> adapter.refund(request));
    }
}
