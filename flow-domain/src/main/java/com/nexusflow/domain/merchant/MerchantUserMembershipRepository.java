package com.nexusflow.domain.merchant;

import java.util.List;

public interface MerchantUserMembershipRepository {

    List<MerchantUserMembership> findByUserId(String userId);

    List<MerchantUserMembership> findByMerchantId(String merchantId);

    MerchantUserMembership save(MerchantUserMembership membership);
}
