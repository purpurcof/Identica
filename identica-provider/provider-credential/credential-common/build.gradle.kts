plugins {
    id("shared")
}

dependencies {
    // capabilities
    compileOnly(projects.capabilityRecognition)

    // general
    compileOnly(projects.providerCredentialApi)

    // capabilities
    testImplementation(projects.capabilityRecognition)

    // general
    testImplementation(projects.providerCredentialApi)
}
