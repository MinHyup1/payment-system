package com.payment.api.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "payment.security.admin.jwt-secret=" + SecurityTestFixture.ADMIN_JWT_SECRET,
            "payment.security.merchant.api-keys.m_1001=" + SecurityTestFixture.MERCHANT_API_KEY
        })
@AutoConfigureMockMvc
class AdminJwtAuthenticationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void 어드민_API는_토큰_없이_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/admin/probe")).andExpect(status().isUnauthorized());
    }

    @Test
    void 유효한_어드민_JWT는_어드민_API를_통과한다() throws Exception {
        mockMvc.perform(
                        get("/admin/probe")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + SecurityTestFixture.adminToken("admin")))
                .andExpect(status().isOk());
    }

    @Test
    void admin_스코프가_없는_JWT는_어드민_API에서_거부된다() throws Exception {
        mockMvc.perform(
                        get("/admin/probe")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + SecurityTestFixture.adminToken("read")))
                .andExpect(status().isForbidden());
    }

    @Test
    void 가맹점_API_키로는_어드민_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(
                        get("/admin/probe")
                                .header(
                                        ApiKeyAuthenticationFilter.HEADER,
                                        SecurityTestFixture.MERCHANT_API_KEY))
                .andExpect(status().isUnauthorized());
    }
}
