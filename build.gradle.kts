import java.io.ByteArrayOutputStream

plugins {
    id("base")
}

val versionMajor: String by project
val versionMinor: String by project
val versionBuild = gitCommitCount()

version = "$versionMajor.$versionMinor.$versionBuild"

allprojects {
    group = "edu.io.net"
    version = rootProject.version

    tasks.withType<Jar> {
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        }
    }
}

println("Building version: $version")

fun gitCommitCount(): Int {
    val tag = ByteArrayOutputStream().also { out ->
        exec {
            commandLine("git", "describe", "--tags", "--abbrev=0")
            standardOutput = out
            isIgnoreExitValue = true
        }
    }.toString().trim()

    if (tag.isBlank()) return 0

    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-list", "--count", "$tag..HEAD")
        standardOutput = stdout
        isIgnoreExitValue = true
    }
    return stdout.toString().trim().toIntOrNull() ?: 0
}
