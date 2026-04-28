import me.whereareiam.attache.plugin.gradle.extension.AttacheExtension
import org.gradle.jvm.tasks.Jar

plugins {
    id("identica.platform-runtime")
    alias(libs.plugins.attache)
}

tasks.named<Jar>("shadowJar").configure {
    archiveClassifier.set("VELOCITY")
}

dependencies {
    implementation(projects.platformVelocityApi)
    implementation(libs.bundles.bStats.velocity)
    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)

    implementation(libs.attache.velocity)
    attache(libs.cloud.velocity)
}

extensions.configure<AttacheExtension>("attache") {
    transitive.set(true)
}
