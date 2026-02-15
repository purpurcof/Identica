import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    "implementation"(project(":identica-provider:provider-cracked:cracked-api"))
    "implementation"(project(":identica-provider:provider-cracked:cracked-common"))
    "implementation"(project(":identica-provider:provider-cracked:cracked-database"))
    "implementation"(project(":identica-provider:provider-cracked:cracked-cryptography:common"))
    "implementation"(project(":identica-provider:provider-cracked:cracked-cryptography:cryptography-bcrypt"))
    "implementation"(project(":identica-provider:provider-cracked:cracked-cryptography:cryptography-argon2id"))

    "testImplementation"(project(":identica-provider:provider-cracked:cracked-api"))
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("Cracked")
    archiveClassifier.set("")

    relocate("org.jdbi", "me.whereareiam.identica.library.jdbi")
    relocate("me.whereareiam.dialectica", "me.whereareiam.identica.library.dialectica")
}
