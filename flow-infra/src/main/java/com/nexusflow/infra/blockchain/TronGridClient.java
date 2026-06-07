package com.nexusflow.infra.blockchain;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Thin seam over the TronGrid HTTP API so {@link TronAdapter}'s response parsing can be
 * unit-tested without network access.
 */
public interface TronGridClient {

    /**
     * POST a JSON body to a TronGrid path (e.g. {@code /wallet/getnowblock}) and return the
     * parsed JSON response.
     */
    JsonNode post(String path, Map<String, Object> body);
}
