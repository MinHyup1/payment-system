package com.payment.api.security;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 가맹점 API Key. 키가 곧 시크릿이므로 값은 환경변수로 주입받는다.
 *
 * @param apiKeys merchantId → apiKey
 */
@ConfigurationProperties(prefix = "payment.security.merchant")
public record MerchantApiKeyProperties(Map<String, String> apiKeys) {

    public MerchantApiKeyProperties {
        apiKeys = apiKeys == null ? Map.of() : Map.copyOf(apiKeys);
    }
}
