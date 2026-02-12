java {
    withSourcesJar()
    withJavadocJar()
}

group = "me.whereareiam.identica.platform.velocity"

dependencies {
    api(project(":identica-api"))
    "compileOnly"(rootProject.libs.velocity)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "api"
            pom {
                name.set("Identica Velocity API")
                description.set("Velocity-specific API for Identica")
            }
        }
    }
}

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:none", "-quiet")
        title = "Identica Velocity API"
        windowTitle = "Identica Velocity API"
    }
}
