plugins {
    base
}

tasks.register("premiumPlatformModules") {
    group = "build"
    description = "Builds all premium provider platform modules."

    dependsOn(":provider-premium-platform-velocity:build")
}
