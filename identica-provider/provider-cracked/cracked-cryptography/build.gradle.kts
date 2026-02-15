subprojects {
    dependencies {
        "compileOnly"(project(":identica-provider:provider-cracked:cracked-api"))
        "testImplementation"(project(":identica-provider:provider-cracked:cracked-api"))
    }
}