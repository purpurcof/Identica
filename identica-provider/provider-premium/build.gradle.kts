import org.gradle.jvm.tasks.Jar

dependencies {
    "api"(project(":identica-api"))
}

tasks.named<Jar>("jar") {
    archiveFileName.set("Premium.jar")
}
