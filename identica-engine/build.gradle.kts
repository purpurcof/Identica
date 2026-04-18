plugins {
    id("identica.java-common")
}

dependencies {
    testImplementation(projects.identicaCommon)
    testImplementation(libs.keystone)
}
