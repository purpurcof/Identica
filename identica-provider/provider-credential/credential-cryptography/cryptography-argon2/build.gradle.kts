plugins {
    id("common")
}

dependencies {
    compileOnly(projects.providerCredentialApi)
    testImplementation(projects.providerCredentialApi)

    compileOnly(libs.argon2)
    testImplementation(libs.argon2)
}
