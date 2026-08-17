// 승인/취소 유스케이스, 멱등성, Outbox 기록.
dependencies {
    api(project(":modules:payment-domain"))
    implementation(project(":modules:pg-adapter"))
    implementation("org.springframework.boot:spring-boot-starter")
}
