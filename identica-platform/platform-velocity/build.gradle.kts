import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import me.whereareiam.spawner.SpawnerConfig
import org.gradle.jvm.tasks.Jar

plugins {
    id("me.whereareiam.spawner")
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("VELOCITY")
}

dependencies {
    "compileOnly"(rootProject.libs.velocity)
    "annotationProcessor"(rootProject.libs.velocity)
    "compileOnly"(rootProject.libs.cloud.velocity)

    "implementation"(rootProject.libs.attache.velocity)
}

extensions.configure<SpawnerConfig>("spawner") {
    serverType.set("paper")
    proxyType.set("velocity")
    velocity.forwardingMode.set("legacy")
    velocity.pluginJar.set(tasks.named<Jar>("shadowJar").flatMap { it.archiveFile })

    val crackedJar = project(":identica-provider:provider-cracked").tasks.named<Jar>("jar")
    val premiumJar = project(":identica-provider:provider-premium").tasks.named<Jar>("jar")
    velocity.extraFiles.from(crackedJar.flatMap { it.archiveFile })
    velocity.extraFiles.from(premiumJar.flatMap { it.archiveFile })
    velocity.extraFiles.builtBy(crackedJar, premiumJar)
    velocity.extraFilesDir.set(
        serverDir.dir("velocity").map { it.dir("plugins/identica/providers") }
    )
}
