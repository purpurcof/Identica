plugins {
    id("common")
}

dependencies {
    testImplementation(libs.bungeecord)
    testImplementation(projects.providerPremiumApi)
    
    compileOnly(projects.providerPremiumApi)
    compileOnly(projects.platformBungeecordApi)
    compileOnly(libs.bungeecord)
}
