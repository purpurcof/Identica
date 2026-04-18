plugins {
    id("identica.java-common")
}

dependencies {
    compileOnly(libs.bundles.database)

    testImplementation(libs.bundles.database)
    testImplementation(libs.dialectica)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.mariadb)
}
