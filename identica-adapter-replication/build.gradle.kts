plugins {
    id("identica.java-common")
}

dependencies {
    compileOnly(libs.jedis)

    testImplementation(libs.jedis)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.redis)
}
