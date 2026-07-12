package com.nexusflow.api.controller;

import com.nexusflow.api.auth.AuthService;
import com.nexusflow.api.auth.ActiveMerchantRequest;
import com.nexusflow.api.auth.LoginRequest;
import com.nexusflow.api.auth.UserInfoResponse;
import com.nexusflow.api.security.MerchantAuthContext;
import com.nexusflow.common.ApiResponse;
import com.nexusflow.common.ErrorCode;
import com.nexusflow.common.NexusFlowException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<UserInfoResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpSession session) {
        UserInfoResponse user = authService.login(request, session);
        return ApiResponse.ok(user);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> me(HttpServletRequest request) {
        String userId = MerchantAuthContext.userId(request)
                .orElseThrow(() -> new NexusFlowException(ErrorCode.UNAUTHORIZED, "Not authenticated"));
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new NexusFlowException(ErrorCode.UNAUTHORIZED, "Not authenticated");
        }
        UserInfoResponse user = authService.me(userId, session);
        return ApiResponse.ok(user);
    }

    @PostMapping("/active-merchant")
    public ApiResponse<UserInfoResponse> switchActiveMerchant(@Valid @RequestBody ActiveMerchantRequest request,
                                                              HttpServletRequest httpRequest) {
        String userId = MerchantAuthContext.userId(httpRequest)
                .orElseThrow(() -> new NexusFlowException(ErrorCode.UNAUTHORIZED, "Not authenticated"));
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            throw new NexusFlowException(ErrorCode.UNAUTHORIZED, "Not authenticated");
        }
        authService.switchActiveMerchant(userId, request.getMerchantId(), session);
        UserInfoResponse user = authService.me(userId, session);
        return ApiResponse.ok(user);
    }
}
