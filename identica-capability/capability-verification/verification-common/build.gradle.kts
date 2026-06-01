plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityVerificationApi)
}

toolkitPublish {
    artifactId.set("verification-common")
}
