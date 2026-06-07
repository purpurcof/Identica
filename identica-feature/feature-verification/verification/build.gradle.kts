plugins {
    id("shared")
}

dependencies {
    api(projects.featureVerificationApi)
    implementation(projects.featureVerificationCommon)
    implementation(projects.featureVerificationDatabase)
}
