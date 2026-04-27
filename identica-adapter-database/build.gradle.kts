import me.whereareiam.attache.plugin.gradle.extension.AttacheMetadataExtension

plugins {
    id("identica.java-common")
    alias(libs.plugins.attache)
}

dependencies {
    attache(libs.dialectica)
    attache(libs.jdbi.core)
    attache(libs.jdbi.sqlobject)
    attache(libs.hikaricp)
    attache(libs.postgresql)
    attache(libs.mariadb)
    attache(libs.sqlite)
    attache(libs.h2)

    testImplementation(libs.bundles.database)
    testImplementation(libs.dialectica)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.mariadb)
}

extensions.configure<AttacheMetadataExtension>("attacheMetadata") {
    repository("https://maven.whereareiam.me/release")
    repository("https://maven.whereareiam.me/development")

    library(libs.dialectica) {
        transitive.set(true)
        relocate("me{}whereareiam{}dialectica", "me.whereareiam.identica.library.dialectica")
        relocate("org{}jdbi", "me{}whereareiam{}identica{}library{}jdbi")
    }

    library(libs.jdbi.core) {
        transitive.set(true)
        relocate("org{}jdbi", "me.whereareiam.identica.library.jdbi")
        relocate("io{}leangen{}geantyref", "me.whereareiam.identica.library.geantyref")
    }

    library(libs.jdbi.sqlobject) {
        transitive.set(true)
        relocate("org{}jdbi", "me.whereareiam.identica.library.jdbi")
        relocate("io{}leangen{}geantyref", "me.whereareiam.identica.library.geantyref")
    }
}
