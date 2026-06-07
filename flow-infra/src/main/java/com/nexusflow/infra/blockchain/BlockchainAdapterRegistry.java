package com.nexusflow.infra.blockchain;

import com.nexusflow.domain.blockchain.BlockchainAdapter;
import com.nexusflow.domain.shared.Chain;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry for managing multiple blockchain adapters.
 */
@Component
public class BlockchainAdapterRegistry {

    private final Map<Chain, BlockchainAdapter> adapters;

    public BlockchainAdapterRegistry(List<BlockchainAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(BlockchainAdapter::supportedChain, Function.identity()));
    }

    public Optional<BlockchainAdapter> getAdapter(Chain chain) {
        return Optional.ofNullable(adapters.get(chain));
    }

    public List<BlockchainAdapter> getAllAdapters() {
        return List.copyOf(adapters.values());
    }
}