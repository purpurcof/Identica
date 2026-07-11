plugins {
    id("capability-artifact")
}

dependencies {
    api(projects.capabilityAuthoritativeUsernameApi)
    implementation(projects.capabilityAuthoritativeUsernameCommon)
    implementation(projects.capabilityAuthoritativeUsernameDatabase)
}

toolkitPublish {
    artifactId.set("authoritative-username")
}
