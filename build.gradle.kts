plugins {
    `java-library`
    alias(libs.plugins.jcommon)
    alias(libs.plugins.bundler)
}

jcommon {
    javaVersion = JavaVersion.VERSION_25

    setupPaperRepository()
    setupJUnit(libs.junit.bom)
    setupMockito(libs.mockito)

    repositories {
        mavenCentral()
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
        compileOnly(libs.worldguard) {
            // WorldGuard strictly constrains Guava/Gson to older versions than Paper API requires
            exclude(group = "com.google.guava", module = "guava")
            exclude(group = "com.google.code.gson", module = "gson")
        }

        compileOnly(libs.configurate.yaml)
        testImplementation(libs.configurate.yaml)

        testImplementation(libs.junit.jupiter)
        testImplementation(libs.paper.api)
        testImplementation(libs.bluemap)
        testImplementation(libs.worldguard) {
            // Keep the test runtime aligned with the production dependency constraints.
            exclude(group = "com.google.guava", module = "guava")
            exclude(group = "com.google.code.gson", module = "gson")
        }
    }
}

bundler {
    copyToRootBuildDirectory("BlueMapMarkers-${project.version}")
    replacePluginVersionForBukkit(project.version)
}

tasks.shadowJar {
    minimize()
}
