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
}