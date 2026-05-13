plugins {
    alias(libs.plugins.spawner)
    id("identica.dev-scenarios")
}

version = providers.environmentVariable("VERSION").orElse("dev").get()

defaultTasks("pluginJars")

tasks.register("pluginJars") {
    group = "build"
    description = "Builds the Velocity bootstrap jar and all bundled provider jars."

    dependsOn(
        ":platform-velocity-bootstrap:shadowJar",
        ":provider-password-runtime:shadowJar",
        ":provider-premium-runtime:shadowJar"
    )
}
