plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityOnlineApi)
}

toolkitPublish {
    artifactId.set("online-common")
}
