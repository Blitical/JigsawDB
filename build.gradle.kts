plugins {
    java
    id("com.gradleup.shadow") version "8.3.3"
}

group = "dev.blitical"
version = project.version

allprojects {
    repositories {
        mavenCentral()
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":processor"))
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    archiveBaseName.set(rootProject.name)
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("") // removes "-all"

    from(project(":core").sourceSets.main.get().output)
    from(project(":processor").sourceSets.main.get().output)
    manifest {
        attributes["Automatic-Module-Name"] = "dev.blitical.jigsawDB"
    }

    configurations = listOf(project.configurations.runtimeClasspath.get())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    mergeServiceFiles()
}
