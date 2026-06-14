package com.nexusflow.wallet;

import com.nexusflow.common.AesGcmEncryption;
import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.wallet.AddressPoolEntry;
import com.nexusflow.domain.wallet.AddressPoolRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AddressPoolProvisioningService {

    private final AddressPoolRepository addressPoolRepository;
    private final KeyGenerator keyGenerator;
    private final AesGcmEncryption encryption;
    private final String seedMnemonic;
    private final List<Chain> chains;
    private final int targetSize;
    private final int batchSize;

    public AddressPoolProvisioningService(AddressPoolRepository addressPoolRepository,
                                          KeyGenerator keyGenerator,
                                          AesGcmEncryption encryption,
                                          @Value("${nexusflow.address-pool.seed-mnemonic:}") String seedMnemonic,
                                          @Value("${nexusflow.address-pool.chains:TRON,ETH,BTC}") String chainList,
                                          @Value("${nexusflow.address-pool.target-size:20}") int targetSize,
                                          @Value("${nexusflow.address-pool.batch-size:10}") int batchSize) {
        this.addressPoolRepository = addressPoolRepository;
        this.keyGenerator = keyGenerator;
        this.encryption = encryption;
        this.seedMnemonic = seedMnemonic;
        this.chains = Arrays.stream(chainList.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Chain::fromString)
                .toList();
        this.targetSize = targetSize;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${nexusflow.address-pool.replenish-interval-ms:60000}")
    @Transactional
    public void replenishIfLow() {
        if (seedMnemonic == null || seedMnemonic.isBlank()) {
            log.debug("Address pool seed mnemonic is not configured; skipping replenishment");
            return;
        }
        for (Chain chain : chains) {
            long available = addressPoolRepository.countAvailableByChain(chain);
            if (available < targetSize) {
                int count = Math.max(batchSize, Math.toIntExact(targetSize - available));
                replenish(chain, count);
            }
        }
    }

    @Transactional
    public void replenish(Chain chain, int count) {
        int startIndex = addressPoolRepository.maxDerivationIndex(chain) + 1;
        for (int i = 0; i < count; i++) {
            int index = startIndex + i;
            String privateKey = keyGenerator.derivePrivateKey(seedMnemonic, chain, index);
            String address = keyGenerator.deriveAddress(privateKey, chain);
            AddressPoolEntry entry = AddressPoolEntry.builder()
                    .id(UUID.randomUUID().toString())
                    .chain(chain)
                    .address(address)
                    .encryptedPrivateKey(encryption.encrypt(privateKey))
                    .derivationPath(keyGenerator.derivationPathText(chain, 0, index))
                    .derivationIndex(index)
                    .build();
            addressPoolRepository.save(entry);
        }
        log.info("Address pool replenished: chain={}, count={}, startIndex={}", chain, count, startIndex);
    }
}
