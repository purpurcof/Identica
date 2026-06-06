plugins {
    id("capability-artifact")
}

dependencies {
    api(projects.capabilityMigrationApi)
    implementation(projects.capabilityMigrationCommon)
}

toolkitPublish {
    artifactId.set("migration")
}
