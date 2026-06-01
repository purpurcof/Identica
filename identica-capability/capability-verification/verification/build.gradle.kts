plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityVerificationApi)
    implementation(projects.capabilityVerificationCommon)
}

toolkitPublish {
    artifactId.set("verification")
}
