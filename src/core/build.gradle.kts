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
    compileOnly("org.jetbrains:annotations:26.0.2-1")
    implementation("com.google.code.gson:gson:2.13.2")

    runtimeOnly("org.xerial:sqlite-jdbc:3.45.2.0")
    runtimeOnly("com.mysql:mysql-connector-j:8.3.0")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.3.3")
    runtimeOnly("org.postgresql:postgresql:42.7.3")

    implementation("org.slf4j:slf4j-api:2.0.13")
}
