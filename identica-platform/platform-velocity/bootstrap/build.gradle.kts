import me.whereareiam.attache.plugin.gradle.extension.AttacheExtension

plugins {
    id("identica.platform")
    alias(libs.plugins.attache)
}

platform {
    name.set("VELOCITY")
    descriptors.add("velocity-plugin.json")
}

dependencies {
    testImplementation(libs.velocity)
    testImplementation(libs.cloud.velocity)

    implementation(projects.platformVelocityApi)
    implementation(libs.bundles.bStats.velocity)
    implementation(libs.attache.velocity)

    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)

    attache(libs.cloud.velocity)
}

extensions.configure<AttacheExtension>("attache") {
    transitive.set(true)
}
