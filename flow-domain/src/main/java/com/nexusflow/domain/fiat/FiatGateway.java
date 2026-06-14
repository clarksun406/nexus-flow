package com.nexusflow.domain.fiat;

/**
 * Port for fiat on/off ramp providers such as MoonPay, Ramp, or Banxa.
 */
public interface FiatGateway {
    String gatewayId();
    String displayName();
    FiatRampQuote quote(FiatRampQuoteRequest request);
    FiatRampOrder createOrder(FiatRampOrderRequest request);
    FiatRampOrder queryOrder(String providerOrderId);
    boolean isHealthy();
}
