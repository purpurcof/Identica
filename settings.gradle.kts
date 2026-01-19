rootProject.name = "Identica"

includeBuild("../Spawner")

include("identica-api")
include("identica-common")

include("identica-adapter-command")
include("identica-adapter-database")
include("identica-adapter-synchronization")

include("identica-platform")
include("identica-platform:platform-velocity")

include("identica-provider")
include("identica-provider:provider-cracked")
include("identica-provider:provider-premium")
