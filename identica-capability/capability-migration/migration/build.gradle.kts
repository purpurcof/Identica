plugins {
    id("capability-artifact")
}

dependencies {
    api(projects.capabilityMigrationApi)
}

toolkitPublish {
    artifactId.set("migration")
}
