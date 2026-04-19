import org.gradle.jvm.tasks.Jar

plugins {
    id("identica.platform-runtime")
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
