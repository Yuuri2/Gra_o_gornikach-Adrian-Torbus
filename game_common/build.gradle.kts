plugins {
    id("java")
}

val versionMajor: String = rootProject.findProperty("versionMajor") as String
val versionMinor: String = rootProject.findProperty("versionMinor") as String
val versionFull: String = rootProject.version.toString()

val generatedDir = layout.buildDirectory.dir("generated/sources/version")

val generateVersionClass by tasks.registering {
    val outputDir = generatedDir.get().asFile
    outputs.dir(outputDir)

    inputs.property("versionFull", versionFull)

    doLast {
        val pkg = "edu.io.net"
        val file = file("${outputDir}/${pkg.replace('.', '/')}/Version.java")
        file.parentFile.mkdirs()
        file.writeText("""
            package $pkg;

            /**
             * Auto-generated version class.
             * Shared version for all modules.
             */
            public final class Version {
                public static final int MAJOR = $versionMajor;
                public static final int MINOR = $versionMinor;
                public static final String FULL = "$versionFull";
            }
        """.trimIndent())
    }
}

sourceSets {
    named("main") {
        java.srcDir(generatedDir)
    }
}

tasks.named("compileJava") {
    dependsOn(generateVersionClass)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.awaitility:awaitility:4.2.1")
}

tasks.test {
    useJUnitPlatform()
}
