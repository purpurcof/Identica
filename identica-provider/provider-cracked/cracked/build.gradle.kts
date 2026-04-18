import org.gradle.jvm.tasks.Jar

plugins {
    id("identica.shadow-runtime")
}

dependencies {
    implementation(projects.providerCrackedApi)
    implementation(projects.providerCrackedCommon)
    implementation(projects.providerCrackedDatabase)
    implementation(projects.providerCrackedCryptographyCommon)
    implementation(projects.providerCrackedCryptographyBcrypt)
    implementation(projects.providerCrackedCryptographyArgon2)

    testImplementation(projects.providerCrackedApi)
}

tasks.named<Jar>("shadowJar").configure {
    archiveBaseName.set("Cracked")
    archiveClassifier.set("")
}
