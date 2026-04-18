import me.whereareiam.spawner.SpawnerConfig
import org.gradle.jvm.tasks.Jar

plugins {
    id("identica.platform-runtime")
    alias(libs.plugins.spawner)
}

tasks.named<Jar>("shadowJar").configure {
    archiveClassifier.set("VELOCITY")
}

dependencies {
    implementation(projects.platformVelocityApi)
    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)
    compileOnly(libs.cloud.velocity)

    implementation(libs.attache.velocity)
}

extensions.configure<SpawnerConfig>("spawner") {
    serverType.set("paper")
    proxyType.set("velocity")
    velocity.forwardingMode.set("legacy")
    velocity.pluginJar.set(tasks.named<Jar>("shadowJar").flatMap { it.archiveFile })

    evaluationDependsOn(":provider-cracked-runtime")
    evaluationDependsOn(":provider-premium-runtime")

    val crackedJar = project(":provider-cracked-runtime").tasks.named("shadowJar", Jar::class.java)
    val premiumJar = project(":provider-premium-runtime").tasks.named("shadowJar", Jar::class.java)

    velocity.extraFiles.from(crackedJar.flatMap { it.archiveFile })
    velocity.extraFiles.from(premiumJar.flatMap { it.archiveFile })
    velocity.extraFiles.builtBy(crackedJar, premiumJar)
    velocity.extraFilesDir.set(
        serverDir.dir("velocity").map { it.dir("plugins/identica/providers") }
    )
}
