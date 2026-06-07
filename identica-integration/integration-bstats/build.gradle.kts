plugins {
    id("shared")
}

dependencies {
    compileOnly(projects.featureVerificationApi)
    compileOnly(libs.bStats)
}
