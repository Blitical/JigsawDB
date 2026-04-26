plugins {
    java
    id("com.gradleup.shadow") version "8.3.3"
    `maven-publish`
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.shadowJar)

            groupId = "dev.blitical"
            artifactId = rootProject.name
            version = project.version.toString()

            pom {
                name.set("jigsawDB")
                description.set("Your description here")
                url.set("https://github.com/YOUR_USERNAME/YOUR_REPO")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("blitical")
                        name.set("Blitical")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/YOUR_USERNAME/YOUR_REPO.git")
                    developerConnection.set("scm:git:ssh://github.com/YOUR_USERNAME/YOUR_REPO.git")
                    url.set("https://github.com/YOUR_USERNAME/YOUR_REPO")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("GPG_PRIVATE_KEY"),
        System.getenv("GPG_PASSPHRASE")
    )
    sign(publishing.publications["mavenJava"])
}
