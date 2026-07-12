package com.nexusflow.domain.merchant;

import java.util.Optional;

public interface MerchantUserRepository {

    Optional<MerchantUser> findById(String userId);

    Optional<MerchantUser> findByEmail(String email);

    MerchantUser save(MerchantUser user);
}
