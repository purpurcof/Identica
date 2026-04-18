plugins {
    base
}

tasks.register("premiumProviderModules") {
    group = "build"
    description = "Builds all premium provider modules."

    dependsOn(
        ":provider-premium-api:build",
        ":provider-premium-platform:build",
        ":provider-premium-runtime:build"
    )
}
