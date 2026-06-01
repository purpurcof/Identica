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
        mavenLocal()
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

include(":identica-platform:bundle")
project(":identica-platform:bundle").projectDir = file("identica-platform/bundle")

include(":platform-velocity")
project(":platform-velocity").projectDir = file("identica-platform/platform-velocity")

include(":platform-bungeecord")
project(":platform-bungeecord").projectDir = file("identica-platform/platform-bungeecord")

include(":platform-velocity-api")
project(":platform-velocity-api").projectDir = file("identica-platform/platform-velocity/api")

include(":platform-bungeecord-api")
project(":platform-bungeecord-api").projectDir = file("identica-platform/platform-bungeecord/api")

include(":platform-velocity-bootstrap")
project(":platform-velocity-bootstrap").projectDir = file("identica-platform/platform-velocity/bootstrap")

include(":platform-bungeecord-bootstrap")
project(":platform-bungeecord-bootstrap").projectDir = file("identica-platform/platform-bungeecord/bootstrap")

include(":identica-provider")
project(":identica-provider").projectDir = file("identica-provider")

include(":identica-capability")
project(":identica-capability").projectDir = file("identica-capability")

include(":capability-offline")
project(":capability-offline").projectDir = file("identica-capability/capability-offline/offline")

include(":capability-offline-api")
project(":capability-offline-api").projectDir = file("identica-capability/capability-offline/offline-api")

include(":capability-offline-common")
project(":capability-offline-common").projectDir = file("identica-capability/capability-offline/offline-common")

include(":capability-online")
project(":capability-online").projectDir = file("identica-capability/capability-online/online")

include(":capability-online-api")
project(":capability-online-api").projectDir = file("identica-capability/capability-online/online-api")

include(":capability-online-common")
project(":capability-online-common").projectDir = file("identica-capability/capability-online/online-common")

include(":capability-migration")
project(":capability-migration").projectDir = file("identica-capability/capability-migration/migration")

include(":capability-migration-api")
project(":capability-migration-api").projectDir = file("identica-capability/capability-migration/migration-api")

include(":capability-migration-common")
project(":capability-migration-common").projectDir = file("identica-capability/capability-migration/migration-common")

include(":capability-verification")
project(":capability-verification").projectDir = file("identica-capability/capability-verification/verification")

include(":capability-verification-api")
project(":capability-verification-api").projectDir = file("identica-capability/capability-verification/verification-api")

include(":capability-verification-common")
project(":capability-verification-common").projectDir = file("identica-capability/capability-verification/verification-common")

include(":capability-authoritative-username")
project(":capability-authoritative-username").projectDir =
    file("identica-capability/capability-authoritative-username/authoritative-username")

include(":capability-authoritative-username-api")
project(":capability-authoritative-username-api").projectDir =
    file("identica-capability/capability-authoritative-username/authoritative-username-api")

include(":capability-authoritative-username-common")
project(":capability-authoritative-username-common").projectDir =
    file("identica-capability/capability-authoritative-username/authoritative-username-common")

include(":capability-recognition")
project(":capability-recognition").projectDir = file("identica-capability/capability-recognition/recognition")

include(":capability-recognition-api")
project(":capability-recognition-api").projectDir = file("identica-capability/capability-recognition/recognition-api")

include(":capability-recognition-common")
project(":capability-recognition-common").projectDir = file("identica-capability/capability-recognition/recognition-common")

include(":provider-credential")
project(":provider-credential").projectDir = file("identica-provider/provider-credential")

include(":provider-credential-runtime")
project(":provider-credential-runtime").projectDir = file("identica-provider/provider-credential/credential")

include(":provider-credential-common")
project(":provider-credential-common").projectDir = file("identica-provider/provider-credential/credential-common")

include(":provider-credential-api")
project(":provider-credential-api").projectDir = file("identica-provider/provider-credential/credential-api")

include(":provider-credential-cryptography")
project(":provider-credential-cryptography").projectDir = file("identica-provider/provider-credential/credential-cryptography")

include(":provider-credential-cryptography-common")
project(":provider-credential-cryptography-common").projectDir =
    file("identica-provider/provider-credential/credential-cryptography/common")

include(":provider-credential-cryptography-argon2")
project(":provider-credential-cryptography-argon2").projectDir =
    file("identica-provider/provider-credential/credential-cryptography/cryptography-argon2")

include(":provider-credential-cryptography-bcrypt")
project(":provider-credential-cryptography-bcrypt").projectDir =
    file("identica-provider/provider-credential/credential-cryptography/cryptography-bcrypt")

include(":provider-credential-database")
project(":provider-credential-database").projectDir = file("identica-provider/provider-credential/credential-database")

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

include(":provider-premium-platform-bungeecord")
project(":provider-premium-platform-bungeecord").projectDir =
    file("identica-provider/provider-premium/premium-platform/platform-bungeecord")
