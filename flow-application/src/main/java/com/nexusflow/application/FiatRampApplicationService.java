package com.nexusflow.application;

import com.nexusflow.application.dto.FiatRampCallbackRequestDto;
import com.nexusflow.application.dto.FiatRampCreateOrderRequestDto;
import com.nexusflow.application.dto.FiatRampOrderResponseDto;
import com.nexusflow.application.dto.FiatRampQuoteRequestDto;
import com.nexusflow.application.dto.FiatRampQuoteResponseDto;
import com.nexusflow.common.ErrorCode;
import com.nexusflow.common.NexusFlowException;
import com.nexusflow.domain.fiat.FiatGateway;
import com.nexusflow.domain.fiat.FiatRampDirection;
import com.nexusflow.domain.fiat.FiatRampOrder;
import com.nexusflow.domain.fiat.FiatRampOrderRequest;
import com.nexusflow.domain.fiat.FiatRampQuote;
import com.nexusflow.domain.fiat.FiatRampQuoteRequest;
import com.nexusflow.domain.fiat.FiatRampRepository;
import com.nexusflow.domain.fiat.FiatRampStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FiatRampApplicationService {

    private final List<FiatGateway> gateways;
    private final FiatRampRepository repository;

    @Transactional(readOnly = true)
    public FiatRampQuoteResponseDto quote(FiatRampQuoteRequestDto request) {
        FiatGateway gateway = resolveGateway(request.getPreferredGateway());
        BigDecimal fiatAmount = parseOptionalPositiveDecimal(request.getFiatAmount(), "fiatAmount");
        BigDecimal cryptoAmount = parseOptionalPositiveDecimal(request.getCryptoAmount(), "cryptoAmount");
        validateAmountPair(fiatAmount, cryptoAmount);
        FiatRampQuote quote = gateway.quote(FiatRampQuoteRequest.builder()
                .merchantId(request.getMerchantId())
                .direction(parseDirection(request.getDirection()))
                .fiatAmount(fiatAmount)
                .fiatCurrency(normalizeOptional(request.getFiatCurrency()))
                .cryptoAmount(cryptoAmount)
                .token(normalizeRequired(request.getToken(), "token"))
                .network(normalizeRequired(request.getNetwork(), "network"))
                .walletAddress(trimToNull(request.getWalletAddress()))
                .country(normalizeOptional(request.getCountry()))
                .paymentMethod(normalizeOptional(request.getPaymentMethod()))
                .build());
        validateAmountPair(quote != null ? quote.getFiatAmount() : null,
                quote != null ? quote.getCryptoAmount() : null);
        return toQuoteResponse(quote);
    }

    @Transactional
    public FiatRampOrderResponseDto createOrder(FiatRampCreateOrderRequestDto request) {
        repository.findByMerchantOrderNo(request.getMerchantId(), request.getMerchantOrderNo())
                .ifPresent(existing -> {
                    throw new NexusFlowException(ErrorCode.PAYMENT_ALREADY_EXISTS,
                            "Duplicate fiat ramp order: " + request.getMerchantOrderNo());
                });

        FiatGateway gateway = resolveGateway(request.getPreferredGateway());
        BigDecimal fiatAmount = parseOptionalPositiveDecimal(request.getFiatAmount(), "fiatAmount");
        BigDecimal cryptoAmount = parseOptionalPositiveDecimal(request.getCryptoAmount(), "cryptoAmount");
        validateAmountPair(fiatAmount, cryptoAmount);
        FiatRampOrder order = gateway.createOrder(FiatRampOrderRequest.builder()
                .merchantId(request.getMerchantId())
                .merchantOrderNo(request.getMerchantOrderNo())
                .paymentId(trimToNull(request.getPaymentId()))
                .direction(parseDirection(request.getDirection()))
                .quoteId(trimToNull(request.getQuoteId()))
                .fiatAmount(fiatAmount)
                .fiatCurrency(normalizeOptional(request.getFiatCurrency()))
                .cryptoAmount(cryptoAmount)
                .token(normalizeRequired(request.getToken(), "token"))
                .network(normalizeRequired(request.getNetwork(), "network"))
                .walletAddress(trimToNull(request.getWalletAddress()))
                .notifyUrl(trimToNull(request.getNotifyUrl()))
                .returnUrl(trimToNull(request.getReturnUrl()))
                .customerReference(trimToNull(request.getCustomerReference()))
                .build());
        validateGatewayOrder(order);
        repository.save(order);
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public FiatRampOrderResponseDto getOrder(String rampOrderId) {
        return repository.findByRampOrderId(rampOrderId)
                .map(this::toOrderResponse)
                .orElseThrow(() -> new NexusFlowException(ErrorCode.INVALID_REQUEST,
                        "Fiat ramp order not found: " + rampOrderId));
    }

    @Transactional
    public FiatRampOrderResponseDto handleProviderCallback(String gatewayId, FiatRampCallbackRequestDto request) {
        FiatRampOrder order = repository.findByProviderOrderId(normalizeRequired(gatewayId, "gatewayId"),
                        request.getProviderOrderId())
                .orElseThrow(() -> new NexusFlowException(ErrorCode.INVALID_REQUEST,
                        "Fiat ramp provider order not found: " + request.getProviderOrderId()));
        FiatRampStatus status = parseStatus(request.getStatus());
        switch (status) {
            case PROCESSING -> order.markProcessing(request.getFiatTransferId(), request.getCryptoTxHash());
            case COMPLETED -> order.markCompleted(request.getFiatTransferId(), request.getCryptoTxHash());
            case FAILED -> order.markFailed(request.getFailureReason());
            case EXPIRED -> order.markExpired();
            case CANCELLED -> order.markCancelled();
            default -> throw new NexusFlowException(ErrorCode.INVALID_REQUEST,
                    "Unsupported fiat ramp callback status: " + request.getStatus());
        }
        repository.save(order);
        return toOrderResponse(order);
    }

    private FiatGateway resolveGateway(String preferredGateway) {
        if (gateways == null || gateways.isEmpty()) {
            throw new NexusFlowException(ErrorCode.NO_AVAILABLE_CHANNEL, "No fiat ramp gateway available");
        }
        String preferred = normalizeOptional(preferredGateway);
        return gateways.stream()
                .filter(FiatGateway::isHealthy)
                .filter(gateway -> preferred == null || gateway.gatewayId().equalsIgnoreCase(preferred))
                .findFirst()
                .orElseThrow(() -> new NexusFlowException(ErrorCode.NO_AVAILABLE_CHANNEL,
                        preferred != null
                                ? "No healthy fiat ramp gateway for " + preferred
                                : "No healthy fiat ramp gateway available"));
    }

    private void validateGatewayOrder(FiatRampOrder order) {
        if (order == null || isBlank(order.getRampOrderId()) || order.getStatus() == null) {
            throw new NexusFlowException(ErrorCode.INTERNAL_ERROR,
                    "Fiat ramp gateway did not return a complete order");
        }
        validateAmountPair(order.getFiatAmount(), order.getCryptoAmount());
    }

    private void validateAmountPair(BigDecimal fiatAmount, BigDecimal cryptoAmount) {
        if ((fiatAmount == null || fiatAmount.compareTo(BigDecimal.ZERO) <= 0)
                && (cryptoAmount == null || cryptoAmount.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new NexusFlowException(ErrorCode.INVALID_REQUEST,
                    "fiatAmount or cryptoAmount is required");
        }
    }

    private FiatRampDirection parseDirection(String value) {
        try {
            return FiatRampDirection.valueOf(normalizeRequired(value, "direction"));
        } catch (IllegalArgumentException e) {
            throw new NexusFlowException(ErrorCode.INVALID_REQUEST,
                    "Unsupported fiat ramp direction: " + value);
        }
    }

    private FiatRampStatus parseStatus(String value) {
        try {
            return FiatRampStatus.valueOf(normalizeRequired(value, "status"));
        } catch (IllegalArgumentException e) {
            throw new NexusFlowException(ErrorCode.INVALID_REQUEST,
                    "Unsupported fiat ramp status: " + value);
        }
    }

    private BigDecimal parseOptionalPositiveDecimal(String value, String fieldName) {
        if (isBlank(value)) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(value);
            if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NexusFlowException(ErrorCode.INVALID_REQUEST, fieldName + " must be greater than zero");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new NexusFlowException(ErrorCode.INVALID_REQUEST, fieldName + " must be a valid decimal");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (isBlank(value)) {
            throw new NexusFlowException(ErrorCode.INVALID_REQUEST, fieldName + " is required");
        }
        return value.trim().toUpperCase();
    }

    private String normalizeOptional(String value) {
        return isBlank(value) ? null : value.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private FiatRampQuoteResponseDto toQuoteResponse(FiatRampQuote quote) {
        return FiatRampQuoteResponseDto.builder()
                .quoteId(quote.getQuoteId())
                .providerId(quote.getProviderId())
                .direction(name(quote.getDirection()))
                .fiatAmount(toPlain(quote.getFiatAmount()))
                .fiatCurrency(quote.getFiatCurrency())
                .cryptoAmount(toPlain(quote.getCryptoAmount()))
                .token(quote.getToken())
                .network(quote.getNetwork())
                .exchangeRate(toPlain(quote.getExchangeRate()))
                .feeAmountFiat(toPlain(quote.getFeeAmountFiat()))
                .expiresAt(toEpochMillis(quote.getExpiresAt()))
                .build();
    }

    private FiatRampOrderResponseDto toOrderResponse(FiatRampOrder order) {
        return FiatRampOrderResponseDto.builder()
                .rampOrderId(order.getRampOrderId())
                .merchantId(order.getMerchantId())
                .merchantOrderNo(order.getMerchantOrderNo())
                .paymentId(order.getPaymentId())
                .direction(name(order.getDirection()))
                .providerId(order.getProviderId())
                .providerOrderId(order.getProviderOrderId())
                .quoteId(order.getQuoteId())
                .fiatAmount(toPlain(order.getFiatAmount()))
                .fiatCurrency(order.getFiatCurrency())
                .cryptoAmount(toPlain(order.getCryptoAmount()))
                .token(order.getToken())
                .network(order.getNetwork())
                .exchangeRate(toPlain(order.getExchangeRate()))
                .feeAmountFiat(toPlain(order.getFeeAmountFiat()))
                .walletAddress(order.getWalletAddress())
                .checkoutUrl(order.getCheckoutUrl())
                .fiatTransferId(order.getFiatTransferId())
                .cryptoTxHash(order.getCryptoTxHash())
                .status(name(order.getStatus()))
                .failureReason(order.getFailureReason())
                .expireTime(toEpochMillis(order.getExpireTime()))
                .completeTime(toEpochMillis(order.getCompleteTime()))
                .createTime(toEpochMillis(order.getCreateTime()))
                .build();
    }

    private String toPlain(BigDecimal value) {
        return value != null ? value.toPlainString() : null;
    }

    private Long toEpochMillis(Instant value) {
        return value != null ? value.toEpochMilli() : null;
    }

    private String name(Enum<?> value) {
        return value != null ? value.name() : null;
    }
}
