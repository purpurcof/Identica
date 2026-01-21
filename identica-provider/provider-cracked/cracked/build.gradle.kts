import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    "implementation"(project(":identica-provider:provider-cracked:cracked-api"))

    "compileOnly"(rootProject.libs.argon2)
    "compileOnly"(rootProject.libs.bcrypt)
    "compileOnly"(rootProject.libs.jdbi.core)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("Cracked")
    archiveClassifier.set("")
}
