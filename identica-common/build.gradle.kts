import me.whereareiam.attache.plugin.gradle.extension.AttacheMetadataExtension

plugins {
    id("identica.java-common")
    alias(libs.plugins.attache)
}

dependencies {
    implementation(libs.attache.standalone)
    attache(libs.guice)
    attache(libs.configura)
    attache(libs.commandant)
    attache(libs.keystone)

    testImplementation(libs.keystone)
    testImplementation(libs.commandant)
}

extensions.configure<AttacheMetadataExtension>("attacheMetadata") {
    repository("https://maven.whereareiam.me/release")
    repository("https://maven.whereareiam.me/development")

    library(libs.guice) {
        transitive.set(true)
        relocate("com{}google{}inject", "me.whereareiam.identica.library.guice")
        relocate("com{}google{}common", "me.whereareiam.identica.library.guava")
    }

    library(libs.configura) {
        transitive.set(true)
        relocate("com{}fasterxml{}jackson", "me.whereareiam.identica.library.jackson")
        relocate("org{}yaml{}snakeyaml", "me.whereareiam.identica.library.snakeyaml")
    }

    library(libs.commandant) {
        transitive.set(true)
    }

    library(libs.keystone) {
        transitive.set(true)
    }
}
