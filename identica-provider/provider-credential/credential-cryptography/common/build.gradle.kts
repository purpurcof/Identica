plugins {
    id("common")
}

dependencies {
    compileOnly(projects.providerCredentialApi)
    testImplementation(projects.providerCredentialApi)
}
