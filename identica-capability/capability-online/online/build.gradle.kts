plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityOnlineApi)
    implementation(projects.capabilityOnlineCommon)
}

toolkitPublish {
    artifactId.set("online")
}
