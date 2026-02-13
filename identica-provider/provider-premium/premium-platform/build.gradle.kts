subprojects {
    dependencies {
        "compileOnly"(project(":identica-provider:provider-premium:premium-api"))
        "testImplementation"(project(":identica-provider:provider-premium:premium-api"))
    }
}
