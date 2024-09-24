

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val junit_version: String by project


plugins {
    kotlin("jvm") version "1.9.23"
    id("eu.cloudnetservice.juppiter") version "0.4.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.github.thecguy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    compileOnly("eu.cloudnetservice.cloudnet:driver:4.0.0-RC10")
    compileOnly("eu.cloudnetservice.cloudnet:node:4.0.0-RC10")
    compileOnly("eu.cloudnetservice.cloudnet:bridge:4.0.0-RC10")

    compileOnly("com.github.cloudnetservice.cloud-command-framework:cloud-core:2.0.0-cn1")
    compileOnly("com.github.cloudnetservice.cloud-command-framework:cloud-annotations:2.0.0-cn1")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-swagger:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")

    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("com.google.code.gson:gson:2.8.9")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("ch.qos.logback:logback-classic:1.5.6")

}


tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(17)
        options.compilerArgs.add("-Xlint:deprecation")

    }
    named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
    }
}



kotlin {
    jvmToolchain(17)
}

moduleJson {
    name = "CloudNet-Rest-Module"
    author = "TheCGuy"
    main = "io.github.thecguy.cloudnet_rest_module.CloudNet_Rest_Module"
    description = "This CloudNet Module enables a Rest API!"
}



