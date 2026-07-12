package com.nexusflow.infra.persistence;

import com.nexusflow.domain.merchant.MerchantUser;
import com.nexusflow.domain.merchant.MerchantUserRepository;
import com.nexusflow.domain.merchant.MerchantUserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMerchantUserRepository implements MerchantUserRepository {

    private final SpringDataMerchantUserRepository repository;

    @Override
    public Optional<MerchantUser> findById(String userId) {
        return repository.findById(userId).map(this::toDomain);
    }

    @Override
    public Optional<MerchantUser> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public MerchantUser save(MerchantUser user) {
        MerchantUserEntity entity = toEntity(user);
        MerchantUserEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private MerchantUser toDomain(MerchantUserEntity entity) {
        return MerchantUser.builder()
                .userId(entity.getUserId())
                .email(entity.getEmail())
                .displayName(entity.getDisplayName())
                .status(MerchantUserStatus.valueOf(entity.getStatus()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private MerchantUserEntity toEntity(MerchantUser user) {
        MerchantUserEntity entity = new MerchantUserEntity();
        entity.setUserId(user.getUserId());
        entity.setEmail(user.getEmail());
        entity.setDisplayName(user.getDisplayName());
        entity.setStatus(user.getStatus().name());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }
}
