plugins {
    id("capability-artifact")
}

dependencies {
    api(projects.capabilityRestrictionApi)
    implementation(projects.capabilityRestrictionCommon)
}

toolkitPublish {
    artifactId.set("restriction")
}
