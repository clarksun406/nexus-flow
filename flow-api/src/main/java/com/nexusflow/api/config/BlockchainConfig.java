package com.nexusflow.api.config;

import com.nexusflow.domain.blockchain.BlockchainAdapter;
import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.infra.adapter.bitmart.BitMartAdapter;
import com.nexusflow.infra.blockchain.HttpTronGridClient;
import com.nexusflow.infra.blockchain.TronAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlockchainConfig {

    @Value("${nexusflow.tron.node-url:https://api.trongrid.io}")
    private String tronNodeUrl;

    @Value("${nexusflow.tron.usdt-contract:TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t}")
    private String tronUsdtContract;

    @Bean
    public BlockchainAdapter tronAdapter() {
        return new TronAdapter(new HttpTronGridClient(tronNodeUrl), tronUsdtContract);
    }

    @Bean
    public ChannelAdapter bitMartAdapter() {
        return new BitMartAdapter();
    }
}