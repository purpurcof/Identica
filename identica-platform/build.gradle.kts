import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.shadow)
}

subprojects {
    plugins.apply(rootProject.libs.plugins.shadow.get().pluginId)

    tasks.withType<ShadowJar> {
        archiveBaseName.set(rootProject.name)
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
