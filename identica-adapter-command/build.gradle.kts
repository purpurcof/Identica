import me.whereareiam.attache.plugin.gradle.extension.AttacheMetadataExtension

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

extensions.configure<AttacheMetadataExtension>("attacheMetadata") {
    repository("https://maven.whereareiam.me/release")
    repository("https://maven.whereareiam.me/development")

    library(libs.cloud.core) {
        transitive.set(true)
    }

    library(libs.cloud.annotations) {
        transitive.set(true)
    }

    library(libs.cloud.cooldowns) {
        transitive.set(true)
    }

    library(libs.cloud.minecraft.extras) {
        transitive.set(true)
    }
}
