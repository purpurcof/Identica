plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityRestrictionJoinApi)
    compileOnly(projects.identicaEngine)
    testImplementation(projects.identicaEngine)
}
