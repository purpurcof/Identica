import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

plugins {
    id("identica.java-common")
    id("com.gradleup.shadow")
}

tasks.withType<ShadowJar>().configureEach {
    archiveBaseName.set(rootProject.name)

    relocate("org.bstats", "me.whereareiam.identica.library.bstats")
    relocate("org.jdbi", "me.whereareiam.identica.library.jdbi")
    relocate("me.whereareiam.dialectica", "me.whereareiam.identica.library.dialectica")

    val defaultDestination = rootProject.layout.buildDirectory.dir("libs")

    if (providers.gradleProperty("output").isPresent) {
        destinationDirectory.set(file(providers.gradleProperty("output").get()))
    } else {
        destinationDirectory.set(defaultDestination)
    }
}

tasks.named<Jar>("jar").configure {
    dependsOn(tasks.named("shadowJar"))
}
