plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityOfflineApi)
    implementation(projects.capabilityOfflineCommon)
}

toolkitPublish {
    artifactId.set("offline")
}
