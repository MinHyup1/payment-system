package com.payment.api.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 규칙만 검증하기 위한 테스트 전용 엔드포인트. 실제 승인/취소 엔드포인트는 Phase 1 에서 붙는다.
 *
 * <p>프로덕션 소스에 두지 않는 이유는, 인증을 증명하려고 실서비스 경로를 만들면 그 경로가 남기 때문이다.
 */
@RestController
class SecurityProbeController {

    @GetMapping("/probe/merchant")
    String merchant(@AuthenticationPrincipal String merchantId) {
        return merchantId;
    }

    @GetMapping("/admin/probe")
    String admin() {
        return "admin";
    }
}
