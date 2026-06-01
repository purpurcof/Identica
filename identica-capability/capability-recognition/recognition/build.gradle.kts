plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityRecognitionApi)
    implementation(projects.capabilityRecognitionCommon)
}
