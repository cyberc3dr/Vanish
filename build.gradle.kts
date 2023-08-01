import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
}

group = "ru.sliva"
version = "1.0"
description = "Vanish"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<ProcessResources>() {
    expand(
        "name" to project.name,
        "version" to project.version,
        "description" to project.description
    )
}
