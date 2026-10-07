import com.android.build.api.dsl.LibraryExtension
import io.github.kakaocup.Github
import java.net.URI

plugins {
    id("maven-publish")
    id("signing")
}

val ghToken: String? = System.getenv("GH_TOKEN")

val releaseMode: String? by project
val versionSuffix = when (releaseMode) {
    "RELEASE" -> ""
    else -> "-SNAPSHOT"
}

group = "io.github.kakaocup"
version = readVersion() + versionSuffix

configure<PublishingExtension> {
    publications.withType<MavenPublication>().configureEach {
        groupId = project.group.toString()

        pom {
            name.set("Kakao Compose")
            url.set("https://github.com/KakaoCup/Compose")
            description.set("Nice and simple DSL for Espresso in Kotlin")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers(findCollaborators())

            scm {
                url.set("https://github.com/KakaoCup/Compose.git")
                connection.set("scm:git:ssh://github.com/KakaoCup/Compose")
                developerConnection.set("scm:git:ssh://github.com/KakaoCup/Compose")
            }
        }
    }
    repositories {
        maven {
            name = "Local"
            setUrl("${project.rootDir}/build/repository")
        }
        maven {
            name = "OSSHR"
            credentials {
                username = System.getenv("SONATYPE_USERNAME")
                password = System.getenv("SONATYPE_PASSWORD")
            }
            url = URI.create(
                when (releaseMode) {
                    "RELEASE" -> System.getenv("SONATYPE_RELEASES_URL")
                        ?: "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"

                    else -> System.getenv("SONATYPE_SNAPSHOTS_URL")
                        ?: "https://central.sonatype.com/repository/maven-snapshots/"
                }
            )
        }
    }
}

// Android-only libraries: a single "default" publication built from the release variant
plugins.withId("com.android.library") {
    configure<LibraryExtension> {
        publishing {
            singleVariant("release") {
                withJavadocJar()
                withSourcesJar()
            }
        }
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("default") {
                components.whenObjectAdded {
                    if (this.name == "release") {
                        from(components["release"])
                    }
                }
            }
        }
    }
}

// Multiplatform libraries: KMP creates the publications itself, Maven Central still requires a javadoc jar per artifact
plugins.withId("org.jetbrains.kotlin.multiplatform") {
    configure<PublishingExtension> {
        publications.withType<MavenPublication>().configureEach {
            val publicationName = name
            val javadocJar = tasks.register<Jar>("${publicationName}JavadocJar") {
                archiveClassifier.set("javadoc")
                archiveAppendix.set(publicationName)
            }
            artifact(javadocJar)
        }
    }
}

val passphrase: String? = System.getenv("GPG_PASSPHRASE")

if (!passphrase.isNullOrBlank()) {
    project.configure<SigningExtension> {
        sign(publishing.publications)
    }

    // With several publications every publish task picks up all signature files, so it must run after all signing tasks
    tasks.withType<AbstractPublishToMaven>().configureEach {
        dependsOn(tasks.withType<Sign>())
    }

    project.extra.set("signing.keyId", "0110979F")
    project.extra.set("signing.password", passphrase)
    project.extra.set("signing.secretKeyRingFile", "${project.rootProject.rootDir}/buildsystem/secring.gpg")
}

tasks.register<Zip>("bundleForCentralSigned") {
    from(project.layout.buildDirectory.dir("repository")) {
        include("**/*")
    }

    archiveFileName.set("${project.name}-signed.zip")
    destinationDirectory.set(layout.buildDirectory.dir("central-bundles"))

    doLast {
        println("✓ Signed bundle ready: ${archiveFile.get().asFile.absolutePath}")
    }
}

fun readVersion(): String {
    return project.findProperty("lib.version.${project.name}")?.toString() ?: throw Exception("Undefined version '\"lib.version.${project.name}\"' in versions.properties")
}

fun findCollaborators() = Action<MavenPomDeveloperSpec> {
    if (!ghToken.isNullOrEmpty()) {
        Github(ghToken).collaborators.forEach {
            developer {
                id.set(it.login)
                url.set("https://github.com/${it.login}")
                name.set(it.name)
            }
        }
    }
}

fun findContributors() = Action<MavenPomContributorSpec> {
    if (!ghToken.isNullOrEmpty()) {
        Github(ghToken).contributors.sortedBy { it.login }.forEach {
            contributor {
                name.set(it.login)
                url.set("https://github.com/${it.login}")
            }
        }
    }
}
