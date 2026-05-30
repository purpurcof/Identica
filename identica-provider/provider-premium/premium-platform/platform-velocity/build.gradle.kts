plugins {
    id("shared")
}

dependencies {
    compileOnly(projects.providerPremiumApi)
    testImplementation(projects.providerPremiumApi)

    compileOnly(projects.platformVelocityApi)
    compileOnly(libs.velocity)
}
