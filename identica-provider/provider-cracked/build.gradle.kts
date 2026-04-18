plugins {
    base
}

tasks.register("crackedProviderModules") {
    group = "build"
    description = "Builds all cracked provider modules."

    dependsOn(
        ":provider-cracked-api:build",
        ":provider-cracked-common:build",
        ":provider-cracked-cryptography:build",
        ":provider-cracked-database:build",
        ":provider-cracked-runtime:build"
    )
}
