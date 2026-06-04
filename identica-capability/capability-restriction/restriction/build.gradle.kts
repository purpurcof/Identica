plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityRestrictionApi)
    implementation(projects.capabilityRestrictionCommon)
}
