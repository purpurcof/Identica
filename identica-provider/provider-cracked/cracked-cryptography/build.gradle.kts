plugins {
    base
}

tasks.register("cryptographyModules") {
    group = "build"
    description = "Builds all cracked provider cryptography modules."

    dependsOn(
        ":provider-cracked-cryptography-common:build",
        ":provider-cracked-cryptography-argon2:build",
        ":provider-cracked-cryptography-bcrypt:build"
    )
}
