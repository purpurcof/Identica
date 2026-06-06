plugins {
    id("capability-artifact")
}

dependencies {
    api(projects.capabilityRecognitionApi)
    implementation(projects.capabilityRecognitionCommon)
}

toolkitPublish {
    artifactId.set("recognition")
}
