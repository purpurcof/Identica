import me.whereareiam.attache.plugin.gradle.extension.AttacheExtension

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

extensions.configure<AttacheExtension>("attache") {
    transitive.set(true)
}
