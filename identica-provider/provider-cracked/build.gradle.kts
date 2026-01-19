import org.gradle.jvm.tasks.Jar

dependencies {
    "api"(project(":identica-api"))

    "compileOnly"(rootProject.libs.argon2)
    "compileOnly"(rootProject.libs.bcrypt)
    "compileOnly"(rootProject.libs.jdbi.core)
}

tasks.named<Jar>("jar") {
    archiveFileName.set("Cracked.jar")
}
