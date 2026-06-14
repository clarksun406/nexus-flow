package com.nexusflow.api.controller;

import com.nexusflow.application.OpsDashboardApplicationService;
import com.nexusflow.application.dto.OpsDashboardResponse;
import com.nexusflow.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops")
@RequiredArgsConstructor
public class OpsDashboardController {

    private final OpsDashboardApplicationService dashboardService;

    @GetMapping("/dashboard")
    public ApiResponse<OpsDashboardResponse> dashboard() {
        return ApiResponse.ok(dashboardService.getDashboard());
    }
}
