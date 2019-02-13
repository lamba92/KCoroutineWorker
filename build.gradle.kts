import groovy.util.Node
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.util.Properties

plugins {
    kotlin("multiplatform") version "1.3.20"
    id("maven-publish")
    signing
}

group = "com.github.lamba92"
version = "2.0.0"

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://jitpack.io")
}

kotlin {

    sourceSets.create("nativeCommon")
    jvm {
        compilations["main"].kotlinOptions.jvmTarget = "1.8"
    }
    iosArm64()
    iosX64()
    mingwX64()
    macosX64()
    linuxX64()

    configure(nativeTargets) {
        compilations("main") {
            defaultSourceSet.dependsOn(sourceSets["nativeCommon"])
            dependencies {
                implementation(kotlinx("coroutines-core-native", "1.1.1"))
            }
        }
    }

    configure(platformIndependentTargets + androidTargets) {
        mavenPublication {
            tasks.withType<AbstractPublishToMaven>().all {
                onlyIf {
                    publication != this@mavenPublication || OperatingSystem.current().isLinux
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(kotlinx("coroutines-core-common", "1.1.1"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(kotlinx("coroutines-core", "1.1.1"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
    }
}

val localProp = properties("local.properties")

val keyId = System.getenv()["SIGNING_KEYID"]
    ?: localProp["signing.keyId"] as String?
    ?: extra.getOrNull("signing.keyId") as String?
val gpgPassword = System.getenv()["SIGNING_PASSWORD"]
    ?: localProp["signing.password"] as String?
    ?: extra.getOrNull("signing.password") as String?
val gpgFile = System.getenv()["SIGNING_SECRETRINGFILE"]
    ?: localProp["signing.secretKeyRingFile"] as String?
    ?: extra.getOrNull("signing.secretKeyRingFile") as String?
val sonatypeUsername = System.getenv()["SONATYPEUSERNAME"]
    ?: localProp["sonatypeUsername"] as String?
    ?: extra.getOrNull("sonatypeUsername") as String?
val sonatypePassword = System.getenv()["SONATYPEPASSWORD"]
    ?: localProp["sonatypePassword"] as String?
    ?: extra.getOrNull("sonatypePassword") as String?

if (listOf(
        keyId,
        gpgPassword,
        gpgFile,
        sonatypeUsername,
        sonatypePassword
    ).none { it == null } && file(gpgFile!!).exists()
) {

    println("Publishing setup detected. Setting up publishing...")

    val javadocJar by tasks.creating(Jar::class) {
        archiveClassifier.value("javadoc")
        // TODO: instead of a single empty Javadoc JAR, generate real documentation for each module
    }

    val sourcesJar by tasks.creating(Jar::class) {
        archiveClassifier.value("sources")
    }

    signing {
        
    }

    extra["signing.keyId"] = keyId
    extra["signing.password"] = gpgPassword
    extra["signing.secretKeyRingFile"] = gpgFile

    publishing {
        publications {
            configure(withType<MavenPublication>()) {
                signing.sign(this)
                customizeForMavenCentral(pom)
                artifact(javadocJar)
            }
            withType<MavenPublication>()["kotlinMultiplatform"].artifact(sourcesJar)
        }
        repositories {
            maven(url = "https://oss.sonatype.org/service/local/staging/deploy/maven2")
                .credentials {
                    username = sonatypeUsername
                    password = sonatypePassword
                }
        }
    }
} else println(buildString {
    appendln("Not enough information to publish:")
    appendln("keyId: ${if (keyId == null) "NOT " else ""}found")
    appendln("gpgPassword: ${if (gpgPassword == null) "NOT " else ""}found")
    appendln("gpgFile: ${gpgFile ?: "NOT found"}")
    appendln("gpgFile presence: ${gpgFile?.let { file(it).exists() } ?: "false"}")
    appendln("sonatypeUsername: ${if (sonatypeUsername == null) "NOT " else ""}found")
    appendln("sonatypePassword: ${if (sonatypePassword == null) "NOT " else ""}found")
})


val KotlinMultiplatformExtension.nativeTargets
    get() = targets.filter { it is KotlinNativeTarget }.map { it as KotlinNativeTarget }

val KotlinMultiplatformExtension.platformIndependentTargets
    get() = targets.filter { it !is KotlinNativeTarget || it.konanTarget == KonanTarget.WASM32 }

val KotlinMultiplatformExtension.appleTargets
    get() = targets.filter {
        it is KotlinNativeTarget && listOf(
            KonanTarget.IOS_ARM64,
            KonanTarget.IOS_X64,
            KonanTarget.MACOS_X64
        ).any { target -> it.konanTarget == target }
    }

val KotlinMultiplatformExtension.windowsTargets
    get() = targets.filter { it is KotlinNativeTarget && it.konanTarget == KonanTarget.MINGW_X64 }

val KotlinMultiplatformExtension.linuxTargets
    get() = targets.filter {
        it is KotlinNativeTarget && listOf(
            KonanTarget.LINUX_ARM32_HFP,
            KonanTarget.LINUX_MIPS32,
            KonanTarget.LINUX_MIPSEL32,
            KonanTarget.LINUX_X64
        ).any { target -> it.konanTarget == target }
    }

val KotlinMultiplatformExtension.androidTargets
    get() = targets.filter {
        it is KotlinNativeTarget && listOf(
            KonanTarget.ANDROID_ARM32,
            KonanTarget.ANDROID_ARM64
        ).any { target -> it.konanTarget == target }
    }

fun KotlinDependencyHandler.kotlinx(module: String, version: String? = null): Any =
    "org.jetbrains.kotlinx:kotlinx-$module${version?.let { ":$version" } ?: "+"}"

fun KotlinNativeTarget.compilations(vararg name: String, config: KotlinNativeCompilation.() -> Unit) =
    name.forEach { compilations[it].apply(config) }

fun Node.add(key: String, value: String) = appendNode(key).setValue(value)

fun Node.node(key: String, content: Node.() -> Unit) = appendNode(key).also(content)

fun org.gradle.api.publish.maven.MavenPom.buildAsNode(builder: Node.() -> Unit) = withXml { asNode().apply(builder) }

fun properties(file: File) = Properties().apply { load(file.apply { if (!exists()) createNewFile() }.inputStream()) }
fun properties(fileSrc: String) = properties(file(fileSrc))

fun customizeForMavenCentral(pom: org.gradle.api.publish.maven.MavenPom) = pom.buildAsNode {
    add("description", "Commodity classes to implement cyclic workers.\ny")
    add("name", project.name)
    add("url", "https://github.com/lamba92/KCoroutineWorker")
    node("organization") {
        add("name", "com.github.lamba92")
        add("url", "https://github.com/lamba92")
    }
    node("issueManagement") {
        add("system", "github")
        add("url", "https://github.com/lamba92/KCoroutineWorker/issues")
    }
    node("licenses") {
        node("license") {
            add("name", "Apache License 2.0")
            add("url", "https://github.com/lamba92/KCoroutineWorker/blob/master/LICENSE")
            add("distribution", "repo")
        }
    }
    node("scm") {
        add("url", "https://github.com/lamba92/KCoroutineWorker")
        add("connection", "scm:git:git://github.com/lamba92/KCoroutineWorker.git")
        add("developerConnection", "scm:git:ssh://github.com/lamba92/KCoroutineWorker.git")
    }
    node("developers") {
        node("developer") {
            add("name", "Lamba92")
        }
    }
}

fun ExtraPropertiesExtension.getOrNull(name: String) = if(has(name)) get(name) else null