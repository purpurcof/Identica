import me.whereareiam.attache.plugin.gradle.extension.AttacheExtension

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

extensions.configure<AttacheExtension>("attache") {
    transitive.set(true)
    repository("https://maven.whereareiam.me/release")
    repository("https://maven.whereareiam.me/development")

    library(libs.guice) {
        relocate("com{}google{}inject", "me.whereareiam.identica.library.guice")
        relocate("com{}google{}common", "me.whereareiam.identica.library.guava")
    }

    library(libs.configura) {
        relocate("com{}fasterxml{}jackson", "me.whereareiam.identica.library.jackson")
        relocate("org{}yaml{}snakeyaml", "me.whereareiam.identica.library.snakeyaml")
    }
}
