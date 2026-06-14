package com.nexusflow.application;

import com.nexusflow.application.dto.GasBankFundingCommand;
import com.nexusflow.application.dto.GasBankFundingResult;
import com.nexusflow.domain.gas.GasBank;
import com.nexusflow.domain.gas.GasBankAction;
import com.nexusflow.domain.gas.GasBankBalance;
import com.nexusflow.domain.gas.GasBankBalanceRequest;
import com.nexusflow.domain.gas.GasBankPolicy;
import com.nexusflow.domain.gas.GasBankRecommendation;
import com.nexusflow.domain.gas.GasEstimate;
import com.nexusflow.domain.gas.GasEstimateRequest;
import com.nexusflow.domain.gas.GasEstimator;
import com.nexusflow.domain.gas.GasTopUpRequest;
import com.nexusflow.domain.gas.GasTopUpResult;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class GasBankFundingService {

    private final GasBank gasBank;
    private final GasEstimator gasEstimator;

    @Transactional
    public GasBankFundingResult evaluateAndFund(GasBankFundingCommand command) {
        validate(command);
        GasBankBalance balance = gasBank.getBalance(GasBankBalanceRequest.builder()
                .chain(command.getChain())
                .network(command.getNetwork())
                .walletAddress(command.getWalletAddress())
                .nativeCurrency(command.getNativeCurrency())
                .build());
        GasEstimate estimate = gasEstimator.estimate(GasEstimateRequest.builder()
                .chain(command.getChain())
                .token(command.getToken())
                .network(command.getNetwork())
                .operation(command.getOperation())
                .amount(command.getAmount())
                .toAddress(command.getToAddress())
                .build());
        GasBankRecommendation recommendation = command.getPolicy().recommend(balance, estimate);
        GasTopUpResult topUpResult = shouldRequestTopUp(recommendation)
                ? gasBank.requestTopUp(GasTopUpRequest.builder()
                        .requestId(command.getRequestId())
                        .chain(command.getChain())
                        .network(command.getNetwork())
                        .walletAddress(command.getWalletAddress())
                        .nativeCurrency(nativeCurrency(command, recommendation, estimate, balance))
                        .amount(recommendation.getTopUpAmount())
                        .reason(recommendation.getReason())
                        .build())
                : null;

        return GasBankFundingResult.builder()
                .balance(balance)
                .estimate(estimate)
                .recommendation(recommendation)
                .topUpResult(topUpResult)
                .build();
    }

    private boolean shouldRequestTopUp(GasBankRecommendation recommendation) {
        if (recommendation == null || recommendation.getAction() == null) {
            return false;
        }
        if (recommendation.getTopUpAmount() == null
                || recommendation.getTopUpAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return recommendation.getAction() == GasBankAction.TOP_UP_NOW
                || recommendation.getAction() == GasBankAction.BATCH_TOP_UP_WHEN_LOW_GAS;
    }

    private String nativeCurrency(GasBankFundingCommand command,
                                  GasBankRecommendation recommendation,
                                  GasEstimate estimate,
                                  GasBankBalance balance) {
        if (hasText(recommendation.getNativeCurrency())) {
            return recommendation.getNativeCurrency();
        }
        if (estimate != null && hasText(estimate.getNativeCurrency())) {
            return estimate.getNativeCurrency();
        }
        if (balance != null && hasText(balance.getNativeCurrency())) {
            return balance.getNativeCurrency();
        }
        return command.getNativeCurrency();
    }

    private void validate(GasBankFundingCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("GasBank funding command is required");
        }
        if (command.getChain() == null) {
            throw new IllegalArgumentException("GasBank funding chain is required");
        }
        if (!hasText(command.getNetwork())) {
            throw new IllegalArgumentException("GasBank funding network is required");
        }
        if (!hasText(command.getWalletAddress())) {
            throw new IllegalArgumentException("GasBank funding wallet address is required");
        }
        GasBankPolicy policy = command.getPolicy();
        if (policy == null) {
            throw new IllegalArgumentException("GasBank funding policy is required");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
