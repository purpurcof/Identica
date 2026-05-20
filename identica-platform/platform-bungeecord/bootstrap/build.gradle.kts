import me.whereareiam.attache.plugin.gradle.extension.AttacheExtension

plugins {
    id("identica.platform")
    alias(libs.plugins.attache)
}

platform {
    name.set("BUNGEECORD")
    descriptors.add("bungee.yml")
}

dependencies {
    testImplementation(libs.bungeecord)
    testImplementation(libs.cloud.bungee)

    implementation(projects.platformBungeecordApi)
    implementation(libs.bundles.bStats.bungeecord)
    implementation(libs.attache.bungeecord)

    compileOnly(libs.bungeecord)

    attache(libs.adventure.platform.bungeecord)
    attache(libs.cloud.bungee)
}

extensions.configure<AttacheExtension>("attache") {
    transitive.set(true)
}

tasks.processResources {
    filesMatching("bungee.yml") {
        expand("version" to project.version)
    }
}
