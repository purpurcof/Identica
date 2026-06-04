plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityRestrictionJoinApi)
    implementation(projects.capabilityRestrictionJoinCommon)
}
