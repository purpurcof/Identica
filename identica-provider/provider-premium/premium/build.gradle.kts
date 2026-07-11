import org.gradle.jvm.tasks.Jar

plugins {
    id("runtime")
}

dependencies {
    // capabilities
    compileOnly(projects.capabilityRestriction)
    compileOnly(projects.capabilityRestrictionJoin)
    compileOnly(projects.capabilityRecognition)
    compileOnly(projects.capabilityAuthoritativeUsername)
    compileOnly(projects.featureVerificationApi)

    // general
    implementation(projects.providerPremiumApi)
    implementation(projects.providerPremiumPlatformBungeecord)
    implementation(projects.providerPremiumPlatformVelocity)

    // capabilities
    testImplementation(projects.capabilityRestriction)
    testImplementation(projects.capabilityRestrictionJoin)
    testImplementation(projects.capabilityRecognition)
    testImplementation(projects.capabilityAuthoritativeUsername)
    testImplementation(projects.featureVerificationApi)

    // general
    testImplementation(projects.providerPremiumApi)
    testImplementation(projects.providerPremiumPlatformBungeecord)
    testImplementation(projects.providerPremiumPlatformVelocity)
}

tasks.named<Jar>("shadowJar").configure {
    archiveBaseName.set("Premium")
    archiveClassifier.set("")
}
