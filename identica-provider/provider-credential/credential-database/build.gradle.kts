plugins {
    id("shared")
}

dependencies {
    compileOnly(projects.providerCredentialApi)
    testImplementation(projects.providerCredentialApi)
    testImplementation(projects.identicaAdapterDatabase)

    compileOnly(libs.jdbi.core)
    compileOnly(libs.jdbi.sqlobject)
    compileOnly(libs.dialectica)

    testImplementation(libs.hikaricp)
    testImplementation(libs.jdbi.core)
    testImplementation(libs.jdbi.sqlobject)
    testImplementation(libs.dialectica)
    testImplementation(libs.h2)
    testImplementation(libs.sqlite)
}
