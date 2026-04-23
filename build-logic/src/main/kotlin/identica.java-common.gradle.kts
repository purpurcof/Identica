import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

plugins {
    `java-library`
    `maven-publish`
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val buildVersion = providers.environmentVariable("VERSION").orElse("dev")

group = "me.whereareiam"
version = buildVersion.get()

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_21.toString()
    targetCompatibility = JavaVersion.VERSION_21.toString()
}

dependencies {
    add("compileOnly", libs.findLibrary("lombok").get())
    add("annotationProcessor", libs.findLibrary("lombok").get())
    add("testImplementation", libs.findLibrary("lombok").get())
    add("testAnnotationProcessor", libs.findLibrary("lombok").get())

    add("compileOnly", libs.findLibrary("guice").get())
    add("compileOnly", libs.findLibrary("annotations").get())
    add("compileOnly", libs.findLibrary("configura").get())
    add("compileOnly", libs.findLibrary("commandant").get())
    add("compileOnly", libs.findLibrary("keystone").get())
    add("compileOnly", libs.findLibrary("dialectica").get())
    add("compileOnly", libs.findBundle("adventure").get())
    add("implementation", libs.findLibrary("attache-common").get())

    add("testRuntimeOnly", libs.findLibrary("junit-platform").get())
    add("testImplementation", libs.findLibrary("junit-jupiter").get())
    add("testImplementation", libs.findLibrary("guice").get())
    add("testImplementation", libs.findLibrary("annotations").get())
    add("testImplementation", libs.findLibrary("configura").get())
    add("testImplementation", libs.findLibrary("keystone").get())
    add("testImplementation", libs.findBundle("adventure").get())
    add("testImplementation", libs.findLibrary("mockito-core").get())
    add("testImplementation", libs.findLibrary("mockito-junit").get())

    if (path != ":identica-api") {
        add("compileOnly", project(":identica-api"))
        add("testImplementation", project(":identica-api"))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    ignoreFailures = providers.gradleProperty("ignoreTestFailures")
        .map(String::toBoolean)
        .orElse(false)
        .get()
}

extensions.configure<PublishingExtension> {
    repositories {
        maven {
            val realm = providers.environmentVariable("PUBLISH_REALM")
                .orElse(
                    buildVersion.map { versionString ->
                        if (versionString.contains("dev", ignoreCase = true)) "development" else "release"
                    }
                )
                .get()
                .lowercase()

            url = uri("https://maven.whereareiam.me/$realm")

            credentials {
                username = providers.environmentVariable("PUBLISH_USER").orNull.orEmpty()
                password = providers.environmentVariable("PUBLISH_TOKEN").orNull.orEmpty()
            }
        }
    }
}
