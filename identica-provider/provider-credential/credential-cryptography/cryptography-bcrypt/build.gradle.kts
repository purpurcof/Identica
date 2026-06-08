plugins {
    id("shared")
}

dependencies {
    compileOnly(projects.providerCredentialApi)
    testImplementation(projects.providerCredentialApi)

    compileOnly(libs.bcrypt)
    testImplementation(libs.bcrypt)
}
