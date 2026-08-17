package com.payment.api.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 인증 축을 소비자별로 나눈다.
 *
 * <ul>
 *   <li>가맹점 서버(server-to-server) — API Key. 결제 업계 표준이다.
 *   <li>어드민 API(사람) — JWT. Phase 4 의 DLQ 재조정·MANUAL_REVIEW 처리가 여기에 붙는다.
 * </ul>
 *
 * <p>체인을 분리하는 이유는 가맹점 키로 어드민 경로에 들어갈 수 없게 만들기 위해서다.
 */
@Configuration
@EnableConfigurationProperties(MerchantApiKeyProperties.class)
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/admin/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAuthority("SCOPE_admin"))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain merchantSecurityFilterChain(HttpSecurity http, MerchantApiKeyProperties properties)
            throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health")
                        .permitAll()
                        .anyRequest()
                        .hasRole("MERCHANT"))
                .addFilterBefore(new ApiKeyAuthenticationFilter(properties), UsernamePasswordAuthenticationFilter.class)
                // 기본값은 403 이다. 키가 없거나 틀린 것은 "권한 부족"이 아니라 "인증 실패"이므로 401 로 돌려준다.
                .exceptionHandling(handling ->
                        handling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .build();
    }

    /** 어드민 토큰은 우리가 발급하고 우리가 검증한다. 외부 IdP 가 없으므로 대칭키(HS256)로 충분하다. */
    @Bean
    JwtDecoder adminJwtDecoder(@Value("${payment.security.admin.jwt-secret}") String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
