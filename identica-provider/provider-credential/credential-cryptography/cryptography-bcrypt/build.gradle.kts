plugins {
    id("common")
}

dependencies {
    compileOnly(projects.providerCredentialApi)
    testImplementation(projects.providerCredentialApi)

    compileOnly(libs.bcrypt)
    testImplementation(libs.bcrypt)
}
