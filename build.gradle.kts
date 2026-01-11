plugins {
    `java-library`
    alias(libs.plugins.jcommon)
    alias(libs.plugins.bundler)
}

jcommon {
    javaVersion = JavaVersion.VERSION_25

    setupPaperRepository()

    repositories {
        maven {
            url = uri("https://maven.enginehub.org/repo/")
        }
        maven {
            url = uri("https://repo.bluecolored.de/releases")
        }
    }

    commonDependencies {
        compileOnly(libs.paper.api)
        compileOnly(libs.bluemap)
        compileOnly(libs.worldguard)

        implementation(libs.codec4j.io.yaml)
    }
}

bundler {
    copyToRootBuildDirectory("BlueMapMarkers-${project.version}")
    replacePluginVersionForBukkit(project.version)
}

tasks.shadowJar {
    minimize()
}
