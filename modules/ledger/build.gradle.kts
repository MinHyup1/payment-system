// 복식부기 원장. payment 이벤트만 구독한다.
// 컨벤션 3.1 — payment-app 을 의존하지 않는다. 통신은 shared-kernel 의 이벤트 레코드로만.
dependencies {
    api(project(":modules:shared-kernel"))
    implementation("org.springframework.boot:spring-boot-starter")
}
