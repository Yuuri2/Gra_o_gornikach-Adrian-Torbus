plugins {
    id("java-library")
    id("maven-publish")
}

group = "edu.io.net"

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")

    implementation(project(":game_common"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.javadoc {
    isFailOnError = false
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        charSet = "UTF-8"
        links("https://docs.oracle.com/en/java/javase/24/docs/api/")
        addBooleanOption("html5", true)
        addBooleanOption("Xdoclint:none", true)
        addStringOption("notnull", "org.jetbrains.annotations.NotNull")
        addStringOption("nullable", "org.jetbrains.annotations.Nullable")
    }
}

tasks.test {
    useJUnitPlatform()
}
