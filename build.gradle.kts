plugins {
    `java-library`
    alias(libs.plugins.jcommon)
    alias(libs.plugins.bundler)
}

jcommon {
    javaVersion = JavaVersion.VERSION_25

    setupPaperRepository()

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

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.paper.api)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens=java.base/java.util=ALL-UNNAMED")
    testLogging.showStandardStreams = true
}
