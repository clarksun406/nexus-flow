package com.nexusflow.domain.wallet;

import com.nexusflow.domain.shared.Chain;

import java.util.List;
import java.util.Optional;

public interface AddressPoolRepository {

    void save(AddressPoolEntry entry);

    Optional<AddressPoolEntry> findById(String id);

    Optional<AddressPoolEntry> findByAddress(String address);

    Optional<AddressPoolEntry> findFirstAvailableByChain(Chain chain);

    long countAvailableByChain(Chain chain);

    int maxDerivationIndex(Chain chain);

    List<AddressPoolEntry> findByStatus(AddressPoolStatus status);
}
