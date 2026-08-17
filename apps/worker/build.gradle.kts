// 배포 단위 2 — Kafka 리스너 + 스케줄러. API 와 다른 프로세스로 뜨고 따로 스케일한다.
plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":modules:payment-app"))
    implementation(project(":modules:ledger"))
    implementation(project(":modules:settlement"))
    implementation("org.springframework.boot:spring-boot-starter")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
