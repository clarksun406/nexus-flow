package com.nexusflow.infra.persistence;

import com.nexusflow.common.PageResult;
import com.nexusflow.domain.order.OrderStatus;
import com.nexusflow.domain.order.PaymentOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Answers.CALLS_REAL_METHODS;

/**
 * Verifies the real default {@link JpaOrderRepository#search} routing/mapping logic
 * by running it against a partial mock with stubbed derived Page queries.
 */
class JpaOrderRepositorySearchTest {

    private JpaOrderRepository repository;

    @BeforeEach
    void setUp() {
        repository = mock(JpaOrderRepository.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
    }

    @Test
    void searchWithoutFiltersUsesFindAllAndMapsDomain() {
        PaymentOrderEntity entity = entity("pay-1", "m-1", "WAITING_PAYMENT");
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createTime"));
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        PageResult<PaymentOrder> result = repository.search(null, null, 0, 20);

        assertEquals(1, result.total());
        assertEquals(1, result.items().size());
        assertEquals("pay-1", result.items().get(0).getPaymentId());
        assertEquals(OrderStatus.WAITING_PAYMENT, result.items().get(0).getStatus());
    }

    @Test
    void searchWithStatusAndMerchantRoutesToCombinedQuery() {
        PaymentOrderEntity entity = entity("pay-2", "m-2", "CONFIRMED");
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createTime"));
        when(repository.findByMerchantIdAndStatus("m-2", "CONFIRMED", pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        PageResult<PaymentOrder> result = repository.search(OrderStatus.CONFIRMED, "m-2", 0, 20);

        assertEquals(1, result.total());
        assertEquals("m-2", result.items().get(0).getMerchantId());
        assertEquals(OrderStatus.CONFIRMED, result.items().get(0).getStatus());
    }

    @Test
    void searchWithStatusOnlyRoutesToStatusQuery() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createTime"));
        when(repository.findByStatus("EXPIRED", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResult<PaymentOrder> result = repository.search(OrderStatus.EXPIRED, null, 0, 20);

        assertEquals(0, result.total());
        assertEquals(0, result.items().size());
    }

    @Test
    void searchClampsOversizedPageSizes() {
        Pageable clamped = PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createTime"));
        when(repository.findAll(clamped)).thenReturn(new PageImpl<>(List.of(), clamped, 0));

        PageResult<PaymentOrder> result = repository.search(null, null, 0, 5000);

        assertEquals(5000, result.size());
        assertEquals(0, result.items().size());
        verify(repository).findAll(clamped);
    }

    private PaymentOrderEntity entity(String paymentId, String merchantId, String status) {
        PaymentOrderEntity e = new PaymentOrderEntity();
        e.setPaymentId(paymentId);
        e.setMerchantId(merchantId);
        e.setMerchantOrderNo("ord-" + paymentId);
        e.setAmountFiat(new BigDecimal("10.00"));
        e.setCurrencyFiat("USD");
        e.setStatus(status);
        e.setCreateTime(Instant.now());
        e.setUpdateTime(Instant.now());
        return e;
    }
}
