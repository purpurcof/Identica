plugins {
    id("shared")
}

dependencies {
    testImplementation(projects.identicaCommon)
    testImplementation(libs.keystone)
}
