dependencies {
    "api"(project(":identica-api"))

    "compileOnly"(rootProject.libs.bundles.database)

    "testImplementation"(rootProject.libs.bundles.database)
    "testImplementation"(rootProject.libs.dialectica)
    "testImplementation"(rootProject.libs.testcontainers.junit)
    "testImplementation"(rootProject.libs.testcontainers.postgresql)
    "testImplementation"(rootProject.libs.testcontainers.mariadb)
}
