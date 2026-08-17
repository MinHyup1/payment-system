// 배포 단위 1 — REST 승인/취소 엔드포인트. 동기 HTTP 경로만 담당한다.
plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":modules:payment-app"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
