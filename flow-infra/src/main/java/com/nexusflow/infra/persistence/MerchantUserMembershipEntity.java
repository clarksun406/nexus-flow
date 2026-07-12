package com.nexusflow.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "merchant_user_memberships")
public class MerchantUserMembershipEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", length = 64, nullable = false)
    private String merchantId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "role_code", length = 64, nullable = false)
    private String roleCode;

    @Column(length = 32, nullable = false)
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;
}
