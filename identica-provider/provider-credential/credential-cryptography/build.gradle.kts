plugins {
    base
}

tasks.register("cryptographyModules") {
    group = "build"
    description = "Builds all credential provider cryptography modules."

    dependsOn(
        ":provider-credential-cryptography-common:build",
        ":provider-credential-cryptography-argon2:build",
        ":provider-credential-cryptography-bcrypt:build"
    )
}
