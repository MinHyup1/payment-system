plugins {
    // 로컬에 JDK 25가 없어도 toolchain 이 자동으로 받아오게 한다.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "payment-system"

include(
    "modules:shared-kernel",
    "modules:payment-domain",
    "modules:payment-app",
    "modules:ledger",
    "modules:settlement",
    "modules:pg-adapter",
    "apps:api",
    "apps:clearing",
)
