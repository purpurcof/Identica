import org.gradle.jvm.tasks.Jar

plugins {
    id("runtime")
}

dependencies {
    // capabilities
    compileOnly(projects.capabilityRestriction)
    compileOnly(projects.capabilityRestrictionJoin)
    compileOnly(projects.capabilityRecognition)

    // features
    compileOnly(projects.featureSentinelApi)
    compileOnly(projects.featureVerificationApi)

    // general
    implementation(projects.providerCredentialApi)
    implementation(projects.providerCredentialCommon)
    implementation(projects.providerCredentialDatabase)
    implementation(projects.providerCredentialCryptographyCommon)
    implementation(projects.providerCredentialCryptographyBcrypt)
    implementation(projects.providerCredentialCryptographyArgon2)

    // capabilities
    testImplementation(projects.capabilityRestriction)
    testImplementation(projects.capabilityRestrictionJoin)
    testImplementation(projects.capabilityRecognition)

    // features
    testImplementation(projects.featureSentinelApi)
    testImplementation(projects.featureVerificationApi)

    // general
    testImplementation(projects.providerCredentialApi)
}

tasks.named<Jar>("shadowJar").configure {
    archiveBaseName.set("Credential")
    archiveClassifier.set("")
}
