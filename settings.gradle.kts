pluginManagement {
    repositories {
        maven("https://maven.whereareiam.me/release")
        maven("https://maven.whereareiam.me/development")
        gradlePluginPortal()
    }
}

rootProject.name = "Identica"

include("identica-api")
include("identica-common")
include("identica-engine")

include("identica-adapter-command")
include("identica-adapter-database")
include("identica-adapter-replication")

include("identica-platform")
include("identica-platform:platform-velocity:api")
include("identica-platform:platform-velocity:bootstrap")

include("identica-provider")

include("identica-provider:provider-cracked")
include("identica-provider:provider-cracked:cracked")
include("identica-provider:provider-cracked:cracked-common")
include("identica-provider:provider-cracked:cracked-api")
include("identica-provider:provider-cracked:cracked-cryptography")
include("identica-provider:provider-cracked:cracked-cryptography:common")
include("identica-provider:provider-cracked:cracked-cryptography:cryptography-bcrypt")
include("identica-provider:provider-cracked:cracked-cryptography:cryptography-argon2id")
include("identica-provider:provider-cracked:cracked-database")
include("identica-provider:provider-premium")

include("identica-provider:provider-premium:premium")
include("identica-provider:provider-premium:premium-api")
include("identica-provider:provider-premium:premium-platform")
include("identica-provider:provider-premium:premium-platform:platform-velocity")
