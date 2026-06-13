package me.whereareiam.identica.buildlogic.dev

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.tasks.Jar
import java.util.*

internal class IdenticaDevArtifacts(
	private val project: Project,
	private val identicaVersion: String,
	private val bundleJar: Any,
	private val credentialProviderJar: Any,
	private val premiumProviderJar: Any,
	private val capabilitySeeds: List<CapabilitySeed>
) {
	fun installProxyArtifacts(pluginDir: String, install: (String, Any) -> Unit) {
		install("$pluginDir", bundleJar)
		install("$pluginDir/providers", credentialProviderJar)
		install("$pluginDir/providers", premiumProviderJar)

		for (seed in capabilitySeeds) {
			if (seed.shared) {
				install(
					"$pluginDir/providers/capabilities/.libraries/shared/me/whereareiam/identica/capability/${seed.artifactId}/$identicaVersion",
					seed.source
				)
			}

			for (providerId in seed.providerIds) {
				install(
					"$pluginDir/providers/.libraries/$providerId/me/whereareiam/identica/capability/${seed.artifactId}/$identicaVersion",
					seed.source
				)
			}
		}
	}

	companion object {
		fun create(project: Project): IdenticaDevArtifacts {
			val identicaVersion = project.resolveIdenticaVersion()

			project.evaluationDependsOn(":platform-bungeecord-bootstrap")
			project.evaluationDependsOn(":platform-velocity-bootstrap")
			project.evaluationDependsOn(":identica-platform:bundle")
			project.evaluationDependsOn(":provider-credential-runtime")
			project.evaluationDependsOn(":provider-premium-runtime")
			project.evaluationDependsOn(":capability-recognition")
			project.evaluationDependsOn(":capability-recognition-api")
			project.evaluationDependsOn(":capability-restriction")
			project.evaluationDependsOn(":capability-restriction-api")
			project.evaluationDependsOn(":capability-restriction-join")
			project.evaluationDependsOn(":capability-restriction-join-api")
			project.evaluationDependsOn(":capability-authoritative-username")
			project.evaluationDependsOn(":capability-authoritative-username-api")

			val bundleShadowJar = project.project(":identica-platform:bundle").tasks.named("shadowJar", Jar::class.java)
			val credentialShadowJar = project.project(":provider-credential-runtime").tasks.named("shadowJar", Jar::class.java)
			val premiumShadowJar = project.project(":provider-premium-runtime").tasks.named("shadowJar", Jar::class.java)
			val recognitionShadowJar = project.project(":capability-recognition").tasks.named("shadowJar", AbstractArchiveTask::class.java)
			val recognitionApiJar = project.project(":capability-recognition-api").tasks.named("jar", AbstractArchiveTask::class.java)
			val restrictionShadowJar = project.project(":capability-restriction").tasks.named("shadowJar", AbstractArchiveTask::class.java)
			val restrictionApiJar = project.project(":capability-restriction-api").tasks.named("jar", AbstractArchiveTask::class.java)
			val restrictionJoinShadowJar = project.project(":capability-restriction-join").tasks.named("shadowJar", AbstractArchiveTask::class.java)
			val restrictionJoinApiJar = project.project(":capability-restriction-join-api").tasks.named("jar", AbstractArchiveTask::class.java)
			val authoritativeUsernameShadowJar = project.project(":capability-authoritative-username").tasks.named("shadowJar", AbstractArchiveTask::class.java)
			val authoritativeUsernameApiJar = project.project(":capability-authoritative-username-api").tasks.named("jar", AbstractArchiveTask::class.java)

			return IdenticaDevArtifacts(
				project = project,
				identicaVersion = identicaVersion,
				bundleJar = bundleShadowJar.flatMap { it.archiveFile },
				credentialProviderJar = credentialShadowJar.flatMap { it.archiveFile },
				premiumProviderJar = premiumShadowJar.flatMap { it.archiveFile },
				capabilitySeeds = listOf(
					CapabilitySeed("recognition-api", project.seedArchive("recognition-api", identicaVersion, recognitionApiJar), shared = true),
					CapabilitySeed("restriction-api", project.seedArchive("restriction-api", identicaVersion, restrictionApiJar), shared = true),
					CapabilitySeed("restriction-join-api", project.seedArchive("restriction-join-api", identicaVersion, restrictionJoinApiJar), shared = true),
					CapabilitySeed("authoritative-username-api", project.seedArchive("authoritative-username-api", identicaVersion, authoritativeUsernameApiJar), shared = true),
					CapabilitySeed("recognition", project.seedArchive("recognition", identicaVersion, recognitionShadowJar), providerIds = listOf("credential", "premium")),
					CapabilitySeed("restriction", project.seedArchive("restriction", identicaVersion, restrictionShadowJar), providerIds = listOf("credential", "premium")),
					CapabilitySeed("restriction-join", project.seedArchive("restriction-join", identicaVersion, restrictionJoinShadowJar), providerIds = listOf("credential", "premium")),
					CapabilitySeed("authoritative-username", project.seedArchive("authoritative-username", identicaVersion, authoritativeUsernameShadowJar), providerIds = listOf("premium"))
				)
			)
		}
	}
}

internal fun Project.identicaDevArtifacts(): IdenticaDevArtifacts = IdenticaDevArtifacts.create(this)

private fun Project.resolveIdenticaVersion(): String {
	val toolkitVersioning = rootProject.extensions.findByName("toolkitVersioning")
		?: error("Expected toolkitVersioning extension to be available before dev scenarios are configured.")
	val resolvedVersionProvider = toolkitVersioning.javaClass.methods
		.firstOrNull { method -> method.name == "resolvedVersion" && method.parameterCount == 0 }
		?.invoke(toolkitVersioning) as? Provider<*>
		?: error("Expected toolkitVersioning.resolvedVersion() to return a Provider.")

	return resolvedVersionProvider.get().toString()
}

private fun Project.seedArchive(
	artifactId: String,
	identicaVersion: String,
	archiveTask: Provider<out AbstractArchiveTask>
): FileCollection {
	val seededArchive = layout.buildDirectory.file(
		"dev/capability-seeds/$artifactId/$identicaVersion/$artifactId-$identicaVersion.jar"
	)
	val seedTask = tasks.register("prepare${artifactId.toPascalCase()}DevSeed", Sync::class.java) {
		from(archiveTask.flatMap { task -> task.archiveFile })
		into(layout.buildDirectory.dir("dev/capability-seeds/$artifactId/$identicaVersion"))
		rename { "$artifactId-$identicaVersion.jar" }
	}

	return files(seededArchive).builtBy(seedTask)
}

internal data class CapabilitySeed(
	val artifactId: String,
	val source: Any,
	val shared: Boolean = false,
	val providerIds: List<String> = emptyList()
)

private fun String.toPascalCase(): String = split('-', '_')
	.filter(String::isNotBlank)
	.joinToString("") { part ->
		part.replaceFirstChar { character ->
			if (character.isLowerCase()) character.titlecase(Locale.ROOT) else character.toString()
		}
	}
