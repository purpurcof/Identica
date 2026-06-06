plugins {
    id("capability-artifact")
}

dependencies {
    api(projects.capabilityVerificationApi)
    implementation(projects.capabilityVerificationCommon)
}

toolkitPublish {
    artifactId.set("verification")
}
