package com.nexusflow.application;

import com.nexusflow.application.dto.GasBankFundingCommand;
import com.nexusflow.domain.gas.GasBank;
import com.nexusflow.domain.gas.GasBankAction;
import com.nexusflow.domain.gas.GasBankBalance;
import com.nexusflow.domain.gas.GasBankBalanceRequest;
import com.nexusflow.domain.gas.GasBankPolicy;
import com.nexusflow.domain.gas.GasEstimate;
import com.nexusflow.domain.gas.GasEstimateRequest;
import com.nexusflow.domain.gas.GasEstimator;
import com.nexusflow.domain.gas.GasOperation;
import com.nexusflow.domain.gas.GasPriceBand;
import com.nexusflow.domain.gas.GasTopUpRequest;
import com.nexusflow.domain.gas.GasTopUpResult;
import com.nexusflow.domain.shared.Chain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GasBankFundingServiceTest {

    private GasBank gasBank;
    private GasEstimator gasEstimator;
    private GasBankFundingService service;

    @BeforeEach
    void setUp() {
        gasBank = mock(GasBank.class);
        gasEstimator = mock(GasEstimator.class);
        service = new GasBankFundingService(gasBank, gasEstimator);
    }

    @Test
    void requestsImmediateTopUpWhenBalanceCannotCoverEstimatedFee() {
        when(gasBank.getBalance(any())).thenReturn(balance("0.0005"));
        when(gasEstimator.estimate(any())).thenReturn(estimate("30", "0.00195"));
        when(gasBank.requestTopUp(any())).thenReturn(topUpResult("REQUESTED", "0.0995"));

        var result = service.evaluateAndFund(command("req-1"));

        assertThat(result.getRecommendation().getAction()).isEqualTo(GasBankAction.TOP_UP_NOW);
        assertThat(result.getRecommendation().getTopUpAmount()).isEqualByComparingTo("0.0995");
        assertThat(result.topUpRequested()).isTrue();

        ArgumentCaptor<GasBankBalanceRequest> balanceCaptor =
                ArgumentCaptor.forClass(GasBankBalanceRequest.class);
        verify(gasBank).getBalance(balanceCaptor.capture());
        assertThat(balanceCaptor.getValue().getChain()).isEqualTo(Chain.ETH);
        assertThat(balanceCaptor.getValue().getWalletAddress()).isEqualTo("0xwallet");

        ArgumentCaptor<GasEstimateRequest> estimateCaptor =
                ArgumentCaptor.forClass(GasEstimateRequest.class);
        verify(gasEstimator).estimate(estimateCaptor.capture());
        assertThat(estimateCaptor.getValue().getOperation()).isEqualTo(GasOperation.REFUND);
        assertThat(estimateCaptor.getValue().getToAddress()).isEqualTo("0xmerchant");

        ArgumentCaptor<GasTopUpRequest> topUpCaptor = ArgumentCaptor.forClass(GasTopUpRequest.class);
        verify(gasBank).requestTopUp(topUpCaptor.capture());
        assertThat(topUpCaptor.getValue().getRequestId()).isEqualTo("req-1");
        assertThat(topUpCaptor.getValue().getNativeCurrency()).isEqualTo("ETH");
        assertThat(topUpCaptor.getValue().getAmount()).isEqualByComparingTo("0.0995");
        assertThat(topUpCaptor.getValue().getReason()).contains("estimated transaction fee");
    }

    @Test
    void requestsBatchTopUpWhenGasIsLowAndBalanceIsBelowTarget() {
        when(gasBank.getBalance(any())).thenReturn(balance("0.06"));
        when(gasEstimator.estimate(any())).thenReturn(estimate("10", "0.00065"));
        when(gasBank.requestTopUp(any())).thenReturn(topUpResult("REQUESTED", "0.04"));

        var result = service.evaluateAndFund(command("req-low"));

        assertThat(result.getRecommendation().getAction()).isEqualTo(GasBankAction.BATCH_TOP_UP_WHEN_LOW_GAS);
        assertThat(result.getRecommendation().getGasPriceBand()).isEqualTo(GasPriceBand.LOW);
        assertThat(result.topUpRequested()).isTrue();

        ArgumentCaptor<GasTopUpRequest> topUpCaptor = ArgumentCaptor.forClass(GasTopUpRequest.class);
        verify(gasBank).requestTopUp(topUpCaptor.capture());
        assertThat(topUpCaptor.getValue().getAmount()).isEqualByComparingTo("0.04");
        assertThat(topUpCaptor.getValue().getReason()).contains("gas price is low");
    }

    @Test
    void doesNotTopUpWhenPolicyDefersHighGasFunding() {
        when(gasBank.getBalance(any())).thenReturn(balance("0.02"));
        when(gasEstimator.estimate(any())).thenReturn(estimate("90", "0.00585"));

        var result = service.evaluateAndFund(command("req-high"));

        assertThat(result.getRecommendation().getAction()).isEqualTo(GasBankAction.DEFER_TOP_UP);
        assertThat(result.getRecommendation().getGasPriceBand()).isEqualTo(GasPriceBand.HIGH);
        assertThat(result.topUpRequested()).isFalse();
        verify(gasBank, never()).requestTopUp(any());
    }

    private GasBankFundingCommand command(String requestId) {
        return GasBankFundingCommand.builder()
                .requestId(requestId)
                .chain(Chain.ETH)
                .network("ERC20")
                .walletAddress("0xwallet")
                .token("USDT")
                .nativeCurrency("ETH")
                .operation(GasOperation.REFUND)
                .amount(new BigDecimal("50"))
                .toAddress("0xmerchant")
                .policy(policy())
                .build();
    }

    private GasBankPolicy policy() {
        return GasBankPolicy.builder()
                .chain(Chain.ETH)
                .network("ERC20")
                .nativeCurrency("ETH")
                .minimumBalance(new BigDecimal("0.05"))
                .targetBalance(new BigDecimal("0.10"))
                .lowGasPriceThreshold(new BigDecimal("15"))
                .highGasPriceThreshold(new BigDecimal("60"))
                .build();
    }

    private GasBankBalance balance(String amount) {
        return GasBankBalance.builder()
                .chain(Chain.ETH)
                .network("ERC20")
                .walletAddress("0xwallet")
                .nativeCurrency("ETH")
                .availableBalance(new BigDecimal(amount))
                .checkedAt(Instant.parse("2026-06-14T00:00:00Z"))
                .build();
    }

    private GasEstimate estimate(String gasPrice, String fee) {
        return GasEstimate.builder()
                .chain(Chain.ETH)
                .network("ERC20")
                .operation(GasOperation.REFUND)
                .gasLimit(65_000L)
                .gasPrice(new BigDecimal(gasPrice))
                .estimatedFee(new BigDecimal(fee))
                .nativeCurrency("ETH")
                .estimatedAt(Instant.parse("2026-06-14T00:00:00Z"))
                .build();
    }

    private GasTopUpResult topUpResult(String status, String amount) {
        return GasTopUpResult.builder()
                .requestId("req-1")
                .chain(Chain.ETH)
                .network("ERC20")
                .walletAddress("0xwallet")
                .nativeCurrency("ETH")
                .amount(new BigDecimal(amount))
                .status(status)
                .requestedAt(Instant.parse("2026-06-14T00:00:00Z"))
                .build();
    }
}
