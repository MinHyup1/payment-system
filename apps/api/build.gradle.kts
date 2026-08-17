// 배포 단위 1 — REST 승인/취소 엔드포인트. 동기 HTTP 경로만 담당한다.
plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":modules:payment-app"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    // 어드민 JWT 검증. 토큰 파싱을 직접 짜지 않기 위해 리소스 서버를 쓴다.
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Boot 4 에서 MockMvc 슬라이스(@AutoConfigureMockMvc)가 별도 아티팩트로 분리됐다.
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
}
