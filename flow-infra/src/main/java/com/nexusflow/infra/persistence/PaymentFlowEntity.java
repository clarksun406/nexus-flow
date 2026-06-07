package com.nexusflow.infra.persistence;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "payment_flows")
public class PaymentFlowEntity {

    @Id @Column(name = "flow_no", length = 64)
    private String flowNo;

    @Column(name = "payment_id", length = 64, nullable = false)
    private String paymentId;

    @Column(name = "channel_id", length = 32) private String channelId;
    @Column(length = 16) private String token;
    @Column(length = 16) private String network;
    @Column(name = "crypto_amount", precision = 36, scale = 18) private BigDecimal cryptoAmount;
    @Column(name = "fiat_amount", precision = 36, scale = 18) private BigDecimal fiatAmount;
    @Column(name = "fiat_currency", length = 8) private String fiatCurrency;
    @Column(name = "exchange_rate", precision = 36, scale = 18) private BigDecimal exchangeRate;
    @Column(name = "pay_address", length = 256) private String payAddress;
    @Column(length = 256) private String memo;
    @Column(name = "payer_address", length = 256) private String payerAddress;
    @Column(length = 32) private String status;
    @Column(name = "tx_hash", length = 128) private String txHash;
    @Column(name = "paid_amount", precision = 36, scale = 18) private BigDecimal paidAmount;
    @Column(name = "create_time") private Instant createTime;
    @Column(name = "update_time") private Instant updateTime;
}