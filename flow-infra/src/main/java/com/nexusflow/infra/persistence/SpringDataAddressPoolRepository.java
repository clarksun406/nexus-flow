package com.nexusflow.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataAddressPoolRepository extends JpaRepository<AddressPoolEntryEntity, String> {

    @Query(value = """
            SELECT *
            FROM address_pool
            WHERE chain = :chain AND status = :status
            ORDER BY derivation_index ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<AddressPoolEntryEntity> lockFirstAvailableByChain(@Param("chain") String chain,
                                                               @Param("status") String status);

    Optional<AddressPoolEntryEntity> findByAddress(String address);

    long countByChainAndStatus(String chain, String status);

    List<AddressPoolEntryEntity> findByStatus(String status);

    @Query("SELECT COALESCE(MAX(a.derivationIndex), -1) FROM AddressPoolEntryEntity a WHERE a.chain = :chain")
    int maxDerivationIndex(@Param("chain") String chain);
}
