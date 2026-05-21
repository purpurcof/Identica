plugins {
    id("common")
}

dependencies {
    compileOnly(projects.identicaApi)
    testImplementation(projects.identicaApi)
}
