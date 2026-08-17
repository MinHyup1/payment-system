package com.payment.api.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

final class SecurityTestFixture {

    /** HS256 은 최소 256비트 키를 요구한다. */
    static final String ADMIN_JWT_SECRET = "test-only-admin-secret-key-32bytes-or-longer";

    static final String MERCHANT_API_KEY = "test-only-merchant-key-1001";

    private SecurityTestFixture() {}

    static String adminToken(String scope) throws JOSEException {
        Instant now = Instant.now(Clock.systemUTC());
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("admin-1")
                .claim("scope", scope)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofMinutes(5))))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(ADMIN_JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
