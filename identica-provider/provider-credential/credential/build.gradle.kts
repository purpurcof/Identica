import org.gradle.jvm.tasks.Jar

plugins {
    id("shadow-runtime")
}

dependencies {
    implementation(projects.providerCredentialApi)
    implementation(projects.providerCredentialCommon)
    implementation(projects.providerCredentialDatabase)
    implementation(projects.providerCredentialCryptographyCommon)
    implementation(projects.providerCredentialCryptographyBcrypt)
    implementation(projects.providerCredentialCryptographyArgon2)

    testImplementation(projects.providerCredentialApi)
}

tasks.named<Jar>("shadowJar").configure {
    archiveBaseName.set("Credential")
    archiveClassifier.set("")
}
