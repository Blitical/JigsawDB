plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    implementation(project(":core"))
    annotationProcessor(project(":processor"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testAnnotationProcessor(project(":processor"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("ch.qos.logback:logback-classic:1.5.6")
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "failed", "skipped", "standard_out", "standard_error")
    }
}

tasks.register<Test>("testWithoutCache") {
    description = "Runs tests without used gradle cached test results"
    group = "verification"

    testClassesDirs = tasks.test.get().testClassesDirs
    classpath = tasks.test.get().classpath
    useJUnitPlatform()

    outputs.upToDateWhen { false }
    outputs.cacheIf { false }

    testLogging {
        events("passed", "failed", "skipped", "standard_out", "standard_error")
    }
}
