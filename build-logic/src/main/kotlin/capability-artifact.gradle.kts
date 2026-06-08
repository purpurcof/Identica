import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

plugins {
    id("capability")
    id("relocations")
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}

tasks.named<Jar>("jar").configure {
    // Keep the normal Java/Maven artifact path, but let Shadow produce its relocated contents.
    actions.clear()
    dependsOn(shadowJar)
}

tasks.named("assemble").configure {
    dependsOn(shadowJar)
}
