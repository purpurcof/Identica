plugins {
    id("shared")
}

dependencies {
    // capabilities
    compileOnly(projects.capabilityMigration)

    // general
    compileOnly(projects.providerCredentialApi)

    // capabilities
    testImplementation(projects.capabilityMigration)

    // general
    testImplementation(projects.providerCredentialApi)
}
