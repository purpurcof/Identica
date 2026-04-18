plugins {
    base
}

tasks.register("providerModules") {
    group = "build"
    description = "Builds all Identica provider modules."

    dependsOn(
        ":provider-cracked:build",
        ":provider-premium:build"
    )
}
