dependencies {
    "compileOnly"(project(":identica-provider:provider-cracked:cracked-api"))
    "testImplementation"(project(":identica-provider:provider-cracked:cracked-api"))

    "compileOnly"(rootProject.libs.jdbi.core)
    "compileOnly"(rootProject.libs.jdbi.sqlobject)
    "compileOnly"(rootProject.libs.dialectica)

    "testImplementation"(rootProject.libs.jdbi.core)
    "testImplementation"(rootProject.libs.jdbi.sqlobject)
    "testImplementation"(rootProject.libs.dialectica)
}
