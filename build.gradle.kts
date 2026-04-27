plugins {
    java
    id("com.gradleup.shadow") version "8.3.3"
    id("com.vanniktech.maven.publish") version "0.36.0"
    signing
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

    configurations = listOf(project.configurations.runtimeClasspath.get())
    manifest {
        attributes["Automatic-Module-Name"] = "dev.blitical.jigsawDB"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    mergeServiceFiles()
}

mavenPublishing {
    publishToMavenCentral()
    coordinates(group.toString(), rootProject.name, version.toString())

    pom {
        name = "JigsawDB"
        description =
            "A Java package made specifically to reduce the need for string-based SQL, by validating as much as possible at compile-time."

        url = "https://github.com/Blitical/JigsawDB"
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id = "blitical"
                name = "Blitical"
                url = "https://github.com/Blitical"
            }
        }

        scm {
            url = "https://github.com/Blitical/JigsawDB"
            connection = "scm:git:git://github.com/Blitical/JigsawDB.git"
            developerConnection = "scm:git:ssh://git@github.com/Blitical/JigsawDB.git"
        }
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("GPG_PRIVATE_KEY"),
        System.getenv("GPG_PASSPHRASE")
    )
    sign(publishing.publications)
}
