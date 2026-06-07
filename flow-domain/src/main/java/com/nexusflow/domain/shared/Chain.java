package com.nexusflow.domain.shared;

/**
 * Supported blockchain networks.
 */
public enum Chain {

    ETH("Ethereum"),
    TRON("Tron"),
    BTC("Bitcoin"),
    SOLANA("Solana");

    private final String displayName;

    Chain(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Chain fromString(String value) {
        for (Chain c : values()) {
            if (c.name().equalsIgnoreCase(value)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown chain: " + value);
    }

    /**
     * Resolve the chain from a currency code such as "USDT_TRC20", "USDT_ERC20", "ETH" or "BTC".
     * The token network suffix (after '_') drives the mapping; bare codes fall back to the chain name.
     */
    public static Chain fromCurrency(String currency) {
        if (currency == null) {
            throw new IllegalArgumentException("currency is null");
        }
        String token = currency.contains("_") ? currency.substring(currency.indexOf('_') + 1) : currency;
        return switch (token.toUpperCase()) {
            case "TRC20", "TRON" -> TRON;
            case "ERC20", "ETH" -> ETH;
            case "BTC" -> BTC;
            case "SOL", "SOLANA" -> SOLANA;
            default -> fromString(token);
        };
    }
}