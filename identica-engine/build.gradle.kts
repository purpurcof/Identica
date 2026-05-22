plugins {
    id("common")
}

dependencies {
    testImplementation(projects.identicaCommon)
    testImplementation(libs.keystone)
}
