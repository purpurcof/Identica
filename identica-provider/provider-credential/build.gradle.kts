plugins {
    base
}

tasks.register("credentialProviderModules") {
    group = "build"
    description = "Builds all credential provider modules."

    dependsOn(
        ":provider-credential-api:build",
        ":provider-credential-common:build",
        ":provider-credential-cryptography:build",
        ":provider-credential-database:build",
        ":provider-credential-runtime:build"
    )
}
