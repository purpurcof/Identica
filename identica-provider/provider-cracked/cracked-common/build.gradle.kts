plugins {
    id("identica.java-common")
}

dependencies {
    compileOnly(projects.providerCrackedApi)
    testImplementation(projects.providerCrackedApi)
}
