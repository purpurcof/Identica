import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    "implementation"(project(":identica-provider:provider-premium:premium-api"))
    "implementation"(project(":identica-provider:provider-premium:premium-adapter-database"))
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("Premium")
    archiveClassifier.set("")
    relocate("org.jdbi", "me.whereareiam.identica.library.jdbi")
    relocate("me.whereareiam.dialectica", "me.whereareiam.identica.library.dialectica")
}
