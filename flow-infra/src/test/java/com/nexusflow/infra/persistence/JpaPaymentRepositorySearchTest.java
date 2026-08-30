package com.nexusflow.infra.persistence;

import com.nexusflow.common.PageResult;
import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaPaymentRepositorySearchTest {

    private SpringDataCryptoPaymentRepository springDataRepository;
    private JpaPaymentRepository repository;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataCryptoPaymentRepository.class);
        repository = new JpaPaymentRepository(springDataRepository);
    }

    @Test
    void searchWithStatusRoutesToStatusQueryAndMapsDomain() {
        CryptoPaymentEntity entity = new CryptoPaymentEntity();
        entity.setId("exec-1");
        entity.setOrderId("pay-1");
        entity.setCurrency("USDT_TRC20");
        entity.setExpectedAmount(new BigDecimal("10.00"));
        entity.setReceivedAmount(new BigDecimal("4.00"));
        entity.setStatus("DETECTED");
        entity.setConfirmations(2);
        entity.setRequiredConfirmations(3);
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(springDataRepository.findByStatus("DETECTED", pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        PageResult<CryptoPayment> result = repository.search(PaymentStatus.DETECTED, 0, 20);

        verify(springDataRepository).findByStatus("DETECTED", pageable);
        assertEquals(1, result.total());
        assertEquals(1, result.items().size());
        assertEquals("exec-1", result.items().get(0).getId());
        assertEquals(PaymentStatus.DETECTED, result.items().get(0).getStatus());
        assertEquals(0, result.items().get(0).getExpected().getAmount().compareTo(new BigDecimal("10.00")));
    }

    @Test
    void searchWithoutStatusUsesFindAll() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(springDataRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResult<CryptoPayment> result = repository.search(null, 0, 20);

        verify(springDataRepository).findAll(pageable);
        assertEquals(0, result.total());
        assertEquals(0, result.items().size());
    }

    @Test
    void moneyIsAbsentWhenAmountsAreNull() {
        CryptoPaymentEntity entity = new CryptoPaymentEntity();
        entity.setId("exec-2");
        entity.setOrderId("pay-2");
        entity.setStatus("PENDING");
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(springDataRepository.findByStatus("PENDING", pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        PageResult<CryptoPayment> result = repository.search(PaymentStatus.PENDING, 0, 20);

        assertNull(result.items().get(0).getExpected());
        assertNull(result.items().get(0).getReceived());
    }
}
