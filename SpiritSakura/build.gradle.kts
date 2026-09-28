plugins {
    java
}

group = "io.spiritsakura"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Compiled against 1.21.4 (first version with item_model + equippable models); runs on newer Paper too.
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") { expand("version" to project.version) }
}

tasks.jar {
    // The bundled zip is already compressed
    from(sourceSets.main.get().output)
    archiveFileName.set("SpiritSakura-${project.version}.jar")
}
