import me.whereareiam.identica.buildlogic.dev.identicaDevArtifacts
import me.whereareiam.identica.buildlogic.dev.readProperty
import me.whereareiam.identica.buildlogic.dev.registerIdenticaDevScenarios

val spawner = extensions.getByName("spawner")
(spawner.readProperty("serverDir") as DirectoryProperty).set(layout.projectDirectory.dir("dev/server"))

val scenarios = spawner.readProperty("scenarios")
private val artifacts = project.identicaDevArtifacts()

registerIdenticaDevScenarios(project, scenarios, artifacts)
