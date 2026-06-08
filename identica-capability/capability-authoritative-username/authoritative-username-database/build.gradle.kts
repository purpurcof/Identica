plugins {
    id("capability")
}

dependencies {
    compileOnly(projects.capabilityAuthoritativeUsernameApi)
    testImplementation(projects.capabilityAuthoritativeUsernameApi)
    testImplementation(projects.capabilityAuthoritativeUsernameCommon)
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

toolkitPublish {
    artifactId.set("authoritative-username-database")
}
