plugins {
    id("identica.java-common")
}

dependencies {
    compileOnly(projects.providerCrackedApi)
    testImplementation(projects.providerCrackedApi)

    compileOnly(libs.jdbi.core)
    compileOnly(libs.jdbi.sqlobject)
    compileOnly(libs.dialectica)

    testImplementation(libs.jdbi.core)
    testImplementation(libs.jdbi.sqlobject)
    testImplementation(libs.dialectica)
}
