package com.nexusflow.api.controller;

import com.nexusflow.application.OrphanTransactionApplicationService;
import com.nexusflow.application.dto.OrphanTransactionResponse;
import com.nexusflow.application.dto.ResolveOrphanTransactionRequest;
import com.nexusflow.common.ApiResponse;
import com.nexusflow.domain.blockchain.OrphanTransactionStatus;
import com.nexusflow.domain.shared.Chain;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/crypto/orphan-transactions")
@RequiredArgsConstructor
public class OrphanTransactionController {

    private final OrphanTransactionApplicationService orphanTransactionService;

    @GetMapping
    public ApiResponse<List<OrphanTransactionResponse>> list(
            @RequestParam(value = "status", defaultValue = "UNMATCHED") OrphanTransactionStatus status) {
        return ApiResponse.ok(orphanTransactionService.list(status));
    }

    @PostMapping("/{chain}/{txHash}/resolve")
    public ApiResponse<OrphanTransactionResponse> resolve(@PathVariable("chain") Chain chain,
                                                          @PathVariable("txHash") String txHash,
                                                          @Valid @RequestBody ResolveOrphanTransactionRequest request) {
        return ApiResponse.ok(orphanTransactionService.resolve(chain, txHash, request.getPaymentId()));
    }

    @PostMapping("/{chain}/{txHash}/ignore")
    public ApiResponse<OrphanTransactionResponse> ignore(@PathVariable("chain") Chain chain,
                                                         @PathVariable("txHash") String txHash) {
        return ApiResponse.ok(orphanTransactionService.ignore(chain, txHash));
    }

    @PostMapping("/{chain}/{txHash}/compensate")
    public ApiResponse<OrphanTransactionResponse> compensate(@PathVariable("chain") Chain chain,
                                                             @PathVariable("txHash") String txHash) {
        return ApiResponse.ok(orphanTransactionService.compensate(chain, txHash));
    }
}
