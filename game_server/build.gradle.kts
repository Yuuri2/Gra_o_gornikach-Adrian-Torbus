import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "edu.io.net"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":game_common"))
    implementation(project(":game_connector_lib"))

    implementation("org.slf4j:slf4j-api:2.0.16")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.13")
    compileOnly("org.jetbrains:annotations:24.1.0")

    testImplementation("org.awaitility:awaitility:4.2.1")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

val mainClassName = "edu.io.net.server.Main"

application {
    mainClass.set(mainClassName)
}

tasks.withType<ShadowJar> {
    archiveVersion.set(project .version.toString())
    archiveClassifier.set("all")

    manifest {
        attributes["Main-Class"] = mainClassName
    }
}

tasks.named("shadowJar") {
    dependsOn(tasks.named("classes"))
}

tasks.jar {
    enabled = false
}
