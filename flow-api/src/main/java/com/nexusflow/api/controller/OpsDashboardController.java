package com.nexusflow.api.controller;

import com.nexusflow.application.OpsDashboardApplicationService;
import com.nexusflow.application.dto.OpsDashboardResponse;
import com.nexusflow.common.ApiResponse;
import com.nexusflow.permission.client.CheckPermission;
import com.nexusflow.permission.client.PermissionCodes;
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
    @CheckPermission(value = PermissionCodes.OpsDashboard.READ, scopeType = "SYSTEM")
    public ApiResponse<OpsDashboardResponse> dashboard() {
        return ApiResponse.ok(dashboardService.getDashboard());
    }
}
