plugins {
    id("identica.shadow-runtime")
}

dependencies {
    add("implementation", project(":identica-api"))
    add("implementation", project(":identica-common"))
    add("implementation", project(":identica-engine"))
    add("implementation", project(":identica-adapter-command"))
    add("implementation", project(":identica-adapter-database"))
    add("implementation", project(":identica-adapter-replication"))
}
