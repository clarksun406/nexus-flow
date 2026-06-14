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
import org.springframework.context.annotation.Configuration;

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
    public ChannelAdapter bitMartAdapter() {
        return new BitMartAdapter();
    }

    @Bean
    public ChannelAdapter binancePayAdapter() {
        return new BinancePayAdapter();
    }

    @Bean
    public ChannelAdapter coinbaseCommerceAdapter() {
        return new CoinbaseCommerceAdapter();
    }
}
