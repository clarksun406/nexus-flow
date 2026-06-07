package com.nexusflow.infra.persistence;

import com.nexusflow.domain.order.FlowRepository;
import com.nexusflow.domain.order.FlowStatus;
import com.nexusflow.domain.order.PaymentFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaFlowRepository extends FlowRepository, JpaRepository<PaymentFlowEntity, String> {

    @Override
    default void save(PaymentFlow flow) { save(toEntity(flow)); }

    @Override
    default Optional<PaymentFlow> findByFlowNo(String flowNo) {
        return findById(flowNo).map(this::toDomain);
    }

    @Override
    default List<PaymentFlow> findByPaymentId(String paymentId) {
        return findAllByPaymentId(paymentId).stream().map(this::toDomain).toList();
    }

    @Override
    default Optional<PaymentFlow> findActiveByPaymentId(String paymentId) {
        return findByPaymentIdAndStatus(paymentId, "WAITING").map(this::toDomain);
    }

    List<PaymentFlowEntity> findAllByPaymentId(String paymentId);
    Optional<PaymentFlowEntity> findByPaymentIdAndStatus(String paymentId, String status);

    default PaymentFlowEntity toEntity(PaymentFlow f) {
        PaymentFlowEntity e = new PaymentFlowEntity();
        e.setFlowNo(f.getFlowNo()); e.setPaymentId(f.getPaymentId()); e.setChannelId(f.getChannelId());
        e.setToken(f.getToken()); e.setNetwork(f.getNetwork()); e.setCryptoAmount(f.getCryptoAmount());
        e.setFiatAmount(f.getFiatAmount()); e.setFiatCurrency(f.getFiatCurrency());
        e.setExchangeRate(f.getExchangeRate()); e.setPayAddress(f.getPayAddress()); e.setMemo(f.getMemo());
        e.setPayerAddress(f.getPayerAddress()); e.setStatus(f.getStatus().name());
        e.setTxHash(f.getTxHash()); e.setPaidAmount(f.getPaidAmount());
        e.setCreateTime(f.getCreateTime()); e.setUpdateTime(f.getUpdateTime());
        return e;
    }

    default PaymentFlow toDomain(PaymentFlowEntity e) {
        return PaymentFlow.builder()
                .flowNo(e.getFlowNo()).paymentId(e.getPaymentId()).channelId(e.getChannelId())
                .token(e.getToken()).network(e.getNetwork()).cryptoAmount(e.getCryptoAmount())
                .fiatAmount(e.getFiatAmount()).fiatCurrency(e.getFiatCurrency())
                .exchangeRate(e.getExchangeRate()).payAddress(e.getPayAddress())
                .memo(e.getMemo()).payerAddress(e.getPayerAddress()).build();
    }
}