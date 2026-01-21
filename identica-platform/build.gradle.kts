import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.shadow)
}

subprojects {
    plugins.apply(rootProject.libs.plugins.shadow.get().pluginId)

    tasks.withType<ShadowJar> {
        archiveBaseName.set(rootProject.name)

        relocate("org.jdbi", "me.whereareiam.identica.library.jdbi")
        relocate("me.whereareiam.dialectica", "me.whereareiam.identica.library.dialectica")
    }

    tasks.named<Jar>("jar") {
        dependsOn("shadowJar")
    }

    dependencies {
        "implementation"(project(":identica-common"))
        "implementation"(project(":identica-adapter-command"))
        "implementation"(project(":identica-adapter-database"))
        "implementation"(project(":identica-adapter-synchronization"))
    }
}
