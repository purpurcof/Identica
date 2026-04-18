plugins {
    id("identica.java-common")
}

dependencies {
    implementation(libs.attache.standalone)
    testImplementation(libs.keystone)
    testImplementation(libs.commandant)
}
