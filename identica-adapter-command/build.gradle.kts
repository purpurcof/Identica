import me.whereareiam.attache.plugin.gradle.extension.AttacheExtension

plugins {
    id("identica.java-common")
    alias(libs.plugins.attache)
}

dependencies {
    attache(libs.cloud.core)
    attache(libs.cloud.annotations)
    attache(libs.cloud.cooldowns)
    attache(libs.cloud.minecraft.extras)
}

extensions.configure<AttacheExtension>("attache") {
    transitive.set(true)
    repository("https://maven.whereareiam.me/release")
    repository("https://maven.whereareiam.me/development")
}
