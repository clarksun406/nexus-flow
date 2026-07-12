package com.nexusflow.infra.persistence;

import com.nexusflow.domain.merchant.MerchantUserMembership;
import com.nexusflow.domain.merchant.MerchantUserMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaMerchantUserMembershipRepository implements MerchantUserMembershipRepository {

    private final SpringDataMerchantUserMembershipRepository repository;

    @Override
    public List<MerchantUserMembership> findByUserId(String userId) {
        return repository.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<MerchantUserMembership> findByMerchantId(String merchantId) {
        return repository.findByMerchantId(merchantId).stream().map(this::toDomain).toList();
    }

    @Override
    public MerchantUserMembership save(MerchantUserMembership membership) {
        MerchantUserMembershipEntity entity = toEntity(membership);
        MerchantUserMembershipEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private MerchantUserMembership toDomain(MerchantUserMembershipEntity entity) {
        return MerchantUserMembership.builder()
                .merchantId(entity.getMerchantId())
                .userId(entity.getUserId())
                .roleCode(entity.getRoleCode())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private MerchantUserMembershipEntity toEntity(MerchantUserMembership membership) {
        MerchantUserMembershipEntity entity = new MerchantUserMembershipEntity();
        entity.setMerchantId(membership.getMerchantId());
        entity.setUserId(membership.getUserId());
        entity.setRoleCode(membership.getRoleCode());
        entity.setStatus(membership.getStatus());
        entity.setCreatedAt(membership.getCreatedAt());
        return entity;
    }
}
