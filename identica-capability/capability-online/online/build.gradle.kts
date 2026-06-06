plugins {
    id("capability-artifact")
}

dependencies {
    api(projects.capabilityOnlineApi)
    implementation(projects.capabilityOnlineCommon)
}

toolkitPublish {
    artifactId.set("online")
}
