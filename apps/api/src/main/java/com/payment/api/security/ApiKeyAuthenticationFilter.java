package com.payment.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 가맹점 서버 인증. {@code X-API-Key} 헤더의 키를 merchantId 로 바꿔 인증 주체로 세운다.
 *
 * <p>키가 없거나 모르는 키면 인증을 세우지 않고 넘긴다. 401 은 인가 단계가 판단한다 — 여기서 응답을 직접 쓰면 인증 실패 응답 형식이 두 군데로 갈라진다.
 */
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    static final String HEADER = "X-API-Key";

    private final Map<String, String> merchantIdByApiKey;

    ApiKeyAuthenticationFilter(MerchantApiKeyProperties properties) {
        // Phase 1 에서 가맹점 테이블 조회로 교체한다. 지금은 설정값이 유일한 출처다.
        this.merchantIdByApiKey = properties.apiKeys().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER);
        if (apiKey != null) {
            String merchantId = merchantIdByApiKey.get(apiKey);
            if (merchantId == null) {
                // 컨벤션 3.8 — 시크릿 전문은 로그에 남기지 않는다.
                log.warn("등록되지 않은 API Key 요청. key={} path={}", mask(apiKey), request.getRequestURI());
            } else {
                SecurityContextHolder.getContext()
                        .setAuthentication(new UsernamePasswordAuthenticationToken(
                                merchantId, null, List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))));
            }
        }
        chain.doFilter(request, response);
    }

    private static String mask(String apiKey) {
        return apiKey.length() <= 4 ? "****" : apiKey.substring(0, 4) + "****";
    }
}
