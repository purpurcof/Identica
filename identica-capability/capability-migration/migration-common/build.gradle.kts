plugins {
    id("capability")
}

dependencies {
    api(projects.capabilityMigrationApi)
}

toolkitPublish {
    artifactId.set("migration-common")
}
