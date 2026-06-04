plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityRecognitionApi)
    api(projects.capabilityRestrictionApi)
    api(projects.capabilityRestrictionJoinApi)
}
