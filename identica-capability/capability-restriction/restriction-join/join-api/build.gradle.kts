plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityRestrictionApi)
}

toolkitPublish {
    artifactId.set("restriction-join-api")
}
