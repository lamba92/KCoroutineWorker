import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.21"
    maven
}

group = "it.lamba"
version = "1.0"

repositories {
    mavenCentral()
    maven(url="https://jitpack.io")
}

dependencies {
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.1.1")
    implementation("io.github.microutils","kotlin-logging", "1.6.22")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val sourcesJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    classifier = "sources"
    from(sourceSets.getAt("main").allSource)
}

artifacts.add("archives", sourcesJar)
