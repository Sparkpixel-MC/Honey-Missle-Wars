plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

group = "org.yeggs"
version = "1.0.0"
description = "Honey Missile Wars - datapack-to-plugin conversion with SlimeWorld multi-instance support"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://repo.infernalsuite.com/repository/maven-snapshots/") {
        name = "infernalsuite-snapshots"
    }
    maven("https://repo.infernalsuite.com/repository/maven-releases/") {
        name = "infernalsuite-releases"
    }
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        name = "placeholderapi"
    }
    maven("https://jitpack.io") {
        name = "jitpack"
    }
}

dependencies {
    // Paper 1.21.10 API (provided by the server at runtime)
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")

    // Advanced Slime Paper API (provided by the ASP plugin installed on the server)
    compileOnly("com.infernalsuite.asp:api:4.0.0-SNAPSHOT")

    // Reference file loader - NOT bundled with ASP, must be shaded into this plugin
    implementation("com.infernalsuite.asp:file-loader:4.0.0-SNAPSHOT")

    // Optional integrations
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

tasks {
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = 21
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    shadowJar {
        archiveBaseName = "HoneyMissileWars"
        archiveClassifier = ""
        archiveVersion = project.version.toString()
        // Only the file-loader (and its deps) need shading; everything else is provided.
        relocate("com.infernalsuite.asp.loaders", "top.sparkpixel.hmw.libs.asp.loaders")
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }
}
