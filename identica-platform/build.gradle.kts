import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.shadow)
}

val platformRuntimeProjectPaths = rootProject.subprojects
    .map { it.path }
    .filter { path ->
        path.startsWith(":identica-")
                && !path.startsWith(":identica-platform")
                && !path.startsWith(":identica-provider")
    }
    .sorted()

subprojects {
    val isApiModule = path.endsWith(":api")

    if (!isApiModule) {
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
            platformRuntimeProjectPaths.forEach { modulePath ->
                "implementation"(project(modulePath))
            }
        }
    }
}
