package com.nexusflow.permission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.permission.dto.RoleCreateRequest;
import com.nexusflow.permission.service.RoleService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PermissionExceptionHandlerTest {

    private final RoleService roleService = mock(RoleService.class);
    private final MockMvc mockMvc = standaloneSetup(new RoleApiController(roleService))
            .setControllerAdvice(new PermissionExceptionHandler())
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsStableValidationErrorEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoleCreateRequest(
                                "bad role",
                                "",
                                "BAD_SCOPE",
                                "description",
                                null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/v1/role"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.violations[*].field", hasItem("code")));

        verifyNoInteractions(roleService);
    }
}
