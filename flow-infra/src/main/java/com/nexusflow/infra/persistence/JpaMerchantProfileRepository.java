package com.nexusflow.infra.persistence;

import com.nexusflow.domain.merchant.MerchantProfile;
import com.nexusflow.domain.merchant.MerchantProfileRepository;
import com.nexusflow.domain.merchant.MerchantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMerchantProfileRepository implements MerchantProfileRepository {

    private final SpringDataMerchantProfileRepository repository;

    @Override
    public List<MerchantProfile> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<MerchantProfile> findById(String merchantId) {
        return repository.findById(merchantId).map(this::toDomain);
    }

    @Override
    public MerchantProfile save(MerchantProfile profile) {
        MerchantProfileEntity entity = toEntity(profile);
        MerchantProfileEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private MerchantProfile toDomain(MerchantProfileEntity entity) {
        return MerchantProfile.builder()
                .merchantId(entity.getMerchantId())
                .merchantCode(entity.getMerchantCode())
                .displayName(entity.getDisplayName())
                .status(MerchantStatus.valueOf(entity.getStatus()))
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    private MerchantProfileEntity toEntity(MerchantProfile profile) {
        MerchantProfileEntity entity = new MerchantProfileEntity();
        entity.setMerchantId(profile.getMerchantId());
        entity.setMerchantCode(profile.getMerchantCode());
        entity.setDisplayName(profile.getDisplayName());
        entity.setStatus(profile.getStatus().name());
        entity.setCreateTime(profile.getCreateTime());
        entity.setUpdateTime(profile.getUpdateTime());
        return entity;
    }
}
