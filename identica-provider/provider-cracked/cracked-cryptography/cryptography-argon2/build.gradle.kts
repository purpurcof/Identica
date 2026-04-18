plugins {
    id("identica.java-common")
}

dependencies {
    compileOnly(projects.providerCrackedApi)
    testImplementation(projects.providerCrackedApi)

    compileOnly(libs.argon2)
    testImplementation(libs.argon2)
}
