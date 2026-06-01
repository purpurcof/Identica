plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityMigrationApi)
    implementation(projects.capabilityMigrationCommon)
}

toolkitPublish {
    artifactId.set("migration")
}
