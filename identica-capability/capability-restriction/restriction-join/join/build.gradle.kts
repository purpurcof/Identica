plugins {
    id("capability-artifact")
}

dependencies {
    api(projects.capabilityRestrictionJoinApi)
    implementation(projects.capabilityRestrictionJoinCommon)
}

toolkitPublish {
    artifactId.set("restriction-join")
}
