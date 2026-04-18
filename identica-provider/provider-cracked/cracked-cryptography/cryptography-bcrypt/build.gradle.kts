plugins {
    id("identica.java-common")
}

dependencies {
    compileOnly(projects.providerCrackedApi)
    testImplementation(projects.providerCrackedApi)

    compileOnly(libs.bcrypt)
    testImplementation(libs.bcrypt)
}
