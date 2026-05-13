plugins {
    id("identica.java-common")
}

dependencies {
    compileOnly(projects.providerCredentialApi)
    testImplementation(projects.providerCredentialApi)
}
