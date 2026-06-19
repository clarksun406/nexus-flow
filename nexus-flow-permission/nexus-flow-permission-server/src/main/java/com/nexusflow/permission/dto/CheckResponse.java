package com.nexusflow.permission.dto;

import lombok.Data;

@Data
public class CheckResponse {

    private final boolean granted;
    private final String reason;

    public static CheckResponse granted() {
        return new CheckResponse(true, null);
    }

    public static CheckResponse denied(String reason) {
        return new CheckResponse(false, reason);
    }
}
