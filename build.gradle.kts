import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.0"
}

group = "ca.kebs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    api("io.github.jwharm.javagi:gio:0.12.2")
    implementation("io.github.jwharm.javagi:gtk:0.12.2")
    implementation("io.github.jwharm.javagi:adw:0.12.2")
    implementation(project(":libs:goa"))
    implementation("com.sun.mail:jakarta.mail:2.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}

val blueprintFiles = listOf(
    "main.blp"
)

val resourcesDir = file("src/main/resources")
val generatedUiDir = layout.buildDirectory.dir("generated/ui")

tasks.register<Exec>("blueprintCompile") {
    workingDir = resourcesDir

    inputs.files(blueprintFiles.map { file(it) })
    outputs.dir(generatedUiDir)

    commandLine(
        "blueprint-compiler",
        "batch-compile",
        generatedUiDir.get().asFile.absolutePath,
        workingDir.absolutePath,
        *blueprintFiles.toTypedArray()
    )
}

tasks.named<ProcessResources>("processResources") {
    dependsOn("blueprintCompile")
    from(generatedUiDir)
}

tasks.register<Exec>("compileResources") {
    dependsOn("blueprintCompile")

    workingDir = resourcesDir
    commandLine(
        "glib-compile-resources",
        "--target=ui.gresource",
        "--sourcedir=${resourcesDir.absolutePath}",
        "--sourcedir=${generatedUiDir.get().asFile.absolutePath}",
        "--generate",
        "ui.gresource.xml"
    )
}

tasks.named<KotlinCompile>("compileKotlin") {
    dependsOn("compileResources")
}