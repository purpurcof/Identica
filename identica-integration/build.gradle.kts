plugins {
    base
}

tasks.register("integrationModules") {
    group = "build"
    description = "Builds all Identica integration modules."

    dependsOn(":integration-bstats:build")
}
