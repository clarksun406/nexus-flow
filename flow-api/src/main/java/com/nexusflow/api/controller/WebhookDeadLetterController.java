package com.nexusflow.api.controller;

import com.nexusflow.application.WebhookDeadLetterApplicationService;
import com.nexusflow.application.WebhookDeadLetterStatus;
import com.nexusflow.application.dto.WebhookDeadLetterResponse;
import com.nexusflow.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ops/webhook-dead-letters")
@RequiredArgsConstructor
public class WebhookDeadLetterController {

    private final WebhookDeadLetterApplicationService service;

    @GetMapping
    public ApiResponse<List<WebhookDeadLetterResponse>> list(
            @RequestParam(value = "status", defaultValue = "PENDING") WebhookDeadLetterStatus status,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return ApiResponse.ok(service.list(status, limit));
    }

    @PostMapping("/{id}/replay")
    public ApiResponse<WebhookDeadLetterResponse> replay(@PathVariable("id") String id) {
        return ApiResponse.ok(service.replay(id));
    }

    @PostMapping("/{id}/ignore")
    public ApiResponse<WebhookDeadLetterResponse> ignore(@PathVariable("id") String id) {
        return ApiResponse.ok(service.ignore(id));
    }
}
