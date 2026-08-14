import org.gradle.api.tasks.SourceSetContainer

plugins {
    java
    id("com.gradleup.shadow") version "8.3.3"
    id("com.vanniktech.maven.publish") version "0.36.0"
    signing
}

group = "dev.blitical"
version = project.version

allprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }
}

val shadedProjects by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val publishedProjects = listOf(project(":core"), project(":processor"))

dependencies {
    shadedProjects(project(":core")) {
        isTransitive = true
    }
    shadedProjects(project(":processor")) {
        isTransitive = true
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    archiveBaseName.set(rootProject.name)
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("") // removes "-all"

    configurations = listOf(shadedProjects)
    exclude(
        "org/sqlite/**",
        "com/mysql/**",
        "org/mariadb/**",
        "org/postgresql/**"
    )
    // Not supported in Java 25, UNCOMMENT IF WE EVER DOWNGRADE TO LTS Java 21
    //relocate("com.google.gson", "dev.blitical.jigsawdb.shaded.gson")
    manifest {
        attributes["Automatic-Module-Name"] = "dev.blitical.jigsawDB"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    mergeServiceFiles()
}

gradle.projectsEvaluated {
    tasks.named<Jar>("sourcesJar") {
        publishedProjects.forEach { subproject ->
            from(subproject.extensions.getByType<SourceSetContainer>()["main"].allSource)
        }
    }

    tasks.named<Javadoc>("javadoc") {
        publishedProjects.forEach { subproject ->
            val mainSourceSet = subproject.extensions.getByType<SourceSetContainer>()["main"]

            source(mainSourceSet.allJava)
            classpath += mainSourceSet.compileClasspath
        }
    }
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
                name.set("GNU General Public License v3.0")
                url.set("https://www.gnu.org/licenses/gpl-3.0.html")
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

val signingKey = System.getenv("GPG_PRIVATE_KEY")
val signingPassphrase = System.getenv("GPG_PASSPHRASE")

gradle.taskGraph.whenReady {
    val publishesToMavenCentral = allTasks.any { it.name.contains("MavenCentral") }

    if (publishesToMavenCentral && signingKey.isNullOrBlank() && !gradle.startParameter.isDryRun) {
        throw GradleException("GPG_PRIVATE_KEY must be set before publishing to Maven Central.")
    }
}

signing {
    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassphrase)
        sign(publishing.publications)
    }
}
