import me.whereareiam.attache.plugin.gradle.extension.AttacheMetadataExtension

plugins {
    id("identica.java-common")
    alias(libs.plugins.attache)
}

dependencies {
    attache(libs.jedis)

    testImplementation(libs.jedis)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.redis)
}

extensions.configure<AttacheMetadataExtension>("attacheMetadata") {
    library(libs.jedis) {
        transitive.set(true)
    }
}
