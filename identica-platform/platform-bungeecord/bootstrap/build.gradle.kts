import me.whereareiam.attache.plugin.gradle.extension.AttacheExtension
import org.gradle.jvm.tasks.Jar

plugins {
    id("identica.platform-runtime")
    alias(libs.plugins.attache)
}

tasks.named<Jar>("shadowJar").configure {
    archiveClassifier.set("BUNGEECORD")
}

dependencies {
    implementation(projects.platformBungeecordApi)
    implementation(libs.bundles.bStats.bungeecord)
    compileOnly(libs.bungeecord)
    testImplementation(libs.bungeecord)
    testImplementation(libs.cloud.bungee)

    implementation(libs.attache.bungeecord)
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
