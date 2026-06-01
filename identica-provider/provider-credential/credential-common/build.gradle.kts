plugins {
    id("shared")
}

dependencies {
    // capabilities
    compileOnly(projects.capabilityRecognition)
    compileOnly(projects.capabilityMigration)

    // general
    compileOnly(projects.providerCredentialApi)

    // capabilities
    testImplementation(projects.capabilityRecognition)
    testImplementation(projects.capabilityMigration)

    // general
    testImplementation(projects.providerCredentialApi)
}
