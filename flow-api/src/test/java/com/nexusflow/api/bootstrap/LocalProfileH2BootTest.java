package com.nexusflow.api.bootstrap;

import com.nexusflow.api.NexusFlowApplication;
import com.nexusflow.api.security.ApiKeyHasher;
import com.nexusflow.domain.merchant.MerchantCredentialRepository;
import com.nexusflow.domain.merchant.MerchantProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full application context on the 'h2' local profile with an in-memory
 * H2 database: verifies that Flyway builds the schema, the demo data is seeded,
 * and seeded credentials actually authenticate (API key lookup + portal login).
 */
@SpringBootTest(classes = NexusFlowApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:nexusflow_boot_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=KEY,VALUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.h2.console.enabled=false"
        })
@ActiveProfiles("h2")
@AutoConfigureMockMvc
class LocalProfileH2BootTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantProfileRepository profileRepo;

    @Autowired
    private MerchantCredentialRepository credentialRepo;

    @Autowired
    private ApiKeyHasher apiKeyHasher;

    @Test
    void appBootsOnH2WithSeededDemoDataAndLoginWorks() throws Exception {
        assertTrue(profileRepo.findById(LocalSeedDataRunner.DEMO_MERCHANT_ID).isPresent(),
                "demo merchant profile should be seeded");
        assertTrue(credentialRepo.findActiveByKeyHash(
                        apiKeyHasher.hash(LocalSeedDataRunner.DEMO_API_KEY), Instant.now()).isPresent(),
                "demo API key should authenticate");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + LocalSeedDataRunner.DEMO_EMAIL
                                + "\",\"password\":\"" + LocalSeedDataRunner.DEMO_PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }
}
