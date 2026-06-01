import org.gradle.jvm.tasks.Jar

plugins {
    id("runtime")
}

dependencies {
    // capabilities
    compileOnly(projects.capabilityOnline)
    compileOnly(projects.capabilityAuthoritativeUsername)
    compileOnly(projects.capabilityMigration)
    compileOnly(projects.capabilityVerification)

    // general
    implementation(projects.providerPremiumApi)
    implementation(projects.providerPremiumPlatformBungeecord)
    implementation(projects.providerPremiumPlatformVelocity)

    // capabilities
    testImplementation(projects.capabilityOnline)
    testImplementation(projects.capabilityAuthoritativeUsername)
    testImplementation(projects.capabilityMigration)
    testImplementation(projects.capabilityVerification)

    // general
    testImplementation(projects.providerPremiumApi)
    testImplementation(projects.providerPremiumPlatformBungeecord)
    testImplementation(projects.providerPremiumPlatformVelocity)
}

tasks.named<Jar>("shadowJar").configure {
    archiveBaseName.set("Premium")
    archiveClassifier.set("")
}
