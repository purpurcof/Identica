plugins {
    id("shared")
}

dependencies {
    // capabilities
    compileOnly(projects.capabilityRecognition)
    compileOnly(projects.featureVerificationApi)

    // general
    compileOnly(projects.providerCredentialApi)

    // capabilities
    testImplementation(projects.capabilityRecognition)
    testImplementation(projects.featureVerificationApi)

    // general
    testImplementation(projects.providerCredentialApi)
}
