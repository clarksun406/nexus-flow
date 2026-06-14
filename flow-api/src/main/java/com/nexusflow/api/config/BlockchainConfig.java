package com.nexusflow.api.config;

import com.nexusflow.domain.blockchain.BlockchainAdapter;
import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.infra.adapter.binance.BinancePayAdapter;
import com.nexusflow.infra.adapter.bitmart.BitMartAdapter;
import com.nexusflow.infra.adapter.coinbase.CoinbaseCommerceAdapter;
import com.nexusflow.infra.blockchain.BitcoinAdapter;
import com.nexusflow.infra.blockchain.EthereumAdapter;
import com.nexusflow.infra.blockchain.HttpTronGridClient;
import com.nexusflow.infra.blockchain.TronAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Profiles;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
public class BlockchainConfig {

    @Value("${nexusflow.tron.node-url:https://api.trongrid.io}")
    private String tronNodeUrl;

    @Value("${nexusflow.tron.usdt-contract:TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t}")
    private String tronUsdtContract;

    @Value("${nexusflow.ethereum.rpc-url:http://localhost:8545}")
    private String ethereumRpcUrl;

    @Value("${nexusflow.ethereum.usdt-contract:0xdAC17F958D2ee523a2206206994597C13D831ec7}")
    private String ethereumUsdtContract;

    @Value("${nexusflow.bitcoin.node-url:http://localhost:8332}")
    private String bitcoinNodeUrl;

    @Value("${nexusflow.bitcoin.username:}")
    private String bitcoinUsername;

    @Value("${nexusflow.bitcoin.password:}")
    private String bitcoinPassword;

    @Value("${nexusflow.coinbase-commerce.base-url:https://api.commerce.coinbase.com}")
    private String coinbaseCommerceBaseUrl;

    @Value("${nexusflow.coinbase-commerce.api-key:}")
    private String coinbaseCommerceApiKey;

    @Value("${nexusflow.coinbase-commerce.api-version:2018-03-22}")
    private String coinbaseCommerceApiVersion;

    @Bean
    public BlockchainAdapter tronAdapter() {
        return new TronAdapter(new HttpTronGridClient(tronNodeUrl), tronUsdtContract);
    }

    @Bean
    public BlockchainAdapter ethereumAdapter() {
        return new EthereumAdapter(ethereumRpcUrl, ethereumUsdtContract);
    }

    @Bean
    public BlockchainAdapter bitcoinAdapter() {
        return new BitcoinAdapter(bitcoinNodeUrl, bitcoinUsername, bitcoinPassword);
    }

    @Bean
    @Profile("!prod")
    public ChannelAdapter bitMartAdapter() {
        return new BitMartAdapter();
    }

    @Bean
    @Profile("!prod")
    public ChannelAdapter binancePayAdapter() {
        return new BinancePayAdapter();
    }

    @Bean
    @Conditional(CoinbaseCommerceAdapterCondition.class)
    public ChannelAdapter coinbaseCommerceAdapter() {
        return new CoinbaseCommerceAdapter(
                coinbaseCommerceBaseUrl,
                coinbaseCommerceApiKey,
                coinbaseCommerceApiVersion);
    }

    static class CoinbaseCommerceAdapterCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            boolean prodProfile = context.getEnvironment().acceptsProfiles(Profiles.of("prod"));
            String apiKey = context.getEnvironment().getProperty("nexusflow.coinbase-commerce.api-key", "");
            return !prodProfile || hasText(apiKey);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
