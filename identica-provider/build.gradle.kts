plugins {
    base
}

tasks.register("providerModules") {
    group = "build"
    description = "Builds all Identica provider modules."

    dependsOn(
        ":provider-credential:build",
        ":provider-premium:build"
    )
}
