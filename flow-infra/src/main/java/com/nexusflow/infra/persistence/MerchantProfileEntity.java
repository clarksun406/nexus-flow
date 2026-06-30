package com.nexusflow.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "merchant_profiles")
public class MerchantProfileEntity {

    @Id
    @Column(name = "merchant_id", length = 64)
    private String merchantId;

    @Column(name = "merchant_code", length = 128, nullable = false, unique = true)
    private String merchantCode;

    @Column(name = "display_name", length = 128, nullable = false)
    private String displayName;

    @Column(length = 32, nullable = false)
    private String status;

    @Column(name = "create_time")
    private Instant createTime;

    @Column(name = "update_time")
    private Instant updateTime;
}
