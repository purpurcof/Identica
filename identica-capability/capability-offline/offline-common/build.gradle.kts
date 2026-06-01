plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityOfflineApi)
}

toolkitPublish {
    artifactId.set("offline-common")
}
