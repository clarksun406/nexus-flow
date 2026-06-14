package com.nexusflow.infra.gas;

import com.nexusflow.domain.gas.GasEstimate;
import com.nexusflow.domain.gas.GasEstimateRequest;
import com.nexusflow.domain.gas.GasEstimator;
import com.nexusflow.domain.gas.GasOperation;
import com.nexusflow.domain.shared.Chain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Conservative static estimator used until live gas or fee APIs are wired.
 */
@Component
public class StaticGasEstimator implements GasEstimator {

    private final BigDecimal erc20GasPriceGwei;
    private final long erc20TransferGasLimit;
    private final BigDecimal trc20FeeTrx;
    private final long btcVirtualBytes;
    private final BigDecimal btcSatsPerVirtualByte;

    public StaticGasEstimator(
            @Value("${nexusflow.gas.ethereum.erc20-gas-price-gwei:30}") BigDecimal erc20GasPriceGwei,
            @Value("${nexusflow.gas.ethereum.erc20-transfer-gas-limit:65000}") long erc20TransferGasLimit,
            @Value("${nexusflow.gas.tron.trc20-fee-trx:15}") BigDecimal trc20FeeTrx,
            @Value("${nexusflow.gas.bitcoin.virtual-bytes:180}") long btcVirtualBytes,
            @Value("${nexusflow.gas.bitcoin.sats-per-virtual-byte:20}") BigDecimal btcSatsPerVirtualByte) {
        this.erc20GasPriceGwei = erc20GasPriceGwei;
        this.erc20TransferGasLimit = erc20TransferGasLimit;
        this.trc20FeeTrx = trc20FeeTrx;
        this.btcVirtualBytes = btcVirtualBytes;
        this.btcSatsPerVirtualByte = btcSatsPerVirtualByte;
    }

    @Override
    public GasEstimate estimate(GasEstimateRequest request) {
        if (request == null || request.getChain() == null) {
            throw new IllegalArgumentException("Gas estimate requires a chain");
        }
        return switch (request.getChain()) {
            case ETH -> estimateEthereum(request);
            case TRON -> estimateTron(request);
            case BTC -> estimateBitcoin(request);
            case SOLANA -> throw new IllegalArgumentException("SOLANA gas estimation is not supported");
        };
    }

    private GasEstimate estimateEthereum(GasEstimateRequest request) {
        long gasLimit = request.getOperation() == GasOperation.NATIVE_TRANSFER ? 21_000L : erc20TransferGasLimit;
        BigDecimal feeEth = erc20GasPriceGwei
                .multiply(BigDecimal.valueOf(gasLimit))
                .divide(new BigDecimal("1000000000"), 18, RoundingMode.HALF_UP);
        return estimate(request, gasLimit, erc20GasPriceGwei, feeEth, "ETH");
    }

    private GasEstimate estimateTron(GasEstimateRequest request) {
        return estimate(request, 1L, trc20FeeTrx, trc20FeeTrx, "TRX");
    }

    private GasEstimate estimateBitcoin(GasEstimateRequest request) {
        BigDecimal feeBtc = btcSatsPerVirtualByte
                .multiply(BigDecimal.valueOf(btcVirtualBytes))
                .divide(new BigDecimal("100000000"), 8, RoundingMode.HALF_UP);
        return estimate(request, btcVirtualBytes, btcSatsPerVirtualByte, feeBtc, "BTC");
    }

    private GasEstimate estimate(GasEstimateRequest request, long gasLimit, BigDecimal gasPrice,
                                 BigDecimal fee, String nativeCurrency) {
        return GasEstimate.builder()
                .chain(request.getChain())
                .network(request.getNetwork())
                .operation(request.getOperation())
                .gasLimit(gasLimit)
                .gasPrice(gasPrice)
                .estimatedFee(fee)
                .nativeCurrency(nativeCurrency)
                .estimatedAt(Instant.now())
                .build();
    }
}
