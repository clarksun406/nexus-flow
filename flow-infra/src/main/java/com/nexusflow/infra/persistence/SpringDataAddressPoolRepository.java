package com.nexusflow.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataAddressPoolRepository extends JpaRepository<AddressPoolEntryEntity, String> {

    Optional<AddressPoolEntryEntity> findFirstByChainAndStatusOrderByDerivationIndexAsc(String chain, String status);

    Optional<AddressPoolEntryEntity> findByAddress(String address);

    long countByChainAndStatus(String chain, String status);

    List<AddressPoolEntryEntity> findByStatus(String status);

    @Query("SELECT COALESCE(MAX(a.derivationIndex), -1) FROM AddressPoolEntryEntity a WHERE a.chain = :chain")
    int maxDerivationIndex(@Param("chain") String chain);
}
