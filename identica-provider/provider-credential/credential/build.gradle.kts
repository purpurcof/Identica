import org.gradle.jvm.tasks.Jar

plugins {
    id("runtime")
}

dependencies {
    // capabilities
    compileOnly(projects.capabilityRecognition)
    compileOnly(projects.capabilityOffline)
    compileOnly(projects.capabilityMigration)
    compileOnly(projects.capabilityVerification)

    // general
    implementation(projects.providerCredentialApi)
    implementation(projects.providerCredentialCommon)
    implementation(projects.providerCredentialDatabase)
    implementation(projects.providerCredentialCryptographyCommon)
    implementation(projects.providerCredentialCryptographyBcrypt)
    implementation(projects.providerCredentialCryptographyArgon2)

    // capabilities
    testImplementation(projects.capabilityRecognition)
    testImplementation(projects.capabilityOffline)
    testImplementation(projects.capabilityMigration)
    testImplementation(projects.capabilityVerification)

    // general
    testImplementation(projects.providerCredentialApi)
}

tasks.named<Jar>("shadowJar").configure {
    archiveBaseName.set("Credential")
    archiveClassifier.set("")
}
