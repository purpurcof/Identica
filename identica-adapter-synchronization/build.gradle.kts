dependencies {
    "api"(project(":identica-api"))
    "compileOnly"(rootProject.libs.jedis)

    "testImplementation"(rootProject.libs.jedis)
    "testImplementation"(rootProject.libs.testcontainers.junit)
    "testImplementation"(rootProject.libs.testcontainers.redis)
}
