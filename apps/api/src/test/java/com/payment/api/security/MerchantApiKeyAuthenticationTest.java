package com.payment.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "payment.security.admin.jwt-secret=" + SecurityTestFixture.ADMIN_JWT_SECRET,
            "payment.security.merchant.api-keys.m_1001=" + SecurityTestFixture.MERCHANT_API_KEY
        })
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class MerchantApiKeyAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void API_키가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/probe/merchant")).andExpect(status().isUnauthorized());
    }

    @Test
    void 등록되지_않은_API_키는_401을_반환한다() throws Exception {
        mockMvc.perform(get("/probe/merchant").header(ApiKeyAuthenticationFilter.HEADER, "unknown-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 유효한_API_키는_merchantId를_인증_주체로_세운다() throws Exception {
        mockMvc.perform(get("/probe/merchant")
                        .header(ApiKeyAuthenticationFilter.HEADER, SecurityTestFixture.MERCHANT_API_KEY))
                .andExpect(status().isOk())
                .andExpect(content().string("m_1001"));
    }

    @Test
    void 헬스체크는_인증_없이_접근할_수_있다() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void 인증_실패_로그에_API_키_전문이_남지_않는다(CapturedOutput output) throws Exception {
        // given
        String leakedKey = "unknown-key-that-must-not-be-logged";

        // when
        mockMvc.perform(get("/probe/merchant").header(ApiKeyAuthenticationFilter.HEADER, leakedKey))
                .andExpect(status().isUnauthorized());

        // then
        assertThat(output).doesNotContain(leakedKey);
        assertThat(output).contains("unkn****");
    }
}
