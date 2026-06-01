plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityAuthoritativeUsernameApi)
    implementation(projects.capabilityAuthoritativeUsernameCommon)
}

toolkitPublish {
    artifactId.set("authoritative-username")
}
