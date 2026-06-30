package com.nexusflow.domain.merchant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MerchantApiKey {
    String merchantId;
    String merchantCode;
    String keyPrefix;
}
