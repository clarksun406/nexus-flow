package com.nexusflow.infra.persistence;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "refund_orders")
public class RefundOrderEntity {

    @Id @Column(name = "refund_order_no", length = 64)
    private String refundOrderNo;

    @Column(name = "payment_id", length = 64, nullable = false)
    private String paymentId;

    @Column(name = "channel_refund_id", length = 128) private String channelRefundId;
    @Column(name = "refund_amount_fiat", precision = 36, scale = 18) private BigDecimal refundAmountFiat;
    @Column(name = "refund_amount_crypto", precision = 36, scale = 18) private BigDecimal refundAmountCrypto;
    @Column(name = "exchange_rate", precision = 36, scale = 18) private BigDecimal exchangeRate;
    @Column(length = 16) private String token;
    @Column(length = 16) private String network;
    @Column(name = "to_address", length = 256) private String toAddress;
    @Column(name = "tx_hash", length = 128) private String txHash;
    @Column(name = "notify_url", length = 512) private String notifyUrl;
    @Column(length = 32) private String status;
    @Column(name = "create_time") private Instant createTime;
    @Column(name = "confirm_time") private Instant confirmTime;
    @Column(name = "update_time") private Instant updateTime;
    @Version private Long version;
}