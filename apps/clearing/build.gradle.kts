// 배포 단위 2 — 매입·청산. 승인 이후의 후속 처리(원장, 정산, 대사, 미확정 복구)를
// Kafka 리스너와 스케줄러로 돌린다. API 와 다른 프로세스로 뜨고 따로 스케일한다.
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
