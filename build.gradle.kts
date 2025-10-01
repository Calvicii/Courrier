plugins {
    kotlin("jvm") version "2.2.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
    application
}

group = "ca.kebs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("io.github.compose4gtk:compose-4-gtk:0.7")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(24)
}

tasks.register<Exec>("compileGResources") {
    workingDir("src/main/gresources")
    commandLine(
        "glib-compile-resources",
        "--target=../resources/resources.gresource",
        "resources.gresource.xml",
    )
}

tasks.withType<Tar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
