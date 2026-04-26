import org.gradle.api.initialization.resolve.RepositoriesMode

rootProject.name = "Identica"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.whereareiam.me/release")
        maven("https://maven.whereareiam.me/development")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        mavenCentral()
        maven("https://maven.whereareiam.me/release")
        maven("https://maven.whereareiam.me/development")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

include(":identica-api")
project(":identica-api").projectDir = file("identica-api")

include(":identica-common")
project(":identica-common").projectDir = file("identica-common")

include(":identica-engine")
project(":identica-engine").projectDir = file("identica-engine")

include(":identica-adapter-command")
project(":identica-adapter-command").projectDir = file("identica-adapter-command")

include(":identica-adapter-database")
project(":identica-adapter-database").projectDir = file("identica-adapter-database")

include(":identica-adapter-replication")
project(":identica-adapter-replication").projectDir = file("identica-adapter-replication")

include(":identica-integration")
project(":identica-integration").projectDir = file("identica-integration")

include(":integration-bstats")
project(":integration-bstats").projectDir = file("identica-integration/integration-bstats")

include(":identica-platform")
project(":identica-platform").projectDir = file("identica-platform")

include(":platform-velocity")
project(":platform-velocity").projectDir = file("identica-platform/platform-velocity")

include(":platform-velocity-api")
project(":platform-velocity-api").projectDir = file("identica-platform/platform-velocity/api")

include(":platform-velocity-bootstrap")
project(":platform-velocity-bootstrap").projectDir = file("identica-platform/platform-velocity/bootstrap")

include(":identica-provider")
project(":identica-provider").projectDir = file("identica-provider")

include(":provider-cracked")
project(":provider-cracked").projectDir = file("identica-provider/provider-cracked")

include(":provider-cracked-runtime")
project(":provider-cracked-runtime").projectDir = file("identica-provider/provider-cracked/cracked")

include(":provider-cracked-common")
project(":provider-cracked-common").projectDir = file("identica-provider/provider-cracked/cracked-common")

include(":provider-cracked-api")
project(":provider-cracked-api").projectDir = file("identica-provider/provider-cracked/cracked-api")

include(":provider-cracked-cryptography")
project(":provider-cracked-cryptography").projectDir = file("identica-provider/provider-cracked/cracked-cryptography")

include(":provider-cracked-cryptography-common")
project(":provider-cracked-cryptography-common").projectDir =
    file("identica-provider/provider-cracked/cracked-cryptography/common")

include(":provider-cracked-cryptography-argon2")
project(":provider-cracked-cryptography-argon2").projectDir =
    file("identica-provider/provider-cracked/cracked-cryptography/cryptography-argon2")

include(":provider-cracked-cryptography-bcrypt")
project(":provider-cracked-cryptography-bcrypt").projectDir =
    file("identica-provider/provider-cracked/cracked-cryptography/cryptography-bcrypt")

include(":provider-cracked-database")
project(":provider-cracked-database").projectDir = file("identica-provider/provider-cracked/cracked-database")

include(":provider-premium")
project(":provider-premium").projectDir = file("identica-provider/provider-premium")

include(":provider-premium-runtime")
project(":provider-premium-runtime").projectDir = file("identica-provider/provider-premium/premium")

include(":provider-premium-api")
project(":provider-premium-api").projectDir = file("identica-provider/provider-premium/premium-api")

include(":provider-premium-platform")
project(":provider-premium-platform").projectDir = file("identica-provider/provider-premium/premium-platform")

include(":provider-premium-platform-velocity")
project(":provider-premium-platform-velocity").projectDir =
    file("identica-provider/provider-premium/premium-platform/platform-velocity")
