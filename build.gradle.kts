plugins {
    id("org.springframework.boot") version "4.1.0" apply false
    id("com.diffplug.spotless") version "8.10.0" apply false
}

allprojects {
    group = "com.payment"
    version = "0.0.1-SNAPSHOT"
}

// modules / apps 는 묶음일 뿐 코드가 없다. 실제 모듈(리프)에만 설정을 적용한다.
configure(subprojects.filter { it.childProjects.isEmpty() }) {
    apply(plugin = "java-library")
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    dependencies {
        val bom = platform("org.springframework.boot:spring-boot-dependencies:4.1.0")

        "implementation"(bom)
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"(bom)
        "annotationProcessor"("org.projectlombok:lombok")

        "testImplementation"(bom)
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"(bom)
        "testAnnotationProcessor"("org.projectlombok:lombok")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    // 컨벤션 3.9 — 포맷은 논쟁하지 않는다. google-java-format(AOSP) = 들여쓰기 4칸.
    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat("1.36.1").aosp()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}
