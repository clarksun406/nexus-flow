package com.nexusflow.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "merchant_users")
public class MerchantUserEntity {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(length = 256, unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", length = 256)
    private String passwordHash;

    @Column(name = "display_name", length = 128, nullable = false)
    private String displayName;

    @Column(length = 32, nullable = false)
    private String status;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
