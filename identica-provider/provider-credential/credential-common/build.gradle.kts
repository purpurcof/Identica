plugins {
    id("shared")
}

dependencies {
    // capabilities
    compileOnly(projects.capabilityRecognition)

    // features
    compileOnly(projects.featureSentinelApi)
    compileOnly(projects.featureVerificationApi)

    // general
    compileOnly(projects.providerCredentialApi)

    // capabilities
    testImplementation(projects.capabilityRecognition)

    // features
    testImplementation(projects.featureSentinelApi)
    testImplementation(projects.featureVerificationApi)

    // general
    testImplementation(projects.providerCredentialApi)
}
