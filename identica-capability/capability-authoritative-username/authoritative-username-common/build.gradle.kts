plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityAuthoritativeUsernameApi)
}

toolkitPublish {
    artifactId.set("authoritative-username-common")
}
