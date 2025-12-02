plugins {
    id("java")
    id("application")
}

val mainClassName = "edu.io.net.Main"

application {
    mainClass.set(mainClassName)
}

group = "edu.io.net"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(fileTree("libs"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}