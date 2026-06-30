package com.nexusflow.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "merchant_webhook_configs")
public class MerchantWebhookConfigEntity {

    @Id
    @Column(name = "config_id", length = 64)
    private String configId;

    @Column(name = "merchant_id", length = 64, nullable = false)
    private String merchantId;

    @Column(length = 512, nullable = false)
    private String url;

    @Column(name = "secret_hash", length = 128)
    private String secretHash;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "create_time")
    private Instant createTime;

    @Column(name = "update_time")
    private Instant updateTime;
}
