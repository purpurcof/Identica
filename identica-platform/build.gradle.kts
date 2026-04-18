plugins {
    base
}

tasks.register("platformModules") {
    group = "build"
    description = "Builds all Identica platform modules."

    dependsOn(":platform-velocity:build")
}
