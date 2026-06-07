package com.nexusflow.infra.persistence;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "payment_orders")
public class PaymentOrderEntity {

    @Id @Column(name = "payment_id", length = 64)
    private String paymentId;

    @Column(name = "merchant_id", length = 64, nullable = false)
    private String merchantId;

    @Column(name = "merchant_order_no", length = 128, nullable = false)
    private String merchantOrderNo;

    @Column(name = "amount_fiat", precision = 36, scale = 18)
    private BigDecimal amountFiat;

    @Column(name = "currency_fiat", length = 8)
    private String currencyFiat;

    @Column(name = "amount_crypto", precision = 36, scale = 18)
    private BigDecimal amountCrypto;

    @Column(name = "currency_crypto", length = 16)
    private String currencyCrypto;

    @Column(length = 16) private String network;
    @Column(name = "exchange_rate", precision = 36, scale = 18) private BigDecimal exchangeRate;
    @Column(name = "channel_id", length = 32) private String channelId;
    @Column(name = "channel_user_id", length = 128) private String channelUserId;
    @Column(name = "channel_order_id", length = 128) private String channelOrderId;
    @Column(length = 32) private String status;
    @Column(name = "pay_address", length = 256) private String payAddress;
    @Column(length = 256) private String memo;
    @Column(name = "paid_amount_crypto", precision = 36, scale = 18) private BigDecimal paidAmountCrypto;
    @Column(name = "paid_amount_fiat", precision = 36, scale = 18) private BigDecimal paidAmountFiat;
    @Column(name = "tx_hash", length = 128) private String txHash;
    @Column(name = "notify_url", length = 512) private String notifyUrl;
    @Column(name = "return_url", length = 512) private String returnUrl;
    @Column(name = "extend_data", columnDefinition = "TEXT") private String extendData;
    @Column(name = "expire_time") private Instant expireTime;
    @Column(name = "pay_time") private Instant payTime;
    @Column(name = "confirm_time") private Instant confirmTime;
    @Column(name = "create_time") private Instant createTime;
    @Column(name = "update_time") private Instant updateTime;
}