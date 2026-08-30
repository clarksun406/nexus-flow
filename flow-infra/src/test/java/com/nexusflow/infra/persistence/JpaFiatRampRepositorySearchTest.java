package com.nexusflow.infra.persistence;

import com.nexusflow.common.PageResult;
import com.nexusflow.domain.fiat.FiatRampOrder;
import com.nexusflow.domain.fiat.FiatRampStatus;
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

class JpaFiatRampRepositorySearchTest {

    private SpringDataFiatRampOrderRepository springDataRepository;
    private JpaFiatRampRepository repository;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataFiatRampOrderRepository.class);
        repository = new JpaFiatRampRepository(springDataRepository);
    }

    @Test
    void searchWithStatusAndMerchantRoutesToCombinedQuery() {
        FiatRampOrderEntity entity = entity("ramp-1", "m-1", "PENDING_PAYMENT");
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createTime"));
        when(springDataRepository.findByMerchantIdAndStatus("m-1", "PENDING_PAYMENT", pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        PageResult<FiatRampOrder> result = repository.search(FiatRampStatus.PENDING_PAYMENT, "m-1", 0, 20);

        verify(springDataRepository).findByMerchantIdAndStatus("m-1", "PENDING_PAYMENT", pageable);
        assertEquals(1, result.total());
        assertEquals("ramp-1", result.items().get(0).getRampOrderId());
        assertEquals(FiatRampStatus.PENDING_PAYMENT, result.items().get(0).getStatus());
    }

    @Test
    void searchWithoutFiltersUsesFindAll() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createTime"));
        when(springDataRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResult<FiatRampOrder> result = repository.search(null, null, 0, 20);

        verify(springDataRepository).findAll(pageable);
        assertEquals(0, result.total());
        assertEquals(0, result.items().size());
    }

    private FiatRampOrderEntity entity(String rampOrderId, String merchantId, String status) {
        FiatRampOrderEntity e = new FiatRampOrderEntity();
        e.setRampOrderId(rampOrderId);
        e.setMerchantId(merchantId);
        e.setMerchantOrderNo("rord-" + rampOrderId);
        e.setDirection("ON_RAMP");
        e.setFiatAmount(new BigDecimal("100.00"));
        e.setFiatCurrency("USD");
        e.setStatus(status);
        e.setCreateTime(Instant.now());
        e.setUpdateTime(Instant.now());
        return e;
    }
}
