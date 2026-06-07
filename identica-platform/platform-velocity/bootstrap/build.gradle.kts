plugins {
    id("platform")
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
    implementation(projects.featureVerification)
    implementation(libs.bundles.bStats.velocity)
    implementation(libs.attache.velocity)

    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)

    attache(libs.cloud.velocity)
}
