plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityRestrictionJoinApi)
    compileOnly(projects.identicaEngine)
    testImplementation(projects.identicaEngine)
}

toolkitPublish {
    artifactId.set("restriction-join-common")
}
