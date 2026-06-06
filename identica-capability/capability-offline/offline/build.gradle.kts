plugins {
    id("capability-artifact")
}

dependencies {
    api(projects.capabilityOfflineApi)
    implementation(projects.capabilityOfflineCommon)
}

toolkitPublish {
    artifactId.set("offline")
}
