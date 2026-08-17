// 일 단위 정산 + PG 대사(reconciliation).
// 컨벤션 3.1 — payment-app 을 의존하지 않는다.
dependencies {
    api(project(":modules:shared-kernel"))
    implementation("org.springframework.boot:spring-boot-starter")
}
