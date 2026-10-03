plugins {
    java
}

group = "org.yeggs"
version = "1.0.0"
description = "Honey Missile Wars lobby companion - NPC-bound join commands via BungeeCord"

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
}

dependencies {
    // Paper API is provided by the lobby server at runtime
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
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
}
