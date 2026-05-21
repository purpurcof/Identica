import me.whereareiam.attache.plugin.gradle.extension.AttacheExtension

plugins {
    alias(libs.plugins.attache)
    alias(libs.plugins.toolkit.versioning)
    alias(libs.plugins.spawner)
    id("dev-scenarios")
}

extensions.configure<AttacheExtension>("attache") {
    transitive.set(true)
    repository("https://maven.whereareiam.me/release")
    repository("https://maven.whereareiam.me/development")
}

defaultTasks("pluginJars")

tasks.register("pluginJars") {
    group = "build"
    description = "Builds the proxy bootstrap jars and all bundled provider jars."

    dependsOn(
        ":platform-bungeecord-bootstrap:shadowJar",
        ":platform-velocity-bootstrap:shadowJar",
        ":identica-platform:bundle:shadowJar",
        ":provider-credential-runtime:shadowJar",
        ":provider-premium-runtime:shadowJar"
    )
}
